/*
 * Copyright (C) 2016 Evangelos Pournaras
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package agents.fitnessFunction;

import agents.fitnessFunction.iterative.PlanCombinator;
import agents.fitnessFunction.iterative.Factor;
import agents.Agent;
import agents.plan.AggregatePlan;
import agents.plan.Plan;
import agents.AgentPlans;
import agents.fitnessFunction.costFunction.CostFunction;
import agents.fitnessFunction.iterative.NoOpCombinator;
import java.util.List;

/**
 * minimize variance (submodular/convex compared to std deviation)
 * weight B according to optimum without aggregate and equal Bi
 * @author Peter
 */
public class IterMinCostGmA extends IterMinCost {
    private final Factor factor;
    
    public IterMinCostGmA(CostFunction costFunc, Factor factor, PlanCombinator combinator) {
        super(costFunc, combinator, combinator, NoOpCombinator.getInstance(), NoOpCombinator.getInstance());
        this.factor = factor;
    }
    
    @Override
    public int select(Agent agent, Plan childAggregatePlan, List<Plan> combinationalPlans, Plan costSignal, AgentPlans historic, AgentPlans previous, int numNodes, int numNodesSubtree, int layer, double avgChildren, int iteration) {
        Plan modifiedCostSignal = new AggregatePlan(agent);
        if(!previous.isEmpty()) {
            modifiedCostSignal.add(previous.globalPlan);
            modifiedCostSignal.subtract(previous.aggregatePlan);
            modifiedCostSignal.multiply(factor.calcFactor(modifiedCostSignal, childAggregatePlan, combinationalPlans, costSignal, previous, numNodes, numNodesSubtree, layer, avgChildren));
            
            Plan c = costSignal.clone();
            if(numNodesSubtree < numNodes) {
                c.multiply(numNodesSubtree/(double)numNodes + iteration*numNodesSubtree/(double)(numNodes-numNodesSubtree));
            }
            modifiedCostSignal.add(c);
        }
        
        return select(agent, childAggregatePlan, combinationalPlans, modifiedCostSignal);
    }

    @Override
    public String toString() {
        return "IterMinCostGmA "+costFunc.toString()+" p+a+"+combinatorG+"(g-a)*" + factor;
    }
}
