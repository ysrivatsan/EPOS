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
    public Plan global;
    public Plan aggregate;
    public Plan selectedLocalPlan;
    public Plan selectedPlan;
    
    public AgentPlans() {
    }
    
    public AgentPlans(AgentPlans other) {
        this.global = other.global;
        this.aggregate = other.aggregate;
        this.selectedLocalPlan = other.selectedLocalPlan;
        this.selectedPlan = other.selectedPlan;
    }

    public void set(Plan globalPlan, Plan aggregatedPlan, Plan selectedPlan, Plan selectedCombinationalPlan) {
        this.global = globalPlan;
        this.aggregate = aggregatedPlan;
        this.selectedLocalPlan = selectedPlan;
        this.selectedPlan = selectedCombinationalPlan;
    }
    
    public void reset() {
        global = null;
        aggregate = null;
        selectedLocalPlan = null;
        selectedPlan = null;
    }
    
    public boolean isEmpty() {
        return global == null;
    }
}
