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
import java.util.List;
import org.joda.time.DateTime;
import protopeer.Experiment;
import protopeer.measurement.MeasurementLog;

/**
 * @author Peter
 */
public class BicyclesExperiment extends ExperimentLauncher {

    private final static int numExperiments = 1;
    private FitnessFunction fitnessFunction;
    private String location;
    private String dataset;
    private int numUser;

    public BicyclesExperiment(int numUser) {
        this.numUser = numUser;
    }

    private static MeasurementLog log = null;

    public static void main(String[] args) throws FileNotFoundException {
        long t0 = System.currentTimeMillis();
        new File("output-data").mkdir();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("output-data/log.log"))) {
            try (PrintStream out = System.out){//new PrintStream("output-data/matlab-script.m")) {
                //for (int t : new int[]{0,2,4,6,8,10,12,14,16,18,20,22}) {
                for (int t : new int[]{10}) {
                    String location = "input-data/bicycle";
                    String dataset = "user_plans_unique_"+t+"to"+(t+2)+"_force_trips";
                /**/
                /*//for (String dataset : new String[]{"1.1","1.3","1.5","5.1","5.3","5.5","7.1","7.3","7.5"}) {
                for (String dataset : new String[]{"5.3"}) {
                    String location = "input-data/Archive";
                /**/
                    for (int i : new int[]{6}) {
                        List<FitnessFunction> comparedFunctions = new ArrayList<>();
                        switch (i) {
                            case 0:
                                comparedFunctions.add(new IterMinVarGmA(new Factor1OverN(), new SumCombinator()));
                                comparedFunctions.add(new IterMinVarGmA(new Factor1OverLayer(), new SumCombinator()));
                                comparedFunctions.add(new IterMinVarGmA(new FactorMOverN(), new SumCombinator()));
                                comparedFunctions.add(new IterMinVarGmA(new FactorMOverNmM(), new SumCombinator()));
                                comparedFunctions.add(new IterMinVarGmA(new FactorDepthOverN(), new SumCombinator()));
                                comparedFunctions.add(new IterMinVarGmA(new FactorNormalizeStd(), new SumCombinator()));
                                comparedFunctions.add(new IterMinVarGmA(new Factor1(), new SumCombinator()));
                                break;
                            case 1:
                                comparedFunctions.add(new IterMinVarG(new Factor1OverN(), new WeightedSumCombinator()));
                                comparedFunctions.add(new IterMinVarG(new Factor1OverLayer(), new WeightedSumCombinator()));
                                comparedFunctions.add(new IterMinVarG(new FactorMOverN(), new WeightedSumCombinator()));
                                comparedFunctions.add(new IterMinVarG(new FactorMOverNmM(), new WeightedSumCombinator()));
                                comparedFunctions.add(new IterMinVarG(new FactorDepthOverN(), new WeightedSumCombinator()));
                                comparedFunctions.add(new IterMinVarG(new FactorNormalizeStd(), new WeightedSumCombinator()));
                                break;
                            case 2:
                                comparedFunctions.add(new IterMinVarGmT(new Factor1OverN(), new SumCombinator()));
                                comparedFunctions.add(new IterMinVarGmT(new Factor1OverLayer(), new SumCombinator()));
                                comparedFunctions.add(new IterMinVarGmT(new FactorMOverN(), new SumCombinator()));
                                comparedFunctions.add(new IterMinVarGmT(new FactorMOverNmM(), new SumCombinator()));
                                comparedFunctions.add(new IterMinVarGmT(new FactorDepthOverN(), new SumCombinator()));
                                comparedFunctions.add(new IterMinVarGmT(new FactorNormalizeStd(), new SumCombinator()));
                                break;
                            case 3:
                                comparedFunctions.add(new IterMinVarHGmA(new Factor1OverN(), new Factor1(), new SumCombinator(), new MostRecentCombinator()));
                                comparedFunctions.add(new IterMinVarHGmA(new Factor1OverLayer(), new Factor1(), new SumCombinator(), new MostRecentCombinator()));
                                comparedFunctions.add(new IterMinVarHGmA(new FactorMOverN(), new Factor1(), new SumCombinator(), new MostRecentCombinator()));
                                comparedFunctions.add(new IterMinVarHGmA(new FactorMOverNmM(), new Factor1(), new SumCombinator(), new MostRecentCombinator()));
                                comparedFunctions.add(new IterMinVarHGmA(new FactorDepthOverN(), new Factor1(), new SumCombinator(), new MostRecentCombinator()));
                                comparedFunctions.add(new IterMinVarHGmA(new FactorNormalizeStd(), new Factor1(), new SumCombinator(), new MostRecentCombinator()));
                                break;
                            case 4:
                                comparedFunctions.add(new IterMaxMatchGmA(new Factor1OverN(), new SumCombinator()));
                                comparedFunctions.add(new IterMaxMatchGmA(new Factor1OverLayer(), new SumCombinator()));
                                comparedFunctions.add(new IterMaxMatchGmA(new FactorMOverN(), new SumCombinator()));
                                comparedFunctions.add(new IterMaxMatchGmA(new FactorMOverNmM(), new SumCombinator()));
                                comparedFunctions.add(new IterMaxMatchGmA(new FactorDepthOverN(), new SumCombinator()));
                                comparedFunctions.add(new IterMaxMatchGmA(new FactorNormalizeStd(), new SumCombinator()));
                                comparedFunctions.add(new IterMaxMatchGmA(new Factor1(), new SumCombinator()));
                                break;
                            case 5:
                                comparedFunctions.add(new IterProbGmA(new Factor1(), new SumCombinator()));
                                //comparedFunctions.add(new IterProbG(new Factor1(), new MostRecentCombinator()));
                                break;
                            case 6:
                                comparedFunctions.add(new IterUCB1Bandit());
                                comparedFunctions.add(new IterLocalSearch());
                                comparedFunctions.add(new IterMinVarGmA(new FactorMOverNmM(), new SumCombinator()));
                                comparedFunctions.add(new IterMaxMatchGmA(new FactorMOverNmM(), new SumCombinator()));
                                comparedFunctions.add(new IterProbGmA(new Factor1(), new SumCombinator()));
                                break;
                            case 7:
                                comparedFunctions.add(new IterMinVarGmA(new Factor1(), new SumCombinator()));
                                //comparedFunctions.add(new IterMinVarGmA(new FactorMOverNmM(), new SumCombinator()));
                                //comparedFunctions.add(new IterMinVarGmA(new FactorNormalizeStd(), new SumCombinator()));
                            default:
                                break;
                        }

                        List<Integer> comparedNumUser = new ArrayList<>();
                        comparedNumUser.add(9999); // max user
                        //comparedNumUser.add(1000);
                        //comparedNumUser.add(50);

                        List<String> names = new ArrayList<>();
                        List<MeasurementLog> logs = new ArrayList<>();

                        for (int numUser : comparedNumUser) {
                            for (FitnessFunction fitnessFunction : comparedFunctions) {
                                BicyclesExperiment launcher = new BicyclesExperiment(numUser);
                                launcher.fitnessFunction = fitnessFunction;
                                launcher.location = location;
                                launcher.dataset = dataset;
                                launcher.treeInstances = numExperiments;
                                launcher.runDuration = 4;
                                launcher.run();

                                names.add(launcher.getName());
                                logs.add(log);
                                oos.writeUTF(launcher.getName());
                                oos.writeObject(log);
                                log = null;
                            }
                        }

                        IEPOSEvaluator.evaluateLogs(i + 1, names, logs, out);

                        long t1 = System.currentTimeMillis();
                        System.out.println("%" + (t1 - t0) / 1000 + "s");
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public EPOSExperiment createExperiment(int num) {
        EPOSExperiment experiment = new EPOSExperiment(getName(num),
                RankPriority.HIGH_RANK, DescriptorType.RANK, TreeType.SORTED_HtL,
                location, dataset, null,
                "3BR" + num, DateTime.parse("0001-01-01"),
                fitnessFunction, DateTime.parse("0001-01-01"), 5, 3, numUser, 200,
                new IEPOSAgent.Factory());
        //new IGreedyAgent.Factory());
        //new OPTAgent.Factory());
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
        return fitnessFunction.toString();
    }

    private String getName(int num) {
        return getName() + " " + num;
    }
}
