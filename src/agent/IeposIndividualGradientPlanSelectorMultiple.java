/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import data.Plan;
import data.Vector;
import func.DifferentiableCostFunction;
import func.DotCostFunction;

/**
 *
 * @author Peter
 */
public class IeposIndividualGradientPlanSelectorMultiple implements PlanSelector<IeposAgentMultiple<Vector>, Vector> {

    private PlanSelector<? super IeposAgentMultiple<Vector>, Vector> initialPlanSelector = new IeposPlanSelectorMultiple<>();
    ;
    private DotCostFunction costFunc = new DotCostFunction();

    public IeposIndividualGradientPlanSelectorMultiple() {
        this.initialPlanSelector = new IeposPlanSelectorMultiple<>();
    }

    @Override
    public int selectPlan(IeposAgentMultiple<Vector> agent) {
//        if (agent.iteration == 0) {
//            return initialPlanSelector.selectPlan(agent);
//        } else {
//            DifferentiableCostFunction<Vector> gradientFunction = (DifferentiableCostFunction<Vector>) agent.globalCostFunc;
//            Vector otherResponse = agent.globalResponse.cloneThis();
//            otherResponse.subtract(agent.prevSelectedPlan.getValue());
//            otherResponse.multiply(agent.numAgents / (agent.numAgents - 1));
//
//            Vector gradient = gradientFunction.calcGradient(otherResponse);
//            costFunc.setCostVector(gradient);
//
//            return agent.optimization.argmin(costFunc, agent.possiblePlans, agent.lambda);
//        }
        return 0;
    }

    @Override
    public int selectPlanMultiple(Vector globalResponse, Plan<Vector> prevSelectedPlan, Vector prevAggregatedResponse, Vector aggregatedResponse, IeposAgentMultiple<Vector> agent) {
        if (agent.iteration == 0) {
            return initialPlanSelector.selectPlanMultiple(globalResponse, prevSelectedPlan, prevAggregatedResponse, aggregatedResponse, agent);
        } else {
            DifferentiableCostFunction<Vector> gradientFunction = (DifferentiableCostFunction<Vector>) agent.globalCostFunc;
            Vector otherResponse = globalResponse.cloneThis();
            otherResponse.subtract(prevSelectedPlan.getValue());
            otherResponse.multiply(agent.numAgents / (agent.numAgents - 1));
            Vector gradient = gradientFunction.calcGradient(otherResponse);
            costFunc.setCostVector(gradient);
            return agent.optimization.argmin(costFunc, agent.possiblePlans, agent.lambda);
        }
    }

    @Override
    public int getNumComputations(IeposAgentMultiple<Vector> agent) {
        return agent.possiblePlans.size();
    }

}
