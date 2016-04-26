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
package experiments;

import agents.dataset.AgentDataset;
import agents.dataset.Dataset;
import agents.dataset.FileDataset;
import agents.plan.Plan;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.DateTime;

/**
 *
 * @author Peter
 */
public class DatasetProperties {

    public static void main(String[] args) {
        //Dataset ds = new FileDataset("input-data" + File.separator + "bicycle", "user_plans_unique_8to10_force_trips");
        Dataset ds = new FileDataset("input-data" + File.separator + "Archive", "5.1");
        double meanSum = 0;
        double stdSum = 0;
        int num = 0;

        double numPlanSum2 = 0;
        int num2 = 0;

        List<AgentDataset> agents = ds.getAgentDataSources(99999);
        List<Plan> allPlans = new ArrayList<>();
        for (AgentDataset ads : agents) {
            for (DateTime phase : ads.getPhases()) {
                List<Plan> plans = ads.getPlans(phase);
                for (Plan p : plans) {
                    stdSum += p.stdDeviation();
                    meanSum += p.avg();
                    num++;
                }
                numPlanSum2 += plans.size();
                num2++;

                allPlans.addAll(plans);
            }
        }

        System.out.println(ds.toString() + ":");
        System.out.println("#agents: " + agents.size());
        System.out.println("#plans: " + numPlanSum2 / num2);
        System.out.println("plan size: " + ds.getPlanSize());
        System.out.println("mean: " + meanSum / num);
        System.out.println("std: " + stdSum / num);

        /*try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("output-data" + File.separator + "avg_cov.obj"))) {
            double[] avg = Plan.meanVector(allPlans);
            double[][] cov = Plan.covarianceMatrix(allPlans);
            oos.writeObject(avg);
            oos.writeObject(cov);
        } catch (IOException ex) {
            Logger.getLogger(DatasetProperties.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }
}
