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

import agents.AgentFactory;
import agents.LocalSearch;
import agents.plan.GlobalPlan;
import agents.plan.Plan;
import agents.fitnessFunction.FitnessFunction;
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
import protopeer.Peer;
import protopeer.PeerFactory;
import protopeer.SimulatedExperiment;
import protopeer.servers.bootstrap.SimplePeerIdentifierGenerator;
import tree.centralized.client.TreeClient;
import tree.centralized.server.TreeServer;

/**
 *
 * @author Evangelos
 */
public class EPOSExperiment extends SimulatedExperiment {

    private final String expSeqNum;
    private String experimentID;

    //Simulation Parameters
    private final int N;

    // Tree building
    private final RankPriority priority;
    private final DescriptorType descriptor;
    private final TreeType type;

    // EPOS Agent
    private String plansLocation;
    private String planConfigurations;
    private String treeStamp; //1. average k-ary tree, 2. Balanced or random k-ary tree, 3. random positioning or nodes 
    private File[] agentMeterIDs;
    private DateTime aggregationPhase;
    private String plansFormat = ".plans";
    private FitnessFunction fitnessFunction;
    private int planSize;
    private DateTime historicAggregationPhase;
    private Plan patternEnergyPlan;
    private int historySize;
    private int maxChildren;
    private int numIterations;
    private LocalSearch ls;

    private AgentFactory factory;

    public EPOSExperiment(String expSeqNum, RankPriority priority, DescriptorType descriptor, TreeType type, String plansLocation, String planConfigurations, String TISFile, String treeStamp, DateTime aggregationPhase, FitnessFunction fitnessFunction, DateTime historicAggregationPhase, int historySize, int maxChildren, int maxAgents, int numIterations, AgentFactory factory, LocalSearch ls) {
        this.expSeqNum = expSeqNum;
        this.experimentID = "Experiment " + expSeqNum + "/";
        this.priority = priority;
        this.descriptor = descriptor;
        this.type = type;
        this.plansLocation = plansLocation;
        this.planConfigurations = planConfigurations;
        this.treeStamp = treeStamp;
        this.aggregationPhase = aggregationPhase;
        this.fitnessFunction = fitnessFunction;
        this.historicAggregationPhase = historicAggregationPhase;
        this.historySize = historySize;
        this.maxChildren = maxChildren;
        this.factory = factory;
        this.numIterations = numIterations;
        this.ls = ls;

        File dir = new File(plansLocation + "/" + planConfigurations);
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
        this.patternEnergyPlan = loadPatternPlan(plansLocation + "/" + TISFile);
    }

    public final void initEPOS() {
        System.out.println("%Experiment " + expSeqNum + ", " + plansLocation + "/" + planConfigurations);

        Experiment.initEnvironment();
        init();

        final File folder = new File("peersLog/" + experimentID);
        clearExperimentFile(folder);
        folder.mkdirs();

        PeerFactory peerFactory = new PeerFactory() {
            @Override
            public Peer createPeer(int peerIndex, Experiment experiment) {
                Peer newPeer = new Peer(peerIndex);
                if (peerIndex == 0) {
                    newPeer.addPeerlet(new TreeServer(N, priority, descriptor, type));
                }
                //newPeer.addPeerlet(new TreeClient(Experiment.getSingleton().getAddressToBindTo(0), new SimplePeerIdentifierGenerator(), Math.random(), maxChildren));
                newPeer.addPeerlet(new TreeClient(Experiment.getSingleton().getAddressToBindTo(0), new SimplePeerIdentifierGenerator(), peerIndex, maxChildren));
                newPeer.addPeerlet(new TreeProvider());

                newPeer.addPeerlet(factory.create(experimentID, plansLocation, planConfigurations, treeStamp, agentMeterIDs[peerIndex].getName(), plansFormat, fitnessFunction, planSize, aggregationPhase, historicAggregationPhase, patternEnergyPlan, historySize, numIterations, ls));

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
