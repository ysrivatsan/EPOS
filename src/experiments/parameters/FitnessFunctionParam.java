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

import agents.fitnessFunction.IterativeFitnessFunction;
import agents.fitnessFunction.costFunction.CostFunction;
import agents.fitnessFunction.costFunction.IterativeCostFunction;
import agents.fitnessFunction.iterative.Factor;
import agents.fitnessFunction.iterative.PlanCombinator;
import experiments.ConfigurableExperiment;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Peter
 */
public class FitnessFunctionParam implements Param<IterativeFitnessFunction> {

    @Override
    public boolean isValid(String param) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String validDescription() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public IterativeFitnessFunction get(String val) {
        String[] parts = val.split("[\\(,\\)]");
        IterativeFitnessFunction ff = null;

        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }

        try {
            FFConstructorParam ffs = new FFConstructorParam();

            if (!ffs.isValid(parts[0])) {
                throw new IllegalArgumentException(parts[0] + " is not a valid fitness function; valid: " + ffs.validDescription());
            }

            Constructor ffConst = ffs.get(parts[0]);
            if (ffConst.getParameterCount() > parts.length - 1) {
                throw new IllegalArgumentException("Too few parameters for fitness function " + parts[0] + " (" + ffConst.getParameterCount() + " expected)");
            }

            Map<Class, Param<?>> params = new HashMap<>();
            params.put(IterativeCostFunction.class, new IterativeCostFunctionParam());
            params.put(Factor.class, new FactorParam());
            params.put(PlanCombinator.class, new PlanCombinatorParam());

            Object[] args = new Object[ffConst.getParameterCount()];
            Class[] types = ffConst.getParameterTypes();
            for (int i = 0; i < args.length; i++) {
                Param<?> options = params.get(types[i]);
                if (!options.isValid(parts[i + 1])) {
                    System.err.println(parts[i + 1] + " is not a valid " + types[i].getSimpleName() + "; valid: " + options.validDescription());
                }
                args[i] = options.get(parts[i + 1]);
            }

            ff = (IterativeFitnessFunction) ffConst.newInstance(args);
        } catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(ConfigurableExperiment.class.getName()).log(Level.SEVERE, null, ex);
        }

        return ff;
    }
}
