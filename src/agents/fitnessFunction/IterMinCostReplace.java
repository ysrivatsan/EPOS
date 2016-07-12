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
import java.util.ArrayList;
import java.util.List;

/**
 * minimize variance (submodular/convex compared to std deviation)
 *
 * @author Peter
 */
public class IterMinCostReplace extends IterMinCost {

    public IterMinCostReplace(IterativeCostFunction costFunc) {
        super(costFunc);
    }

    Plan gma;
    @Override
    public void afterIteration(AgentPlans current, Plan costSignal, int iteration, int numNodes) {
        gma = current.global.clone();
        gma.subtract(current.aggregate);
    }

    @Override
    public int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, int numNodes, int numNodesSubtree, int layer, double avgChildren, int iteration) {
        Plan zeroPlan = aggregate.clone();
        zeroPlan.set(0);
        
        if(iteration == 0) {
            gma = zeroPlan;
        }
        
        List<Plan> newPlans = new ArrayList<>();
        for(Plan p : plans) {
            Plan x = gma.clone();
            x.add(p);
            x.add(aggregate);
            newPlans.add(x);
        }
        plans = newPlans;

        return select(agent, zeroPlan, plans, costSignal, (Plan) null);
    }

    @Override
    public String toString() {
        return "IterMinCostReplace " + costFunc.toString();
    }
}
