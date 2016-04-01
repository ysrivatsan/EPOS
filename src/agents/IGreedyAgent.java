/*
 * Copyright (C) 2015 Evangelos Pournaras
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

import agents.plan.AggregatePlan;
import agents.plan.CombinationalPlan;
import agents.plan.GlobalPlan;
import agents.fitnessFunction.FitnessFunction;
import agents.fitnessFunction.IterativeFitnessFunction;
import agents.plan.Plan;
import agents.plan.PossiblePlan;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import messages.EPOSBroadcast;
import messages.EPOSRequest;
import messages.IEPOSIteration;
import messages.IEPOSRequest;
import org.joda.time.DateTime;
import protopeer.Experiment;
import protopeer.Finger;
import protopeer.measurement.MeasurementLog;
import protopeer.network.Message;

/**
 *
 * @author Evangelos
 */
public class IGreedyAgent extends Agent {
    private final int MAX_ITERATIONS = 500;
    private int measurementEpoch;
    private int iteration;

    private final int planSize;
    private final int historySize;
    private final TreeMap<DateTime, AgentPlans> history = new TreeMap<>();

    private int numNodes;
    private int numNodesSubtree;
    private int layer;
    private double avgNumChildren;
    private final Map<Finger, EPOSRequest> messageBuffer = new HashMap<>();

    private final IterativeFitnessFunction fitnessFunctionPrototype;
    private IterativeFitnessFunction fitnessFunction;
    private double robustness;
    
    private Plan costSignal;
    private AgentPlans current = new AgentPlans();
    private AgentPlans previous = new AgentPlans();
    private AgentPlans historic;
    
    private Plan childAggregatePlan;

    public static class Factory implements AgentFactory {

        @Override
        public Agent create(String experimentID, String plansLocation, String planConfigurations, String treeStamp, String agentMeterID, String plansFormat, FitnessFunction fitnessFunction, int planSize, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize) {
            if(!(fitnessFunction instanceof IterativeFitnessFunction)) {
                throw new IllegalArgumentException("Fitness function has to be iterative");
            }
            return new IGreedyAgent(experimentID, plansLocation, planConfigurations, treeStamp, agentMeterID, plansFormat, (IterativeFitnessFunction)fitnessFunction, planSize, initialPhase, previousPhase, costSignal, historySize);
        }
    }

    public IGreedyAgent(String experimentID, String plansLocation, String planConfigurations, String treeStamp, String agentMeterID, String plansFormat, IterativeFitnessFunction fitnessFunction, int planSize, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize) {
        super(experimentID, plansLocation, planConfigurations, treeStamp, agentMeterID, initialPhase, plansFormat, planSize);
        this.fitnessFunctionPrototype = fitnessFunction;
        this.planSize = planSize;
        this.historySize = historySize;
        this.costSignal = costSignal;
    }

    @Override
    void runPhase() {
        initPhase();
        initIteration();
        if (this.isLeaf()) {
            readPlans();
            select();
            update();
            informParent();
        }
    }

    private void initPhase() {
        if (this.history.size() > this.historySize) {
            this.history.remove(this.history.firstKey());
        }
        this.iteration = 0;
        this.previous.reset();
        this.possiblePlans.clear();
        
        if(previousPhase != null) {
            this.historic = history.get(previousPhase);
        } else {
            this.historic = null;
        }
        numNodes = -1;
        fitnessFunction = fitnessFunctionPrototype.clone();
    }
    
    private void initIteration() {
        this.robustness = 0.0;
        this.childAggregatePlan = new AggregatePlan(this);
        current = new AgentPlans();
        current.globalPlan = new GlobalPlan(this);
        current.aggregatePlan = new AggregatePlan(this);
        current.selectedPlan = new PossiblePlan(this);
        current.selectedCombinationalPlan = new CombinationalPlan(this);
        this.messageBuffer.clear();
        this.numNodesSubtree = 1;
        this.avgNumChildren = children.size();
        this.layer = 0;
    }

    public void informParent() {
        IEPOSRequest request = new IEPOSRequest();
        request.child = getPeer().getFinger();
        request.aggregatePlan = current.aggregatePlan;
        request.numNodes = numNodesSubtree;
        getPeer().sendMessage(parent.getNetworkAddress(), request);
    }

