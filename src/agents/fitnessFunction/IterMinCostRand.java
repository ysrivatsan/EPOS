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
import agents.fitnessFunction.iterative.Factor;
import agents.Agent;
import agents.plan.Plan;
import agents.AgentPlans;
import agents.fitnessFunction.costFunction.IterativeCostFunction;
import agents.plan.GlobalPlan;
import agents.plan.PossiblePlan;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * minimize variance (submodular/convex compared to std deviation) weight B
 * according to optimum without aggregate and equal Bi
 *
 * @author Peter
 */
public class IterMinCostRand extends IterMinCost {

    private final Factor factor;

    private final PlanCombinator combinator;
    private Plan iterativeCost;
    private Agent agent;

    public IterMinCostRand(IterativeCostFunction costFunc, Factor factor, PlanCombinator combinator) {
        super(costFunc);
        this.factor = factor;
        this.combinator = combinator;
    }
    
    private PrintStream out;

    @Override
    public void afterIteration(AgentPlans current, Plan costSignal, int iteration, int numNodes) {
        if(agent.getPeer().getIndexNumber() == 0) {
            if(iteration == 0) {
                //out = System.out;
                try {
                    out = new PrintStream("output-data/out.m");
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(IterMinCostRand.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            if(iterativeCost == null) {
                out.println("X(:,"+(1+iteration)+") = " + new GlobalPlan(agent) + ";");
            } else {
                out.println("X(:,"+(1+iteration)+") = " + iterativeCost + ";");
            }
            
            Plan gma = current.global.clone();
            gma.subtract(current.aggregate);
            out.println("G(:,"+(1+iteration)+") = " + current.global + ";");
            out.println("GmA(:,"+(1+iteration)+") = " + gma + ";");
        }
        Random r = new SecureRandom((iteration+"").getBytes());
        iterativeCost = new PossiblePlan(agent);
        for(int i = 0; i < iterativeCost.getNumberOfStates(); i++) {
            iterativeCost.setValue(i, r.nextGaussian()*10);
        }
    }

    @Override
    public int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, int numNodes, int numNodesSubtree, int layer, double avgChildren, int iteration) {
        this.agent = agent; //TODO: remove
        Plan iterativeCost = null;

        if (iteration > 0) {
            iterativeCost = this.iterativeCost.clone();
            double f = factor.calcFactor(iterativeCost, plans, numNodes, numNodesSubtree, layer, avgChildren);
            iterativeCost.multiply(f);
        }

        return select(agent, aggregate, plans, costSignal, iterativeCost);
    }

    @Override
    public String toString() {
        return "IterMinCostGmA " + costFunc.toString() + " p+a+" + combinator + "(g-a)*" + factor;
    }
}
