/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import data.Vector;
import func.DifferentiableCostFunction;
import func.DotCostFunction;

/**
 *
 * @author Peter
 */
public class IeposIndividualGradientPlanSelector_Multiple implements PlanSelector<IeposAgent_Multiple<Vector>, Vector> {

    private PlanSelector<? super IeposAgent_Multiple<Vector>, Vector> initialPlanSelector;
    private DotCostFunction costFunc = new DotCostFunction();

    public IeposIndividualGradientPlanSelector_Multiple() {
        this.initialPlanSelector = new IeposPlanSelector_2<>();
    }

    @Override
    public int selectPlan(IeposAgent_Multiple<Vector> agent) {
        if (agent.iteration == 0) {
            return initialPlanSelector.selectPlan(agent);
        } else {
            DifferentiableCostFunction<Vector> gradientFunction = (DifferentiableCostFunction<Vector>) agent.globalCostFunc;
            Vector otherResponse = agent.globalResponse.cloneThis();
            otherResponse.subtract(agent.prevSelectedPlan.getValue());
            otherResponse.multiply(agent.numAgents / (agent.numAgents - 1));

            Vector gradient = gradientFunction.calcGradient(otherResponse);
            costFunc.setCostVector(gradient);

            return agent.optimization.argmin(costFunc, agent.possiblePlans, agent.lambda);
        }
    }

    @Override
    public int getNumComputations(IeposAgent_Multiple<Vector> agent) {
        return agent.possiblePlans.size();
    }

}
