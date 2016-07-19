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
package experiments.parameters;

import agents.fitnessFunction.costFunction.CostFunction;
import agents.plan.GlobalPlan;
import agents.plan.Plan;
import java.util.Comparator;

/**
 *
 * @author Peter
 */
public class OrderParam implements Param<Comparator<Plan>> {
    CostFunctionParam costParam = new CostFunctionParam();

    @Override
    public boolean isValid(String param) {
        if(costParam.isValid(param)) {
            return true;
        } else if("sort".equals(param)) {
            return true;
        }
        return false;
    }

    @Override
    public String validDescription() {
        return "<cost function> or 'sort'";
    }

    @Override
    public Comparator<Plan> get(String param) {
        if(costParam.isValid(param)) {
            CostFunction func = costParam.get(param);
            return (a,b) -> Double.compare(func.calcCost(a, null, 0, 0, true), func.calcCost(b, null, 0, 0, true));
        } else {
            return (a,b) -> {
                for(int i = 0; i < a.getNumberOfStates(); i++) {
                    if(a.getValue(i) < b.getValue(i)) {
                        return -1;
                    } else if(a.getValue(i) > b.getValue(i)) {
                        return 1;
                    }
                }
                return 0;
            };
        }
    }
}
