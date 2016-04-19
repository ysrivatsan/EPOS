/*
 * Copyright (C) 2015 Evangelos Pournaras
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package experiments;

import agents.network.TreeArchitecture;
import agents.fitnessFunction.iterative.*;
import agents.*;
import agents.fitnessFunction.*;
import agents.fitnessFunction.costFunction.CostFunction;
import agents.fitnessFunction.costFunction.DirectionCostFunction;
import agents.fitnessFunction.costFunction.EntropyCostFunction;
import agents.fitnessFunction.costFunction.MatchEstimateCostFunction;
import agents.fitnessFunction.costFunction.QuadraticCostFunction;
import agents.fitnessFunction.costFunction.RelStdDevCostFunction;
import agents.fitnessFunction.costFunction.StdDevCostFunction;
import agents.network.NumPlanRankGenerator;
import agents.network.StdRankGenerator;
import agents.plan.FilePlanGenerator;
import agents.plan.FuncPlanGenerator;
import agents.plan.PlanGenerator;
import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.DateTime;
import protopeer.measurement.MeasurementFileDumper;
import protopeer.measurement.MeasurementLog;
import tree.BalanceType;
import util.Util;

/**
 * @author Peter
 */
public class BicyclesExperiment extends ExperimentLauncher implements Cloneable, Runnable {
    private String peersLog;

    private AgentFactory agentFactory;
    private String dataset;
    private int numUser;
    private int numIterations;
    private String title;
    private String label;
    private TreeArchitecture architecture;
    private PlanGenerator planGenerator;

    private static String currentConfig = null;
    private static BicyclesExperiment launcher;
    private static String outFile = null;
    
    private static Map<String,Consumer<AgentFactory>> agentFactoryProperties = new HashMap<>();
    
    private static final Map<String,CostFunction> costFuncs = new HashMap<>();
    static {
        costFuncs.put("std", new StdDevCostFunction());
        costFuncs.put("dot", new DirectionCostFunction());
        costFuncs.put("match", new MatchEstimateCostFunction());
        costFuncs.put("rand", new QuadraticCostFunction());
        costFuncs.put("relStd", new RelStdDevCostFunction());
        costFuncs.put("entropy", new EntropyCostFunction());
    }
    
