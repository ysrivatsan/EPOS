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

import agents.dataset.AgentDataset;
import agents.fitnessFunction.FitnessFunction;
import agents.fitnessFunction.costFunction.CostFunction;
import agents.plan.AggregatePlan;
import agents.plan.GlobalPlan;
import agents.plan.Plan;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import protopeer.Finger;
import protopeer.measurement.MeasurementLog;
import protopeer.network.Message;
import protopeer.network.NetworkAddress;
import protopeer.time.Timer;
import protopeer.util.quantities.Time;

/**
 *
 * @author Peter
 */
public class CohdaAgent extends Agent {

    private int age;
    private int step;
    private int numSteps;
    private KnowledgeBase best;
    private KnowledgeBase current;
    private FitnessFunction fitnessFunction;

    private List<Finger> neighbours = new ArrayList<>();

    private Plan zero;

    public static class Factory extends AgentFactory {

        @Override
        public Agent create(int id, AgentDataset dataSource, String treeStamp, File outFolder, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize) {
            return new CohdaAgent(id, dataSource, treeStamp, outFolder, initialPhase, getMeasures(), getLocalMeasures(), inMemory, fitnessFunction, numIterations);
        }

        @Override
        public String toString() {
            return "COHDA";
        }
    }

    public CohdaAgent(int experimentId, AgentDataset dataSource, String treeStamp, File outFolder, DateTime initialPhase, List<CostFunction> measures, List<CostFunction> localMeasures, boolean inMemory, FitnessFunction fitnessFunction, int numSteps) {
        super(experimentId, dataSource, treeStamp, outFolder, initialPhase, measures, localMeasures, inMemory);
        this.fitnessFunction = fitnessFunction;
        this.numSteps = numSteps;
        this.step = numSteps;
    }

    @Override
    void runActiveState() {
        if (step < numSteps - 1) {
            Timer loadAgentTimer = getPeer().getClock().createNewTimer();
            loadAgentTimer.addTimerListener((Timer timer) -> {
                runStep();
                runActiveState();
            });
            loadAgentTimer.schedule(Time.inMilliseconds(1000));
        } else {
            super.runActiveState();
        }
    }

    @Override
    final void runPhase() {
        step = -1;
        possiblePlans.clear();

        initPhase();
        runStep();
    }

    private void runStep() {
        step++;

        if (step < numSteps) {
            initStep();
            publish();
        }
    }

    private void initPhase() {
        neighbours.clear();
        neighbours.addAll(children);
        if (parent != null) {
            neighbours.add(parent);
        }
        zero = new AggregatePlan(this);
        current = new KnowledgeBase();
        best = new KnowledgeBase();
        age = 0;

        readPlans();
        update(null);
    }

    private void initStep() {
        /*if(isRoot()) {
            System.out.println("\nstep " + step + ":");
        }*/
    }

    @Override
    public void handleIncomingMessage(Message message) {
        if (message instanceof CohdaMessage) {
            CohdaMessage msg = (CohdaMessage) message;
            update(msg);
        }
    }

    private void update(CohdaMessage msg) {
        boolean somethingChanged = false;
        if (msg == null) {
            somethingChanged = true;
        } else {
            somethingChanged = current.updateWith(msg.current);
            if (betterThanBest(msg.best)) {
                best = msg.best;
                somethingChanged = true;
            }
        }

        if (somethingChanged) {
            choose();
        }
    }

    private void choose() {
        Plan aggregate = current.aggregate();

        int selected = fitnessFunction.select(this, aggregate, possiblePlans, zero, null);
        Plan selectedPlan = possiblePlans.get(selected);

        if (selectedPlan.equals(current.getLocal()) && current.size() <= best.size()) {
            current = new KnowledgeBase(best);
            selectedPlan = current.getLocal();
        }
        current.updateLocal(selectedPlan);

        measureLocal(selectedPlan, zero, selected, possiblePlans.size());

        if (betterThanBest(current)) {
            best = new KnowledgeBase(current);
        }

        /*if(isRoot()) {
            System.out.println(best.size());
        }*/
        measureGlobal(best.global(), zero);
        //measureGlobal(current.global(), zero);
        if(isRoot()) {
            //System.out.println();
        }
    }

