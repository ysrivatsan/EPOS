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
package agents;

import agents.aggregator.Aggregator;
import agents.aggregator.AllAggregator;
import agents.plan.Plan;
import agents.fitnessFunction.costFunction.CostFunction;
import java.io.File;
import org.joda.time.DateTime;
import agents.dataset.AgentDataset;
import agents.fitnessFunction.IterativeFitnessFunction;
import agents.log.AgentLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Peter
 */
public abstract class AgentFactory implements Cloneable {

    public int numIterations;
    public IterativeFitnessFunction fitnessFunction;
    public Aggregator aggregator;
    public Double rampUpRate;
    public CostFunction measure = null;
    public CostFunction localMeasure = null;
    public Map<String, AgentLogger> loggers = new HashMap<>();
    public boolean inMemory = true;

    public abstract Agent create(int id, AgentDataset dataSource, String treeStamp, File outFolder, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize);

    public List<CostFunction> getMeasures() {
        if (measure != null) {
            return Arrays.asList(measure);
        } else {
            return Arrays.asList(fitnessFunction);
        }
    }

    public List<CostFunction> getLocalMeasures() {
        if (localMeasure != null) {
            return Arrays.asList(localMeasure);
        } else {
            return new ArrayList<>();
        }
    }

    public List<AgentLogger> getLoggers() {
        List<AgentLogger> loggerList = new ArrayList<>();
        for(AgentLogger logger : loggers.values()) {
            loggerList.add(logger.clone());
        }
        return loggerList;
    }

    public void addLogger(String name, AgentLogger logger) {
        if (logger == null) {
            loggers.remove(name);
        } else {
            loggers.put(name, logger);
        }
    }
    
    public Aggregator getAggregator() {
        if(aggregator == null) {
            return new AllAggregator();
        } else {
            return aggregator.clone();
        }
    }

    @Override
    public AgentFactory clone() throws CloneNotSupportedException {
        AgentFactory f = (AgentFactory) super.clone();
        if (loggers != null) {
            f.loggers = new HashMap<>(loggers);
        }
        return f;
    }
}
