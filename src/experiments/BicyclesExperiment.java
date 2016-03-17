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

import agents.*;
import agents.fitnessFunction.*;
import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import protopeer.Experiment;
import protopeer.measurement.MeasurementLog;

/**
 * @author Peter
 */
public class BicyclesExperiment extends ExperimentLauncher {
    private final static int numExperiments = 3;
    private FitnessFunction fitnessFunction;
    private int numUser;
    
    public BicyclesExperiment(int numUser) {
        this.numUser = numUser;
    }
    
    private static MeasurementLog log = null;

    public static void main(String[] args) {
        List<FitnessFunction> comparedFunctions = new ArrayList<>();
        //comparedFunctions.add(new IterativeMinVariance1());
        //comparedFunctions.add(new IterativeMinVariance2());
        //comparedFunctions.add(new IterativeMinVariance3());
        comparedFunctions.add(new IterativeMinVariance4());
        //comparedFunctions.add(new IterativeMinVariance5());
        
        List<Integer> comparedNumUser = new ArrayList<>();
        //comparedNumUser.add(10000); // max user
        //comparedNumUser.add(1000);
        comparedNumUser.add(100);
        //comparedNumUser.add(10);
        
        List<String> names = new ArrayList<>();
        List<MeasurementLog> logs = new ArrayList<>();
        
        for(int numUser : comparedNumUser) {
            for(FitnessFunction fitnessFunction : comparedFunctions) {
                BicyclesExperiment launcher = new BicyclesExperiment(numUser);
                launcher.fitnessFunction = fitnessFunction;
                launcher.treeInstances = numExperiments;
                launcher.runDuration = 4;
                launcher.run();

                names.add(launcher.getName());
                logs.add(log);
                log = null;
            }
        }
        
        IEPOSEvaluator.evaluateLogs(names, logs);
    }

    @Override
    public EPOSExperiment createExperiment(int num) {
        EPOSExperiment experiment = new EPOSExperiment(getName(num),
                RankPriority.HIGH_RANK, DescriptorType.RANK, TreeType.SORTED_HtL,
                "input-data/bicycle", "user_plans_unique_8to10_force_trips", "cost.txt",
                "3BR" + num, DateTime.parse("0001-01-01"),
                fitnessFunction, DateTime.parse("0001-01-01"), 5, 3, numUser,//Integer.MAX_VALUE,
                new IEPOSAgent.Factory());
        return experiment;
    }
    
    @Override
    public void evaluateRun() {
        if(log == null) {
            log = Experiment.getSingleton().getRootMeasurementLog();
        } else {
            log.mergeWith(Experiment.getSingleton().getRootMeasurementLog());
        }
    }
    
    private String getName() {
        return fitnessFunction.getClass().getSimpleName() + " " + numUser + "U";
    }
    
    private String getName(int num) {
        return getName() + " " + num;
    }
}
