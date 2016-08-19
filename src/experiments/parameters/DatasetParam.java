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

import agents.dataset.Dataset;
import agents.dataset.FileDataset;
import agents.dataset.NoiseDataset;
import agents.dataset.SparseDataset;
import agents.plan.Plan;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author Peter
 */
public class DatasetParam implements Param<Dataset> {
    private OrderParam orderParam = new OrderParam();

    @Override
    public boolean isValid(String x) {
        try {
            String[] params = x.trim().split("_");
            x = params[0];
            
            if(orderParam.isValid(params[params.length-1])) {
                params = Arrays.copyOf(params, params.length-1);
            }
            
            if (x.matches("^E[12345678](\\.[135])?$")) {
                return params.length == 1;
            } else if (x.startsWith("B")) {
                if(params.length == 1) {
                    int num = Integer.parseInt(params[0].substring(1));
                    return 0 <= num && num <= 22 && (num & 1) == 0;
                }
            } else if (x.startsWith("N")) {
                if (params.length >= 5 || params.length <= 6) {
                    if(params[1].contains("-")) {
                        String[] numPlanStr = params[1].split("-");
                        Integer.parseUnsignedInt(numPlanStr[0]);
                        Integer.parseUnsignedInt(numPlanStr[1]);
                    } else {
                        Integer.parseUnsignedInt(params[1]);
                    }
                    Integer.parseUnsignedInt(params[2]);
                    Double.parseDouble(params[3]);
                    double std = Double.parseDouble(params[4]);
                    if(params.length >= 6) {
                        if(params[5].contains("-")) {
                            String[] nonZeroStr = params[5].split("-");
                            Integer.parseUnsignedInt(nonZeroStr[0]);
                            Integer.parseUnsignedInt(nonZeroStr[1]);
                        } else {
                            Integer.parseUnsignedInt(params[5]);
                        }
                    }
                    return std >= 0;
                }
            } else if(x.startsWith("S") || x.startsWith("O")) {
                if (params.length >= 4 || params.length <= 5) {
                    Integer.parseUnsignedInt(params[1]);
                    Integer.parseUnsignedInt(params[2]);
                    double std = Double.parseDouble(params[3]);
                    if(params.length == 5) {
                        int p = Integer.parseUnsignedInt(params[4]);
                        return std >= 0 && p > 0;
                    }
                    return std >= 0;
                }
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String validDescription() {
        return "E<1, 5 or 7>[.<1, 3 or 5>][_<order func>], B<even int from 0 to 22>[_<order func>], Noise_<numPlans/range>_<planSize>_<mean>_<std>[_<nonZero>][_<order func>], Sparse_<numPlans>_<planSize>_<std>[_<generationSteps>][_<order func>]";
    }

    @Override
    public Dataset get(String x) {
        String[] params = x.trim().split("_");
        x = params[0];
        
        Comparator<Plan> order = null;
        if(orderParam.isValid(params[params.length-1])) {
            order = orderParam.get(params[params.length-1]);
            params = Arrays.copyOf(params, params.length-1);
        }
        
        if (x.startsWith("E")) {
            if(x.contains(".")) {
                return new FileDataset("input-data" + File.separator + "Archive", x.charAt(x.length() - 3) + "." + x.charAt(x.length() - 1), order);
            } else {
                return new FileDataset("input-data" + File.separator + "Archive", x.charAt(x.length() - 1) + "", order);
            }
        } else if (x.startsWith("B")) {
            int num = Integer.parseInt(x.substring(1));
            return new FileDataset("input-data/bicycle", "user_plans_unique_" + num + "to" + (num + 2) + "_force_trips", order);
        } else if (x.startsWith("N")) {
            String[] numPlansStr = params[1].split("-");
            int numPlansMin = Integer.parseInt(numPlansStr[0]);
            int numPlansMax = numPlansStr.length == 1 ? numPlansMin : Integer.parseInt(numPlansStr[1]);
            int planSize = Integer.parseInt(params[2]);
            int nonZeroMin = planSize;
            int nonZeroMax = nonZeroMin;
            if(params.length >= 6) {
                String[] nonZeroStr = params[5].split("-");
                nonZeroMin = Integer.parseInt(nonZeroStr[0]);
                nonZeroMax = nonZeroStr.length == 1 ? nonZeroMin : Integer.parseInt(nonZeroStr[1]);
            }
            return new NoiseDataset(numPlansMin, numPlansMax, planSize, Double.parseDouble(params[3]), Double.parseDouble(params[4]), nonZeroMin, nonZeroMax, order);
        } else if (x.startsWith("S")) {
            int generationSteps = 0;
            if(params.length >= 5) {
                generationSteps = Integer.parseUnsignedInt(params[4]);
            }
            return new SparseDataset(Integer.parseInt(params[1]), Integer.parseInt(params[2]), Double.parseDouble(params[3]), generationSteps, order);
        }
        return null;
    }
}
