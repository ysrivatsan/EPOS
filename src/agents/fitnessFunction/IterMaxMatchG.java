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

import agents.Agent;
import agents.plan.AggregatePlan;
import agents.plan.Plan;
import agents.AgentPlans;
import agents.fitnessFunction.iterative.Factor;
import agents.fitnessFunction.iterative.NoOpCombinator;
import agents.fitnessFunction.iterative.PlanCombinator;
import agents.plan.GlobalPlan;
import java.util.List;

/**
 * minimize variance (submodular/convex compared to std deviation)
 *
 * @author Peter
 */
public class IterMaxMatchG extends IterativeFitnessFunction {
    private Factor factor;

    public IterMaxMatchG(Factor factor, PlanCombinator combinator) {
        super(combinator, combinator, NoOpCombinator.getInstance(), NoOpCombinator.getInstance());
        this.factor = factor;
    }

    @Override
    public double getRobustness(Plan plan, Plan costSignal, AgentPlans historic) {
        return Math.sqrt(plan.variance());
    }

    @Override
    public int select(Agent agent, Plan childAggregatePlan, List<Plan> combinationalPlans, Plan pattern, AgentPlans historic) {
        double minCost = Double.MAX_VALUE;
        int selected = -1;

        for (int i = 0; i < combinationalPlans.size(); i++) {
            Plan combinationalPlan = combinationalPlans.get(i);
            Plan testAggregatePlan = new AggregatePlan(agent);
            testAggregatePlan.add(childAggregatePlan);
            testAggregatePlan.add(combinationalPlan);

            Plan target = new GlobalPlan(agent);
            target.set(pattern);
            double f1 = 1.0/target.norm();
            if(!Double.isFinite(f1)) {
                f1 = 1.0;
            }
            target.multiply(f1);
            
            double f2 = 1.0/testAggregatePlan.norm();
            if(!Double.isFinite(f2)) {
                f2 = 1.0;
            }
            testAggregatePlan.multiply(f2);
            
            double cost = -Math.abs(testAggregatePlan.dot(target));
            if (cost < minCost) {
                minCost = cost;
                selected = i;
            }
        }
        
        return selected;
    }

    @Override
    public int select(Agent agent, Plan childAggregatePlan, List<Plan> combinationalPlans, Plan pattern, AgentPlans historic, AgentPlans previous, int numNodes, int numNodesSubtree, int layer, double avgChildren) {
        Plan costPlan = new GlobalPlan(agent);
        costPlan.set(1);
        
        Plan x = new GlobalPlan(agent);
        if(previous.globalPlan != null) {
            x.set(previous.globalPlan);
            x.multiply(factor.calcFactor(x, childAggregatePlan, combinationalPlans, pattern, previous, numNodes, numNodesSubtree, layer, avgChildren));
        }
        
        return select(agent, x, combinationalPlans, costPlan, historic);
    }

}
