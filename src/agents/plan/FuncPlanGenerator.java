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
package agents.plan;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Function;

/**
 *
 * @author Peter
 */
public class FuncPlanGenerator implements PlanGenerator {
    private Function<Double, Double> func;
    
    public FuncPlanGenerator(Function<Double, Double> func) {
        this.func = func;
    }

    @Override
    public Plan generatePlan(int size) {
        Plan plan = new GlobalPlan();
        List<Double> vals = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            vals.add(func.apply(i/(double)size));
        }

        plan.init(vals.size());
        for (int i = 0; i < vals.size(); i++) {
            plan.setValue(i, vals.get(i));
        }

        return plan;
    }
    
}
