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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import protopeer.Experiment;
import protopeer.measurement.MeasurementLog;

/**
 * @author Peter
 */
public class BicyclesExperiment extends ExperimentLauncher {

    private AgentFactory agentFactory;
    private FitnessFunction fitnessFunction;
    private String location;
    private String dataset;
    private int numUser;
    private int numChildren;
    private int numIterations;
    private LocalSearch localSearch;

    private static MeasurementLog log = null;

    public static void main(String[] args) throws FileNotFoundException {
        long t0 = System.currentTimeMillis();
        new File("output-data").mkdir();

        BicyclesExperiment launcher = new BicyclesExperiment();
        launcher.numExperiments = 20;
        launcher.runDuration = 4;
        launcher.numIterations = 200;
        launcher.numUser = 99999;

        String[] datasets = null;
        switch (0) {
            case 0:
                launcher.location = "input-data/bicycle";
                datasets = Arrays.stream(new int[]{8}).mapToObj(t -> "user_plans_unique_" + t + "to" + (t + 2) + "_force_trips").toArray(n -> new String[n]);
                break;
            case 1:
                launcher.location = "input-data/Archive";
                datasets = new String[]{"5.1", "5.3"};
                break;
        }

        int[] numChildren = new int[]{2};
        LocalSearch[] localSearch = new LocalSearch[]{null};

        List<List<FitnessFunction>> ffConfigs = new ArrayList<>();
        List<FitnessFunction> f = new ArrayList<>();
        f.add(new IterMinVarGmA(new Factor1OverN(), new SumCombinator()));
        f.add(new IterMinVarGmA(new Factor1OverLayer(), new SumCombinator()));
        f.add(new IterMinVarGmA(new FactorMOverN(), new SumCombinator()));
        f.add(new IterMinVarGmA(new FactorMOverNmM(), new SumCombinator()));
        f.add(new IterMinVarGmA(new FactorDepthOverN(), new SumCombinator()));
        f.add(new IterMinVarGmA(new FactorNormalizeStd(), new SumCombinator()));
        f.add(new IterMinVarGmA(new Factor1(), new SumCombinator()));
        ffConfigs.add(f);
        f = new ArrayList<>();
        f.add(new IterLocalSearch());
        f.add(new IterMinVarGmA(new FactorMOverNmM(), new SumCombinator()));
        f.add(new IterMinVarGmA(new Factor1(), new SumCombinator()));
        f.add(new IterMaxMatchGmA(new FactorMOverNmM(), new SumCombinator()));
        f.add(new IterMaxMatchGmA(new Factor1(), new SumCombinator()));
        f.add(new IterProbGmA(new Factor1(), new SumCombinator()));
        ffConfigs.add(f);
        f = new ArrayList<>();
        //comparedFunctions.add(new IterMinVarGmA(new FactorMOverNmM(), new SumCombinator()));
        f.add(new IterMinVarGmA(new FactorMOverNmM(), new WeightedSumCombinator2(0.0)));
        f.add(new IterMinVarGmA(new FactorMOverNmM(), new WeightedSumCombinator2(0.1)));
        f.add(new IterMinVarGmA(new FactorMOverNmM(), new WeightedSumCombinator2(0.2)));
        f.add(new IterMinVarGmA(new FactorMOverNmM(), new WeightedSumCombinator2(0.3)));
        f.add(new IterMinVarGmA(new FactorMOverNmM(), new WeightedSumCombinator2(0.4)));
        //comparedFunctions.add(new IterMinVarGmA(new FactorMOverNmM(), new WeightedSumCombinator3()));
        //comparedFunctions.add(new IterMaxMatchGmA(new FactorMOverNmM(), new SumCombinator()));
        //comparedFunctions.add(new IterProbGmA(new Factor1(), new SumCombinator()));
        ffConfigs.add(f);

        try (PrintStream out = System.out) {//new PrintStream("output-data/E"+System.currentTimeMillis()+".m")) {//
            for (int ffConfigId = 0; ffConfigId < ffConfigs.size(); ffConfigId++) {
                List<FitnessFunction> comparedFunctions = ffConfigs.get(ffConfigId);

                for (String dataset : datasets) {
                    launcher.dataset = dataset;

                    for (int nc : numChildren) {
                        launcher.numChildren = nc;

                        List<String> names = new ArrayList<>();
                        List<MeasurementLog> logs = new ArrayList<>();

                        for (LocalSearch ls : localSearch) {
                            launcher.localSearch = ls;

                            for (FitnessFunction fitnessFunction : comparedFunctions) {
                                launcher.fitnessFunction = fitnessFunction;
                                launcher.run();

                                names.add(launcher.getName());
                                logs.add(log);
                                log = null;
                            }
                        }

                        IEPOSEvaluator.evaluateLogs(ffConfigId + 1, names, logs, out);

                        long t1 = System.currentTimeMillis();
                        System.out.println("%" + (t1 - t0) / 1000 + "s");
                    }
                }
            }
        }
    }

    @Override
    public EPOSExperiment createExperiment(int num) {
        System.out.println("%Experiment " + getName(num) + ", " + location + "/" + dataset);

        EPOSExperiment experiment = new EPOSExperiment(getName(num),
                RankPriority.HIGH_RANK, DescriptorType.RANK, TreeType.SORTED_HtL,
                location, dataset, null,
                "3BR" + num, DateTime.parse("0001-01-01"),
                fitnessFunction, DateTime.parse("0001-01-01"), 5, numChildren + 1, numUser, numIterations,
                new IEPOSAgent.Factory(), localSearch);
        //new IGreedyAgent.Factory(),ls);
        //new OPTAgent.Factory(),ls);
        return experiment;
    }

    @Override
    public void evaluateRun() {
        if (log == null) {
            log = Experiment.getSingleton().getRootMeasurementLog();
        } else {
            log.mergeWith(Experiment.getSingleton().getRootMeasurementLog());
        }
    }

    private String getName() {
        return fitnessFunction.toString() + (localSearch != null ? " +LS" : "");
    }

    private String getName(int num) {
        return getName() + " " + num;
    }
}
