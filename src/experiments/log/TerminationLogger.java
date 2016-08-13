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
import protopeer.measurement.Aggregate;
import protopeer.measurement.MeasurementLog;

/**
 *
 * @author Peter
 */
public class TerminationLogger extends AgentLogger {
    
    private int index;
    private Plan prevGlobal;
    private Token token = new Token();

    @Override
    public void init(int agentId) {
    }

    @Override
    public void initRoot(Plan costSignal) {
    }

    @Override
    public void log(MeasurementLog log, int epoch, Agent agent) {
    }

    @Override
    public void logRoot(MeasurementLog log, int epoch, Agent agent, Plan global) {
        if(agent.getIteration() == 0) {
            index = Integer.MAX_VALUE;
        }
        if(prevGlobal == null) {
            index = agent.getIteration()+1;
            prevGlobal = global;
        }
        if(prevGlobal != null && global.variance() < prevGlobal.variance()) {
            index = agent.getIteration()+1;//Math.min(index, agent.getIteration());
            prevGlobal = global;
        } else {
            //index = Integer.MAX_VALUE;
        }
        //prevGlobal = global;
        
        if(agent.getIteration() == agent.getNumIterations()-1) {
            log.log(epoch, "iterations", index);
        }
    }

    @Override
    public void print(MeasurementLog log) {
        for(Object t : log.getTagsOfType(String.class)) {
            if(t.equals("iterations")) {
                Aggregate a = log.getAggregate(t);
                System.out.print("Termination after " + a.getAverage() + "+-" + a.getStdDev() + " iterations, " + a.getMin() + "/" + a.getMax());
            }
        }
        System.out.println();
    }
    
    private class Token {
    }
}
