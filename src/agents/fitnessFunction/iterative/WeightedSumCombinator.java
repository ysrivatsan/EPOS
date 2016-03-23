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
package agents.fitnessFunction.iterative;

import agents.plan.Plan;

/**
 *
 * @author Peter
 */
public class WeightedSumCombinator implements PlanCombinator {
    private double exp = -0.25;
    
    public WeightedSumCombinator() {
    }
    
    public WeightedSumCombinator(double iterExp) {
        this.exp = iterExp;
    }

    @Override
    public Plan combine(Plan target, Plan other, int iteration) {
        if (target == null) {
            return other;
        }
        other.multiply(Math.pow(iteration,exp));
        target.add(other);
        return target;
    }

    @Override
    public String toString() {
        return "wsum";
    }
}
