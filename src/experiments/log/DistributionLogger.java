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
package experiments.log;

import agents.Agent;
import agents.plan.Plan;
import java.util.List;
import protopeer.measurement.Aggregate;
import protopeer.measurement.MeasurementLog;

/**
 *
 * @author Peter
 */
public class DistributionLogger extends AgentLogger {

    @Override
    public void init(int agentId) {
    }

    @Override
    public void initRoot(Plan costSignal) {
    }

    @Override
    public void log(MeasurementLog log, int epoch, Agent agent) {
        int idx = agent.getSelectedPlanIdx();
        if(agent.getIteration() == agent.getNumIterations()-1) {
            log.log(epoch, new Token(), idx);
        }
    }

    @Override
    public void logRoot(MeasurementLog log, int epoch, Agent agent, Plan global) {
    }

    @Override
    public void print(MeasurementLog log) {
        for(Object token : log.getTagsOfType(Token.class)) {
            Aggregate a = log.getAggregate(token);
            System.out.print("," + ((int)a.getSum()));
        }
        System.out.println();
    }
    
    private class Token {
    }
}
