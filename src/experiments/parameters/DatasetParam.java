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
import java.io.File;

/**
 *
 * @author Peter
 */
public class DatasetParam implements Param<Dataset> {

    @Override
    public boolean isValid(String x) {
        try {
            if (x.matches("^E[357]\\.[135]$")) {
                return true;
            } else if (x.startsWith("B")) {
                int num = Integer.parseInt(x.substring(1));
                return 0 <= num && num <= 22 && (num & 1) == 0;
            } else if (x.startsWith("N")) {
                String[] params = x.trim().split("_");
                if (params.length == 5) {
                    Integer.parseUnsignedInt(params[1]);
                    Integer.parseUnsignedInt(params[2]);
                    Double.parseDouble(params[3]);
                    double std = Double.parseDouble(params[4]);
                    return std >= 0;
                }
            } else if(x.startsWith("S")) {
                String[] params = x.trim().split("_");
                if (params.length == 4 || params.length == 5) {
                    Integer.parseUnsignedInt(params[1]);
                    Integer.parseUnsignedInt(params[2]);
                    double std = Double.parseDouble(params[3]);
                    if(params.length == 5) {
                        double p = Double.parseDouble(params[4]);
                        return std >= 0 && p >= 0;
                    } else {
                        return std >= 0;
                    }
                }
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String validDescription() {
        return "E<3, 5 or 7>.<1, 3 or 5>, B<even int from 0 to 22>, Noise_<numPlans>_<planSize>_<mean>_<std>, Sparse_<numPlans>_<planSize>_<std>";
    }

    @Override
    public Dataset get(String x) {
        if (x.startsWith("E")) {
            return new FileDataset("input-data" + File.separator + "Archive", x.charAt(x.length() - 3) + "." + x.charAt(x.length() - 1));
        } else if (x.startsWith("B")) {
            int num = Integer.parseInt(x.substring(1));
            return new FileDataset("input-data/bicycle", "user_plans_unique_" + num + "to" + (num + 2) + "_force_trips");
        } else if (x.startsWith("N")) {
            String[] params = x.trim().split("_");
            return new NoiseDataset(Integer.parseInt(params[1]), Integer.parseInt(params[2]), Double.parseDouble(params[3]), Double.parseDouble(params[4]));
        } else if (x.startsWith("S")) {
            String[] params = x.trim().split("_");
            if(params.length == 4) {
                return new SparseDataset(Integer.parseInt(params[1]), Integer.parseInt(params[2]), Double.parseDouble(params[3]), 0.0);
            } else {
                return new SparseDataset(Integer.parseInt(params[1]), Integer.parseInt(params[2]), Double.parseDouble(params[3]), Double.parseDouble(params[4]));
            }
        }
        return null;
    }
}
