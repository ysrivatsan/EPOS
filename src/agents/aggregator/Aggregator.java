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
package agents.aggregator;

import agents.Agent;
import agents.fitnessFunction.FitnessFunction;
import agents.plan.AggregatePlan;
import agents.plan.Plan;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Peter
 */
public abstract class Aggregator implements Cloneable {

    private List<Boolean> selected;
    private List<Plan> prevAggregates;
    private List<Plan> newAggregates;

    public void initPhase() {
    }

    public final List<Boolean> getSelected() {
        return selected;
    }
    
    public final void discardChanges() {
        Collections.fill(selected, false);
    }

    abstract List<Boolean> calcSelected(Agent agent, List<Plan> childAggregates, List<Plan> prevAggregates, Plan globalPlan, Plan costSignal, FitnessFunction fitnessFunction);

    public final Plan calcAggregate(Agent agent, List<Plan> childAggregates, Plan globalPlan, Plan costSignal, FitnessFunction fitnessFunction) {
        prevAggregates = getAggregates(agent);
        newAggregates = childAggregates;
        
        selected = calcSelected(agent, newAggregates, prevAggregates, globalPlan, costSignal, fitnessFunction);
        
        return getAggregate(agent);
    }
    
    private Plan getAggregate(Agent agent) {
        Plan plan = new AggregatePlan(agent);
        for (int i = 0; i < selected.size(); i++) {
            if (selected.get(i)) {
                plan.add(newAggregates.get(i));
            } else {
                plan.add(prevAggregates.get(i));
            }
        }
        return plan;
    }
    private List<Plan> getAggregates(Agent agent) {
        if(prevAggregates == null) {
            return new ArrayList<>();
        }
        
        List<Plan> aggregates = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            if (selected.get(i)) {
                aggregates.add(newAggregates.get(i));
            } else {
                aggregates.add(prevAggregates.get(i));
            }
        }
        return aggregates;
    }

    @Override
    public Aggregator clone() {
        try {
            return (Aggregator) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(Aggregator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
