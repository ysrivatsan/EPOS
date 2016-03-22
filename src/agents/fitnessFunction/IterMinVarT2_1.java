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
import agents.energyPlan.CombinationalPlan;
import java.util.ArrayList;
import java.util.List;

/**
 * minimize variance (submodular/convex compared to std deviation)
 *
 * @author Peter
 */
public class IterMinVarT2_1 extends FitnessFunction {

    @Override
    public double getRobustness(Plan plan, Plan costSignal, AgentPlans historic) {
        return Math.sqrt(plan.variance());
    }

    @Override
    public int select(Agent agent, Plan aggregatePlan, List<Plan> combinationalPlans, Plan pattern) {
        double minVariance = Double.MAX_VALUE;
        int selected = -1;

        for (int i = 0; i < combinationalPlans.size(); i++) {
            Plan combinationalPlan = combinationalPlans.get(i);
            Plan testAggregatePlan = new AggregatePlan(agent);
            testAggregatePlan.add(aggregatePlan);
            testAggregatePlan.add(combinationalPlan);

            double variance = testAggregatePlan.variance();
            if (variance < minVariance) {
                minVariance = variance;
                selected = i;
            }
        }

        return selected;
    }

    @Override
    public int select(Agent agent, Plan childAggregatePlan, List<Plan> combinationalPlans, Plan pattern, AgentPlans historic, List<AgentPlans> previous, int numNodes, int numNodesSubtree, int layer, double avgChildren) {
        Plan modifiedChildAggregatePlan = new AggregatePlan(agent);
        
        List<Plan> modifiedCombinationalPlans = new ArrayList<>();
        for(Plan p : combinationalPlans) {
            Plan np = new CombinationalPlan(agent);
            np.set(p);
            np.multiply(Math.pow(avgChildren, layer));
            modifiedCombinationalPlans.add(np);
        }
        if (!previous.isEmpty()) {
            double factor = 1.0 / Math.pow(avgChildren, 1);
            if (!Double.isFinite(factor)) {
                factor = 1;
            }

            for (AgentPlans p : previous) {
                modifiedChildAggregatePlan.add(p.globalPlan);
                Plan allAggregates = new AggregatePlan(agent);
                allAggregates.set(p.aggregatePlan);
                allAggregates.multiply(Math.pow(avgChildren, 1));
                modifiedChildAggregatePlan.subtract(allAggregates);
            }
            modifiedChildAggregatePlan.multiply(factor);
            Plan a = new AggregatePlan(agent);
            a.set(childAggregatePlan);
            a.multiply(Math.pow(avgChildren, 1));
            modifiedChildAggregatePlan.add(a);
        } else {
            Plan a = new AggregatePlan(agent);
            a.set(childAggregatePlan);
            a.multiply(Math.pow(avgChildren, 1));
            modifiedChildAggregatePlan.set(a);
        }
        return select(agent, modifiedChildAggregatePlan, modifiedCombinationalPlans, pattern);
    }

}
