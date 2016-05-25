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
import agents.fitnessFunction.costFunction.IterativeCostFunction;
import java.util.List;
import java.util.Random;

/**
 * minimize variance (submodular/convex compared to std deviation) weight B
 * according to optimum without aggregate and equal Bi
 *
 * @author Peter
 */
public abstract class IterMinCost extends IterativeFitnessFunction {

    IterativeCostFunction costFunc;
    public Double rampUpBias;

    public IterMinCost(IterativeCostFunction costFunc) {
        this.costFunc = costFunc;
    }

    @Override
    public double getRobustness(Plan plan, Plan costSignal, AgentPlans historic) {
        return costFunc.calcCost(plan, costSignal, 0, 0);
    }

    @Override
    public String getRobustnessMeasure() {
        return costFunc.getMetric();
    }

    public int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, Plan iterativeCost) {
        double minCost = Double.POSITIVE_INFINITY;
        int selected = -1;
        int numOpt = 0;

        Random random = null;
        if(rampUpBias != null) {
            random = new Random(agent.getPeer().getIndexNumber());
        }

        for (int i = 0; i < plans.size(); i++) {
            Plan combinationalPlan = plans.get(i);
            Plan testAggregatePlan = new AggregatePlan(agent);

            testAggregatePlan.add(aggregate);
            testAggregatePlan.add(combinationalPlan);

            double cost = costFunc.calcCost(testAggregatePlan, costSignal, iterativeCost);
            if (rampUpBias != null && !agent.isRoot()) {
                cost *= (1 + rampUpBias * i/(double)combinationalPlan.getNumberOfStates());
            }
            if (cost < minCost) {
                minCost = cost;
                selected = i;
                numOpt = 1;
            } else if (cost == minCost) {
                numOpt++;
                if (Math.random() <= 1.0 / numOpt) {
                    selected = i;
                }
            }
        }

        return selected;
    }

    @Override
    public abstract int select(Agent agent, Plan childAggregatePlan, List<Plan> combinationalPlans, Plan pattern, int numNodes, int numNodesSubtree, int layer, double avgChildren, int iteration);

}
