/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package experiments;

import agents.EPOSAgent;
import agents.dataset.FileAgentDataset;
import agents.plan.Plan;
import agents.fitnessFunction.FitnessFunction;
import agents.fitnessFunction.MatchEstimate2FitnessFunction;
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
public class EPOS_Bicycles extends SimulatedExperiment {

    private final static String expSeqNum = "01";
    private static String experimentID = "Experiment " + expSeqNum + "/";

    //Simulation Parameters
    private final static int runDuration = 5;
    private final static int N = 2300;

    // Tree building
    private static final RankPriority priority = RankPriority.HIGH_RANK;
    private static final DescriptorType descriptor = DescriptorType.RANK;
    private static final TreeType type = TreeType.SORTED_HtL;
    private final static int[] v = new int[]{4};
    // EPOS Agent
    private static int treeInstances = 1;
    private static String plansLocation = "input-data/bicycle";
    private static String planConfigurations = "user_plans_unique_8to10_force_trips";
    private static String TISLocation = plansLocation + "/cost.txt";
    static File dir = new File(plansLocation + "/" + planConfigurations);
    private static String treeStamp = "2BR"; //1. average k-ary tree, 2. Balanced or random k-ary tree, 3. random positioning or nodes 
    private static File[] agentMeterIDs = dir.listFiles(new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    });
    private static DateTime aggregationPhase = DateTime.parse("0001-01-01");
    private static String plansFormat = ".plans";
    private static FitnessFunction fitnessFunction = new MatchEstimate2FitnessFunction();
    private static int planSize = 98;
    private static DateTime historicAggregationPhase = DateTime.parse("0001-01-01");
    private static Plan patternEnergyPlan;
    private static int historySize = 5;

    public static void main(String[] args) {
        for (int i = 0; i < treeInstances; i++) {
            treeStamp = "3BR" + i;
            System.out.println("Experiment " + expSeqNum + "\n");
            Experiment.initEnvironment();
            final EPOS_Bicycles test = new EPOS_Bicycles();
            test.init();
            experimentID = "Experiment " + i + "/";
            final File folder = new File("peersLog/" + experimentID);
            clearExperimentFile(folder);
            folder.mkdir();
//            patternEnergyPlan=getPatternPlan(planSize);
            patternEnergyPlan = loadTIS(TISLocation);
            PeerFactory peerFactory = new PeerFactory() {
                public Peer createPeer(int peerIndex, Experiment experiment) {
                    Peer newPeer = new Peer(peerIndex);
                    if (peerIndex == 0) {
                        newPeer.addPeerlet(new TreeServer(N, priority, descriptor, type));
                    }
                    newPeer.addPeerlet(new TreeClient(Experiment.getSingleton().getAddressToBindTo(0), new SimplePeerIdentifierGenerator(), peerIndex, 2));
                    newPeer.addPeerlet(new TreeProvider());
                    FileAgentDataset dataSource = new FileAgentDataset(plansLocation, planConfigurations, agentMeterIDs[peerIndex].getName(), plansFormat, planSize);
                    newPeer.addPeerlet(new EPOSAgent(dataSource, treeStamp, fitnessFunction, aggregationPhase, historicAggregationPhase, patternEnergyPlan, historySize, null));
                    return newPeer;
                }
            };
            test.initPeers(0, N, peerFactory);
            test.startPeers(0, N);
            //run the simulation
            test.runSimulation(Time.inSeconds(runDuration));
        }
    }

    public final static Plan loadTIS(String TISLocation) {
        Plan patternEnergyPlan = new Plan();
        File file = new File(TISLocation);
        try {
            Scanner sc = new Scanner(file);
            while (sc.hasNextDouble()) {
                patternEnergyPlan.addValue(sc.nextDouble());
            }
            sc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
        return patternEnergyPlan;
    }

    public final static ArithmeticListState getPatternPlan(int planSize) {
        ArithmeticListState patternEnergyPlan = new ArithmeticListState(new ArrayList());
        for (int i = 0; i < planSize; i++) {
            if (i >= 2 && i <= 9) {
                patternEnergyPlan.addArithmeticState(new ArithmeticState(0.8));
            } else {
                patternEnergyPlan.addArithmeticState(new ArithmeticState(0.2));
            }

        }
        return patternEnergyPlan;
    }

    public final static void clearExperimentFile(File experiment) {
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
