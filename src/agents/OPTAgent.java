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

import agents.plan.AggregatePlan;
import agents.plan.Plan;
import agents.fitnessFunction.FitnessFunction;
import agents.fitnessFunction.costFunction.CostFunction;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import messages.OPTAggregate;
import messages.OPTOptimal;
import org.joda.time.DateTime;
import protopeer.Finger;
import protopeer.measurement.MeasurementLog;
import protopeer.network.Message;
import protopeer.network.NetworkAddress;
import agents.dataset.AgentDataset;
import java.util.Arrays;
import java.util.Collections;

/**
 *
 * @author Peter
 */
public class OPTAgent extends Agent {

    private Plan costSignal;
    private FitnessFunction fitnessFunction;
    private double robustness;
    private int iteration;

    private int activeChild = -1;
    
    private Plan globalPlan;
    private Plan selectedPlan;

    public static class Factory extends AgentFactory {

        @Override
        public Agent create(int id, AgentDataset dataSource, String treeStamp, File outFolder, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize) {
            return new OPTAgent(id, dataSource, treeStamp, initialPhase, outFolder, costSignal, fitnessFunction, getMeasures());
        }
    }

    public OPTAgent(int id, AgentDataset dataSource, String treeStamp, DateTime initialPhase, File outFolder, Plan costSignal, FitnessFunction fitnessFunction, List<CostFunction> measures) {
        super(id, dataSource, treeStamp, outFolder, initialPhase, new ArrayList<>(), new ArrayList<>());
        this.costSignal = costSignal;
        this.fitnessFunction = fitnessFunction;
    }

    @Override
    void runPhase() {
        if (isRoot()) {
            readPlans();

            OPTAggregate msg = new OPTAggregate();
            for (int i = 0; i < possiblePlans.size(); i++) {
                Map<NetworkAddress, Integer> selection = new HashMap<>();
                selection.put(getPeer().getNetworkAddress(), i);
                msg.aggregatedPossiblePlans.put(possiblePlans.get(i), selection);
            }
            activeChild++;
            if (activeChild < children.size()) {
                getPeer().sendMessage(children.get(activeChild).getNetworkAddress(), msg);
            }
        }
    }

    @Override
    public void handleIncomingMessage(Message message) {
        if (message instanceof OPTAggregate) {
            OPTAggregate msg = (OPTAggregate) message;

            if (activeChild == -1) {
                readPlans();

                Map<Plan, Map<NetworkAddress, Integer>> aggregatedPossiblePlans = new HashMap<>();
                for (Map.Entry<Plan, Map<NetworkAddress, Integer>> entry : msg.aggregatedPossiblePlans.entrySet()) {
                    Plan aggregatedPlan = entry.getKey();
                    Map<NetworkAddress, Integer> selection = entry.getValue();

                    for (int i = 0; i < possiblePlans.size(); i++) {
                        Plan localPlan = possiblePlans.get(i);
                        Plan newAggregatedPlan = new AggregatePlan(this);
                        newAggregatedPlan.set(aggregatedPlan);
                        newAggregatedPlan.add(localPlan);

                        HashMap<NetworkAddress, Integer> newSelection = new HashMap<>(selection);
                        newSelection.put(getPeer().getNetworkAddress(), i);

                        aggregatedPossiblePlans.put(newAggregatedPlan, newSelection);
                    }
                }

                msg = new OPTAggregate();
                msg.aggregatedPossiblePlans = aggregatedPossiblePlans;
            }

            if (activeChild + 1 < children.size()) {
                activeChild++;
                getPeer().sendMessage(children.get(activeChild).getNetworkAddress(), msg);
            } else if (!isRoot()) {
                getPeer().sendMessage(parent.getNetworkAddress(), msg);
            } else {
                List<Plan> combinationalPlans = new ArrayList<>(msg.aggregatedPossiblePlans.keySet());
                this.globalPlan = combinationalPlans.get(fitnessFunction.select(this, new AggregatePlan(this), combinationalPlans, costSignal, null));
                Map<NetworkAddress, Integer> selection = msg.aggregatedPossiblePlans.get(globalPlan);
                int selectedPlanIdx = selection.get(getPeer().getNetworkAddress());
                this.selectedPlan = possiblePlans.get(selectedPlanIdx);

                OPTOptimal m = new OPTOptimal();
                m.globalPlan = globalPlan;
                m.selection = selection;
                for (Finger c : children) {
                    getPeer().sendMessage(c.getNetworkAddress(), m);
                }

                robustness = fitnessFunction.getRobustness(globalPlan, costSignal, null);
                //System.out.println(globalPlan.getNumberOfStates() + "," + currentPhase.toString("yyyy-MM-dd") +","+ robustness + ": " + globalPlan);
            }
        } else if (message instanceof OPTOptimal) {
            OPTOptimal msg = (OPTOptimal) message;
            globalPlan = msg.globalPlan;
            selectedPlan = possiblePlans.get(msg.selection.get(getPeer().getNetworkAddress()));
            for (Finger c : children) {
                getPeer().sendMessage(c.getNetworkAddress(), msg);
            }
        }
    }

    private boolean measured = false;

    @Override
    void measure(MeasurementLog log, int epochNumber) {
        if(isRoot()) {
            log.log(epochNumber, iteration, robustness);
            iteration++;
        }
    }

    private void writeGraphData(int epochNumber) {
        System.out.println(getPeer().getNetworkAddress().toString() + ","
                + ((parent != null) ? parent.getNetworkAddress().toString() : "-") + ","
                + findSelectedPlan() + "," + Arrays.toString(possiblePlans.toArray()));
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
