/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import data.Plan;
import func.CostFunction;
import func.PlanCostFunction;
import agent.logging.AgentLoggingProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import data.DataType;
import data.Vector;
import experiment.SimpleMultipleExperiment;
import func.DifferentiableCostFunction;

/**
 * This agent performs the I-EPOS algorithm for combinatorial optimization.
 *
 * @author Peter
 * @param <V> the type of the data this agent should handle
 */
public class IeposAgentMultiple<V extends DataType<V>> extends IterativeTreeAgent<V, IeposAgentMultiple<V>.UpMessage, IeposAgentMultiple<V>.DownMessage> {

    // agent info
    Plan<V> prevSelectedPlan;
    V aggregatedResponse;
    V prevAggregatedResponse;
    List<Plan<V>> prevSelectedPlanMultiple = new ArrayList<>();
    List<V> aggregatedResponseMultiple = new ArrayList<>();
    List<V> prevAggregatedResponseMultiple = new ArrayList<>();

    // per child info
    private final List<V> subtreeResponses = new ArrayList<>();
    private final List<V> prevSubtreeResponses = new ArrayList<>();
    private final List<Boolean> approvals = new ArrayList<>();
    private final List<List<V>> subtreeResponsesMultiple = new ArrayList<>();
    private final List<List<V>> prevSubtreeResponsesMultiple = new ArrayList<>();
    private final List<List<Boolean>> approvalsMultiple = new ArrayList<>();
    List<V> globalResponseMultiple;
    List<Plan<V>> selectedPlanMultiple;
//    int NumOfExperiments;
//    boolean ParentNoPassing;
//    boolean ParentPassing;
//    boolean ChildNoPassing;
//    boolean ChildPassing;
//    int miniIterationPassing;
//    int miniIterationNoPassing;
    // misc
    Optimization optimization;
    double lambda; // parameter for lambda-PREF local cost minimization
    private PlanSelector<IeposAgentMultiple<V>, V> planSelector;
    private List<SimpleMultipleExperiment.Experiment> experiments;

    /**
     * Creates a new IeposAgent. Using the same RNG seed will result in the same
     * execution order in a simulation environment.
     *
     * @param numIterations the number of iterations
     * @param possiblePlans the plans this agent can choose from
     * @param globalCostFunc the global cost function
     * @param localCostFunc the local cost function
     * @param loggingProvider the object that extracts data from the agent and
     * writes it into its log.
     * @param seed a seed for the RNG
     */
    public IeposAgentMultiple(int numIterations, List<Plan<V>> possiblePlans, CostFunction<V> globalCostFunc, PlanCostFunction<V> localCostFunc, AgentLoggingProvider<? extends IeposAgentMultiple<V>> loggingProvider, long seed) {
        super(numIterations, possiblePlans, globalCostFunc, localCostFunc, loggingProvider, seed);
        this.optimization = new Optimization(random);
        this.lambda = 0;
        this.planSelector = new IeposPlanSelectorMultiple<>();
    }

//    public IeposAgentMultiple(int numIterations, List<Plan<V>> possiblePlans, CostFunction<V> globalCostFunc, PlanCostFunction<V> localCostFunc, AgentLoggingProvider<? extends IeposAgentMultiple<V>> loggingProvider, long seed, int NumOfExp, Boolean get, Boolean get0, Boolean get1, Boolean get2, int iterationPassing, int iterationNoPassing) {
//        super(numIterations, possiblePlans, globalCostFunc, localCostFunc, loggingProvider, seed);
//        this.optimization = new Optimization(random);
//        this.lambda = 0;
//        this.planSelector = new IeposPlanSelectorMultiple<>();
//        this.miniIterationNoPassing = iterationNoPassing;
//        this.miniIterationPassing = iterationPassing;
//        this.NumOfExperiments = NumOfExp;
//        this.ParentNoPassing = get;
//        this.ParentPassing = get0;
//        this.ChildNoPassing = get1;
//        this.ChildPassing = get2;
//    }
    public IeposAgentMultiple(int numIterations, List<Plan<V>> possiblePlans, CostFunction<V> globalCostFunc, PlanCostFunction<V> localCostFunc, AgentLoggingProvider<? extends IeposAgentMultiple<V>> loggingProvider, long seed, List<SimpleMultipleExperiment.Experiment> Experiments) {
        super(numIterations, possiblePlans, globalCostFunc, localCostFunc, loggingProvider, seed);
        this.optimization = new Optimization(random);
        this.lambda = 0;
        this.planSelector = new IeposPlanSelectorMultiple<>();//To change body of generated methods, choose Tools | Templates.
        this.experiments = Experiments;
    }

