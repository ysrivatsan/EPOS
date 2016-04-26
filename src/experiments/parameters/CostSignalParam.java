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

import agents.dataset.FilePlanGenerator;
import agents.dataset.FuncPlanGenerator;
import agents.dataset.PlanGenerator;
import java.io.File;

/**
 *
 * @author Peter
 */
public class CostSignalParam extends MapParam<PlanGenerator> {

    public CostSignalParam() {
        map.put("zero", new FuncPlanGenerator((x) -> 0.0));
        map.put("one", new FuncPlanGenerator((x) -> 1.0));
        map.put("sin", new FuncPlanGenerator((x) -> 10 * Math.sin(x * 2 * Math.PI)));
    }

    @Override
    public PlanGenerator get(String param) {
        PlanGenerator pg = super.get(param);
        if (pg != null) {
            return pg;
        }

        return new FilePlanGenerator(param);
    }

    @Override
    public String validDescription() {
        return super.toString() + " or a valid path to a textfile containing exactly one double per line";
    }

    @Override
    public boolean isValid(String param) {
        return super.isValid(param) || new File(param).exists();
    }

}
