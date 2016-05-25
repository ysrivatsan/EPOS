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
package agents.log;

import agents.plan.Plan;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.measurement.MeasurementLog;

/**
 *
 * @author Peter
 */
public abstract class AgentLogger implements Cloneable {

    public abstract void init(int agentId);

    public abstract void initRoot(Plan costSignal);

    public abstract void log(MeasurementLog log, int epoch, int iteration, Plan selectedLocalPlan);

    public abstract void logRoot(MeasurementLog log, int epoch, int iteration, Plan global, int numIterations);

    public abstract void print(MeasurementLog log);

    @Override
    public AgentLogger clone() {
        try {
            return (AgentLogger) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(AgentLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
