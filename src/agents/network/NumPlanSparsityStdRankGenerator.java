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
package agents.network;

import agents.Agent;
import agents.plan.Plan;
import java.util.List;
import java.util.function.BiFunction;
import org.joda.time.DateTime;

/**
 *
 * @author Peter
 */
public class NumPlanSparsityStdRankGenerator implements BiFunction<Integer, Agent, Double> {

    @Override
    public Double apply(Integer idx, Agent agent) {
        double rank = 0;
        int count = 0;
        
        double avgStd = 0;
        int numPlans = 0;
        
        double avgNonZero = 0;
        int planSize = 0;
        
        for (DateTime phase : agent.dataSource.getPhases()) {
            List<Plan> plans = agent.dataSource.getPlans(phase);
            for (Plan plan : plans) {
                planSize = plan.getNumberOfStates();
                avgStd += plan.stdDeviation();
                
                for(int i = 0; i < plan.getNumberOfStates(); i++) {
                    if(plan.getValue(i) == 0) {
                        avgNonZero++;
                    }
                }
            }
            numPlans += plans.size();
            count++;
        }
        
        avgStd = avgStd / numPlans;
        double avgNumPlans = numPlans/count;
        
        avgNonZero = avgNonZero / numPlans;
        
        rank += avgNumPlans*planSize + avgNonZero - 1 / (2 + avgStd);
        return rank;
    }
}
