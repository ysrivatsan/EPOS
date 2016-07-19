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
import agents.AgentPlans;
import agents.fitnessFunction.costFunction.CostFunction;
import agents.plan.AggregatePlan;
import agents.plan.Plan;
import java.util.List;

/**
 *
 * @author Peter
 */
public class MinCost extends FitnessFunction {
    
    CostFunction costFunc;

    public MinCost(CostFunction costFunc) {
        this.costFunc = costFunc;
    }

    @Override
    public double getRobustness(Plan plan, Plan costSignal, AgentPlans historic) {
        return costFunc.calcCost(plan, costSignal, 0, 0, true);
    }

    @Override
    public String getRobustnessMeasure() {
        return costFunc.getMetric();
    }

    @Override
    public int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, AgentPlans historic) {
        double minCost = Double.POSITIVE_INFINITY;
        int selected = -1;
        int numOpt = 0;

        for (int i = 0; i < plans.size(); i++) {
            Plan combinationalPlan = plans.get(i);
            Plan testAggregatePlan = new AggregatePlan(agent);

            testAggregatePlan.add(aggregate);
            testAggregatePlan.add(combinationalPlan);

            double cost = costFunc.calcCost(testAggregatePlan, costSignal, i, plans.size(), true);
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
}
