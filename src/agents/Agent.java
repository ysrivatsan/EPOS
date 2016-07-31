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

import agents.fitnessFunction.costFunction.CostFunction;
import agents.plan.Plan;
import dsutil.protopeer.services.topology.trees.TreeApplicationInterface;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import protopeer.BasePeerlet;
import protopeer.Finger;
import protopeer.measurement.MeasurementFileDumper;
import protopeer.measurement.MeasurementLog;
import protopeer.measurement.MeasurementLoggerListener;
import protopeer.network.Message;
import protopeer.time.Timer;
import protopeer.time.TimerListener;
import protopeer.util.quantities.Time;
import agents.dataset.AgentDataset;
import experiments.log.AgentLogger;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 *
 * @author Peter
 */
public abstract class Agent extends BasePeerlet implements TreeApplicationInterface {
    Finger parent = null;
    final List<Finger> children = new ArrayList<>();
    private TopologicalState topologicalState = TopologicalState.DISCONNECTED;
    public final int experimentId;
    
    public final AgentDataset dataSource;
    private final String treeStamp;
    private final File outFolder;
    
    DateTime currentPhase;
    DateTime previousPhase = null;
    final List<DateTime> phases;
    private int phaseIndex = 0;
    
    final List<Plan> possiblePlans = new ArrayList<>();
    
    private MeasurementFileDumper measurementDumper;
    private final List<CostFunction> measures;
    private final List<CostFunction> localMeasures;
    private final Map<String, Object> measurements = new HashMap<>();
    private final Map<String, Object> localMeasurements = new HashMap<>();
    private List<AgentLogger> loggers = new ArrayList<>();
    private boolean inMemory;
    
    private Random random;
    
    private final String config;
    
    private static enum TopologicalState {
        ROOT, LEAF, IN_TREE, DISCONNECTED
    }

    public Agent(int experimentId, AgentDataset dataSource, String treeStamp, File outFolder, DateTime initialPhase, List<CostFunction> measures, List<CostFunction> localMeasures, List<AgentLogger> loggers, boolean inMemory) {
        this.experimentId = experimentId;
        this.dataSource = dataSource;
        this.treeStamp = treeStamp;
        this.outFolder = outFolder;
        this.currentPhase = initialPhase;
        this.measures = measures;
        this.localMeasures = localMeasures;
        this.phases = dataSource.getPhases();
        this.loggers = loggers;
        this.inMemory = inMemory;
        this.random = new Random(experimentId);
        
        this.config = dataSource.getConfig() + "-" + treeStamp;
    }

    @Override
    public void start() {
        this.runBootstrap();
        scheduleMeasurements();
    }

    @Override
    public void stop() {
    }

    private void runBootstrap() {
        Timer loadAgentTimer = getPeer().getClock().createNewTimer();
        loadAgentTimer.addTimerListener(new TimerListener() {
            public void timerExpired(Timer timer) {
                runActiveState();
            }
        });
        loadAgentTimer.schedule(Time.inMilliseconds(2000));
    }
    
    void runActiveState() {
        Timer loadAgentTimer = getPeer().getClock().createNewTimer();
        loadAgentTimer.addTimerListener(new TimerListener() {
            public void timerExpired(Timer timer) {
                if (phaseIndex < phases.size()) {
                    
                    currentPhase = phases.get(phaseIndex);
                    if (phaseIndex > 0) {
                        previousPhase = phases.get(phaseIndex - 1);
                    }
                    phaseIndex++;
                    
                    initPhase();
                    runPhase();
                    runActiveState();
                }
            }
        });
        loadAgentTimer.schedule(Time.inMilliseconds(1000));
    }
    
    private void initPhase() {
        // init loggers
        for (AgentLogger logger : loggers) {
            logger.init(getPeer().getIndexNumber());
            if (isRoot()) {
                logger.initRoot(getCostSignal());
            }
        }
    }
    
    abstract void runPhase();
    
    @Override
    public abstract void handleIncomingMessage(Message message);
    
    void readPlans() {
        possiblePlans.clear();
        possiblePlans.addAll(dataSource.getPlans(currentPhase));
    }

    @Override
    public void setParent(Finger parent) {
        if (parent != null) {
            this.parent = parent;
        }
        this.computeTopologicalState();
    }

    @Override
    public void setChildren(List<Finger> list) {
        children.addAll(list);
        this.computeTopologicalState();
    }

    @Override
    public void setTreeView(Finger parent, List<Finger> children) {
        this.setParent(parent);
        this.setChildren(children);
        this.computeTopologicalState();
    }
    
    public boolean isRoot() {
        return topologicalState == TopologicalState.ROOT;
    }
    
