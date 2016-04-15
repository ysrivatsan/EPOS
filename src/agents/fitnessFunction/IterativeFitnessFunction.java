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
import agents.fitnessFunction.iterative.PlanCombinator;
import agents.AgentPlans;
import agents.plan.Plan;
import java.util.List;

/**
 *
 * @author Peter
 */
public abstract class IterativeFitnessFunction extends FitnessFunction implements Cloneable {
    public PlanCombinator combinatorG;
    public PlanCombinator combinatorA;
    public PlanCombinator combinatorS;
    public PlanCombinator combinatorSC;
    
    public IterativeFitnessFunction(PlanCombinator combinatorG, PlanCombinator combinatorA, PlanCombinator combinatorS, PlanCombinator combinatorSC) {
        this.combinatorG = combinatorG;
        this.combinatorA = combinatorA;
        this.combinatorS = combinatorS;
        this.combinatorSC = combinatorSC;
    }
    
    public void updatePrevious(AgentPlans previous, AgentPlans current, int iteration) {
        previous.globalPlan = combinatorG.combine(previous.globalPlan, current.globalPlan, iteration);
        previous.aggregatePlan = combinatorA.combine(previous.aggregatePlan, current.aggregatePlan, iteration);
        previous.selectedPlan = combinatorS.combine(previous.selectedPlan, current.selectedPlan, iteration);
        previous.selectedCombinationalPlan = combinatorSC.combine(previous.selectedCombinationalPlan, current.selectedCombinationalPlan, iteration);
    }

    public int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, AgentPlans historic, AgentPlans previous) {
        return select(agent, aggregate, plans, costSignal, historic);
    }

    public int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, AgentPlans historic, AgentPlans previous, int numNodes, int numNodesSubtree, int layer, double avgChildren, int iteration) {
        return select(agent, aggregate, plans, costSignal, historic, previous);
    }
    
    @Override
    public IterativeFitnessFunction clone() {
        IterativeFitnessFunction clone = null;
        try {
            clone = (IterativeFitnessFunction) super.clone();
            clone.combinatorA = combinatorA;
            clone.combinatorG = combinatorG;
            clone.combinatorS = combinatorS;
            clone.combinatorSC = combinatorSC;
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace();
        }
        return clone;
    }
}
