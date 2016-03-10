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
import dsutil.generic.state.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import messages.EPOSBroadcast;
import messages.EPOSRequest;
import messages.EPOSResponse;
import messages.IEPOSIteration;
import org.joda.time.DateTime;
import protopeer.Finger;
import protopeer.measurement.MeasurementLog;
import protopeer.network.Message;

/**
 *
 * @author Evangelos
 */
public class IEPOSAgent extends Agent {
    private final int MAX_ITERATIONS = 10;
    private int iteration;

    private final int planSize;
    private final int historySize;
    private final TreeMap<DateTime, HistoricPlans> history = new TreeMap<>();

    private final Map<Finger, EPOSRequest> messageBuffer = new HashMap<>();

    private final FitnessFunction fitnessFunction;
    private Plan childAggregatePlan;
    private Plan aggregatePlan;
    private Plan historicSelectedPlan;
    private Plan historicAggregatePlan;
    private Plan historicGlobalPlan;
    private double robustness;

    private Plan selectedCombinationalPlan;
    private List<Integer> selectedCombination;
    
    private Plan prevSelectedPlan;
    private Plan prevChildAggregatedPlan;
    private Plan prevSelectedCombinationalPlan;
    private Plan prevGlobalPlan;

    public static class Factory implements AgentFactory {

        @Override
        public Agent create(String experimentID, String plansLocation, String planConfigurations, String treeStamp, String agentMeterID, String plansFormat, FitnessFunction fitnessFunction, int planSize, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize) {
            return new IEPOSAgent(experimentID, plansLocation, planConfigurations, treeStamp, agentMeterID, plansFormat, fitnessFunction, planSize, initialPhase, previousPhase, costSignal, historySize);
        }
    }

    public IEPOSAgent(String experimentID, String plansLocation, String planConfigurations, String treeStamp, String agentMeterID, String plansFormat, FitnessFunction fitnessFunction, int planSize, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize) {
        super(experimentID, plansLocation, planConfigurations, treeStamp, agentMeterID, initialPhase, plansFormat, planSize, costSignal);
        this.fitnessFunction = fitnessFunction;
        this.planSize = planSize;
        this.historySize = historySize;
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
        this.prevSelectedPlan = null;
        this.prevChildAggregatedPlan = null;
        this.prevSelectedCombinationalPlan = null;
        this.prevGlobalPlan = null;
    }
    
    private void initIteration() {
        this.robustness = 0.0;
        this.selectedCombinationalPlan = new CombinationalPlan(this);
        this.possiblePlans.clear();
        this.selectedPlan = new PossiblePlan(this);
        this.aggregatePlan = new AggregatePlan(this);
        this.childAggregatePlan = new AggregatePlan(this);
        this.globalPlan = new GlobalPlan(this);
        this.historicSelectedPlan = new PossiblePlan(this);
        this.historicAggregatePlan = new AggregatePlan(this);
        this.historicGlobalPlan = new GlobalPlan(this);
        this.messageBuffer.clear();
    }

    public void informParent() {
        EPOSRequest request = new EPOSRequest();
        request.child = getPeer().getFinger();
        request.possiblePlans = this.possiblePlans;
        request.aggregatePlan = this.aggregatePlan;
        if (previousPhase != null) {
            HistoricPlans historicPlans = history.get(previousPhase);
            request.aggregateHistoryPlan = historicPlans.aggregatedPlan;
        } else {
            request.aggregateHistoryPlan = null;
        }
        this.getPeer().sendMessage(this.parent.getNetworkAddress(), request);
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

        HistoricPlans historicPlans = null;
        if (previousPhase != null) {
            historicPlans = history.get(previousPhase);
        }

        // select best combination
        Plan modifiedChildAggregatePlan = new AggregatePlan(this);
        modifiedChildAggregatePlan.set(childAggregatePlan);
        if(iteration > 0) {
            modifiedChildAggregatePlan.add(prevGlobalPlan);
            modifiedChildAggregatePlan.subtract(prevChildAggregatedPlan);
            modifiedChildAggregatePlan.subtract(prevSelectedCombinationalPlan);
        }
        int selectedCombination = fitnessFunction.select(this, modifiedChildAggregatePlan, combinationalPlans, costSignal, historicPlans);
        this.selectedCombinationalPlan = combinationalPlans.get(selectedCombination);
        this.selectedCombination = combinationalSelections.get(selectedCombination);
    }

    private void update() {
        aggregatePlan.set(childAggregatePlan);
        aggregatePlan.add(selectedCombinationalPlan);
        historicAggregatePlan.set(aggregatePlan);
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
        prevSelectedPlan = selectedPlan;
        prevChildAggregatedPlan = childAggregatePlan;
        prevSelectedCombinationalPlan = selectedCombinationalPlan;
        prevGlobalPlan = globalPlan;
    }

