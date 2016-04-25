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
import agents.plan.GlobalPlan;
import agents.fitnessFunction.IterativeFitnessFunction;
import agents.fitnessFunction.costFunction.CostFunction;
import agents.plan.Plan;
import agents.plan.PossiblePlan;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import messages.IGreedyDown;
import messages.IGreedyUp;
import org.joda.time.DateTime;
import protopeer.measurement.MeasurementLog;
import agents.dataset.AgentDataset;
import cern.colt.Arrays;

/**
 *
 * @author Evangelos
 */
public class IGreedyAgent extends IterativeAgentTemplate<IGreedyUp, IGreedyDown> {
    private boolean outputMovie;

    private final int historySize;
    private final TreeMap<DateTime, AgentPlans> history = new TreeMap<>();

    private int numNodes;
    private int numNodesSubtree;
    private int layer;
    private double avgNumChildren;

    private IterativeFitnessFunction fitnessFunctionPrototype;
    private IterativeFitnessFunction fitnessFunction;
    private List<Double> measurements = new ArrayList<>();

    private Plan costSignal;
    private AgentPlans current = new AgentPlans();
    private AgentPlans previous = new AgentPlans();
    private AgentPlans prevAggregate = new AgentPlans();
    private AgentPlans historic;

    private List<Integer> selectedCombination = new ArrayList<>();
    private LocalSearch localSearch;

    public static class Factory extends AgentFactory {
        public boolean outputMovie;

        @Override
        public Agent create(int id, AgentDataset dataSource, String treeStamp, File outFolder, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize) {
            return new IGreedyAgent(id, dataSource, treeStamp, outFolder, (IterativeFitnessFunction) fitnessFunction, initialPhase, previousPhase, costSignal, historySize, numIterations, localSearch, outputMovie, measures);
        }
    
        @Override
        public String toString() {
            return "IGreedy";
        }
    }

    public IGreedyAgent(int id, AgentDataset dataSource, String treeStamp, File outFolder, IterativeFitnessFunction fitnessFunction, DateTime initialPhase, DateTime previousPhase, Plan costSignal, int historySize, int numIterations, LocalSearch localSearch, boolean outputMovie, List<CostFunction> measures) {
        super(id, dataSource, treeStamp, outFolder, initialPhase, numIterations, measures);
        this.fitnessFunctionPrototype = fitnessFunction;
        this.historySize = historySize;
        this.costSignal = costSignal;
        this.localSearch = localSearch==null?null:localSearch.clone();
        this.outputMovie = outputMovie;
        if(measures.isEmpty()) {
            measures.add(fitnessFunctionPrototype);
        }
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
    }

    @Override
    void initIteration() {
        measurements.clear();
        current = new AgentPlans();
        current.globalPlan = new GlobalPlan(this);
        current.aggregatePlan = new AggregatePlan(this);
        current.selectedPlan = new PossiblePlan(this);
        current.selectedCombinationalPlan = new PossiblePlan(this);
        numNodesSubtree = 1;
        avgNumChildren = children.size();
        layer = 0;
    }

    @Override
    public IGreedyUp up(List<IGreedyUp> msgs) {
        Plan childAggregatePlan = new AggregatePlan(this);
        
        
        if(localSearch != null) {
            List<Plan> childAggregatePlans = new ArrayList<>();
            for(IGreedyUp msg : msgs) {
                childAggregatePlans.add(msg.aggregatePlan);
            }
            childAggregatePlan = localSearch.calcAggregate(this, childAggregatePlans, previous.globalPlan);
        } else {
            for(IGreedyUp msg : msgs) {
                childAggregatePlan.add(msg.aggregatePlan);
            }
        }

        for (IGreedyUp msg : msgs) {
            numNodesSubtree += msg.numNodes;
        }

        // select best combination
        int selectedPlan = fitnessFunction.select(this, childAggregatePlan, possiblePlans, costSignal, historic, prevAggregate, numNodes, numNodesSubtree, layer, avgNumChildren, iteration);
        current.selectedPlan = possiblePlans.get(selectedPlan);
        current.selectedCombinationalPlan = current.selectedPlan;
        current.aggregatePlan.set(childAggregatePlan);
        current.aggregatePlan.add(current.selectedPlan);

        IGreedyUp msg = new IGreedyUp();
        msg.aggregatePlan = current.aggregatePlan;
        msg.numNodes = numNodesSubtree;
        return msg;
    }

    @Override
    public IGreedyDown atRoot(IGreedyUp rootMsg) {
        current.globalPlan.set(current.aggregatePlan);

        this.history.put(this.currentPhase, current);

        // Log + output
        if(outputMovie) {
            if(iteration == 0) {
                System.out.println("C(1:"+costSignal.getNumberOfStates()+","+(iteration+1)+")="+costSignal+";");
            }
            System.out.println("D(1:"+costSignal.getNumberOfStates()+","+(iteration+1)+")="+current.globalPlan+";");
            
            Plan c = costSignal.clone();
            if(prevAggregate.globalPlan != null) {
                c.multiply(1+iteration);
                c.add(prevAggregate.globalPlan);
            }
            System.out.println("T(1:"+costSignal.getNumberOfStates()+","+(iteration+1)+")="+c+";");
        } else {
            if(iteration%10 == 9) {
                System.out.print("%");
            }
            if(iteration%100 == 99) {
                System.out.print(" ");
            }
            if(iteration+1 == numIterations) {
                System.out.println("");
            }
        }

        IGreedyDown msg = new IGreedyDown(current.globalPlan, numNodesSubtree, 0, 0);
        return msg;
    }

    @Override
    public List<IGreedyDown> down(IGreedyDown parent) {
        current.globalPlan.set(parent.globalPlan);
        if(parent.discard) {
            current.aggregatePlan = previous.aggregatePlan;
            current.selectedPlan = previous.selectedPlan;
            current.selectedCombinationalPlan = previous.selectedCombinationalPlan;
        }
        numNodes = parent.numNodes;
        layer = parent.hops;
        avgNumChildren = parent.sumChildren / Math.max(0.1, (double) parent.hops);

        for(CostFunction func : measures) {
            measurements.add(func.calcCost(current.globalPlan, costSignal));
        }
        fitnessFunction.updatePrevious(prevAggregate, current, iteration);
        previous = current;

        List<IGreedyDown> msgs = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            IGreedyDown msg = new IGreedyDown(parent.globalPlan, parent.numNodes, parent.hops + 1, parent.sumChildren + children.size());
            if(localSearch != null) {
                msg.discard = parent.discard || !localSearch.getSelected().get(i);
            }
            msgs.add(msg);
        }
        return msgs;
    }

    @Override
    void measure(MeasurementLog log, int epochNumber) {
        if(isRoot()) {
            for(int i=0; i<measures.size(); i++) {
                log.log(epochNumber, iteration, measurements.get(i));
            }
        }
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
