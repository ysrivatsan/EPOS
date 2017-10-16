package experiment;

import agent.logging.LoggingProvider;
import agent.logging.AgentLoggingProvider;
import agent.dataset.Dataset;
import agent.*;
import data.Plan;
import data.Vector;
import func.DifferentiableCostFunction;
import func.IndexCostFunction;
import func.PlanCostFunction;
import func.PlanScoreCostFunction;
import func.StdDevCostFunction;
import func.VarCostFunction;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
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

    static String dir = "E:\\Java_Workspace\\NetBeansProjects\\EPOS-master\\datasets\\bicycle";

    public static void main(String[] args) throws FileNotFoundException {
        Random random = new Random(0);
        Dataset<Vector> dataset = new agent.dataset.FileVectorDataset(dir + "/Plans");
       // File output = new File(dir + "/Output");
       // output.mkdir();
        int numAgents = new File(dir + "/Plans").list().length;
        // optimization functions
        double lambda = 0;
        DifferentiableCostFunction globalCostFunc = new VarCostFunction();
        PlanCostFunction localCostFunc = new PlanScoreCostFunction();
        // Options
        int subIterations = 5;
        int numResponses = 2;
        Map<Integer, Integer> depthMap = new HashMap<>();
        Map<Integer, Integer> heightMap = new HashMap<>();
        int numChildren = 2;
        int depthOfTree = depthoftree(numAgents, numChildren);
        for (int i = 0; i < numAgents; i++) {
            depthMap.put(i, depthCalc(i, numChildren, numAgents));
        }
        for (int i = 0; i < numAgents; i++) {
            int height = 0;
            if (depthMap.get(i) <= depthOfTree) {
                height = depthOfTree - depthMap.get(i);
            }
            heightMap.put(i, height);
        }
        int numIterations = (depthOfTree * subIterations) + 20;

        //System.out.println("Agents:  " + numAgents + " Children " + numChildren + " depthoftree " + depthOfTree + " depthMap " + depthMap + " heightMap " + heightMap + " iterations " + numIterations);

        // logging
        LoggingProvider<IeposAgentMultiple<Vector>> loggingProvider = new LoggingProvider<>();

        int numSimulations = 1;
        for (int sim = 0; sim < numSimulations; sim++) {
            final int simulationId = sim;

            // algorithm
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
                        random.nextLong(), numAgents, heightMap.get(agentIdx), subIterations, numResponses, lambda);
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
    
    private static Integer depthCalc(int i, int numChildren, int numAgents) {
        int node_id = numAgents - i;
        int count = 0;
        int node_count = 1;
        while (true) {
            if (node_id <= node_count) {
                break;
            } else {
                count++;
                node_count += Math.pow(numChildren, count);
            }
        }
        return count;
    }

    private static int depthoftree(int numAgents, int numChildren) {
        int count = 0;
        int nodes_count = 1;
        while (true) {

            if (numAgents > nodes_count) {
                count++;                
                nodes_count += Math.pow(numChildren,count);
            } else {
                break;
            }
        }
        return count;
    }
}
