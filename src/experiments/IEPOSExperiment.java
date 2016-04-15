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
import agents.AgentFactory;
import agents.plan.GlobalPlan;
import agents.plan.Plan;
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

    public IEPOSExperiment(String inFolder, File outFolder, String config, String costFile, TreeArchitecture architecture, String treeStamp, DateTime aggregationPhase, DateTime historicAggregationPhase, int historySize, int maxAgents, AgentFactory factory) {
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
        this.costSignal = loadCostSignal(inFolder + "/" + costFile);
    }

    public final void initEPOS() {
        Experiment.initEnvironment();
        init();

        PeerFactory peerFactory = new PeerFactory() {
            @Override
            public Peer createPeer(int peerIndex, Experiment experiment) {
                Peer newPeer = new Peer(peerIndex);
                architecture.addPeerlets(newPeer, peerIndex, N);

                newPeer.addPeerlet(factory.create(inFolder, config, treeStamp, agentMeterIDs[peerIndex].getName(), plansFormat, planSize, outFolder, aggregationPhase, historicAggregationPhase, costSignal, historySize));

                return newPeer;
            }
        };
        initPeers(0, N, peerFactory);
        startPeers(0, N);
    }

    public final Plan loadCostSignal(String filename) {
        Plan costSignal = new GlobalPlan();
        List<Double> vals = new ArrayList<>();

        File file = new File(filename);
        if (file.exists()) {
            try (Scanner scanner = new Scanner(file)) {
                scanner.useLocale(Locale.US);
                while (scanner.hasNextDouble()) {
                    vals.add(scanner.nextDouble());
                }
            } catch (FileNotFoundException | NoSuchElementException e) {
                e.printStackTrace();
            }
        } else {
            for (int i = 0; i < planSize; i++) {
                vals.add(0.0);
            }
        }

        costSignal.init(vals.size());
        for (int i = 0; i < vals.size(); i++) {
            costSignal.setValue(i, 10*Math.sin(i*2*Math.PI/vals.size()));
            //costSignal.setValue(i, vals.get(i));
        }

        return costSignal;
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
