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
import agents.energyPlan.AggregatePlan;
import agents.energyPlan.Plan;
import agents.AgentPlans;
import agents.energyPlan.GlobalPlan;
import java.util.List;

/**
 * minimize variance (submodular/convex compared to std deviation)
 * @author Peter
 */
public class IterMinCost extends FitnessFunction {

    @Override
    public double getRobustness(Plan plan, Plan costSignal, AgentPlans historic) {
        return Math.sqrt(plan.variance());
    }

    @Override
    public int select(Agent agent, Plan childAggregatePlan, List<Plan> combinationalPlans, Plan pattern, AgentPlans historic, List<AgentPlans> previous, int numNodes, int numNodesSubtree, int layer, double avgChildren) {
        double minCost = Double.MAX_VALUE;
        int selected = -1;

        for (int i = 0; i < combinationalPlans.size(); i++) {
            Plan combinationalPlan = combinationalPlans.get(i);
            Plan testAggregatePlan = new AggregatePlan(agent);
            testAggregatePlan.add(childAggregatePlan);
            testAggregatePlan.add(combinationalPlan);
            
            Plan costPlan = new GlobalPlan(agent);
            for(AgentPlans p : previous) {
                costPlan.add(p.globalPlan);
                Plan x = new AggregatePlan(agent);
                x.add(p.aggregatePlan);
                x.multiply(Math.pow(avgChildren, layer));
                costPlan.add(x);
            }
            //costPlan.pow(0.5);
            
            testAggregatePlan.multiply(costPlan);
            double cost = testAggregatePlan.sum();
            if (cost < minCost) {
                minCost = cost;
                selected = i;
            }
        }

        return selected;
    }

}
