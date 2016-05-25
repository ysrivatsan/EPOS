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
public abstract class IterativeCostFunction implements CostFunction {
    
    public abstract double calcCost(Plan plan, Plan costSignal, Plan iterationCost);
    public abstract Plan calcGradient(Plan plan, Plan costSignal);

    @Override
    public final double calcCost(Plan plan, Plan costSignal, int idx, int numPlans) {
        return calcCost(plan, costSignal, null);
    }
}