    /**
     * Sets lambda, the traidoff between global and local cost minimization. A
     * value of 0 indicates pure global cost minimization, while a value of 1
     * indicates pure local cost minimization.
     *
     * @param lambda traidoff between global and local cost minimization
     */
    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    /**
     * An I-EPOS agent can have different strategies for plan selection. The
     * plan selector decides which plan to select given the current state of the
     * system.
     *
     * @param planSelector the plan selector
     */
    public void setPlanSelector(PlanSelector<IeposAgentMultiple<V>, V> planSelector) {
        this.planSelector = planSelector;
    }

    @Override
    public V getGlobalResponse() {
        return globalResponse.cloneThis();
    }

    @Override
    void initPhase() {
        aggregatedResponse = createValue();
        prevAggregatedResponse = createValue();
        globalResponse = createValue();
        prevSelectedPlan = createPlan();

        for (int i = 0; i < experiments.size(); i++) {
            aggregatedResponseMultiple.add(i, createValue());
            prevAggregatedResponseMultiple.add(i, createValue());
            globalResponseMultiple.add(i, createValue());
            prevSelectedPlanMultiple.add(i, createPlan());
        }
    }

    @Override
    void initIteration() {
        if (iteration > 0) {
            prevSelectedPlan = selectedPlan;
            prevAggregatedResponse.set(aggregatedResponse);
            prevSubtreeResponses.clear();
            prevSubtreeResponses.addAll(subtreeResponses);

            selectedPlan = null;
            aggregatedResponse.reset();
            subtreeResponses.clear();
            approvals.clear();
        }
        if (iteration > 0) {
            for (int i = 0; i < experiments.size(); i++) {
                prevSelectedPlanMultiple.set(i, selectedPlanMultiple.get(i));
                prevAggregatedResponseMultiple.set(i, aggregatedResponseMultiple.get(i));
                prevSubtreeResponsesMultiple.get(i).clear();
                prevSubtreeResponsesMultiple.add(i, subtreeResponsesMultiple.get(i));

                selectedPlanMultiple.set(i, null);
                aggregatedResponseMultiple.get(i).reset();
                subtreeResponsesMultiple.get(i).clear();
                approvalsMultiple.get(i).clear();
            }
        }
    }

    @Override
    UpMessage up(List<UpMessage> childMsgs) {
        for (UpMessage msg : childMsgs) {
            subtreeResponsesMultiple.add(msg.subtreeResponseMultiple);
        }
        for (int i = 0; i < experiments.size(); i++) {
            aggregateNew(i);
            selectedPlanMultiple.set(i, selectPlanNew(i));
        }
        return informParent();
    }

    @Override
    DownMessage atRoot(UpMessage rootMsg) {
        List<Boolean> temp_list = new ArrayList<>();
        for (int i = 0; i < experiments.size(); i++) {
            temp_list.add(i, true);
        }
        return new DownMessage(rootMsg.subtreeResponseMultiple, temp_list);
    }

    @Override
    List<DownMessage> down(DownMessage parentMsg) {
        updateGlobalResponseMultiple(parentMsg);
        approveOrRejectChangesMultiple(parentMsg);
        return informChildren();
    }

