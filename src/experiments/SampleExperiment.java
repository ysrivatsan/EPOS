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
import agents.*;
import agents.dataset.FileDataset;
import agents.fitnessFunction.SampleFitnessFunction;
import agents.dataset.FilePlanGenerator;
import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import java.io.File;
import org.joda.time.DateTime;
import tree.BalanceType;

/**
 * @author Peter
 */
public class SampleExperiment extends ExperimentLauncher {
    // EPOS Agent

    public static void main(String[] args) {
        ExperimentLauncher launcher = new SampleExperiment();
        launcher.numExperiments = 1;
        launcher.runDuration = 25;
        launcher.run();
    }

    @Override
    public IEPOSExperiment createExperiment(int num) {
        TreeArchitecture architecture = new TreeArchitecture();
        architecture.priority = RankPriority.HIGH_RANK;
        architecture.rank = DescriptorType.RANK;
        architecture.type = TreeType.SORTED_HtL;
        architecture.balance = BalanceType.WEIGHT_BALANCED;
        architecture.maxChildren = 3;
        architecture.rankGenerator = (idx, agent) -> (double)idx;
        
        AgentFactory agentFactory = new EPOSAgent.Factory();
        //AgentFactory agentFactory = new OPTAgent.Factory();
        agentFactory.fitnessFunction = new SampleFitnessFunction(80);
        //agentFactory.fitnessFunction = new SampleFitnessFunction(0);
        
        IEPOSExperiment experiment = new IEPOSExperiment(
                new FileDataset("input-data/samples", "equalAgents"),
                new File("peersLog/Experiment 01"),
                architecture,
                "3BR" + num, DateTime.parse("0001-01-01"),
                // with factor 0, results are the same (excl. root), compared to factor 80
                DateTime.parse("0001-01-01"), 5, 15, agentFactory,
                new FilePlanGenerator("input-data/samples/cost.txt"));
        return experiment;
    }
}
