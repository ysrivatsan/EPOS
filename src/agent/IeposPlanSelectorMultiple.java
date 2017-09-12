/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import data.DataType;

/**
 *
 * @author Peter
 */
public class IeposPlanSelectorMultiple<V extends DataType<V>> implements PlanSelector<IeposAgentMultiple<V>, V> {

    @Override
    public int selectPlan(IeposAgentMultiple<V> agent) {
        V otherResponse = agent.globalResponse.cloneThis();
        otherResponse.subtract(agent.prevSelectedPlan.getValue());
        otherResponse.subtract(agent.prevAggregatedResponse);
        otherResponse.add(agent.aggregatedResponse);

        return agent.optimization.argmin(agent.globalCostFunc, agent.possiblePlans, otherResponse, agent.lambda);
    }
    public int selectPlanNew() {
        V otherResponse = agent.globalResponse.cloneThis();
        otherResponse.subtract(agent.prevSelectedPlan.getValue());
        otherResponse.subtract(agent.prevAggregatedResponse);
        otherResponse.add(agent.aggregatedResponse);

        return agent.optimization.argmin(agent.globalCostFunc, agent.possiblePlans, otherResponse, agent.lambda);
    }

    @Override
    public int getNumComputations(IeposAgentMultiple<V> agent) {
        return agent.possiblePlans.size();
    }

}
