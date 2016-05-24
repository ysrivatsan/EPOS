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
public class EntropyCostFunction extends IterativeCostFunction {

    @Override
    public double calcCost(Plan plan, Plan costSignal, Plan iterationCost) {
        Plan p = plan.clone();
        p.add(costSignal);
        
        if(iterationCost == null) {
            return -p.entropy();
        }
        return -p.entropy() + iterationCost.dot(p);
    }

    @Override
    public Plan calcGradient(Plan plan, Plan costSignal) {
        double sum = plan.sum();
        
        Plan grad = plan.clone();
        if (sum == 0) {
            grad.set(1);
            return grad;
        }
        grad.multiply(-1/(sum*sum));
        grad.add(1/sum);
        
        for(int i = 0; i < plan.getNumberOfStates(); i++) {
            double xi = plan.getValue(i);
            double p = xi / sum;
            if (p <= 0.0) {
                grad.setValue(i, grad.getValue(i) * Double.NEGATIVE_INFINITY);
            } else {
                grad.setValue(i, grad.getValue(i) * (Math.log(p)+1));
            }
        }
        
        grad.multiply(plan.norm()/grad.norm());
        return grad;/**/
    }

    @Override
    public String toString() {
        return "EntropyCost";
    }

    @Override
    public String getMetric() {
        return "entropy";
    }
    
}
