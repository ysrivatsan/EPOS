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
public class RelStdDevCostFunction implements CostFunction {

    @Override
    public double calcCost(Plan plan, Plan costSignal) {
        return plan.relativeStdDeviation() + costSignal.dot(plan);/**/
        /*Plan p = plan.clone();
        p.add(costSignal);
        return p.relativeStdDeviation();/**/
    }

    @Override
    public Plan calcGradient(Plan plan) {
        /*return plan; /**/
        int n = plan.getNumberOfStates();
        double mean = plan.avg();
        
        Plan c = plan.clone();
        c.subtract(c.avg());
        
        if(mean == 0) {
            c.set(1);
            return c;
        }
        
        double cc = Math.sqrt(c.dot(c));
        
        if(cc == 0) {
            c.set(0);
            return c;
        }
        
        Plan grad = c;
        grad.multiply(1/(cc*mean));
        grad.subtract(cc/(mean*mean*n));
        grad.multiply(1.0/(n-1.0));
        
        grad.multiply(plan.norm()/grad.norm());
        return grad;/**/
    }

    @Override
    public String toString() {
        return "RelStdDevCost";
    }

    @Override
    public String getMetric() {
        return "rel std deviation";
    }
    
}
