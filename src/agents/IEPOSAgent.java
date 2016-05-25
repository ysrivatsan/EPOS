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
import agents.fitnessFunction.IterativeFitnessFunction;
import agents.fitnessFunction.costFunction.CostFunction;
import agents.plan.Plan;
import agents.plan.PossiblePlan;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import messages.IEPOSDown;
import messages.IEPOSUp;
import org.joda.time.DateTime;
import agents.dataset.AgentDataset;
import agents.log.AgentLogger;
import java.util.Random;
import protopeer.measurement.MeasurementLog;

/**
 *
 * @author Evangelos
 */
public class IEPOSAgent extends IterativeAgentTemplate<IEPOSUp, IEPOSDown> {

    private final int historySize;
    private final TreeMap<DateTime, AgentPlans> history = new TreeMap<>();

    private int numNodes;
    private int numNodesSubtree;
    private int layer;
    private double avgNumChildren;

    private final IterativeFitnessFunction fitnessFunctionPrototype;
    private IterativeFitnessFunction fitnessFunction;
    private IterativeFitnessFunction fitnessFunctionRoot;
    private final List<Double> measurements = new ArrayList<>();

    private Plan costSignal;
    private AgentPlans current = new AgentPlans();
    private AgentPlans previous = new AgentPlans();
    private AgentPlans prevAggregate = new AgentPlans();
    private AgentPlans historic;

    private Double rampUpRate;

    private List<Integer> selectedCombination = new ArrayList<>();
    private LocalSearch localSearch;

    private List<AgentLogger> loggers = new ArrayList<>();

    public static class Factory extends AgentFactory {

        @Override
        public Agent create(int id, AgentDataset dataSource, String treeStamp, File outFolder, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize) {
            return new IEPOSAgent(id, dataSource, treeStamp, outFolder, (IterativeFitnessFunction) fitnessFunction, initialPhase, previousPhase, costSignal, historySize, numIterations, localSearch, getLoggers(), getMeasures(), getLocalMeasures(), rampUpRate, inMemory);
        }

        @Override
        public String toString() {
            return "IEPOS";
        }
    }

    public IEPOSAgent(int id, AgentDataset dataSource, String treeStamp, File outFolder, IterativeFitnessFunction fitnessFunction, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize, int numIterations, LocalSearch localSearch, List<AgentLogger> loggers, List<CostFunction> measures, List<CostFunction> localMeasures, Double rampUpRate, boolean inMemory) {
        super(id, dataSource, treeStamp, outFolder, initialPhase, numIterations, measures, localMeasures, inMemory);
        this.fitnessFunctionPrototype = fitnessFunction;
        this.historySize = historySize;
        this.costSignal = costSignal;
        this.localSearch = localSearch == null ? null : localSearch.clone();
        this.loggers = loggers;
        this.rampUpRate = rampUpRate;
    }

    @Override
    void initPhase() {
        if (this.history.size() > this.historySize) {
            this.history.remove(this.history.firstKey());
        }
        prevAggregate.reset();
        previous.reset();

        if (previousPhase != null) {
            this.historic = history.get(previousPhase);
        } else {
            this.historic = null;
        }
        numNodes = -1;
        fitnessFunction = fitnessFunctionPrototype.clone();
        if (isRoot()) {
            fitnessFunctionRoot = fitnessFunctionPrototype.clone();
        }

        // init loggers
        for (AgentLogger logger : loggers) {
            logger.init(getPeer().getIndexNumber());
            if (isRoot()) {
                logger.initRoot(costSignal);
            }
        }
    }

    @Override
    void initIteration() {
        current = new AgentPlans();
        current.global = new GlobalPlan(this);
        current.aggregate = new AggregatePlan(this);
        current.selectedLocalPlan = new PossiblePlan(this);
        current.selectedPlan = new CombinationalPlan(this);
        numNodesSubtree = 1;
        avgNumChildren = children.size();
        layer = 0;
    }