    private void preProcessing() {
        for (Finger child : children) {
            EPOSRequest msg = messageBuffer.get(child);
            childAggregatePlan.add(msg.aggregatePlan);
        }
    }

    private void select() {
        // select best combination
        int selectedPlan = fitnessFunction.select(this, childAggregatePlan, possiblePlans, costSignal, historic, previous, numNodes, numNodesSubtree, layer, avgNumChildren);
        current.selectedPlan = possiblePlans.get(selectedPlan);
    }

    private void update() {
        current.aggregatePlan.set(childAggregatePlan);
        current.aggregatePlan.add(current.selectedPlan);
    }
    
    private void betweenIterations() {
        iteration++;
        fitnessFunction.updatePrevious(previous, current, iteration);
    }

    @Override
    public void handleIncomingMessage(Message message) {
        if (message instanceof IEPOSRequest) {
            IEPOSRequest request = (IEPOSRequest) message;
            this.messageBuffer.put(request.child, request);
            this.numNodesSubtree += request.numNodes;
            if (this.children.size() == this.messageBuffer.size()) {
                if(possiblePlans.isEmpty()) {
                    this.readPlans();
                }
                this.preProcessing();
                this.select();
                this.update();
                if (this.isRoot()) {
                    current.globalPlan.set(current.aggregatePlan);

                    this.history.put(this.currentPhase, current);

                    this.robustness = fitnessFunction.getRobustness(current.globalPlan, costSignal, historic);

                    Experiment.getSingleton().getRootMeasurementLog().log(measurementEpoch, iteration, robustness);
                    //getPeer().getMeasurementLogger().log(measurementEpoch, iteration, robustness);
                    //System.out.println(planSize + "," + currentPhase.toString("yyyy-MM-dd") + "," + robustness + ": " + current.globalPlan);
                    if(iteration+1 < MAX_ITERATIONS) {
                        numNodes = numNodesSubtree;
                        betweenIterations();
                        broadcast(new IEPOSIteration(current.globalPlan, numNodes, 1, children.size()));
                        initIteration();
                        if(iteration%10 == 0) {
                            System.out.print(".");
                        }
                        if(iteration%100 == 0) {
                            System.out.print(" ");
                        }
                    } else {
                        System.out.println(".");
                        broadcast(new EPOSBroadcast(current.globalPlan));
                    }
                } else {
                    this.informParent();
                }
            }
        } else if (message instanceof EPOSBroadcast) {
            EPOSBroadcast broadcast = (EPOSBroadcast) message;
            current.globalPlan.set(broadcast.globalPlan);
            
            this.history.put(this.currentPhase, current);

            this.robustness = fitnessFunction.getRobustness(current.globalPlan, costSignal, current);

            if (!this.isLeaf()) {
                broadcast(broadcast);
            }
        } else if (message instanceof IEPOSIteration) {
            IEPOSIteration iter = (IEPOSIteration) message;
            numNodes = iter.numNodes;
            current.globalPlan.set(iter.globalPlan);
            
            betweenIterations();
            initIteration();
            
            layer = iter.hops;
            avgNumChildren = iter.sumChildren / iter.hops;
            
            if(this.isLeaf()) {
                // plans are not read again (only in first iteration)
                select();
                update();
                informParent();
            } else {
                iter = new IEPOSIteration(iter.globalPlan, iter.numNodes, iter.hops+1, iter.sumChildren+children.size());
                broadcast(iter);
            }
        }
    }

    @Override
    void measure(MeasurementLog log, int epochNumber) {
        this.measurementEpoch = epochNumber+1;
        //writeGraphData(epochNumber);
    }

    private void writeGraphData(int epochNumber) {
        System.out.println(getPeer().getNetworkAddress().toString() + ","
                + ((parent != null) ? parent.getNetworkAddress().toString() : "-") + ","
                + findSelectedPlan());
    }

    private int findSelectedPlan() {
        for (int i = 0; i < possiblePlans.size(); i++) {
            if (possiblePlans.get(i).equals(current.selectedPlan)) {
                return i;
            }
        }
        return -1;
    }
}
