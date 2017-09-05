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
public class IeposPlanSelector_2<V extends DataType<V>> implements PlanSelector<IeposAgent_Multiple<V>, V> {

    @Override
    public int selectPlan(IeposAgent_Multiple<V> agent) {
        V otherResponse = agent.globalResponse.cloneThis();
        otherResponse.subtract(agent.prevSelectedPlan.getValue());
        otherResponse.subtract(agent.prevAggregatedResponse);
        otherResponse.add(agent.aggregatedResponse);

        return agent.optimization.argmin(agent.globalCostFunc, agent.possiblePlans, otherResponse, agent.lambda);
    }

    @Override
    public int getNumComputations(IeposAgent_Multiple<V> agent) {
        return agent.possiblePlans.size();
    }

}
