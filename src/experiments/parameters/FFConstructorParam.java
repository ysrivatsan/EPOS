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

import agents.fitnessFunction.*;
import agents.fitnessFunction.costFunction.IterativeCostFunction;
import agents.fitnessFunction.iterative.Factor;
import agents.fitnessFunction.iterative.PlanCombinator;
import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Peter
 */
public class FFConstructorParam extends MapParam<Constructor> {

    public FFConstructorParam() {
        try {
            map.put("MinCostG", IterMinCostG.class.getConstructor(IterativeCostFunction.class, Factor.class, PlanCombinator.class));
            map.put("MinCostGmS", IterMinCostGmS.class.getConstructor(IterativeCostFunction.class, Factor.class, PlanCombinator.class));
            map.put("MinCostGmA", IterMinCostGmA.class.getConstructor(IterativeCostFunction.class, Factor.class, PlanCombinator.class));
            map.put("MinCostAdamG", IterMinCostAdamG.class.getConstructor(IterativeCostFunction.class, Factor.class));
            map.put("MinCostAdamGmS", IterMinCostAdamGmS.class.getConstructor(IterativeCostFunction.class, Factor.class));
            map.put("MinCostAdamGmA", IterMinCostAdamGmA.class.getConstructor(IterativeCostFunction.class, Factor.class));
            map.put("MinCostReplace", IterMinCostReplace.class.getConstructor(IterativeCostFunction.class));
            map.put("MinCostRand", IterMinCostRand.class.getConstructor(IterativeCostFunction.class, Factor.class, PlanCombinator.class));
            map.put("MaxMatchGmA", IterMaxMatchGmA.class.getConstructor(Factor.class, PlanCombinator.class));
            map.put("ProbGmA", IterProbGmA.class.getConstructor(Factor.class, PlanCombinator.class));
            map.put("UCB1", IterUCB1Bandit.class.getConstructor());
        } catch (NoSuchMethodException | SecurityException ex) {
            Logger.getLogger(FFConstructorParam.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
