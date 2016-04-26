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

import agents.fitnessFunction.iterative.Factor;
import agents.fitnessFunction.iterative.Factor1;
import agents.fitnessFunction.iterative.Factor1OverLayer;
import agents.fitnessFunction.iterative.Factor1OverN;
import agents.fitnessFunction.iterative.Factor1OverSqrtN;
import agents.fitnessFunction.iterative.FactorDepthOverN;
import agents.fitnessFunction.iterative.FactorMOverN;
import agents.fitnessFunction.iterative.FactorMOverNmM;
import agents.fitnessFunction.iterative.FactorNormalizeStd;

/**
 *
 * @author Peter
 */
public class FactorParam extends MapParam<Factor> {

    public FactorParam() {
        map.put("1", new Factor1());
        map.put("1/l", new Factor1OverLayer());
        map.put("1/n", new Factor1OverN());
        map.put("1/sqrtn", new Factor1OverSqrtN());
        map.put("d/n", new FactorDepthOverN());
        map.put("m/n", new FactorMOverN());
        map.put("m/n-m", new FactorMOverNmM());
        map.put("std", new FactorNormalizeStd());
    }
}
