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
import agents.energyPlan.Plan;
import agents.AgentPlans;
import java.util.List;

/**
 *
 * @author Peter
 */
public abstract class FitnessFunction {

    public abstract double getRobustness(Plan plan, Plan costSignal, AgentPlans historic);

    public int select(Agent agent, List<Plan> plans, Plan costSignal) {
        return 0;
    }

    public int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal) {
        return select(agent, plans, costSignal);
    }

    public int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, AgentPlans historic) {
        return select(agent, aggregate, plans, costSignal);
    }

    public int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, AgentPlans historic, List<AgentPlans> previous) {
        return select(agent, aggregate, plans, costSignal, historic);
    }

    public int select(Agent agent, Plan aggregate, List<Plan> plans, Plan costSignal, AgentPlans historic, List<AgentPlans> previous, int numNodes, int numNodesSubtree) {
        return select(agent, aggregate, plans, costSignal, historic, previous);
    }
}
