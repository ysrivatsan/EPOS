/*
 * Copyright (C) 2015 Evangelos Pournaras
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

import agents.plan.AggregatePlan;
import agents.plan.GlobalPlan;
import agents.fitnessFunction.FitnessFunction;
import agents.fitnessFunction.IterLocalSearch;
import agents.fitnessFunction.IterativeFitnessFunction;
import agents.plan.Plan;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import messages.DownMessage;
import messages.UpMessage;
import org.apache.commons.collections15.CollectionUtils;

/**
 *
 * @author Evangelos
 */
public class LocalSearch {
    private Plan aggregate;
    private List<Plan> prevAggregates;
    private List<Boolean> useNew;
    private FitnessFunction fitnessFunction = new IterLocalSearch();

    void initPhase() {
    }

    void initIteration() {
        prevAggregates = null;
    }
    
    public List<Boolean> getSelected() {
        return useNew;
    }

    public Plan calcAggregate(Agent agent, List<Plan> childAggregates, Plan globalPlan) {
        if(prevAggregates != null) {
            List<Plan> combPlans = new ArrayList<>();
            List<List<Boolean>> combSelections = new ArrayList<>();

            int numCombinations = 1 << childAggregates.size();
            for (int i = 0; i < numCombinations; i++) {
                Plan init = new GlobalPlan(agent);
                init.set(globalPlan);
                combPlans.add(init);
                combSelections.add(new ArrayList<>());
            }

            // calc all possible combinations
            int factor = 1;
            for (int child = 0; child < childAggregates.size(); child++) {
                Plan childAggregate = childAggregates.get(child);
                Plan prevAggregate = prevAggregates.get(child);

                Plan change = new GlobalPlan(agent);
                change.set(childAggregate);
                change.subtract(prevAggregate);

                List<Plan> childPlans = new ArrayList<>();
                childPlans.add(new GlobalPlan(agent));
                childPlans.add(change);
                int numPlans = childPlans.size();
                for (int i = 0; i < numCombinations; i++) {
                    int planIdx = (i / factor) % numPlans;
                    Plan combinationalPlan = combPlans.get(i);
                    combinationalPlan.add(childPlans.get(planIdx));
                    combSelections.get(i).add(planIdx == 1);
                }
                factor *= numPlans;
            }

            int selected = fitnessFunction.select(agent, new AggregatePlan(agent), combPlans, new GlobalPlan(agent));
            useNew = combSelections.get(selected);
            /*useNew.clear();
            for(Plan p : childAggregates) {
                useNew.add(false);
            }*/
        } else {
            useNew = new ArrayList<>();
            for(Plan p : childAggregates) {
                useNew.add(true);
            }
            prevAggregates = childAggregates;
        }
        
        aggregate = new AggregatePlan(agent);
        for (int i = 0; i < childAggregates.size(); i++) {
            if (useNew.get(i)) {
                aggregate.add(childAggregates.get(i));
                prevAggregates.set(i,childAggregates.get(i));
            } else {
                aggregate.add(prevAggregates.get(i));
            }
        }
        return aggregate;
    }
}
