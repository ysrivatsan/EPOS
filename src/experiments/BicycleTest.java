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


import agents.EPOSAgent;
import dsutil.generic.RankPriority;
import dsutil.generic.state.ArithmeticListState;
import dsutil.generic.state.ArithmeticState;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeProvider;
import dsutil.protopeer.services.topology.trees.TreeType;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;
import org.joda.time.DateTime;
import protopeer.Experiment;
import protopeer.Peer;
import protopeer.PeerFactory;
import protopeer.SimulatedExperiment;
import protopeer.servers.bootstrap.SimplePeerIdentifierGenerator;
import protopeer.util.quantities.Time;
import tree.centralized.client.TreeClient;
import tree.centralized.server.TreeServer;

/**
 *
 * @author Evangelos
 */
public class BicycleTest extends ExperimentLauncher {
    
    public static void main(String[] args) {
        ExperimentLauncher launcher = new BicycleTest();
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
                EPOSAgent.FitnessFunction.MINIMIZING_DEVIATIONS, DateTime.parse("0001-01-01"), 5);
        return experiment;
    }
}
