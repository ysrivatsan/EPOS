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
import agents.fitnessFunction.costFunction.IterativeCostFunction;
import agents.plan.Plan;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Peter
 */
public abstract class IterativeFitnessFunction extends FitnessFunction implements Cloneable {
    
    public void afterIteration(AgentPlans current, Plan costSignal, int iteration) {
    }
    
    @Override
    public final int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, AgentPlans historic) {
        return select(agent, aggregate, plans, costSignal, 0, 0, 0, 0, 0);
    }
    
    public abstract int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, int numNodes, int numNodesSubtree, int layer, double avgChildren, int iteration);
    
    @Override
    public IterativeFitnessFunction clone() {
        try {
            return (IterativeFitnessFunction) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(IterativeFitnessFunction.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
