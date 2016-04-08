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
import java.util.List;
import java.util.Random;
import java.util.stream.DoubleStream;

/**
 * minimize variance (submodular/convex compared to std deviation)
 * weight B according to optimum without aggregate and equal Bi
 * @author Peter
 */
public abstract class IterMinVar extends IterativeFitnessFunction {

    public IterMinVar(PlanCombinator combinatorG, PlanCombinator combinatorA, PlanCombinator combinatorS, PlanCombinator combinatorSC) {
        super(combinatorG, combinatorA, combinatorS, combinatorSC);
    }
    
    private static double[][] A;
    private static double[] B;

    @Override
    public double getRobustness(Plan plan, Plan costSignal, AgentPlans historic) {
        return Math.sqrt(plan.variance());
        /*Plan c = plan.clone();
        c.subtract(c.avg());
        return c.norm(1);*/
        /*int n = plan.getNumberOfStates();
        if(A == null) {
            Random rand = new Random(1);
            A = new double[n][n];
            B = new double[n];
            
            for(int i=0; i<n; i++) {
                for(int j=0; j<n; j++) {
                    A[i][j] = rand.nextGaussian();
                }
                B[i] = rand.nextGaussian();
            }
        }
        double v = 0;
        for(int i=0; i<n; i++) {
            for(int j=0; j<n; j++) {
                v += A[i][j]*plan.getValue(i)*plan.getValue(j);
            }
            v += B[i]*plan.getValue(i);
        }
        //System.out.println(v + " " + plan.variance());
        return v;*/
    }
    
    public String getRobustnessMeasure() {
        return "std deviation";
    }

    @Override
    public int select(Agent agent, Plan aggregatePlan, List<Plan> combinationalPlans, Plan pattern) {
        double minVariance = Double.MAX_VALUE;
        int selected = -1;
        int numOpt = 0;

        for (int i = 0; i < combinationalPlans.size(); i++) {
            Plan combinationalPlan = combinationalPlans.get(i);
            Plan testAggregatePlan = new AggregatePlan(agent);
            testAggregatePlan.add(aggregatePlan);
            testAggregatePlan.add(combinationalPlan);

            //double variance = testAggregatePlan.variance();
            double variance = getRobustness(testAggregatePlan, pattern, null);
            if (variance < minVariance) {
                minVariance = variance;
                selected = i;
                numOpt = 1;
            } else if(variance == minVariance) {
                numOpt++;
                if(Math.random()<=1.0/numOpt) {
                    selected = i;
                }
            }
        }

        return selected;
    }

    @Override
    public abstract int select(Agent agent, Plan childAggregatePlan, List<Plan> combinationalPlans, Plan pattern, AgentPlans historic, AgentPlans previous, int numNodes, int numNodesSubtree, int layer, double avgChildren);

}
