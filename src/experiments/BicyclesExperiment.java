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
import agents.network.RandomRankGenerator;
import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.DateTime;
import protopeer.measurement.LogReplayer;
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
    
    private MeasurementLog log;

    private static int currentConfig = -1;
    private static BicyclesExperiment launcher;

    public static void main(String[] args) throws FileNotFoundException {
        long t0 = System.currentTimeMillis();
        new File("output-data").mkdir();

        IEPOSEvaluator evaluator = new MatlabEvaluator();
        
        launcher = new BicyclesExperiment();
        launcher.numExperiments = 1;
        launcher.numIterations = 20;
        launcher.runDuration = 4+launcher.numIterations;
        launcher.numUser = 99999;
        launcher.architecture = new TreeArchitecture();
        launcher.architecture.priority = RankPriority.HIGH_RANK;
        launcher.architecture.rank = DescriptorType.RANK;
        launcher.architecture.type = TreeType.SORTED_HtL;

        class Dim<T> {

            Function<T, Object> func;
            Iterable<? extends T> iterable;

            public Dim(Function<T, Object> func, Iterable<? extends T> iterable) {
                this.func = func;
                this.iterable = iterable;
            }
        }

        List<Dim> outer = new ArrayList<>();
        List<Dim> inner = new ArrayList<>();

        Map<String,BalanceType> treeBalance = new HashMap<>();
        treeBalance.put("BALANCED", BalanceType.WEIGHT_BALANCED);
        treeBalance.put("LIST", BalanceType.LIST);
        inner.add(new Dim<>(o -> launcher.architecture.balance = treeBalance.get(o), Arrays.asList(
                "BALANCED"
                //"LIST"
        )));
        outer.add(new Dim<>(o -> launcher.architecture.rankGenerator = o, Arrays.asList(
                new RandomRankGenerator()
                //new IndexRankGenerator()
        )));
        inner.add(new Dim<>(o -> launcher.architecture.maxChildren = o, Arrays.asList(
                2
        )));
        
        outer.add(new Dim<>(s -> {
            if(s.startsWith("E")) {
                return launcher.dataset = "input-data/Archive/" + s.charAt(s.length()-3) + "." + s.charAt(s.length()-1);
            } else {
                int num = Integer.parseInt(s.substring(s.indexOf('_')+1));
                return launcher.dataset = "input-data/bicycle/user_plans_unique_" + num + "to" + (num + 2) + "_force_trips";
            }
        }, Arrays.asList(
            "E_5_1"//, "B_8"
        )));

        outer.add(new Dim<>(o -> launcher.agentFactory = o, Arrays.asList(
                new IEPOSAgent.Factory(false)
        //new IGreedyAgent.Factory(false)
        //new OPTAgent.Factory()
        )));

        Map<Integer, List<IterativeFitnessFunction>> ffConfigs = new HashMap<>();
        outer.add(new Dim<>(o -> currentConfig = o, Arrays.asList(
                2
        )));
        ffConfigs.put(0, Arrays.asList(
                new IterMinVarGmA(new Factor1OverN(), new SumCombinator()),
                new IterMinVarGmA(new Factor1OverLayer(), new SumCombinator()),
                new IterMinVarGmA(new FactorMOverN(), new SumCombinator()),
                new IterMinVarGmA(new FactorMOverNmM(), new SumCombinator()),
                new IterMinVarGmA(new FactorDepthOverN(), new SumCombinator()),
                new IterMinVarGmA(new FactorNormalizeStd(), new SumCombinator()),
                new IterMinVarGmA(new Factor1(), new SumCombinator())
        ));
        ffConfigs.put(1, Arrays.asList(
                new IterLocalSearch(),
                new IterMinVarGmA(new FactorMOverNmM(), new SumCombinator()),
                new IterMinVarGmA(new Factor1(), new SumCombinator()),
                new IterMaxMatchGmA(new FactorMOverNmM(), new SumCombinator()),
                new IterMaxMatchGmA(new Factor1(), new SumCombinator()),
                new IterProbGmA(new Factor1(), new SumCombinator())
        ));
        ffConfigs.put(2, Arrays.asList(
                new IterMinVarGmA(new FactorMOverNmM(), new SumCombinator())
                //new IterMinVarGmA(new Factor1(), new SumCombinator())
        ));
        ffConfigs.put(3, Arrays.asList(
                new IterMinVarGmA(new FactorMOverNmM(), new SumCombinator()),
                new IterMinVarGmA(new FactorMOverNmM(), new WeightedSumCombinator2(0.0)),
                new IterMinVarGmA(new FactorMOverNmM(), new WeightedSumCombinator2(0.1)),
                new IterMinVarGmA(new FactorMOverNmM(), new WeightedSumCombinator2(0.2)),
                new IterMinVarGmA(new FactorMOverNmM(), new WeightedSumCombinator2(0.3)),
                new IterMinVarGmA(new FactorMOverNmM(), new WeightedSumCombinator2(0.4)),
                new IterMinVarGmA(new FactorMOverNmM(), new WeightedSumCombinator3()),
                new IterMaxMatchGmA(new FactorMOverNmM(), new SumCombinator()),
                new IterProbGmA(new Factor1(), new SumCombinator())
        ));
        
        inner.add(new Dim<>(o -> launcher.agentFactory.localSearch = o, Arrays.asList(
                (LocalSearch) null
        //new LocalSearch(),
        )));
        
        inner.add(new Dim<>(o -> launcher.agentFactory.fitnessFunction = o, () -> ffConfigs.get(currentConfig).iterator()));
        
        try (PrintStream out = System.out) {//new PrintStream("output-data/E"+System.currentTimeMillis()+".m")) {//
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
                    outer.get(i).func.apply(obj);
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
                        inner.get(i).func.apply(obj);
                        innerState.set(i, innerState.get(i));
                        innerName.set(i, obj == null ? null : obj.toString());
                    }
                    if (eoi) { // all dimensions explored (iterator at the end)
                        break;
                    }

                    // perform experiment
                    launcher.peersLog = "peersLog/Experiment " + System.currentTimeMillis();
                    launcher.title = title;
                    launcher.label = Util.merge(innerName);
                    launcher.run();
                    launcher.evaluateRun();
                    
                    experiments.add(launcher.peersLog);
                    
                    launcher.log = null;
                }

                // plot result
                evaluator.evaluateLogs(plotNumber, experiments, out);
            }
            
            long t1 = System.currentTimeMillis();
            System.out.println("%" + (t1 - t0) / 1000 + "s");
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
                location, outFolder, config, null,
                architecture,
                "3BR" + num, DateTime.parse("0001-01-01"), 
                DateTime.parse("0001-01-01"), 5, numUser,
                agentFactory);
        
        return experiment;
    }
    
    public void evaluateRun() {
        try {
            LogReplayer replayer = new LogReplayer();
            log = new MeasurementLog();
            for(File f : new File(peersLog).listFiles()) {
                log.mergeWith(replayer.loadLogFromFile(f.getPath()));
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(BicyclesExperiment.class.getName()).log(Level.SEVERE, null, ex);
        }
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
}
