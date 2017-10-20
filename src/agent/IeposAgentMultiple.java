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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * This agent performs the I-EPOS algorithm for combinatorial optimization.
 *
 * @author Peter
 * @param <V> the type of the data this agent should handle
 */
public class IeposAgentMultiple<V extends DataType<V>> extends IterativeTreeAgent<V, IeposAgentMultiple<V>.UpMessage, IeposAgentMultiple<V>.DownMessage> {

    List<Plan<V>> prevSelectedPlanMultiple = new ArrayList<>();
    List<V> aggregatedResponseMultiple = new ArrayList<>();
    List<V> prevAggregatedResponseMultiple = new ArrayList<>();
    List<V> subtreeResponseMultiple = new ArrayList<>();

    // per child info
    private final List<List<V>> subtreeResponsesMultiple = new ArrayList<>();
    private final List<List<V>> prevSubtreeResponsesMultiple = new ArrayList<>();
    private final List<List<Boolean>> approvalsMultiple = new ArrayList<>();
    List<V> globalResponseMultiple = new ArrayList<>();
    List<Plan<V>> selectedPlanMultiple = new ArrayList<>();
    // misc
    Optimization optimization;
    double lambda; // parameter for lambda-PREF local cost minimization
    private PlanSelector<IeposAgentMultiple<V>, V> planSelector;
    int numofAgents;
    int numResponses;
    int subIterations;
    int height;
    boolean agentParticipating = true;
    DataOutputStream output;