    @Override
    public IEPOSUp up(List<IEPOSUp> msgs) {
        if (!msgs.isEmpty()) {
            Plan childAggregate = new AggregatePlan(this);
            List<Plan> combinationalPlans = new ArrayList<>();
            List<List<Integer>> combinationalSelections = new ArrayList<>();

            if (localSearch != null) {
                List<Plan> childAggregates = new ArrayList<>();
                for (IEPOSUp msg : msgs) {
                    childAggregates.add(msg.aggregate);
                }
                childAggregate = localSearch.calcAggregate(this, childAggregates, previous.global, costSignal);
            } else {
                for (IEPOSUp msg : msgs) {
                    childAggregate.add(msg.aggregate);
                }
            }

            // init combinations
            int numCombinations = 1;
            for (IEPOSUp msg : msgs) {
                numNodesSubtree += msg.numNodes;
                numCombinations *= msg.possiblePlans.size();
            }

            for (int i = 0; i < numCombinations; i++) {
                combinationalPlans.add(new CombinationalPlan(this));
                combinationalSelections.add(new ArrayList<>());
            }

            // calc all possible combinations
            int factor = 1;
            for (IEPOSUp msg : msgs) {
                List<Plan> childPlans = msg.possiblePlans;
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
            List<Plan> subSelectablePlans = combinationalPlans;
            List<List<Integer>> subCombinationalSelections = combinationalSelections;
            if (rampUpRate != null && !isRoot()) {
                Random r = new Random(getPeer().getIndexNumber());
                int numPlans = (int) Math.floor(iteration * rampUpRate + 2 + r.nextDouble() * 2 - 1);
                subSelectablePlans = combinationalPlans.subList(0, Math.min(numPlans, possiblePlans.size()));
                subCombinationalSelections = subCombinationalSelections.subList(0, Math.min(numPlans, combinationalPlans.size()));;
            }
            int selectedCombination = fitnessFunction.select(this, childAggregate, subSelectablePlans, costSignal, numNodes, numNodesSubtree, layer, avgNumChildren, iteration);
            this.selectedCombination = subCombinationalSelections.get(selectedCombination);

            current.selectedPlan = subSelectablePlans.get(selectedCombination);
            current.aggregate.set(childAggregate);
            current.aggregate.add(current.selectedPlan);
        }

        IEPOSUp msg = new IEPOSUp();
        msg.possiblePlans = possiblePlans;
        msg.aggregate = current.aggregate;
        msg.numNodes = numNodesSubtree;
        return msg;
    }

    @Override
    public IEPOSDown atRoot(IEPOSUp rootMsg) {
        int selected = fitnessFunctionRoot.select(this, current.aggregate, possiblePlans, costSignal, numNodes, numNodesSubtree, layer, avgNumChildren, iteration);
        current.selectedLocalPlan = possiblePlans.get(selected);
        measureLocal(current.selectedLocalPlan, costSignal, selected, possiblePlans.size());
        current.global.set(current.aggregate);
        current.global.add(current.selectedLocalPlan);

        this.history.put(this.currentPhase, current);

        IEPOSDown msg = new IEPOSDown(current.global, numNodesSubtree, 0, 0, selected);
        return msg;
    }

    @Override
    public List<IEPOSDown> down(IEPOSDown parent) {
        current.global.set(parent.globalPlan);
        if (parent.discard) {
            current.aggregate = previous.aggregate;
            current.selectedLocalPlan = previous.selectedLocalPlan;
            current.selectedPlan = previous.selectedPlan;
        } else {
            current.selectedLocalPlan.set(possiblePlans.get(parent.selected));
        }
        measureLocal(current.selectedLocalPlan, costSignal, parent.selected, possiblePlans.size());

        if (isRoot()) {
            measureGlobal(costSignal, costSignal);
        }

        numNodes = parent.numNodes;
        layer = parent.hops;
        avgNumChildren = parent.sumChildren / Math.max(0.1, (double) parent.hops);

        fitnessFunction.updatePrevious(current, costSignal, iteration);
        if (isRoot()) {
            fitnessFunctionRoot.updatePrevious(current, costSignal, iteration);
        }
        previous = current;

        List<IEPOSDown> msgs = new ArrayList<>();
        for (int i = 0; i < selectedCombination.size(); i++) {
            int selected = selectedCombination.get(i);
            IEPOSDown msg = new IEPOSDown(parent.globalPlan, parent.numNodes, parent.hops + 1, parent.sumChildren + children.size(), selected);
            if (localSearch != null) {
                msg.discard = parent.discard || !localSearch.getSelected().get(i);
            }
            msgs.add(msg);
        }
        return msgs;
    }

    @Override
    void measure(MeasurementLog log, int epochNumber) {
        super.measure(log, epochNumber);

        for (AgentLogger logger : loggers) {
            logger.log(log, epochNumber, iteration, current.selectedLocalPlan);
            if (isRoot()) {
                logger.logRoot(log, epochNumber, iteration, current.global, numIterations);
            }
        }
    }
}
