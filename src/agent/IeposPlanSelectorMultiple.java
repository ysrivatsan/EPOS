/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import data.DataType;
import data.Plan;

/**
 *
 * @author Peter
 * @param <V>
 */
public class IeposPlanSelectorMultiple<V extends DataType<V>> implements PlanSelector<IeposAgentMultiple<V>, V> {

    @Override
    public int selectPlan(IeposAgentMultiple<V> agent) {
//        V otherResponse = agent.globalResponse.cloneThis();
//        otherResponse.subtract(agent.prevSelectedPlan.getValue());
//        otherResponse.subtract(agent.prevAggregatedResponse);
//        otherResponse.add(agent.aggregatedResponse);
//        return agent.optimization.argmin(agent.globalCostFunc, agent.possiblePlans, otherResponse, agent.lambda);
        return 0;
    }

    @Override
    public int selectPlanMultiple(V globalResponse, Plan<V> prevSelectedPlan, V prevAggregatedResponse, V aggregatedResponse, IeposAgentMultiple<V> agent) {
        V otherResponse = globalResponse.cloneThis();
        otherResponse.subtract(prevSelectedPlan.getValue());
        otherResponse.subtract(prevAggregatedResponse);
        otherResponse.add(aggregatedResponse);
        return agent.optimization.argmin(agent.globalCostFunc, agent.possiblePlans, otherResponse, agent.lambda);
    }

    @Override
    public int getNumComputations(IeposAgentMultiple<V> agent) {
        return agent.possiblePlans.size();
    }
}
