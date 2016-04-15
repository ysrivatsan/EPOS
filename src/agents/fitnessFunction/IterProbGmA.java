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
import java.util.Arrays;
import java.util.List;

/**
 * minimize variance (submodular/convex compared to std deviation)
 *
 * @author Peter
 */
public class IterProbGmA extends IterativeFitnessFunction {

    private Factor factor;
    private double[] prevProbs;

    public IterProbGmA(Factor factor, PlanCombinator combinator) {
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

        if (prevProbs == null) {
            prevProbs = new double[combinationalPlans.size()];
            Arrays.fill(prevProbs, 1);
        }

        double cumProb = 0;
        double[] probs = new double[combinationalPlans.size()];
        for (int i = 0; i < combinationalPlans.size(); i++) {
            Plan combinationalPlan = combinationalPlans.get(i);
            Plan testAggregatePlan = new AggregatePlan(agent);
            testAggregatePlan.add(combinationalPlan);

            Plan target = new GlobalPlan(agent);
            target.set(pattern);
            target.subtract(target.min());
            
            //target.pow(2);
            double f1 = 1.0/target.norm();
            if(!Double.isFinite(f1)) {
                f1 = 1.0;
            }
            target.multiply(f1);
            
            testAggregatePlan.subtract(testAggregatePlan.min());
            double f2 = 1.0 / testAggregatePlan.norm();
            if (!Double.isFinite(f2)) {
                f2 = 1.0;
            }
            testAggregatePlan.multiply(f2);

            double cost = testAggregatePlan.dot(target);
            /*if(cost < minCost) {
                minCost = cost;
                selected = i;
            }*/
            
            //double prob = 1-cost*cost;
            //double prob = 1/(0.0001+cost*cost);
            //double prob = Math.exp(-cost*cost*400);
            double prob = Math.exp(-cost*400);
            probs[i] = prevProbs[i] * prob;
            cumProb += probs[i];
        }
        
        for(int i=0; i<combinationalPlans.size(); i++) {
            probs[i] /= cumProb;
            if(Double.isNaN(probs[i])) {
                probs[i] = 1.0/combinationalPlans.size();
            }
        }

        double rand = Math.random();
        cumProb = 0;
        for (int i = 0; i < combinationalPlans.size(); i++) {
            cumProb += probs[i];
            if (rand <= cumProb) {
                selected = i;
                break;
            }
        }

        prevProbs = probs;

        return selected;
    }

    @Override
    public int select(Agent agent, Plan childAggregatePlan, List<Plan> combinationalPlans, Plan pattern, AgentPlans historic, AgentPlans previous, int numNodes, int numNodesSubtree, int layer, double avgChildren, int iteration) {
        Plan incentive = new GlobalPlan(agent);
        incentive.set(0);

        Plan x = new GlobalPlan(agent);
        if (previous.globalPlan != null) {
            x.set(previous.globalPlan);
            x.subtract(previous.aggregatePlan);
            x.multiply(factor.calcFactor(x, childAggregatePlan, combinationalPlans, pattern, previous, numNodes, numNodesSubtree, layer, avgChildren));
        }
        x.add(childAggregatePlan);
        incentive.add(x);

        return select(agent, childAggregatePlan, combinationalPlans, incentive, historic);
    }

    @Override
    public String toString() {
        return "IterProbGmA a+" + combinatorG + "(g-a)*" + factor;
    }

    @Override
    public IterProbGmA clone() {
        IterProbGmA clone = (IterProbGmA) super.clone();
        clone.factor = factor;
        clone.prevProbs = null;
        return clone;
    }
}