    private void publish() {
        CohdaMessage msg = new CohdaMessage();
        msg.best = new KnowledgeBase(best);
        msg.current = new KnowledgeBase(current);
        //System.out.print(current.size()+",");
        for (Finger neighbour : neighbours) {
            getPeer().sendMessage(neighbour.getNetworkAddress(), msg);
        }
    }

    private boolean betterThanBest(KnowledgeBase other) {
        if (best.size() < other.size()) {
            return true;
        } else if (best.size() == other.size()) {
            int selected = fitnessFunction.select(this, zero, Arrays.asList(best.global(), other.global()), zero, null);
            return selected == 1;
        } else {
            return false;
        }
    }

    @Override
    void measure(MeasurementLog log, int epochNumber) {
        TreeNode node = null;
        if (!localMeasures.isEmpty()) {
            node = new TreeNode(getPeer().getFinger(), children);
        }
        for (CostFunction func : localMeasures) {
            log.log(epochNumber, step, "local-" + func.getMetric(), node, (Double) localMeasurements.get(func.getMetric()));
        }
        if (isRoot()) {
            for (CostFunction func : measures) {
                log.log(epochNumber, step, "global-" + func.getMetric(), (Double) measurements.get(func.getMetric()));
            }
        }
    }

    private final class KnowledgeBase {

        private Map<NetworkAddress, Weight> weights = new HashMap<>();
        private Plan global;

        public KnowledgeBase() {
        }

        public KnowledgeBase(KnowledgeBase other) {
            set(other);
        }

        public void set(KnowledgeBase newKb) {
            weights.clear();
            weights.putAll(newKb.weights);
            global = newKb.global;
        }

        public boolean updateWith(KnowledgeBase other) {
            boolean changed = false;

            /*if(isRoot()) {
                System.out.print(weights.keySet() + " + " + other.weights.keySet());
            }*/
            
            global = null;

            for (NetworkAddress agent : other.weights.keySet()) {
                Weight weight = weights.get(agent);
                Weight otherWeight = other.weights.get(agent);

                if (weight == null || weight.age < otherWeight.age) {
                    weights.put(agent, otherWeight);
                    changed = true;
                }
            }
            
            /*if(isRoot()) {
                System.out.println(" = " + weights.keySet());
            }*/

            return changed;
        }

        public void updateLocal(Plan newPlan) {
            global = null;
            NetworkAddress key = getPeer().getNetworkAddress();

            Weight prevWeight = weights.get(key);

            Weight newWeight = new Weight();
            if (prevWeight != null) {
                newWeight.age = CohdaAgent.this.age++;
            }
            newWeight.weight = newPlan;

            weights.put(key, newWeight);
        }

        public Plan getLocal() {
            Weight weight = weights.get(getPeer().getNetworkAddress());
            if (weight != null) {
                return weight.weight;
            } else {
                return null;
            }
        }

        public Plan global() {
            if (global == null) {
                global = weights.values().stream()
                        .map(x -> x.weight)
                        .reduce(new GlobalPlan(CohdaAgent.this), (x, y) -> {
                            x.add(y);
                            return x;
                        });
            }
            return global;
        }

        public Plan aggregate() {
            Plan aggregate = weights.entrySet().stream()
                    .filter(x -> !getPeer().getNetworkAddress().equals(x.getKey()))
                    .map(x -> x.getValue().weight)
                    .reduce(new AggregatePlan(CohdaAgent.this), (x, y) -> {
                        x.add(y);
                        return x;
                    });
            return aggregate;
        }

        public int size() {
            return weights.size();
        }
    }

    private class Weight {

        public Plan weight;
        public int age;
    }

    private class CohdaMessage extends Message {

        public KnowledgeBase best;
        public KnowledgeBase current;
    }
}