    public boolean isLeaf() {
        return topologicalState == TopologicalState.LEAF;
    }
    
    public boolean isInnerNode() {
        return topologicalState == TopologicalState.IN_TREE;
    }
    
    public boolean isDisconnected() {
        return topologicalState == TopologicalState.DISCONNECTED;
    }
    
    public int getIteration() {
        return 0;
    }
    
    public int getNumIterations() {
        return 1;
    }
    
    public final List<Plan> getPossiblePlans() {
        return possiblePlans;
    }
    
    public final Plan getSelectedPlan() {
        return possiblePlans.get(getSelectedPlanIdx());
    }
    
    public abstract int getSelectedPlanIdx();
    
    public abstract Plan getGlobalResponse();
    
    public abstract Plan getCostSignal();
    
    public final Random getRandom() {
        return random;
    }
    

    private void computeTopologicalState() {
        if (parent == null && !children.isEmpty()) {
            this.topologicalState = TopologicalState.ROOT;
        }
        if (parent != null && children.isEmpty()) {
            this.topologicalState = TopologicalState.LEAF;
        }
        if (parent != null && !children.isEmpty()) {
            this.topologicalState = TopologicalState.IN_TREE;
        }
        if (parent == null && children.isEmpty()) {
            this.topologicalState = TopologicalState.DISCONNECTED;
        }
    }
    
    private MeasurementFileDumper getMeasurementDumper() {
        if(measurementDumper == null) {
            measurementDumper = new MeasurementFileDumper(outFolder.getPath() + "/" + experimentId + "_" + getPeer().getIndexNumber());
        }
        return measurementDumper;
    }
    
    private void scheduleMeasurements() {
        getPeer().getMeasurementLogger().addMeasurementLoggerListener(new MeasurementLoggerListener() {
            @Override
            public void measurementEpochEnded(MeasurementLog log, int epochNumber) {
                if(epochNumber >= 2) {
                    measure(log, epochNumber);
                    boolean skip = false;
                    try {
                        log.getSubLog(epochNumber, epochNumber+1).getMinEpochNumber();
                    } catch(NoSuchElementException e) {
                        skip = true;
                    }
                    if(!skip && !inMemory) {
                        getMeasurementDumper().measurementEpochEnded(log, epochNumber);
                    }
                    if(!inMemory) {
                        log.shrink(epochNumber, epochNumber+1);
                    }
                } else {
                    log.shrink(epochNumber, epochNumber+1);
                }
            }
        });
    }
    
    public List<Finger> getChildren() {
        return children;
    }
    
    void measureGlobal(Plan plan, Plan costSignal) {
        measurements.clear();
        for(CostFunction func : measures) {
            measurements.put(func.getMetric(), func.calcCost(plan, costSignal, 0, 0, true));
        }
    }
    
    void measureLocal(Plan plan, Plan costSignal, int selected, int numPlans, boolean changed) {
        localMeasurements.clear();
        for(CostFunction func : localMeasures) {
            localMeasurements.put(func.getMetric(), func.calcCost(plan, costSignal, selected, numPlans, changed));
        }
    }
    
    void measure(MeasurementLog log, int epochNumber) {
        Experiment exp = null;
        if(!localMeasures.isEmpty()) {
            exp = new Experiment();
            exp.experimentId = experimentId;
        }
        for(CostFunction func : localMeasures) {
            log.log(epochNumber, getIteration(), "local-" + func.getMetric(), exp, (Double) localMeasurements.get(func.getMetric()));
        }
        if(isRoot()) {
            for(CostFunction func : measures) {
                log.log(epochNumber, getIteration(), "global-" + func.getMetric(), (Double) measurements.get(func.getMetric()));
            }
        }
        
        for (AgentLogger logger : loggers) {
            logger.log(log, epochNumber, this);
            if (isRoot()) {
                logger.logRoot(log, epochNumber, this, getGlobalResponse());
            }
        }
    }

    public void initPlan(Plan plan) {
        plan.init(dataSource.getPlanSize());
        plan.setCoordinationPhase(currentPhase);
        plan.setDiscomfort(0.0);
        plan.setAgentMeterID(dataSource.getId());
        plan.setConfiguration(config);
    }
    
    public void broadcast(Message msg) {
        for(Finger c : children) {
            getPeer().sendMessage(c.getNetworkAddress(), msg);
        }
    }
    
    public class Experiment {
        public int experimentId;

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 17 * hash + this.experimentId;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Experiment other = (Experiment) obj;
            if (this.experimentId != other.experimentId) {
                return false;
            }
            return true;
        }
    }
}
