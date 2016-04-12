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
import agents.LocalSearch;
import agents.plan.GlobalPlan;
import agents.plan.Plan;
import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeProvider;
import dsutil.protopeer.services.topology.trees.TreeType;
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
import protopeer.servers.bootstrap.SimplePeerIdentifierGenerator;
import tree.BalanceType;
import tree.centralized.client.TreeClient;
import tree.centralized.server.TreeServer;

/**
 *
 * @author Evangelos
 */
public class EPOSExperiment extends LiveExperiment {

    private final String experimentID;

    //Simulation Parameters
    private final int N;

    // Tree building
    private final TreeArchitecture architecture;

    // EPOS Agent
    private String plansLocation;
    private String planConfigurations;
    private String treeStamp; //1. average k-ary tree, 2. Balanced or random k-ary tree, 3. random positioning or nodes 
    private File[] agentMeterIDs;
    private DateTime aggregationPhase;
    private String plansFormat = ".plans";
    private int planSize;
    private DateTime historicAggregationPhase;
    private Plan costSignal;
    private int historySize;

    private AgentFactory factory;

    public EPOSExperiment(String id, TreeArchitecture architecture, String folder, String config, String costFile, String treeStamp, DateTime aggregationPhase, DateTime historicAggregationPhase, int historySize, int maxAgents, AgentFactory factory) {
        this.experimentID = id;
        this.architecture = architecture;
        this.plansLocation = folder;
        this.planConfigurations = config;
        this.treeStamp = treeStamp;
        this.aggregationPhase = aggregationPhase;
        this.historicAggregationPhase = historicAggregationPhase;
        this.historySize = historySize;
        this.factory = factory;

        File dir = new File(folder + "/" + config);
        this.agentMeterIDs = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        if (agentMeterIDs == null) {
            System.out.println("ERROR: directory " + dir.getPath() + " is empty");
        }

        this.N = Math.min(maxAgents, agentMeterIDs.length);
        this.planSize = getPlanSize();
        this.costSignal = loadPatternPlan(folder + "/" + costFile);
    }

    public final void initEPOS() {
        Experiment.initEnvironment();
        init();

        final File folder = new File("peersLog/Experiment " + experimentID);
        clearExperimentFile(folder);
        folder.mkdirs();

        PeerFactory peerFactory = new PeerFactory() {
            @Override
            public Peer createPeer(int peerIndex, Experiment experiment) {
                Peer newPeer = new Peer(peerIndex);
                architecture.addPeerlets(newPeer, peerIndex, N);

                newPeer.addPeerlet(factory.create(plansLocation, planConfigurations, treeStamp, agentMeterIDs[peerIndex].getName(), plansFormat, planSize, aggregationPhase, historicAggregationPhase, costSignal, historySize));

                return newPeer;
            }
        };
        initPeers(0, N, peerFactory);
        startPeers(0, N);
    }

    public final Plan loadPatternPlan(String TISLocation) {
        Plan patternEnergyPlan = new GlobalPlan();
        List<Double> vals = new ArrayList<>();

        File file = new File(TISLocation);
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

        patternEnergyPlan.init(vals.size());
        for (int i = 0; i < vals.size(); i++) {
            patternEnergyPlan.setValue(i, vals.get(i));
        }

        return patternEnergyPlan;
    }

    private int getPlanSize() {
        File file = agentMeterIDs[0];
        File[] planFiles = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(plansFormat);
            }
        });
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

    public final void clearExperimentFile(File experiment) {
        File[] files = experiment.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    clearExperimentFile(f);
                } else {
                    f.delete();
                }
            }
        }
        experiment.delete();
    }
}
