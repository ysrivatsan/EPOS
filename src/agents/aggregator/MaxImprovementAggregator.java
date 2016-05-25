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
import agents.plan.GlobalPlan;
import agents.plan.Plan;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Peter
 */
public class MaxImprovementAggregator extends Aggregator {

    private List<Plan> prevAggregates;

    @Override
    public void initPhase() {
        prevAggregates = new ArrayList<>();
    }

    @Override
    List<Boolean> calcSelected(Agent agent, List<Plan> childAggregates, Plan global, Plan costSignal, FitnessFunction fitnessFunction) {
        try {
            List<Boolean> selected;

            if (!prevAggregates.isEmpty()) {
                List<Plan> combPlans = new ArrayList<>();
                List<List<Boolean>> combSelections = new ArrayList<>();

                int numCombinations = 1 << childAggregates.size();
                for (int i = 0; i < numCombinations; i++) {
                    Plan init = global.clone();
                    combPlans.add(init);
                    combSelections.add(new ArrayList<>());
                }

                // calc all possible combinations
                int factor = 1;
                List<Plan> childPlans = new ArrayList<>();
                childPlans.add(new GlobalPlan(agent));
                childPlans.add(null);
                for (int child = 0; child < childAggregates.size(); child++) {
                    Plan childAggregate = childAggregates.get(child);
                    Plan prevAggregate = prevAggregates.get(child);

                    Plan change = childAggregate.clone();
                    change.subtract(prevAggregate);

                    childPlans.set(1, change);

                    int numPlans = childPlans.size();
                    for (int i = 0; i < numCombinations; i++) {
                        int planIdx = (i / factor) % numPlans;
                        Plan combPlan = combPlans.get(i);
                        combPlan.add(childPlans.get(planIdx));
                        combSelections.get(i).add(planIdx == 1);
                    }
                    factor *= numPlans;
                }

                int selectedCombination = fitnessFunction.select(agent, new AggregatePlan(agent), combPlans, costSignal, null);
                selected = combSelections.get(selectedCombination);

                for (int i = 0; i < childAggregates.size(); i++) {
                    if (selected.get(i)) {
                        prevAggregates.set(i, childAggregates.get(i));
                    }
                }
            } else {
                selected = new ArrayList<>();
                for (Plan p : childAggregates) {
                    selected.add(true);
                }
                prevAggregates.addAll(childAggregates);
            }

            return selected;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
