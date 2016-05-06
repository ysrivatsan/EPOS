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
package agents.fitnessFunction.costFunction;

import agents.plan.Plan;

/**
 * 
 * @author Peter
 */
public class MatchEstimateCostFunction implements CostFunction {

    @Override
    public double calcCost(Plan plan, Plan costSignal) {
        Plan cPlan = plan.clone();
        cPlan.subtract(cPlan.avg());
        
        // estimate target signal based on cost signal
        Plan cCost = costSignal.clone();
        cCost.subtract(cCost.avg());
        cCost.multiply(-1);
        double factor = cPlan.stdDeviation()/cCost.stdDeviation();
        if(!Double.isFinite(factor)) {
            factor = 0;
        }
        cCost.multiply(factor);
        
        // cost is the difference of the plan and the target signal
        cPlan.subtract(cCost);
        return cPlan.norm();
    }

    @Override
    public Plan calcGradient(Plan plan) {
        return plan;
    }

    @Override
    public String toString() {
        return "MatchEstimateCost";
    }

    @Override
    public String getMetric() {
        return "distance to estimate";
    }
}