    private void aggregateNew(int exp) {
        if (iteration == 0) {
            for (int i = 0; i < children.size(); i++) {
                approvals.add(true);
                approvalsMultiple.add(exp, approvals);
            }
        } else if (children.size() > 0) {
            List<List<V>> choicesPerAgent = new ArrayList<>();
            for (int i = 0; i < children.size(); i++) {
                List<V> choices = new ArrayList<>();
                choices.add(prevSubtreeResponsesMultiple.get(exp).get(i));
                choices.add(subtreeResponsesMultiple.get(exp).get(i));
                choicesPerAgent.add(choices);
            }
            List<V> combinations = optimization.calcAllCombinations(choicesPerAgent);

            V othersResponse = globalResponseMultiple.get(exp).cloneThis();
            for (V prevSubtreeResponce : prevSubtreeResponsesMultiple.get(exp)) {
                othersResponse.subtract(prevSubtreeResponce);
            }
            int selectedCombination = optimization.argmin(globalCostFunc, combinations, othersResponse);
            numComputed += combinations.size();

            List<Integer> selections = optimization.combinationToSelections(selectedCombination, choicesPerAgent);
            for (int selection : selections) {
                approvals.add(selection == 1);
                approvalsMultiple.add(exp, approvals);
            }
        }
        for (int i = 0; i < children.size(); i++) {
            V prelSubtreeResponse = approvalsMultiple.get(exp).get(i) ? subtreeResponsesMultiple.get(exp).get(i) : prevSubtreeResponsesMultiple.get(exp).get(i);
            subtreeResponsesMultiple.get(exp).set(i, prelSubtreeResponse);
            aggregatedResponse.add(prelSubtreeResponse);
        }
        aggregatedResponseMultiple.set(exp, aggregatedResponse);
    }

    Plan<V> selectPlanNew(int exp) {
        int selected = planSelector.selectPlanMultiple(globalResponseMultiple.get(exp), prevSelectedPlanMultiple.get(exp), prevAggregatedResponseMultiple.get(exp), aggregatedResponseMultiple.get(exp), this);
        numComputed += planSelector.getNumComputations(this);
        selectedPlan = possiblePlans.get(selected);
        return selectedPlan;
    }

    private UpMessage informParent() {
        List<V> subtreeResponseMultiple = new ArrayList<>();
        for (int exp = 0; exp < experiments.size(); exp++) {
            V subtreeResponse = aggregatedResponseMultiple.get(exp).cloneThis();
            subtreeResponse.add(selectedPlanMultiple.get(exp).getValue());
        }
        return new UpMessage(subtreeResponseMultiple);
    }

//    private void updateGlobalResponse(DownMessage parentMsg) {
//        globalResponse.set(parentMsg.globalResponse);
//    }
    private void updateGlobalResponseMultiple(DownMessage parentMsg) {
        for (int i = 0; i < experiments.size(); i++) {
            globalResponseMultiple.set(i, parentMsg.globalResponseMultiple.get(i));
        }
    }

    private void approveOrRejectChangesMultiple(DownMessage parentMsg) {
        for (int i = 0; i < experiments.size(); i++) {
            if (!parentMsg.approvedMultiple.get(i)) {
                selectedPlanMultiple.set(i, prevSelectedPlanMultiple.get(i));
                aggregatedResponseMultiple.set(i, prevAggregatedResponseMultiple.get(i));

                subtreeResponsesMultiple.get(i).clear();
                subtreeResponsesMultiple.set(i, prevSubtreeResponsesMultiple.get(i));
                Collections.fill(approvals, false);
            }
        }
    }

    private List<DownMessage> informChildren() {
        List<DownMessage> msgs = new ArrayList<>();
            for (int i = 0; i < children.size(); i++) {
                msgs.add(new DownMessage(globalResponseMultiple, approvalsMultiple.get(i)));//Probably wont work Think again
            }
        return msgs;
    }

    // message classes
    class UpMessage extends IterativeTreeAgent.UpMessage {

        public List<V> subtreeResponseMultiple;

        public UpMessage(List<V> subtreeResponseMultiple) {
            this.subtreeResponseMultiple = subtreeResponseMultiple;
        }

        @Override
        public int getNumTransmitted() {
            return 1;
        }
    }

    class DownMessage extends IterativeTreeAgent.DownMessage {

        public List<V> globalResponseMultiple;
        public List<Boolean> approvedMultiple;

        public DownMessage(List<V> globalResponse, List<Boolean> approvedMultiple) {
            this.globalResponseMultiple = globalResponse;
            this.approvedMultiple = approvedMultiple;
        }

        @Override
        public int getNumTransmitted() {
            return 1;
        }
    }
}
