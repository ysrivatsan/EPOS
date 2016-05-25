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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Peter
 */
public abstract class Aggregator implements Cloneable {

    private List<Boolean> selected;

    public abstract void initPhase();

    public final List<Boolean> getSelected() {
        return selected;
    }

    abstract List<Boolean> calcSelected(Agent agent, List<Plan> childAggregates, Plan globalPlan, Plan costSignal, FitnessFunction fitnessFunction);

    public final Plan calcAggregate(Agent agent, List<Plan> childAggregates, Plan globalPlan, Plan costSignal, FitnessFunction fitnessFunction) {
        selected = calcSelected(agent, childAggregates, globalPlan, costSignal, fitnessFunction);

        Plan plan = new AggregatePlan(agent);
        for (int i = 0; i < selected.size(); i++) {
            if (selected.get(i)) {
                plan.add(childAggregates.get(i));
            }
        }
        return plan;
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
