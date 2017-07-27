package experiment;

import agent.logging.GlobalCostLogger;
import agent.logging.TerminationLogger;
import agent.logging.LoggingProvider;
import agent.logging.AgentLoggingProvider;
import agent.logging.CostViewer;
import agent.dataset.Dataset;
import agent.*;
import agent.dataset.GaussianDataset;
import agent.logging.DetailLogger;
import agent.logging.GraphLogger;
import agent.logging.LocalCostLogger;
import data.DataType;
import data.Plan;
import data.Vector;
import data.io.VectorIO;
import func.DifferentiableCostFunction;
import func.IndexCostFunction;
import func.PlanCostFunction;
import func.PlanScoreCostFunction;
import func.SqrDistCostFunction;
import func.StdDevCostFunction;
import func.VarCostFunction;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Peter
 */
public class SimpleExperiment {

    static String participants = "2779";
//static String dir = "C:\\Users\\syadhuna\\Data\\170320_EV_2779_XXXX_1234_Mixed\\170320_EV_2779_"+participants+"_1234_Mixed\\FRI12-SAT12";
//static String dir = "C:\\Users\\syadhuna\\Data\\170320_EV_2779_XXXX_1234_Tesla\\170320_EV_2779_"+participants+"_1234_Tesla\\MON12-TUE12";
//static String dir = "C:\\Users\\syadhuna\\Data\\170320_EV_2779_XXXX_1234_Mixed\\170320_EV_2779_"+participants+"_1234_Mixed\\SAT12-SUN0";
//static String dir = "C:\\Users\\syadhuna\\Data\\170320_EV_2779_XXXX_1234_Mixed\\170320_EV_2779_"+participants+"_1234_Mixed\\SUN0-SUN12";
//static String dir = "C:\\Users\\syadhuna\\Data\\170320_EV_2779_XXXX_1234_Mixed\\170320_EV_2779_"+participants+"_1234_Mixed\\SUN12-MON12";
//static String dir = "C:\\Users\\syadhuna\\Data\\170320_EV_2779_XXXX_1234_Mixed\\170320_EV_2779_"+participants+"_1234_Mixed\\THU12-FRI12";
//public static String dir = "C:\\Users\\syadhuna\\Downloads\\EPOS-master\\EPOS-master\\datasets\\energy";
//static String dir = "C:\\Users\\syadhuna\\Downloads\\EPOS-master\\EPOS-master\\datasets\\gaussian";
    static String dir = "C:\\Users\\syadhuna\\Downloads\\EPOS-master\\EPOS-master\\datasets\\bicycle";

    public static void main(String[] args) throws FileNotFoundException {
        exp();

    }

    public static void exp() {   //Dataset<Vector> dataset2 = new GaussianDataset(16, 100, 0, 1, random);
        // String targetFile = dir+".txt";
        //DifferentiableCostFunction globalCostFunc = new SqrDistCostFunction(VectorIO.readVector(new File(targetFile)));

        Random random = new Random(0);
        Dataset<Vector> dataset = new agent.dataset.FileVectorDataset(dir + "/Plans");
        File output = new File(dir + "/Output");
        output.mkdir();
        int numAgents = new File(dir + "/Plans").list().length;

        // optimization functions
        double lambda = 0;
        DifferentiableCostFunction globalCostFunc = new StdDevCostFunction();
        PlanCostFunction localCostFunc = new PlanScoreCostFunction();

        // network
        int numChildren = 4;

        // logging
        LoggingProvider<IeposAgent<Vector>> loggingProvider = new LoggingProvider<>();
        loggingProvider.add(new GlobalCostLogger(output + "/Global_Cost.txt"));
        loggingProvider.add(new LocalCostLogger(output + "/Local_Cost.txt"));
        loggingProvider.add(new TerminationLogger());
        loggingProvider.add(new CostViewer());
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
            PlanSelector<IeposAgent<Vector>, Vector> planSelector = new IeposIndividualGradientPlanSelector();
            Function<Integer, Agent> createAgent = agentIdx -> {
                List<Plan<Vector>> possiblePlans = dataset.getPlans(agentIdx);
                AgentLoggingProvider agentLP = loggingProvider.getAgentLoggingProvider(agentIdx, simulationId);

                IeposAgent newAgent = new IeposAgent(
                        numIterations,
                        possiblePlans,
                        globalCostFunc,
                        localCostFunc,
                        agentLP,
                        random.nextLong());
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

    public static void exp(File output, int numAgents, int numChildren, boolean mychoice, List<List<Plan<Vector>>> possiblePlans_input, boolean selected_given, HashMap<Integer, Integer> selection_map, int iteration, boolean graph, boolean cost) {
        //Dataset<Vector> dataset2 = new GaussianDataset(16, 100, 0, 1, random);
        // String targetFile = dir+".txt";
        //DifferentiableCostFunction globalCostFunc = new SqrDistCostFunction(VectorIO.readVector(new File(targetFile)));
        //Dataset<Vector> dataset = new agent.dataset.FileVectorDataset(dir+"/Plans");
        //File output = new File(dir+"/Output");
        //int numAgents =  new File(dir+"/Plans").list().length;

        Random random = new Random(0);
        output.mkdir();
        Dataset<Vector> dataset = new agent.dataset.FileVectorDataset(dir + "/Plans");

        // optimization functions
        double lambda = 0;
        DifferentiableCostFunction globalCostFunc = new StdDevCostFunction();
        PlanCostFunction localCostFunc = new PlanScoreCostFunction();

        // network
        //int numChildren = 4;
        // logging
        LoggingProvider<IeposAgent<Vector>> loggingProvider = new LoggingProvider<>();
        loggingProvider.add(new GlobalCostLogger(output + "/Global_Cost.txt"));
        loggingProvider.add(new LocalCostLogger(output + "/Local_Cost.txt"));
        loggingProvider.add(new TerminationLogger());
        loggingProvider.add(new CostViewer(cost));
        loggingProvider.add(new DetailLogger(output + "/Plan_Output"));
        if (graph) {
            loggingProvider.add(new GraphLogger<>(GraphLogger.Type.Index));
        }
        loggingProvider.add(new agent.logging.GlobalResponseLogger(output + "/Global_Response"));
        loggingProvider.add(new agent.logging.DistributionLogger());
        //loggingProvider.add(new agent.logging.FileWriter(dir+"\\Output\\simple.log"));
        int numSimulations = 1;
        for (int sim = 0; sim < numSimulations; sim++) {
            final int simulationId = sim;

            // algorithm
            int numIterations = iteration;
            PlanSelector<IeposAgent<Vector>, Vector> planSelector = new IeposIndividualGradientPlanSelector();
            Function<Integer, Agent> createAgent = agentIdx -> {
                List<Plan<Vector>> possiblePlans;
                if (!mychoice) {
                    possiblePlans = dataset.getPlans(agentIdx);
                } else {
                    possiblePlans = possiblePlans_input.get(agentIdx);
                }
                AgentLoggingProvider agentLP = loggingProvider.getAgentLoggingProvider(agentIdx, simulationId);
                IeposAgent newAgent = null;
                if (!selected_given) {
                    newAgent = new IeposAgent(
                            numIterations,
                            possiblePlans,
                            globalCostFunc,
                            localCostFunc,
                            agentLP,
                            random.nextLong());
                } else {
                    newAgent = new IeposAgent(
                            numIterations,
                            possiblePlans,
                            globalCostFunc,
                            localCostFunc,
                            agentLP,
                            random.nextLong(),
                            selected_given,
                            selection_map);
                }
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
}
