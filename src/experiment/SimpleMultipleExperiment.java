package experiment;

import agent.logging.GlobalCostLogger;
import agent.logging.TerminationLogger;
import agent.logging.LoggingProvider;
import agent.logging.AgentLoggingProvider;
import agent.dataset.Dataset;
import agent.*;
import agent.logging.DetailLogger;
import agent.logging.GraphLogger;
import agent.logging.LocalCostLogger;
import data.Plan;
import data.Vector;
import static experiment.Multiple_Exp.q;
import func.DifferentiableCostFunction;
import func.PlanCostFunction;
import func.PlanScoreCostFunction;
import func.StdDevCostFunction;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 *
 * @author Peter
 */
public class SimpleMultipleExperiment {

    static Queue<Integer> q = new LinkedList<>();
    static Map<Integer, LinkedList<Integer>> g = new HashMap<>();
    static LinkedList<Integer> result = new LinkedList<Integer>();
    public static int node;

    static String dir = "C:\\Users\\syadhuna\\Downloads\\EPOS-master\\EPOS-master\\datasets\\bicycle";

    public static void main(String[] args) throws FileNotFoundException {
        Random random = new Random(0);
        Dataset<Vector> dataset = new agent.dataset.FileVectorDataset(dir + "/Plans");
        File output = new File(dir + "/Output");
        output.mkdir();
        int numAgents = new File(dir + "/Plans").list().length;

        // optimization functions
        double lambda = 0;
        DifferentiableCostFunction globalCostFunc = new StdDevCostFunction();
        PlanCostFunction localCostFunc = new PlanScoreCostFunction();
        // Options
        int miniIterationPassing = 5 ;
        int miniIterationNoPassing = 5;
        Experiment exp3 = new Experiment("Passing", 100, subtree_calc(100),miniIterationPassing);
        Experiment exp2 = new Experiment("NOPassing", 100, subtree_calc(100),miniIterationNoPassing);
        Experiment exp1 = new Experiment();
        List<Experiment> Experiments = Arrays.asList(exp1, exp2, exp3);
        
//        int Num_of_exp = Options.size();
//        List<Boolean> Parent_in_Passing = new ArrayList<>();
//        List<Boolean> Child_in_Passing = new ArrayList<>();
//        List<Boolean> Parent_in_NOPassing = new ArrayList<>();
//        List<Boolean> Child_in_NOPassing = new ArrayList<>();
//        Collections.fill(Parent_in_NOPassing, Boolean.FALSE);
//        Collections.fill(Parent_in_Passing, Boolean.FALSE);
//        Collections.fill(Child_in_NOPassing, Boolean.FALSE);
//        Collections.fill(Child_in_Passing, Boolean.FALSE);
//        for (int i = 0; i < numAgents; i++) {
//            for (Option option : Options) {
//                if (option.Experiment.startsWith("NO")) {
//                    if (option.root == i) {
//                        Parent_in_NOPassing.set(i, true);
//                    } else if (option.Children.contains(i)) {
//                        Child_in_NOPassing.set(i, true);
//                    }
//                } else if (option.Experiment.contains("Simple")) {
//                } else {
//                    if (option.root == i) {
//                        Parent_in_Passing.set(i, true);
//                    } else if (option.Children.contains(i)) {
//                        Child_in_Passing.set(i, true);
//                    }
//                }
//            }
//        }
        // network
        int numChildren = 5;

        // logging
        LoggingProvider<IeposAgentMultiple<Vector>> loggingProvider = new LoggingProvider<>();
        loggingProvider.add(new GlobalCostLogger(output + "/Global_Cost.txt"));
        loggingProvider.add(new LocalCostLogger(output + "/Local_Cost.txt"));
        loggingProvider.add(new TerminationLogger());
        //loggingProvider.add();
        loggingProvider.add(new DetailLogger(output + "/Plan_Output"));
        loggingProvider.add(new GraphLogger<>(GraphLogger.Type.Change));
        loggingProvider.add(new agent.logging.GlobalResponseLogger(output + "/Global_Response"));
        loggingProvider.add(new agent.logging.DistributionLogger());
        //loggingProvider.add(new agent.logging.FileWriter(dir+"\\Output\\simple.log"));
        int numSimulations = 1;
        for (int sim = 0; sim < numSimulations; sim++) {
            final int simulationId = sim;

            // algorithm
            int numIterations = 50;
            PlanSelector<IeposAgentMultiple<Vector>, Vector> planSelector = new IeposIndividualGradientPlanSelectorMultiple();
            Function<Integer, Agent> createAgent = agentIdx -> {
                List<Plan<Vector>> possiblePlans = dataset.getPlans(agentIdx);
                AgentLoggingProvider agentLP = loggingProvider.getAgentLoggingProvider(agentIdx, simulationId);

                IeposAgentMultiple newAgent = new IeposAgentMultiple(
                        numIterations,
                        possiblePlans,
                        globalCostFunc,
                        localCostFunc,
                        agentLP,
                        random.nextLong(),Experiments);
                newAgent.setLambda(lambda);
                newAgent.setPlanSelector(planSelector);
                return newAgent;
            };

            // start experiment
            IeposExperiment.runSimulation(
                    numChildren,
                    numIterations,
                    numAgents,
                    createAgent);
        }
        loggingProvider.print();
    }

