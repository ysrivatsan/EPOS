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
import agents.energyPlan.AggregatePlan;
import agents.energyPlan.Plan;
import agents.AgentPlans;
import java.util.List;

/**
 *
 * @author Peter
 */
public class ReversingDeviationFitnessFunction extends FitnessFunction {

    @Override
    public double getRobustness(Plan globalPlan, Plan pattern, AgentPlans historic) {
        if (historic == null) {
            return 0.0;
        } else {
            Plan historicGlobalPlan = historic.globalPlan;
            return globalPlan.correlationCoefficient(historicGlobalPlan);
        }
    }

    @Override
    public int select(Agent agent, Plan aggregatePlan, List<Plan> combinationalPlans, Plan pattern, AgentPlans historic) {
        int selected = -1;

        if (historic == null) {
            selected = (int) (Math.random() * combinationalPlans.size());
        } else {
            double minStandardDeviation = Double.MAX_VALUE;

            for (int i = 0; i < combinationalPlans.size(); i++) {
                Plan combinationalPlan = combinationalPlans.get(i);
                Plan testAggregatePlan = new AggregatePlan(agent);
                testAggregatePlan.add(historic.globalPlan);
                testAggregatePlan.subtract(historic.aggregatePlan);
                testAggregatePlan.subtract(historic.selectedPlan);
                testAggregatePlan.add(aggregatePlan);
                testAggregatePlan.add(combinationalPlan);

                double standardDeviation = testAggregatePlan.stdDeviation();
                if (standardDeviation < minStandardDeviation) {
                    minStandardDeviation = standardDeviation;
                    selected = i;
                }
            }
        }

        return selected;
    }

}
