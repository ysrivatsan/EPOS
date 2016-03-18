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

import agents.energyPlan.AggregatePlan;
import agents.energyPlan.CombinationalPlan;
import agents.energyPlan.GlobalPlan;
import agents.fitnessFunction.FitnessFunction;
import agents.energyPlan.Plan;
import agents.energyPlan.PossiblePlan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import messages.EPOSBroadcast;
import messages.EPOSRequest;
import messages.EPOSResponse;
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
public class IEPOSAgent extends Agent {
    private final int MAX_ITERATIONS = 1000;
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

    private final FitnessFunction fitnessFunction;
    private double robustness;
    
    private Plan costSignal;
    private AgentPlans current = new AgentPlans();
    private LinkedList<AgentPlans> previous = new LinkedList<>();
    private AgentPlans historic;
    
    private Plan childAggregatePlan;
    private List<Integer> selectedCombination;

    public static class Factory implements AgentFactory {

        @Override
        public Agent create(String experimentID, String plansLocation, String planConfigurations, String treeStamp, String agentMeterID, String plansFormat, FitnessFunction fitnessFunction, int planSize, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize) {
            return new IEPOSAgent(experimentID, plansLocation, planConfigurations, treeStamp, agentMeterID, plansFormat, fitnessFunction, planSize, initialPhase, previousPhase, costSignal, historySize);
        }
    }

    public IEPOSAgent(String experimentID, String plansLocation, String planConfigurations, String treeStamp, String agentMeterID, String plansFormat, FitnessFunction fitnessFunction, int planSize, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize) {
        super(experimentID, plansLocation, planConfigurations, treeStamp, agentMeterID, initialPhase, plansFormat, planSize);
        this.fitnessFunction = fitnessFunction;
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
            informParent();
        }
    }

    private void initPhase() {
        if (this.history.size() > this.historySize) {
            this.history.remove(this.history.firstKey());
        }
        this.iteration = 0;
        this.previous.clear();
        
        if(previousPhase != null) {
            this.historic = history.get(previousPhase);
        } else {
            this.historic = null;
        }
        numNodes = -1;
    }
    
    private void initIteration() {
        this.robustness = 0.0;
        this.possiblePlans.clear();
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
        request.possiblePlans = possiblePlans;
        request.aggregatePlan = current.aggregatePlan;
        if (historic != null) {
            request.aggregateHistoryPlan = historic.aggregatePlan;
        } else {
            request.aggregateHistoryPlan = null;
        }
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
        List<Plan> combinationalPlans = new ArrayList<>();
        List<List<Integer>> combinationalSelections = new ArrayList<>();

        // init combinations
        int numCombinations = 1;
        for (Finger child : children) {
            EPOSRequest msg = messageBuffer.get(child);
            numCombinations *= msg.possiblePlans.size();
        }
        for (int i = 0; i < numCombinations; i++) {
            combinationalPlans.add(new CombinationalPlan(this));
            combinationalSelections.add(new ArrayList<>());
        }

        // calc all possible combinations
        int factor = 1;
        for (Finger child : children) {
            List<Plan> childPlans = messageBuffer.get(child).possiblePlans;
            int numPlans = childPlans.size();
            for (int i = 0; i < numCombinations; i++) {
                int planIdx = (i / factor) % numPlans;
                Plan combinationalPlan = combinationalPlans.get(i);
                combinationalPlan.add(childPlans.get(planIdx));
                combinationalPlan.setDiscomfort(combinationalPlan.getDiscomfort() + childPlans.get(planIdx).getDiscomfort());
                combinationalSelections.get(i).add(planIdx);
            }
            factor *= numPlans;
        }

        // select best combination
        int selectedCombination = fitnessFunction.select(this, childAggregatePlan, combinationalPlans, costSignal, historic, previous, numNodes, numNodesSubtree, layer, avgNumChildren);
        current.selectedCombinationalPlan = combinationalPlans.get(selectedCombination);
        this.selectedCombination = combinationalSelections.get(selectedCombination);
    }

    private void update() {
        current.aggregatePlan.set(childAggregatePlan);
        current.aggregatePlan.add(current.selectedCombinationalPlan);
    }

    private void informChildren() {
        for (int c = 0; c < children.size(); c++) {
            Finger child = children.get(c);
            int selected = selectedCombination.get(c);

            EPOSResponse response = new EPOSResponse();
            response.selectedPlan = messageBuffer.get(child).possiblePlans.get(selected);
            getPeer().sendMessage(child.getNetworkAddress(), response);
        }
    }
    
    private void betweenIterations() {
        iteration++;
        previous.addFirst(current);
    }

    @Override
    public void handleIncomingMessage(Message message) {
        if (message instanceof IEPOSRequest) {
            IEPOSRequest request = (IEPOSRequest) message;
            this.messageBuffer.put(request.child, request);
            this.numNodesSubtree += request.numNodes;
            if (this.children.size() == this.messageBuffer.size()) {
                this.preProcessing();
                this.select();
                this.update();
                this.informChildren();
                this.readPlans();
                if (this.isRoot()) {
                    int selected = fitnessFunction.select(this, current.aggregatePlan, possiblePlans, costSignal, historic, previous, numNodes, numNodesSubtree, layer, avgNumChildren);
                    Plan selectedPlan = possiblePlans.get(selected);
                    current.selectedPlan.set(selectedPlan);
                    current.globalPlan.set(current.aggregatePlan);
                    current.globalPlan.add(selectedPlan);

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
                        System.out.print(".");
                        if(iteration%10 == 0) {
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
        } else if (message instanceof EPOSResponse) {
            EPOSResponse response = (EPOSResponse) message;
            current.selectedPlan.set(response.selectedPlan);
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
                readPlans();
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
