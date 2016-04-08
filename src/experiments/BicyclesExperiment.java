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

import agents.fitnessFunction.iterative.*;
import agents.*;
import agents.fitnessFunction.*;
import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.joda.time.DateTime;
import protopeer.Experiment;
import protopeer.measurement.MeasurementLog;

/**
 * @author Peter
 */
public class BicyclesExperiment extends ExperimentLauncher {

    private AgentFactory agentFactory;
    private String dataset;
    private int numUser;
    private int numChildren;
    private int numIterations;
    private String name;

    private static int currentConfig = -1;

    public static void main(String[] args) throws FileNotFoundException {
        long t0 = System.currentTimeMillis();
        new File("output-data").mkdir();

        BicyclesExperiment launcher = new BicyclesExperiment();
        launcher.numExperiments = 20;
        launcher.runDuration = 4;
        launcher.numIterations = 100;
        launcher.numUser = 99999;

        class Dim {

            Function<Object, Object> func;
            Iterable<? extends Object> iterable;

            public Dim(Function<Object, Object> func, Iterable<? extends Object> iterable) {
                this.func = func;
                this.iterable = iterable;
            }
        }

        List<Dim> outer = new ArrayList<>();
        List<Dim> inner = new ArrayList<>();

        outer.add(new Dim(o -> launcher.dataset = (String) o, Arrays.asList(Stream.concat(Arrays.stream(new String[]{
            "5.1"//, "5.3"
        }).map(s -> "input-data/Archive/" + s), Arrays.stream(new int[]{
            //8
        }).mapToObj(t -> "input-data/bicycle/user_plans_unique_" + t + "to" + (t + 2) + "_force_trips")).toArray(n -> new String[n]))));

        outer.add(new Dim(o -> launcher.numChildren = (Integer) o, Arrays.asList(
                2
        )));

        outer.add(new Dim(o -> launcher.agentFactory = (AgentFactory) o, Arrays.asList(
                new IEPOSAgent.Factory(false)
        //new IGreedyAgent.Factory(false)
        //new OPTAgent.Factory()
        )));

        Map<Integer, List<IterativeFitnessFunction>> ffConfigs = new HashMap<>();
        outer.add(new Dim(o -> currentConfig = (Integer) o, Arrays.asList(2)));
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
                new IterMinVarGmA(new FactorMOverNmM(), new SumCombinator()),
                new IterMinVarGmA(new Factor1(), new SumCombinator())
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
        
        inner.add(new Dim(o -> launcher.agentFactory.localSearch = (LocalSearch) o, Arrays.asList(
                (Object) null
        //new LocalSearch(),
        )));
        
        inner.add(new Dim(o -> launcher.agentFactory.fitnessFunction = (IterativeFitnessFunction) o, new Iterable<IterativeFitnessFunction>() {
            @Override
            public Iterator<IterativeFitnessFunction> iterator() {
                return ffConfigs.get(currentConfig).iterator();
            }
        }));

        try (PrintStream out = System.out) {//new PrintStream("output-data/E"+System.currentTimeMillis()+".m")) {//
            int plotNumber = 0;
            
            // outer loops
            List<Iterator<? extends Object>> outerState = repeat(outer.size(), (Iterator<? extends Object>) null);
            List<String> outerName = repeat(outer.size(), (String) null);
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
                List<String> names = new ArrayList<>();
                List<MeasurementLog> logs = new ArrayList<>();
                plotNumber++;

                // inner loops
                List<Iterator<? extends Object>> innerState = repeat(inner.size(), (Iterator<? extends Object>) null);
                List<String> innerName = repeat(inner.size(), (String) null);
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
                    launcher.agentFactory.log = new MeasurementLog();
                    launcher.name = merge(outerName) + " - " + merge(innerName);
                    launcher.run();
                    names.add(merge(innerName));
                    logs.add(launcher.agentFactory.log); // set in method evaluateRun
                }

                // plot result
                IEPOSEvaluator.evaluateLogs(plotNumber, merge(outerName), names, launcher.agentFactory.fitnessFunction.getRobustnessMeasure(), logs, out);

                long t1 = System.currentTimeMillis();
                System.out.println("%" + (t1 - t0) / 1000 + "s");
            }
        }
    }

    @Override
    public EPOSExperiment createExperiment(int num) {
        System.out.print("%Experiment " + getName(num) + ": ");

        String location = dataset.substring(0, dataset.lastIndexOf('/'));
        String config = dataset.substring(dataset.lastIndexOf('/') + 1);
        
        agentFactory.numIterations = numIterations;

        EPOSExperiment experiment = new EPOSExperiment(getName(num),
                RankPriority.HIGH_RANK, DescriptorType.RANK, TreeType.SORTED_HtL,
                location, config, null,
                "3BR" + num, DateTime.parse("0001-01-01"), 
                DateTime.parse("0001-01-01"), 5, numChildren + 1, numUser,
                agentFactory);
        return experiment;
    }

    private String getName(int num) {
        return name + " - " + num;
    }

    private static <T> List<T> repeat(int times, T item) {
        List<T> list = new ArrayList<>(times);
        for (int i = 0; i < times; i++) {
            list.add(item);
        }
        return list;
    }

    private static String merge(List<String> list) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iter = list.iterator();
        String s = null;
        while (iter.hasNext() && (s = iter.next()) == null) {
        }
        if (s != null) {
            sb.append(s);
        }
        while (iter.hasNext()) {
            s = iter.next();
            if (s != null) {
                sb.append(' ');
                sb.append(s);
            }
        }
        return sb.toString();
    }
}
