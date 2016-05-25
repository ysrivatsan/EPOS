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
import agents.plan.Plan;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Peter
 */
public class AllAggregator extends Aggregator {

    @Override
    public void initPhase() {
    }

    @Override
     List<Boolean> calcSelected(Agent agent, List<Plan> childAggregates, Plan globalPlan, Plan costSignal, FitnessFunction fitnessFunction) {
        List<Boolean> selected = new ArrayList<>();
        childAggregates.stream().forEach(x -> selected.add(true));
        return selected;
    }
}
