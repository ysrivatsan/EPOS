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

import agents.energyPlan.AggregatePlan;
import agents.EPOSAgent;
import agents.energyPlan.Plan;
import agents.HistoricPlans;
import dsutil.generic.state.ArithmeticListState;
import java.util.List;

/**
 *
 * @author Peter
 */
public class MinCorrelationFitnessFunction implements FitnessFunction {

    @Override
    public double getRobustness(Plan globalPlan, Plan pattern, HistoricPlans historic) {
        if (historic == null) {
            return 0.0;
        } else {
            return globalPlan.correlationCoefficient(historic.globalPlan);
        }
    }

    @Override
    public Plan select(EPOSAgent agent, Plan aggregatePlan, List<Plan> combinationalPlans, Plan pattern, HistoricPlans historic) {
        Plan selected = null;

        if (historic == null) {
            selected = combinationalPlans.get((int) (Math.random() * combinationalPlans.size()));
        } else {
            double minCorrelation = 1.0;
            for (Plan combinationalPlan : combinationalPlans) {
                Plan testAggregatePlan = new AggregatePlan(agent);
                testAggregatePlan.add(aggregatePlan);
                testAggregatePlan.add(combinationalPlan);

                double correlation = testAggregatePlan.correlationCoefficient(historic.aggregatedPlan);
                if (correlation < minCorrelation) {
                    minCorrelation = correlation;
                    selected = combinationalPlan;
                }
            }
        }

        return selected;
    }

}
