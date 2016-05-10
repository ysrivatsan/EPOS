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
public class StdDevCostFunction implements CostFunction {

    @Override
    public double calcCost(Plan plan, Plan costSignal) {
        /*Plan p = plan.clone();
        p.add(costSignal);
        p.subtract(p.avg());
        return Math.sqrt(p.variance());/**/
        
        return Math.sqrt(plan.variance()) + costSignal.dot(plan);
    }

    @Override
    public Plan calcGradient(Plan plan) {
        Plan p = plan.clone();
        p.subtract(p.avg());
        double x = Math.sqrt(p.dot(p));
        if(x == 0.0) {
            p.set(0);
        } else {
            p.multiply(1/x * 1.0/Math.sqrt(plan.getNumberOfStates()-1));
        }
        return p;
    }

    @Override
    public String toString() {
        return "StdDevCost";
    }

    @Override
    public String getMetric() {
        return "std deviation";
    }
}