    public IeposAgentMultiple(int numIterations, List<Plan<V>> possiblePlans, CostFunction<V> globalCostFunc, PlanCostFunction<V> localCostFunc, AgentLoggingProvider<? extends IeposAgentMultiple<V>> loggingProvider, long seed, int numofAgents, int height, int subIterations, int numResponses, double lambda) {
        super(numIterations, possiblePlans, globalCostFunc, localCostFunc, loggingProvider, seed, true);
        this.optimization = new Optimization(random);
        this.lambda = lambda;
        this.planSelector = new IeposPlanSelectorMultiple<>();
        this.numofAgents = numofAgents;
        this.numResponses = numResponses;
        this.height = height;
        this.subIterations = subIterations;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    public void setPlanSelector(PlanSelector<IeposAgentMultiple<V>, V> planSelector) {
        this.planSelector = planSelector;
    }

    @Override
    public V getGlobalResponse() {
        return globalResponse.cloneThis();
    }

    @Override
    void initPhase() {
        globalResponse = null;
        V temp = createValue();
        Plan<V> temp2 = createPlan();
        for (int i = 0; i < numResponses; i++) {
            aggregatedResponseMultiple.add(i, temp);
            prevAggregatedResponseMultiple.add(i, temp);
            globalResponseMultiple.add(i, temp);
            prevSelectedPlanMultiple.add(i, temp2);
        }
    }

    @Override
    void initIteration() {
        if (iteration > 0) {
            if ((height - 1) * subIterations >= iteration && height >= 3) {
                agentParticipating = false;
            } else {
                agentParticipating = true;
            }
            numAgents = numofAgents;
            prevSubtreeResponsesMultiple.clear();

            for (int j = 0; j < children.size(); j++) {
                prevSubtreeResponsesMultiple.add(new ArrayList(subtreeResponsesMultiple.get(j)));
            }
            for (int i = 0; i < numResponses; i++) {
                prevSelectedPlanMultiple.set(i, selectedPlanMultiple.get(i));
                prevAggregatedResponseMultiple.set(i, aggregatedResponseMultiple.get(i));
                aggregatedResponseMultiple.get(i).reset();
            }
            selectedPlanMultiple.clear();
            subtreeResponsesMultiple.clear();
            approvalsMultiple.clear();
        }
    }

    @Override
    UpMessage up(List<UpMessage> childMsgs) {
        for (UpMessage msg : childMsgs) {
            subtreeResponsesMultiple.add(msg.subtreeResponseMultiple);
        }
//        
//        if (getPeer().getIndexNumber() == 199 ) {
//            System.out.println(iteration + "Subtree Response " + subtreeResponsesMultiple);
//        }
//        if (getPeer().getIndexNumber() == 199 ) {
//            System.out.println(iteration + "Prev Subtree Response" + prevSubtreeResponsesMultiple);
//        }

        if (height > 2 && iteration == ((height - 1) * subIterations) + 1) { // works only for 2 children
            List<Double> SDs = new ArrayList<>();
            List<Integer> selecfinal = new ArrayList<>();
            for (int i = 0; i <= 3; i++) {
                V aggResp = createValue();
                if (i == 0) {
                    aggResp.add(subtreeResponsesMultiple.get(0).get(0));
                    aggResp.add(subtreeResponsesMultiple.get(1).get(0));
                }
                if (i == 1) {
                    aggResp.add(subtreeResponsesMultiple.get(0).get(0));
                    aggResp.add(subtreeResponsesMultiple.get(1).get(1));
                }
                if (i == 2) {
                    aggResp.add(subtreeResponsesMultiple.get(0).get(1));
                    aggResp.add(subtreeResponsesMultiple.get(1).get(0));
                }
                if (i == 3) {
                    aggResp.add(subtreeResponsesMultiple.get(0).get(1));
                    aggResp.add(subtreeResponsesMultiple.get(1).get(1));
                }

                int selec = 0;
                double minSD = 0;
                for (int j = 0; j < possiblePlans.size(); j++) {
                    V subResp = createValue();
                    subResp.add(aggResp);
                    subResp.add(possiblePlans.get(j).getValue());
                    Vector v = (Vector) subResp;
                    if (j == 0) {
                        minSD = v.std();
                    }
                    if (v.std() < minSD) {
                        selec = j;
                        minSD = v.std();
                    }
                }
                selecfinal.add(i, selec);
                SDs.add(i, minSD);
            }

            int minindex1 = 0;
            int minindex2 = 1;
            double minSd1 = SDs.get(0);
            double minSd2 = SDs.get(1);

            for (int i = 2; i < SDs.size(); i++) {
                if (SDs.get(i) < minSd1) {
                    minindex2 = minindex1;
                    minSd2 = minSd1;
                    minindex1 = i;
                    minSd1 = SDs.get(i);
                } else if (SDs.get(i) < minSd2) {
                    minindex2 = i;
                    minSd2 = SDs.get(i);
                }
            }
            V aggtemp1 = createValue();
            V aggtemp2 = createValue();
            List<Boolean> approvalsTemp = new ArrayList<>();
            aggtemp1.add(subtreeResponsesMultiple.get(0).get(minindex1 / 2));
            aggtemp1.add(subtreeResponsesMultiple.get(1).get(minindex1 % 2));
            aggtemp2.add(subtreeResponsesMultiple.get(0).get(minindex2 / 2));
            aggtemp2.add(subtreeResponsesMultiple.get(1).get(minindex2 % 2));
            aggregatedResponseMultiple.set(0, aggtemp1);
            aggregatedResponseMultiple.set(1, aggtemp2);
            for (int exp = 0; exp < numResponses; exp++) {
                for (int i = 0; i < children.size(); i++) {
                    approvalsTemp.add(true);
                }
                approvalsMultiple.add(exp, new ArrayList(approvalsTemp));
                approvalsTemp.clear();
            }
            selectedPlanMultiple.add(0, possiblePlans.get(selecfinal.get(minindex1)));
            selectedPlanMultiple.add(1, possiblePlans.get(selecfinal.get(minindex2)));

        } else {
            for (int i = 0; i < numResponses; i++) {
                aggregateNew(i);
                selectedPlanMultiple.add(i, selectPlanNew(i));
            }
            if (!agentParticipating || (height == 2 && iteration <= subIterations)) {
                for (int i = 0; i < selectedPlanMultiple.size(); i++) {
                    if (height == 2 && i != 0) {
                        continue;
                    }
                    selectedPlanMultiple.set(i, createPlan());
                }
            }
        }
        return informParent();
    }

    @Override
    DownMessage atRoot(UpMessage rootMsg) {
        List<Boolean> temp_list = new ArrayList<>();
        for (int i = 0; i < numResponses; i++) {
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
        List<Boolean> approvalsTemp = new ArrayList<>();
        V aggregatedResponseTemp = createValue();
        if (iteration == 0 || !agentParticipating || height == 2 && iteration <= subIterations && exp == 0) {
            for (int i = 0; i < children.size(); i++) {
                approvalsTemp.add(true);
            }
            approvalsMultiple.add(exp, new ArrayList(approvalsTemp));
            approvalsTemp.clear();
        } else if (children.size() > 0) {
            //if(getPeer().getIndexNumber() == 100) System.out.println("--- Here now --- ");
            List<V> prevSubtreeResponseTemp = new ArrayList<>();
            List<List<V>> choicesPerAgent = new ArrayList<>();
            for (int i = 0; i < children.size(); i++) {
                List<V> choices = new ArrayList<>();
                choices.add(prevSubtreeResponsesMultiple.get(i).get(exp));
                choices.add(subtreeResponsesMultiple.get(i).get(exp));
                choicesPerAgent.add(choices);
                prevSubtreeResponseTemp.add(prevSubtreeResponsesMultiple.get(i).get(exp));
            }
            List<V> combinations = optimization.calcAllCombinations(choicesPerAgent);
            V othersResponse = globalResponseMultiple.get(exp).cloneThis();

            for (V prevSubtreeResponse : prevSubtreeResponseTemp) {
                othersResponse.subtract(prevSubtreeResponse);
            }
            int selectedCombination = optimization.argmin(globalCostFunc, combinations, othersResponse);
            numComputed += combinations.size();
            List<Integer> selections = optimization.combinationToSelections(selectedCombination, choicesPerAgent);
            for (int selection : selections) {
                approvalsTemp.add(selection == 1);
            }
//            if (getPeer().getIndexNumber() == 24) {
//                System.out.println(exp + " ::: " + approvalsTemp);
//            }
            approvalsMultiple.add(exp, new ArrayList(approvalsTemp));
//            if (getPeer().getIndexNumber() == 999 && exp == 0) {
//                System.out.println(iteration + " -- " + approvalsMultiple);
//            }
            approvalsTemp.clear();
        }

        for (int i = 0; i < children.size(); i++) {
            V prelSubtreeResponse;
            if (prevSubtreeResponsesMultiple.size() > 0) {
                prelSubtreeResponse = approvalsMultiple.get(exp).get(i) ? subtreeResponsesMultiple.get(i).get(exp) : prevSubtreeResponsesMultiple.get(i).get(exp);
            } else {
                prelSubtreeResponse = subtreeResponsesMultiple.get(i).get(exp);
            }
            subtreeResponsesMultiple.get(i).set(exp, prelSubtreeResponse);
            aggregatedResponseTemp.add(prelSubtreeResponse);
        }

        aggregatedResponseMultiple.set(exp, aggregatedResponseTemp.cloneThis());
        // if(height == 4)  System.out.println(aggregatedResponseMultiple);
        aggregatedResponseTemp.reset();
    }

    Plan<V> selectPlanNew(int exp) {
        int selected = planSelector.selectPlanMultiple(globalResponseMultiple.get(exp), prevSelectedPlanMultiple.get(exp), prevAggregatedResponseMultiple.get(exp), aggregatedResponseMultiple.get(exp), this);
        numComputed += planSelector.getNumComputations(this);
        selectedPlan = possiblePlans.get(selected);
//        if (iteration == 10 && (getPeer().getIndexNumber() == 489 || getPeer().getIndexNumber() == 490)&&exp == 0) {
//            System.out.println("Agent: " + getPeer().getIndexNumber() + " " + exp + " :: " + selectedPlan.getValue());
//        }
//        if (iteration == numIterations - 1) {
//            System.out.println("Agent: " + getPeer().getIndexNumber() + " " + exp + " :: " + selected);
//        }
//        if (iteration == numIterations - 1) {
//            System.out.println("Agent: " + getPeer().getIndexNumber() + " " + exp + " :: " + selectedPlan.getValue());
//        }
        return selectedPlan;
    }

    private UpMessage informParent() {
        subtreeResponseMultiple = new ArrayList<>();
        List<V> upmsg = new ArrayList<>();
        for (int exp = 0; exp < numResponses; exp++) {
            V subtreeResponse = aggregatedResponseMultiple.get(exp).cloneThis();
            subtreeResponse.add(selectedPlanMultiple.get(exp).getValue());
            subtreeResponseMultiple.add(exp, subtreeResponse.cloneThis());
            if ((height == 1 && iteration < subIterations && exp == 0) || !agentParticipating || (height >= 2 && iteration <= (height) * subIterations)) { // agent has to pass 0 to its parents
                subtreeResponse.reset();
                upmsg.add(exp, subtreeResponse.cloneThis());
            } else {
                upmsg.add(exp, subtreeResponse.cloneThis());
            }
        }
        return new UpMessage(upmsg);
    }

    private void updateGlobalResponseMultiple(DownMessage parentMsg) {

        for (int i = 0; i < numResponses; i++) {
            if ((height == 1 && iteration <= subIterations && i == 0) || height >= 2 && iteration <= height * subIterations) {
                globalResponseMultiple.set(i, subtreeResponseMultiple.get(i).cloneThis());
            } else {
                globalResponseMultiple.set(i, parentMsg.globalResponseMultiple.get(i).cloneThis());
            }
        }
        if (getPeer().getIndexNumber() == numAgents - 1 && iteration >= numIterations - 20) {
            System.out.println(iteration + " glob resp -0- " + globalCostFunc.calcCost(globalResponseMultiple.get(0)));
            System.out.println(iteration + " glob resp -1- " + globalCostFunc.calcCost(globalResponseMultiple.get(1)));
        }
    }

    private void approveOrRejectChangesMultiple(DownMessage parentMsg) {

        for (int i = 0; i < numResponses; i++) {
            if (!parentMsg.approvedMultiple.get(i)) {
                selectedPlanMultiple.set(i, prevSelectedPlanMultiple.get(i));
                aggregatedResponseMultiple.set(i, prevAggregatedResponseMultiple.get(i));
                for (int j = 0; j < children.size(); j++) {
                    subtreeResponsesMultiple.get(j).set(i, prevSubtreeResponsesMultiple.get(j).get(i));
                }
                Collections.fill(approvalsMultiple.get(i), false);
            }
        }
    }

    private List<DownMessage> informChildren() {

        List<DownMessage> msgs = new ArrayList<>();
        List<Boolean> approvedMultipleTemp = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            for (int exp = 0; exp < numResponses; exp++) {
                approvedMultipleTemp.add(exp, approvalsMultiple.get(exp).get(i));
            }
            msgs.add(new DownMessage(globalResponseMultiple, new ArrayList(approvedMultipleTemp)));
            approvedMultipleTemp.clear();
        }
//        if (getPeer().getIndexNumber() == 489){
//            System.out.println(children);
//        }
//        if (iteration == height*subIterations && height >=2) {
//            System.out.println(iteration + " glob resp -0- " +getPeer().getIndexNumber()+ " : "+ globalCostFunc.calcCost(globalResponseMultiple.get(0)));
//            System.out.println(iteration + " glob resp -1- " +getPeer().getIndexNumber()+ " : "+ globalCostFunc.calcCost(globalResponseMultiple.get(1)));
//        }
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
