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
import org.joda.time.DateTime;
import protopeer.Finger;
import protopeer.measurement.MeasurementLog;
import protopeer.network.Message;

/**
 *
 * @author Evangelos
 */
public class EPOSAgent extends Agent {

    private final int planSize;
    private final int historySize;
    private final TreeMap<DateTime, HistoricPlans> history = new TreeMap<>();

    private final Map<Finger, EPOSRequest> messageBuffer = new HashMap<>();

    private final FitnessFunction fitnessFunction;
    private Plan aggregatePlan;
    private Plan historicSelectedPlan;
    private Plan historicAggregatePlan;
    private Plan historicGlobalPlan;
    private double robustness;

    private Plan selectedCombinationalPlan;
    private List<Integer> selectedCombination;

    public static class Factory implements AgentFactory {

        @Override
        public Agent create(String experimentID, String plansLocation, String planConfigurations, String treeStamp, String agentMeterID, String plansFormat, FitnessFunction fitnessFunction, int planSize, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize) {
            return new EPOSAgent(experimentID, plansLocation, planConfigurations, treeStamp, agentMeterID, plansFormat, fitnessFunction, planSize, initialPhase, previousPhase, costSignal, historySize);
        }
    }

    public EPOSAgent(String experimentID, String plansLocation, String planConfigurations, String treeStamp, String agentMeterID, String plansFormat, FitnessFunction fitnessFunction, int planSize, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize) {
        super(experimentID, plansLocation, planConfigurations, treeStamp, agentMeterID, initialPhase, plansFormat, planSize, costSignal);
        this.fitnessFunction = fitnessFunction;
        this.planSize = planSize;
        this.historySize = historySize;
    }

    @Override
    void runPhase() {
        initPhase();
        if (this.isLeaf()) {
            readPlans();
            informParent();
        }
    }

    private void initPhase() {
        if (this.history.size() > this.historySize) {
            this.history.remove(this.history.firstKey());
        }
        this.robustness = 0.0;
        this.possiblePlans.clear();
        this.selectedPlan = new PossiblePlan(this);
        this.aggregatePlan = new AggregatePlan(this);
        this.globalPlan = new GlobalPlan(this);
        this.historicSelectedPlan = new PossiblePlan(this);
        this.historicAggregatePlan = new AggregatePlan(this);
        this.historicGlobalPlan = new GlobalPlan(this);
        this.selectedCombinationalPlan = new CombinationalPlan(this);
        this.messageBuffer.clear();
    }

    public void informParent() {
        EPOSRequest request = new EPOSRequest();
        request.child = getPeer().getFinger();
        request.possiblePlans = this.possiblePlans;
        request.aggregatePlan = this.aggregatePlan;
        if (previousPhase != null) {
            HistoricPlans historicPlans = history.get(previousPhase);
            //if (this.historicEnergyPlans.size() != 0) {
            //    Map<HistoricEnergyPlans, Plan> historicPlans = this.historicEnergyPlans.get(this.historicAggregationPhase);
            request.aggregateHistoryPlan = historicPlans.aggregatedPlan;
        } else {
            request.aggregateHistoryPlan = null;
        }
        this.getPeer().sendMessage(this.parent.getNetworkAddress(), request);
    }

    private void preProcessing() {
        for (Finger child : children) {
            EPOSRequest msg = messageBuffer.get(child);
            aggregatePlan.add(msg.aggregatePlan);
        }
    }

    public void select() {
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
        int selectedCombination = fitnessFunction.select(this, aggregatePlan, combinationalPlans, costSignal, historicPlans);
        this.selectedCombinationalPlan = combinationalPlans.get(selectedCombination);
        this.selectedCombination = combinationalSelections.get(selectedCombination);
        //this.selectedCombination = combinationalPlansMap.get(selectedCombinationalPlan);
    }

    public void update() {
        this.aggregatePlan.add(this.selectedCombinationalPlan);
        this.historicAggregatePlan.set(aggregatePlan);
    }

    public void informChildren() {
        for (int c = 0; c < children.size(); c++) {
            Finger child = children.get(c);
            int selected = selectedCombination.get(c);

            EPOSResponse response = new EPOSResponse();
            response.selectedPlan = messageBuffer.get(child).possiblePlans.get(selected);
            getPeer().sendMessage(child.getNetworkAddress(), response);
        }
    }

    public void broadcast() {
        EPOSBroadcast broadcast = new EPOSBroadcast();
        broadcast.coordinationPhase = this.globalPlan.getCoordinationPhase();
        broadcast.globalPlan = this.globalPlan;
        for (Finger child : children) {
            getPeer().sendMessage(child.getNetworkAddress(), broadcast);
        }
    }

    @Override
    public void handleIncomingMessage(Message message) {
        if (message instanceof EPOSRequest) {
            EPOSRequest request = (EPOSRequest) message;
            State requestState = new State();
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
                    int selected = fitnessFunction.select(this, aggregatePlan, possiblePlans, globalPlan, historicPlans);
                    Plan selectedPlan = possiblePlans.get(selected);
                    this.selectedPlan.set(selectedPlan);
                    this.selectedPlan.setDiscomfort(selectedPlan.getDiscomfort());

                    this.aggregatePlan.add(selectedPlan);
                    this.globalPlan.add(aggregatePlan);

                    historicPlans = new HistoricPlans(globalPlan, aggregatePlan, selectedPlan);
                    this.history.put(this.currentPhase, historicPlans);

                    this.robustness = fitnessFunction.getRobustness(globalPlan, costSignal, historicPlans);

                    System.out.println(globalPlan.getNumberOfStates() + "," + currentPhase.toString("yyyy-MM-dd") + "," + robustness + ": " + globalPlan);
                    this.broadcast();
                } else {
                    this.informParent();
                }
            }
        }
        if (message instanceof EPOSResponse) {
            EPOSResponse response = (EPOSResponse) message;
            this.selectedPlan.set(response.selectedPlan);
            this.selectedPlan.setDiscomfort(response.selectedPlan.getDiscomfort());
            this.historicSelectedPlan.set(selectedPlan);
        }
        if (message instanceof EPOSBroadcast) {
            EPOSBroadcast broadcast = (EPOSBroadcast) message;
            this.globalPlan.set(broadcast.globalPlan);
            this.historicGlobalPlan.set(globalPlan);

            HistoricPlans historicPlans = new HistoricPlans(globalPlan, historicAggregatePlan, historicSelectedPlan);
            this.history.put(this.currentPhase, historicPlans);

            this.robustness = fitnessFunction.getRobustness(globalPlan, costSignal, historicPlans);

            if (this.isLeaf()) {
//                this.plan();
//                this.informParent();
            } else {
                for (Finger child : children) {
                    getPeer().sendMessage(child.getNetworkAddress(), broadcast);
                }
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
            //log.log(epochNumber, Measurements.SELECTED_PLAN_VALUE, selectedPlan.getArithmeticState(0).getValue());
            writeGraphData(epochNumber);
        }
    }

    /**
     * Problems encountered:
     *
     * 1. Root plan cannot be detected in MIN-COST 2. There are nodes in the EDF
     * dataset that have possible plans with more than 144 size.
     *
     * @param epochNumber
     */
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
