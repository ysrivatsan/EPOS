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

import agents.fitnessFunction.iterative.NoOpCombinator;
import agents.fitnessFunction.iterative.PlanCombinator;
import agents.fitnessFunction.iterative.Factor;
import agents.Agent;
import agents.plan.AggregatePlan;
import agents.plan.Plan;
import agents.AgentPlans;
import java.util.List;

/**
 * minimize variance (submodular/convex compared to std deviation)
 * weight B according to optimum without aggregate and equal Bi
 * @author Peter
 */
public class IterMinVarHGmA extends IterMinVar {
    private final Factor factorG;
    private final Factor factorA;
    
    public IterMinVarHGmA(Factor factorG, Factor factorA, PlanCombinator combinatorG, PlanCombinator combinatorA) {
        super(combinatorG, combinatorA, NoOpCombinator.getInstance(), NoOpCombinator.getInstance());
        this.factorG = factorG;
        this.factorA = factorA;
    }

    @Override
    public int select(Agent agent, Plan childAggregatePlan, List<Plan> combinationalPlans, Plan pattern, AgentPlans historic, AgentPlans previous, int numNodes, int numNodesSubtree, int layer, double avgChildren) {
        Plan targetPlan = new AggregatePlan(agent);
        if(!previous.isEmpty()) {
            targetPlan.set(previous.globalPlan);
            targetPlan.multiply(factorG.calcFactor(targetPlan, childAggregatePlan, combinationalPlans, pattern, previous, numNodes, numNodesSubtree, layer, avgChildren));
            
            Plan modifiedA = new AggregatePlan(agent);
            modifiedA.set(previous.aggregatePlan);
            modifiedA.multiply(factorA.calcFactor(modifiedA, childAggregatePlan, combinationalPlans, pattern, previous, numNodes, numNodesSubtree, layer, avgChildren));
            
            targetPlan.subtract(modifiedA);
            targetPlan.add(childAggregatePlan);
        } else {
            targetPlan.set(childAggregatePlan);
        }
        return select(agent, targetPlan, combinationalPlans, pattern);
    }

    @Override
    public String toString() {
        return "IterMinVar p+a+"+combinatorG+"(g)*" + factorG + "-"+combinatorA+"(a)*"+factorA;
    }
}
