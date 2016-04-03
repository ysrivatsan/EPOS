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
import agents.fitnessFunction.iterative.MostRecentCombinator;
import agents.fitnessFunction.iterative.NoOpCombinator;
import agents.fitnessFunction.iterative.PlanCombinator;
import agents.plan.CombinationalPlan;
import agents.plan.Plan;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Peter
 */
public class IterUCB1Bandit extends IterativeFitnessFunction {

    private double[] estimate;
    private double[] timesSelected;
    private int iteration = 0;
    private int prevSelected;
    
    private double minVar = Double.MAX_VALUE;

    public IterUCB1Bandit() {
        super(new MostRecentCombinator(), NoOpCombinator.getInstance(), NoOpCombinator.getInstance(), new MostRecentCombinator());
    }

    @Override
    public double getRobustness(Plan plan, Plan costSignal, AgentPlans historic) {
        return Math.sqrt(plan.variance());
    }

    @Override
    public int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, AgentPlans historic, AgentPlans previous) {
        int selected = -1;
        
        if (iteration == 0) {
            estimate = new double[plans.size()];
            timesSelected = new double[plans.size()];

            selected = (int) (Math.random() * plans.size());
        } else {
            double prevVar = previous.globalPlan.variance();
            if(prevVar < minVar) {
                for(int i=0;i<estimate.length;i++) {
                    estimate[i] *= prevVar / minVar;
                }
                minVar = prevVar;
            }
            //minVar = Math.min(minVar, prevVar);
            double reward = minVar / prevVar;
            
            estimate[prevSelected] += (reward - estimate[prevSelected]) / timesSelected[prevSelected];

            double maxUCB = 0;
            int numOpt = 0;
            for (int i = 0; i < plans.size(); i++) {
                double UCB = estimate[i] + Math.sqrt(2.0 * Math.log(iteration) / timesSelected[i]);
                if (UCB > maxUCB) {
                    maxUCB = UCB;
                    selected = i;
                    numOpt = 1;
                } else if(UCB == maxUCB) {
                    numOpt++;
                    if(Math.random()<=1.0/numOpt) {
                        selected = i;
                    }
                }
            }
        }
        iteration++;

        timesSelected[selected]++;
        prevSelected = selected;
        return selected;
    }

    @Override
    public String toString() {
        return "IterUCB1Bandit";
    }
}
