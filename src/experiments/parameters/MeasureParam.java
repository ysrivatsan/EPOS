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
import agents.fitnessFunction.costFunction.DirectionCostFunction;
import agents.fitnessFunction.costFunction.EntropyCostFunction;
import agents.fitnessFunction.costFunction.MatchEstimateCostFunction;
import agents.fitnessFunction.costFunction.MaxCostFunction;
import agents.fitnessFunction.costFunction.QuadraticCostFunction;
import agents.fitnessFunction.costFunction.RelStdDevCostFunction;
import agents.fitnessFunction.costFunction.StdDevCostFunction;

/**
 *
 * @author Peter
 */
public class MeasureParam extends MapParam<CostFunction> {

    public MeasureParam() {
        /*map.put("std", Arrays.asList(new StdDevCostFunction()));
        map.put("dot", Arrays.asList(new DirectionCostFunction()));
        map.put("match", Arrays.asList(new MatchEstimateCostFunction()));
        map.put("rand", Arrays.asList(new QuadraticCostFunction(1)));
        map.put("relStd", Arrays.asList(new RelStdDevCostFunction()));
        map.put("entropy", Arrays.asList(new EntropyCostFunction()));
        map.put("max", Arrays.asList(new MaxCostFunction()));
        map.put("", new ArrayList<>());*/
        map.put("std", new StdDevCostFunction());
        map.put("dot", new DirectionCostFunction());
        map.put("match", new MatchEstimateCostFunction());
        map.put("rand", new QuadraticCostFunction(1));
        map.put("relStd", new RelStdDevCostFunction());
        map.put("entropy", new EntropyCostFunction());
        map.put("max", new MaxCostFunction());
        map.put("", null);
    }
}
