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

import agents.fitnessFunction.MinDeviationFitnessFunction;
import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import org.joda.time.DateTime;

/**
 * @author Peter
 */
public class BicyclesExperiment extends ExperimentLauncher {
    // EPOS Agent

    public static void main(String[] args) {
        ExperimentLauncher launcher = new BicyclesExperiment();
        launcher.treeInstances = 1;
        launcher.runDuration = 25;
        launcher.run();
    }

    @Override
    public EPOSExperiment createExperiment(int num) {
        EPOSExperiment experiment = new EPOSExperiment("01",
                RankPriority.HIGH_RANK, DescriptorType.RANK, TreeType.SORTED_HtL,
                "input-data/bicycle", "user_plans_unique_8to10_force_trips", "cost.txt",
                "3BR" + num, DateTime.parse("0001-01-01"),
                new MinDeviationFitnessFunction(), DateTime.parse("0001-01-01"), 5, 2);
        return experiment;
    }
}
