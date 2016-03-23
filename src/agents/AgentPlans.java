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
package agents;

import agents.plan.Plan;

/**
 *
 * @author Peter
 */
public class AgentPlans {
    public Plan globalPlan;
    public Plan aggregatePlan;
    public Plan selectedPlan;
    public Plan selectedCombinationalPlan;
    
    public AgentPlans() {
    }
    
    public AgentPlans(AgentPlans other) {
        this.globalPlan = other.globalPlan;
        this.aggregatePlan = other.aggregatePlan;
        this.selectedPlan = other.selectedPlan;
        this.selectedCombinationalPlan = other.selectedCombinationalPlan;
    }

    public void set(Plan globalPlan, Plan aggregatedPlan, Plan selectedPlan, Plan selectedCombinationalPlan) {
        this.globalPlan = globalPlan;
        this.aggregatePlan = aggregatedPlan;
        this.selectedPlan = selectedPlan;
        this.selectedCombinationalPlan = selectedCombinationalPlan;
    }
    
    public void reset() {
        globalPlan = null;
        aggregatePlan = null;
        selectedPlan = null;
        selectedCombinationalPlan = null;
    }
    
    public boolean isEmpty() {
        return globalPlan == null;
    }
}
