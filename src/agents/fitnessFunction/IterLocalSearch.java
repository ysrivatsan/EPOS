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
import agents.fitnessFunction.iterative.MostRecentCombinator;
import agents.fitnessFunction.iterative.PlanCombinator;
import java.util.List;

/**
 * minimize variance (submodular/convex compared to std deviation) weight B
 * according to optimum without aggregate and equal Bi
 *
 * @author Peter
 */
public class IterLocalSearch extends IterativeFitnessFunction {

    private final PlanCombinator combinator = MostRecentCombinator.getInstance();
    private Plan totalGmA;

    @Override
    public double getRobustness(Plan plan, Plan costSignal, AgentPlans historic) {
        return Math.sqrt(plan.variance());
    }

    @Override
    public void updatePrevious(AgentPlans current, Plan costSignal, int iteration) {
        Plan p = current.global.clone();
        p.subtract(current.aggregate);
        totalGmA = combinator.combine(totalGmA, p, iteration);
    }

    private int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal) {
        double minVariance = Double.MAX_VALUE;
        int selected = -1;

        for (int i = 0; i < plans.size(); i++) {
            Plan combinationalPlan = plans.get(i);
            Plan testAggregatePlan = new AggregatePlan(agent);
            testAggregatePlan.add(aggregate);
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
    public int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, int numNodes, int numNodesSubtree, int layer, double avgChildren, int iteration) {
        Plan modifiedAggregate = aggregate.clone();
        if (iteration > 0) {
            modifiedAggregate.add(totalGmA);
        }
        return select(agent, modifiedAggregate, plans, costSignal);
    }

    @Override
    public String toString() {
        return "IterLocalSearch";
    }
}
