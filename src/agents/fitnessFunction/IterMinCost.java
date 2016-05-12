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
import agents.Agent;
import agents.plan.AggregatePlan;
import agents.plan.Plan;
import agents.AgentPlans;
import agents.fitnessFunction.costFunction.CostFunction;
import java.util.List;
import java.util.Random;

/**
 * minimize variance (submodular/convex compared to std deviation)
 * weight B according to optimum without aggregate and equal Bi
 * @author Peter
 */
public abstract class IterMinCost extends IterativeFitnessFunction {
    CostFunction costFunc;

    public IterMinCost(CostFunction costFunc, PlanCombinator combinatorG, PlanCombinator combinatorA, PlanCombinator combinatorS, PlanCombinator combinatorSC) {
        super(combinatorG, combinatorA, combinatorS, combinatorSC);
        this.costFunc = costFunc;
    }

    @Override
    public double getRobustness(Plan plan, Plan costSignal, AgentPlans historic) {
        return costFunc.calcCost(plan, costSignal);
    }
    
    public String getRobustnessMeasure() {
        return costFunc.getMetric();
    }

    @Override
    public Plan calcGradient(Plan plan) {
        return costFunc.calcGradient(plan);
    }

    @Override
    public int select(Agent agent, Plan aggregatePlan, List<Plan> combinationalPlans, Plan pattern) {
        double minCost = Double.POSITIVE_INFINITY;
        int selected = -1;
        int numOpt = 0;

        for (int i = 0; i < combinationalPlans.size(); i++) {
            Plan combinationalPlan = combinationalPlans.get(i);
            Plan testAggregatePlan = new AggregatePlan(agent);
            
            //WTF
            Plan plan = combinationalPlan.clone();
            Random r = new Random(i);
            for(int j = 0; j < plan.getNumberOfStates(); j++) {
                plan.setValue(j, plan.getValue(j) + r.nextGaussian()/100.0);
            }
            combinationalPlan = plan;/**/
            
            testAggregatePlan.add(aggregatePlan);
            testAggregatePlan.add(combinationalPlan);

            double cost = getRobustness(testAggregatePlan, pattern, null);
            if (cost < minCost) {
                minCost = cost;
                selected = i;
                numOpt = 1;
            } else if(cost == minCost) {
                numOpt++;
                if(Math.random()<=1.0/numOpt) {
                    selected = i;
                }
            }
        }

        return selected;
    }

    @Override
    public abstract int select(Agent agent, Plan childAggregatePlan, List<Plan> combinationalPlans, Plan pattern, AgentPlans historic, AgentPlans previous, int numNodes, int numNodesSubtree, int layer, double avgChildren, int iteration);

}
