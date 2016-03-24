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
import agents.fitnessFunction.iterative.NoOpCombinator;
import agents.fitnessFunction.iterative.PlanCombinator;
import agents.plan.GlobalPlan;
import java.util.List;

/**
 * minimize variance (submodular/convex compared to std deviation)
 *
 * @author Peter
 */
public class IterMinCost2 extends IterativeFitnessFunction {

    public IterMinCost2(PlanCombinator combinator) {
        super(combinator, combinator, NoOpCombinator.getInstance(), NoOpCombinator.getInstance());
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
            target.multiply(1.0/target.norm());
            
            testAggregatePlan.multiply(1.0/testAggregatePlan.norm());
            
            double aSqr = Math.abs(testAggregatePlan.dot(target));
            aSqr = aSqr*aSqr;
            double cSqr = testAggregatePlan.normSqr();
            double cost = cSqr-aSqr;
            //double cost = Math.acos(Math.sqrt(aSqr));
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
        x.set(childAggregatePlan);
        if(previous.globalPlan != null) {
            x.add(previous.globalPlan);
        }

        return select(agent, x, combinationalPlans, costPlan, historic);
    }

}
