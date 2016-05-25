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
import agents.fitnessFunction.costFunction.IterativeCostFunction;
import agents.fitnessFunction.iterative.NoOpCombinator;
import java.util.List;

/**
 * minimize variance (submodular/convex compared to std deviation)
 * weight B according to optimum without aggregate and equal Bi
 * @author Peter
 */
public class IterMinCostGmA extends IterMinCost {
    private final Factor factor;
    
    private final PlanCombinator combinator;
    private Plan totalGmAGradient;
    
    public IterMinCostGmA(IterativeCostFunction costFunc, Factor factor, PlanCombinator combinator) {
        super(costFunc, combinator, combinator, NoOpCombinator.getInstance(), NoOpCombinator.getInstance());
        this.factor = factor;
        this.combinator = combinator;
    }

    @Override
    public void updatePrevious(AgentPlans previous, AgentPlans current, Plan costSignal, int iteration) {
        super.updatePrevious(previous, current, costSignal, iteration);
        
        Plan p = current.global.clone();
        p.subtract(current.aggregate);
        totalGmAGradient = combinator.combine(totalGmAGradient, costFunc.calcGradient(p, costSignal), iteration);
    }
    
    @Override
    public int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, int numNodes, int numNodesSubtree, int layer, double avgChildren, int iteration) {
        Plan iterativeCost = null;
        
        if(iteration > 0) {
            iterativeCost = totalGmAGradient.clone();
            double f = factor.calcFactor(iterativeCost, plans, numNodes, numNodesSubtree, layer, avgChildren);
            iterativeCost.multiply(f);
        }
        
        return select(agent, aggregate, plans, costSignal, iterativeCost);
    }

    @Override
    public String toString() {
        return "IterMinCostGmA "+costFunc.toString()+" p+a+"+combinatorG+"(g-a)*" + factor;
    }
}
