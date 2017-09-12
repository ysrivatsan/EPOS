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
public class IeposGlobalGradientPlanSelector implements PlanSelector<IeposAgent<Vector>, Vector> {

    private PlanSelector<? super IeposAgent<Vector>, Vector> initialPlanSelector;
    private DotCostFunction costFunc = new DotCostFunction();

    public IeposGlobalGradientPlanSelector() {
        this.initialPlanSelector = new IeposPlanSelector<>();
    }

    @Override
    public int selectPlan(IeposAgent<Vector> agent) {
        if (agent.iteration == 0) {
            return initialPlanSelector.selectPlan(agent);
        } else {
            DifferentiableCostFunction<Vector> gradientFunction = (DifferentiableCostFunction<Vector>) agent.globalCostFunc;

            Vector gradient = gradientFunction.calcGradient(agent.globalResponse);
            costFunc.setCostVector(gradient);

            return agent.optimization.argmin(costFunc, agent.possiblePlans, agent.lambda);
        }
    }

    @Override
    public int getNumComputations(IeposAgent<Vector> agent) {
        return agent.possiblePlans.size();
    }
    
    @Override
    public int selectPlanMultiple(Vector globalResponse, Plan<Vector> prevSelectedPlan, Vector prevAggregatedResponse, Vector aggregatedResponse, IeposAgent<Vector> agent) {
        return 0; //To change body of generated methods, choose Tools | Templates.
    }
}
