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
    
    public void updatePrevious(AgentPlans previous, AgentPlans current, Plan costSignal, int iteration) {
        if(previous == null) {
            return;
        }
        previous.global = combinatorG.combine(previous.global, current.global, iteration);
        previous.aggregate = combinatorA.combine(previous.aggregate, current.aggregate, iteration);
        previous.selectedLocalPlan = combinatorS.combine(previous.selectedLocalPlan, current.selectedLocalPlan, iteration);
        previous.selectedPlan = combinatorSC.combine(previous.selectedPlan, current.selectedPlan, iteration);
    }
    
    @Override
    public final int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, AgentPlans historic) {
        return select(agent, aggregate, plans, costSignal, 0, 0, 0, 0, 0);
    }
    
    public abstract int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, int numNodes, int numNodesSubtree, int layer, double avgChildren, int iteration);
    
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