    public static void main(String[] args) {
        long t0 = System.currentTimeMillis();
        new File("output-data").mkdir();
        String configFile = "experiments/default.cfg";
        
        if(args.length > 0) {
            configFile = args[0];
        }
        File peersLogDir = new File(configFile);
        String peersLog = "peersLog/" + peersLogDir.getName().substring(0, peersLogDir.getName().indexOf('.'));
        peersLogDir = new File(peersLog);
        peersLogDir.mkdirs();
        Util.clearDirectory(peersLogDir);

        IEPOSEvaluator evaluator = new MatlabEvaluator();
        
        Properties p = new Properties();
        try(Reader configReader = new FileReader(configFile)) {
            p.load(configReader);
        } catch (IOException ex) {
            Logger.getLogger(BicyclesExperiment.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        launcher = new BicyclesExperiment();
        launcher.architecture = new TreeArchitecture();
        
        Map<String, Consumer<String>> assignments = new HashMap<>();
        assignments.put("out", (x) -> outFile = x);
        assignments.put("numExperiments", (x) -> launcher.numExperiments = Integer.parseInt(x));
        assignments.put("numIterations", (x) -> launcher.numIterations = Integer.parseInt(x));
        assignments.put("numUser", (x) -> launcher.numUser = Integer.parseInt(x));
        assignments.put("architecture.priority", (x) -> launcher.architecture.priority = RankPriority.valueOf(x));
        assignments.put("architecture.rank", (x) -> launcher.architecture.rank = DescriptorType.valueOf(x));
        assignments.put("architecture.type", (x) -> launcher.architecture.type = TreeType.valueOf(x));
        assignments.put("architecture.balance", (x) -> launcher.architecture.balance = BalanceType.valueOf(x));
        
        Map<String, BiFunction<Integer,Agent,Double>> rankGenerators = new HashMap<>();
        rankGenerators.put("RandomRank", (idx, agent) -> Math.random());
        rankGenerators.put("IndexRank", (idx, agent) -> (double)idx);
        rankGenerators.put("StdRank", new StdRankGenerator());
        rankGenerators.put("NumPlanRank", new NumPlanRankGenerator());
        assignments.put("architecture.rankGenerator", (x) -> {
            launcher.architecture.rankGenerator = rankGenerators.get(x);
            if(!rankGenerators.containsKey(x)) {
                System.err.println(x + " not a valid rank generator; valid: " + rankGenerators.keySet().toString());
            }
        });
        assignments.put("architecture.maxChildren", (x) -> launcher.architecture.maxChildren = Integer.parseInt(x));
        
        // e.g. E5.1 for energy dataset 5.1 or B8 for bicycle dataset 8to10
        assignments.put("dataset", (x) -> {
            if(x.startsWith("E")) {
                launcher.dataset = "input-data/Archive/" + x.charAt(x.length()-3) + "." + x.charAt(x.length()-1);
            } else {
                int num = Integer.parseInt(x.substring(1));
                launcher.dataset = "input-data/bicycle/user_plans_unique_" + num + "to" + (num + 2) + "_force_trips";
            }
        });
        
        Map<String, PlanGenerator> planGenerators = new HashMap<>();
        planGenerators.put("zero", new FuncPlanGenerator((x) -> 0.0));
        planGenerators.put("one", new FuncPlanGenerator((x) -> 1.0));
        planGenerators.put("sin", new FuncPlanGenerator((x) -> 10*Math.sin(x*2*Math.PI)));
        assignments.put("costSignal", (x) -> {
            if(planGenerators.containsKey(x)) {
                launcher.planGenerator = planGenerators.get(x);
            } else {
                launcher.planGenerator = new FilePlanGenerator(x);
            }
        });
        
        Map<String, AgentFactory> agentFactories = new HashMap<>();
        agentFactories.put("IEPOS", new IEPOSAgent.Factory());
        agentFactories.put("IGreedy", new IGreedyAgent.Factory());
        agentFactories.put("Opt", new OPTAgent.Factory());
        assignments.put("agentFactory", (String x) -> {
            launcher.agentFactory = agentFactories.get(x);
            if(!agentFactories.containsKey(x)) {
                System.err.println(x + " not a valid agentFactory; valid: " + agentFactories.keySet().toString());
            }
        });
        assignments.put("outputMovie", (String x) -> {    
            agentFactoryProperties.put("outputMovie", (a) -> {
                try {
                    a.getClass().getDeclaredField("outputMovie").setBoolean(launcher.agentFactory, Boolean.parseBoolean(x));
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                    System.err.println(x + " not a valid boolean; valid: [true, false]");
                }
            });
        });
        assignments.put("fitnessFunction", (x) -> currentConfig = x);
        assignments.put("measure", (x) -> {
            agentFactoryProperties.put("measures", (a) -> {
                a.measures.clear();
                if(!x.isEmpty()) {
                    a.measures.add(costFuncs.get(x));
                }
            });
        });
        
        Map<String, LocalSearch> localSearches = new HashMap<>();
        localSearches.put("", null);
        localSearches.put("LS", new LocalSearch());
        assignments.put("localSearch", (x) -> {
            agentFactoryProperties.put("localSearch", (a) -> a.localSearch = localSearches.get(x));
            //launcher.agentFactory.localSearch = localSearches.get(x);
            if(!localSearches.containsKey(x)) {
                System.err.println(x + " not a valid local search strategy; valid: " + localSearches.keySet().toString());
            }
        });
        
        class Dim<T> {

            Consumer<T> func;
            Iterable<? extends T> iterable;

            public Dim(Consumer<T> func, Iterable<? extends T> iterable) {
                this.func = func;
                this.iterable = iterable;
            }
        }

        List<Dim> init = new ArrayList<>();
        List<Dim> outer = new ArrayList<>();
        List<Dim> inner = new ArrayList<>();
        Map<String, List<IterativeFitnessFunction>> ffConfigs = new HashMap<>();
        
        for(Map.Entry assignment : p.entrySet()) {
            String var = (String) assignment.getKey();
            String propertyName = var.substring(var.indexOf('-')+1);
            List<Dim> target;
            if(var.startsWith("init")){
                target = init;
            } else if(var.startsWith("plot")) {
                target = outer;
            } else if(var.startsWith("comp")) {
                target = inner;
            } else if(var.startsWith("list")) {
                ffConfigs.put(propertyName, Arrays.asList(
                        Arrays.stream(((String)assignment.getValue()).split("\\),"))
                                .map(s -> FFfromString(s))
                                .toArray(num -> new IterativeFitnessFunction[num])));
                continue;
            } else {
                System.err.println("Invalid prefix for " + var);
                continue;
            }
            
            if(assignments.containsKey(propertyName)) {
                target.add(new Dim<>(assignments.get(propertyName), Util.trimSplit((String)assignment.getValue(),",")));
            } else {
                System.err.println("Property " + var + " not supported");
            }
        }
        inner.add(new Dim<>((o) -> {
            agentFactoryProperties.put("fitnessFunction", (a) -> a.fitnessFunction = o);
        }, () -> ffConfigs.get(currentConfig).iterator()));
        
        for(Dim d : init) {
            d.func.accept(d.iterable.iterator().next());
        }
        
        try (PrintStream out = outFile == null ? System.out : new PrintStream(outFile)) {
            int plotNumber = 0;
            
            // outer loops
            List<Iterator<? extends Object>> outerState = Util.repeat(outer.size(), (Iterator<? extends Object>) null);
            List<String> outerName = Util.repeat(outer.size(), (String) null);
            while (true) {
                boolean boi = true, eoi = false; // begin/end of iteration
                for (int i = 0; i < outer.size() && (boi || eoi); i++) {
                    if ((boi = outerState.get(i) == null) || (eoi = !outerState.get(i).hasNext())) {
                        outerState.set(i, outer.get(i).iterable.iterator());
                    }
                    Object obj = outerState.get(i).next();
                    outer.get(i).func.accept(obj);
                    outerState.set(i, outerState.get(i));
                    outerName.set(i, obj == null ? null : obj.toString());
                }
                if (eoi) { // all dimensions explored (iterator at the end)
                    break;
                }

                // init plot
                List<String> experiments = new ArrayList<>();
                String title = Util.merge(outerName);
                plotNumber++;
                
                // inner loops
                List<Iterator<? extends Object>> innerState = Util.repeat(inner.size(), (Iterator<? extends Object>) null);
                List<String> innerName = Util.repeat(inner.size(), (String) null);
                while (true) {
                    boi = true; eoi = false; // begin/end of iteration
                    for (int i = 0; i < inner.size() && (boi || eoi); i++) {
                        if ((boi = innerState.get(i) == null) || (eoi = !innerState.get(i).hasNext())) {
                            innerState.set(i, inner.get(i).iterable.iterator());
                        }
                        Object obj = innerState.get(i).next();
                        inner.get(i).func.accept(obj);
                        innerState.set(i, innerState.get(i));
                        innerName.set(i, obj == null ? null : obj.toString());
                    }
                    if (eoi) { // all dimensions explored (iterator at the end)
                        break;
                    }

                    // perform experiment
                    for(Consumer<AgentFactory> c : agentFactoryProperties.values()) {
                        c.accept(launcher.agentFactory);
                    }
                    launcher.runDuration = 4+launcher.numIterations;
                    launcher.peersLog = peersLog + "/Experiment " + System.currentTimeMillis();
                    launcher.title = title;
                    launcher.label = Util.merge(innerName);
                    launcher.run();
                    
                    experiments.add(launcher.peersLog);
                }

                // plot result
                evaluator.evaluateLogs(plotNumber, experiments, out);
            }
            
            long t1 = System.currentTimeMillis();
            System.out.println("%" + (t1 - t0) / 1000 + "s");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BicyclesExperiment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void run() {
        File peersLog = new File(this.peersLog);
        Util.clearDirectory(peersLog);
        peersLog.mkdir();
        
        MeasurementFileDumper logger = new MeasurementFileDumper(this.peersLog + "/info");
        MeasurementLog log = new MeasurementLog();
        log.log(1, "title=" + title, 0);
        log.log(1, "label=" + label, 0);
        log.log(1, "measure=" + launcher.agentFactory.fitnessFunction.getRobustnessMeasure(), 0);
        logger.measurementEpochEnded(log, 2);
        
        super.run();
    }

    @Override
    public IEPOSExperiment createExperiment(int num) {
        System.out.println("%Experiment " + getName(num) + ":");

        String location = dataset.substring(0, dataset.lastIndexOf('/'));
        String config = dataset.substring(dataset.lastIndexOf('/') + 1);
        
        agentFactory.numIterations = numIterations;

        File outFolder = new File(peersLog);
        IEPOSExperiment experiment = new IEPOSExperiment(
                location, outFolder, config,
                architecture,
                "3BR" + num, DateTime.parse("0001-01-01"), 
                DateTime.parse("0001-01-01"), 5, numUser,
                agentFactory,
                planGenerator);
        
        return experiment;
    }

    private String getName(int num) {
        return title + " - " + label + " - " + num;
    }
    
    @Override
    public BicyclesExperiment clone() {
        try {
            BicyclesExperiment clone = (BicyclesExperiment) super.clone();
            clone.agentFactory = agentFactory.clone();
            clone.architecture = architecture.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(BicyclesExperiment.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    private static IterativeFitnessFunction FFfromString(String s) {
        String[] parts = s.split("[\\(,\\)]");
        IterativeFitnessFunction ff = null;
        
        for(int i=0; i<parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        
        try {
            Map<String,Constructor> ffs = new HashMap<>();
            ffs.put("MinCostGmA", IterMinCostGmA.class.getConstructor(CostFunction.class, Factor.class, PlanCombinator.class));
            ffs.put("MinCostG", IterMinCostG.class.getConstructor(CostFunction.class, Factor.class, PlanCombinator.class));
            ffs.put("MinCostHGmA", IterMinCostHGmA.class.getConstructor(CostFunction.class, Factor.class, Factor.class, PlanCombinator.class, PlanCombinator.class));
            ffs.put("MaxMatchGmA", IterMaxMatchGmA.class.getConstructor(Factor.class, PlanCombinator.class));
            ffs.put("LocalSearch", IterLocalSearch.class.getConstructor());
            ffs.put("ProbGmA", IterProbGmA.class.getConstructor(Factor.class, PlanCombinator.class));
            ffs.put("UCB1", IterUCB1Bandit.class.getConstructor());

            if(!ffs.containsKey(parts[0])) {
                System.err.println(parts[0] + " is not a valid fitness function; valid: " + ffs.keySet());
            }
            
            Constructor ffConst = ffs.get(parts[0]);
            if(ffConst.getParameterCount() > parts.length-1) {
                System.err.println("Too few parameters for fitness function " + parts[0] + " (" + ffConst.getParameterCount() + " expected)");
            }

            Map<Class,Map<String,?>> params = new HashMap<>();
            params.put(CostFunction.class, costFuncs);
            
            Map<String,Factor> factors = new HashMap<>();
            factors.put("1", new Factor1());
            factors.put("1/l", new Factor1OverLayer());
            factors.put("1/n", new Factor1OverN());
            factors.put("1/sqrtn", new Factor1OverSqrtN());
            factors.put("d/n", new FactorDepthOverN());
            factors.put("m/n", new FactorMOverN());
            factors.put("m/n-m", new FactorMOverNmM());
            factors.put("std", new FactorNormalizeStd());
            params.put(Factor.class, factors);
            
            Map<String,PlanCombinator> combinators = new HashMap<>();
            combinators.put("sum", new SumCombinator());
            combinators.put("avg", new AvgCombinator());
            combinators.put("prev", new MostRecentCombinator());
            combinators.put("wsum", new WeightedSumCombinator2());
            params.put(PlanCombinator.class, combinators);
            
            Object[] args = new Object[ffConst.getParameterCount()];
            Class[] types = ffConst.getParameterTypes();
            for(int i=0; i<args.length; i++) {
                Map<String,? extends Object> options = params.get(types[i]);
                if(!options.containsKey(parts[i+1])) {
                    System.err.println(parts[i+1] + " is not a valid "+types[i].getSimpleName()+"; valid: " + options.keySet());
                }
                args[i] = options.get(parts[i+1]);
            }
            
            ff = (IterativeFitnessFunction) ffConst.newInstance(args);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(BicyclesExperiment.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return ff;
    }
}