    public static class Experiment {

        int root;
        String Experiment;
        List<Integer> Children;
        int iterations;

        public Experiment() {
            this.root = 199;
            this.Experiment = "Simple";
        }

        private Experiment(String xx, int i, List<Integer> children, int iteration) {
            this.root = i;
            this.Experiment = xx;
            this.Children = children;
            this.iterations = iteration;
        }
    }

    public static void graph_create(int numAgents, int numChildren) {
        int n = numAgents;
        q.add(n);
        n--;
        while (!q.isEmpty()) {
            int v = q.poll();
            int nn = n;
            for (int j = 1; j <= Math.min(numChildren, nn); j++) {
                if (g.containsKey(v - 1)) {
                    g.get(v - 1).add(n - 1);
                } else {
                    LinkedList<Integer> tmp = new LinkedList<>();
                    tmp.add(n - 1);
                    g.put(v - 1, tmp);
                }
                q.add(n);
                n--;
            }
        }

    }

    private static void depth_first_search(int v) {
        result.add(v);
        if (g.containsKey(v)) {
            for (int u : g.get(v)) {
                depth_first_search(u);
            }
        }
    }

    private static List<Integer> subtree_calc(int node) {
        List<Integer> tmp = new ArrayList<>();
        result.clear();
        depth_first_search(node);
        for (int value : result) {
            tmp.add(value);
        }
        Collections.sort(tmp);
        return tmp;
    }

    private static List<List<Plan<Vector>>> Create_Possible_Plans_Selection_Map(List<Integer> subtree_list, Dataset<Vector> dataset, HashMap<Integer, Integer> agent_id_map, int numAgents, HashMap<Integer, Integer> selection_map_tmp, HashMap<Integer, Integer> selection_map) {
        int i = 0;
        List<List<Plan<Vector>>> possible = new ArrayList<>();
        for (int value : subtree_list) {
            possible.add(dataset.getPlans(value));
            agent_id_map.put(i, value);
            if (node != numAgents - 1) {
                selection_map_tmp.put(i, selection_map.get(value));
            }
            i++;
        }

        return possible;
    }

    private static void update_Selection_map(String out, HashMap<Integer, Integer> selection_map, HashMap<Integer, Integer> agent_id_map) {

        BufferedReader br = null;
        FileReader fr = null;
        String[] values = new String[2];

        try {
            fr = new FileReader(out);
            br = new BufferedReader(fr);
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {

                values = sCurrentLine.split(":");
                int sub_agent_id = Integer.parseInt(values[0]);
                int selection = Integer.parseInt(values[1]);
                int orig_id = agent_id_map.get(sub_agent_id);
                selection_map.put(orig_id, selection);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            try {
                if (br != null) {
                    br.close();
                }
                if (fr != null) {
                    fr.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static Map<Integer, Integer> Level_Identifier(int full_subtree_start, int numAgents, double numChildren) {
        Map<Integer, Integer> level_map = new HashMap<>();
        for (int i = numAgents - 1; i >= full_subtree_start; i--) {
            int level_count = 1;
            int subtree_size = subtree_calc(i).size();
            while (subtree_size >= Math.pow(numChildren, level_count)) {
                level_count++;
            }
            level_map.put(i, level_count--);
        }
        return level_map;
    }

    private static int Combination(int n, int r) {
        int diff = 1;
        for (int i = n - r + 1; i <= n; i++) {
            diff *= i;
        }
        int fact_r = 1;
        for (int j = 1; j <= r; j++) {
            fact_r *= j;
        }
        return diff / fact_r;
    }
}
