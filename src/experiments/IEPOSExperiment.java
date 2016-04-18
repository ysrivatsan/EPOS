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

import agents.Agent;
import agents.network.TreeArchitecture;
import agents.AgentFactory;
import agents.plan.GlobalPlan;
import agents.plan.Plan;
import agents.plan.PlanGenerator;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import org.joda.time.DateTime;
import protopeer.Experiment;
import protopeer.LiveExperiment;
import protopeer.Peer;
import protopeer.PeerFactory;
import protopeer.SimulatedExperiment;

/**
 *
 * @author Evangelos
 */
public class IEPOSExperiment extends SimulatedExperiment {

    //Simulation Parameters
    private final int N;

    // Tree building
    private final TreeArchitecture architecture;

    // EPOS Agent
    private final String inFolder;
    private final File outFolder;
    private final String config;
    private final String treeStamp; //1. average k-ary tree, 2. Balanced or random k-ary tree, 3. random positioning or nodes 
    private final File[] agentMeterIDs;
    private final DateTime aggregationPhase;
    private final String plansFormat = ".plans";
    private final int planSize;
    private final DateTime historicAggregationPhase;
    private final Plan costSignal;
    private final int historySize;

    private final AgentFactory factory;

    public IEPOSExperiment(String inFolder, File outFolder, String config, TreeArchitecture architecture, String treeStamp, DateTime aggregationPhase, DateTime historicAggregationPhase, int historySize, int maxAgents, AgentFactory factory, PlanGenerator planGenerator) {
        this.outFolder = outFolder;
        this.architecture = architecture;
        this.inFolder = inFolder;
        this.config = config;
        this.treeStamp = treeStamp;
        this.aggregationPhase = aggregationPhase;
        this.historicAggregationPhase = historicAggregationPhase;
        this.historySize = historySize;
        this.factory = factory;

        File dir = new File(inFolder + "/" + config);
        this.agentMeterIDs = dir.listFiles((File pathname) -> pathname.isDirectory());
        if (agentMeterIDs == null) {
            System.out.println("ERROR: directory " + dir.getPath() + " is empty");
        }

        this.N = Math.min(maxAgents, agentMeterIDs.length);
        this.planSize = getPlanSize();
        this.costSignal = planGenerator.generatePlan(planSize);
    }

    public final void initEPOS() {
        Experiment.initEnvironment();
        init();

        PeerFactory peerFactory = new PeerFactory() {
            @Override
            public Peer createPeer(int peerIndex, Experiment experiment) {
                Agent newAgent = factory.create(inFolder, config, treeStamp, agentMeterIDs[peerIndex].getName(), plansFormat, planSize, outFolder, aggregationPhase, historicAggregationPhase, costSignal, historySize);
                Peer newPeer = new Peer(peerIndex);
                architecture.addPeerlets(newPeer, newAgent, peerIndex, N);
                return newPeer;
            }
        };
        initPeers(0, N, peerFactory);
        startPeers(0, N);
    }

    private int getPlanSize() {
        File file = agentMeterIDs[0];
        File[] planFiles = file.listFiles((File dir, String name) -> name.endsWith(plansFormat));
        file = planFiles[0];
        try (Scanner scanner = new Scanner(file)) {
            scanner.useLocale(Locale.US);
            String line = scanner.nextLine();
            return line.split(",").length;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

}