    @Override
    public void handleIncomingMessage(Message message) {
        if (message instanceof EPOSRequest) {
            EPOSRequest request = (EPOSRequest) message;
            this.messageBuffer.put(request.child, request);
            if (this.children.size() == this.messageBuffer.size()) {
                this.preProcessing();
                this.select();
                this.update();
                this.informChildren();
                this.readPlans();
                if (this.isRoot()) {
                    HistoricPlans historicPlans = null;
                    if (previousPhase != null) {
                        historicPlans = history.get(previousPhase);
                    }
                    
                    Plan modifiedAggregatePlan = new AggregatePlan(this);
                    modifiedAggregatePlan.set(aggregatePlan);
                    if(prevSelectedPlan != null) {
                        modifiedAggregatePlan.add(prevGlobalPlan);
                        modifiedAggregatePlan.subtract(prevChildAggregatedPlan);
                        modifiedAggregatePlan.subtract(prevSelectedCombinationalPlan);
                        modifiedAggregatePlan.subtract(prevSelectedPlan);
                    }
                    int selected = fitnessFunction.select(this, modifiedAggregatePlan, possiblePlans, globalPlan, historicPlans);
                    Plan selectedPlan = possiblePlans.get(selected);
                    this.selectedPlan.set(selectedPlan);
                    this.selectedPlan.setDiscomfort(selectedPlan.getDiscomfort());

                    this.globalPlan.set(aggregatePlan);
                    this.globalPlan.add(selectedPlan);

                    historicPlans = new HistoricPlans(globalPlan, aggregatePlan, selectedPlan);
                    this.history.put(this.currentPhase, historicPlans);

                    this.robustness = fitnessFunction.getRobustness(globalPlan, costSignal, historicPlans);

                    System.out.println(globalPlan.getNumberOfStates() + "," + currentPhase.toString("yyyy-MM-dd") + "," + robustness + ": " + globalPlan);
                    if(iteration+1 < MAX_ITERATIONS) {
                        betweenIterations();
                        broadcast(new IEPOSIteration(globalPlan));
                        initIteration();
                    } else {
                        broadcast(new EPOSBroadcast(globalPlan));
                    }
                } else {
                    this.informParent();
                }
            }
        } else if (message instanceof EPOSResponse) {
            EPOSResponse response = (EPOSResponse) message;
            this.selectedPlan.set(response.selectedPlan);
            this.selectedPlan.setDiscomfort(response.selectedPlan.getDiscomfort());
            this.historicSelectedPlan.set(selectedPlan);
        } else if (message instanceof EPOSBroadcast) {
            EPOSBroadcast broadcast = (EPOSBroadcast) message;
            this.globalPlan.set(broadcast.globalPlan);
            this.historicGlobalPlan.set(globalPlan);

            HistoricPlans historicPlans = new HistoricPlans(globalPlan, historicAggregatePlan, historicSelectedPlan);
            this.history.put(this.currentPhase, historicPlans);

            this.robustness = fitnessFunction.getRobustness(globalPlan, costSignal, historicPlans);

            if (!this.isLeaf()) {
                broadcast(broadcast);
            }
        } else if (message instanceof IEPOSIteration) {
            IEPOSIteration iter = (IEPOSIteration) message;
            this.globalPlan.set(iter.globalPlan);
            betweenIterations();
            initIteration();
            if(this.isLeaf()) {
                readPlans();
                informParent();
            } else {
                broadcast(message);
            }
        }
    }

    @Override
    void measure(MeasurementLog log, int epochNumber) {
        if (epochNumber == 2) {
            if (this.isRoot()) {
                log.log(epochNumber, globalPlan, 1.0);
                log.log(epochNumber, EPOSMeasures.PLAN_SIZE, planSize);
                log.log(epochNumber, EPOSMeasures.ROBUSTNESS, robustness);
            }
            log.log(epochNumber, selectedPlan, 1.0);
            log.log(epochNumber, EPOSMeasures.DISCOMFORT, selectedPlan.getDiscomfort());
            //writeGraphData(epochNumber);
        }
    }

    private void writeGraphData(int epochNumber) {
        System.out.println(getPeer().getNetworkAddress().toString() + ","
                + ((parent != null) ? parent.getNetworkAddress().toString() : "-") + ","
                + findSelectedPlan());
    }

    private int findSelectedPlan() {
        for (int i = 0; i < possiblePlans.size(); i++) {
            if (possiblePlans.get(i).equals(selectedPlan)) {
                return i;
            }
        }
        return -1;
    }
}
