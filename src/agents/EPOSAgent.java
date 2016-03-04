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
import dsutil.generic.state.ArithmeticListState;
import dsutil.generic.state.ArithmeticState;
import dsutil.generic.state.State;
import dsutil.protopeer.FingerDescriptor;
import dsutil.protopeer.services.topology.trees.TreeApplicationInterface;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.TreeMap;
import messages.EPOSBroadcast;
import messages.EPOSRequest;
import messages.EPOSResponse;
import org.joda.time.DateTime;
import protopeer.BasePeerlet;
import protopeer.Finger;
import protopeer.Peer;
import protopeer.measurement.MeasurementFileDumper;
import protopeer.measurement.MeasurementLog;
import protopeer.measurement.MeasurementLoggerListener;
import protopeer.network.Message;
import protopeer.time.Timer;
import protopeer.time.TimerListener;
import protopeer.util.quantities.Time;

/**
 *
 * @author Evangelos
 */
public class EPOSAgent extends BasePeerlet implements TreeApplicationInterface {

    private String experimentID;
    private String plansLocation;
    private String planConfigurations;
    private String treeStamp;
    private String agentMeterID;

    private int planSize;

    private double robustness;

    private DateTime currentPhase;
    private DateTime previousPhase;
    private List<DateTime> phases;
    private int phaseIndex;

    private String plansFormat;
    private MeasurementFileDumper measurementDumper;

    public static enum TopologicalState {
        ROOT,
        LEAF,
        IN_TREE,
        DISCONNECTED
    }

    private int historySize;
    private TreeMap<DateTime, HistoricPlans> history;

    private FingerDescriptor myAgentDescriptor;
    private Finger parent = null;
    private List<Finger> children = new ArrayList<Finger>();
    private TopologicalState topologicalState;
    private Map<Finger, EPOSRequest> messageBuffer;

    private FitnessFunction fitnessFunction;
    private Plan globalPlan;
    private Plan patternPlan;
    private Plan aggregatePlan;
    private Plan selectedPlan;
    private List<Plan> possiblePlans;
    private Plan historicSelectedPlan;
    private Plan historicAggregatePlan;
    private Plan historicGlobalPlan;
    private List<Plan> combinationalPlans;

    private Map<Plan, Map<Finger, Plan>> combinationalPlansMap;
    private Plan selectedCombinationalPlan;

    public EPOSAgent(String experimentID, String plansLocation, String planConfigurations, String treeStamp, String agentMeterID, String plansFormat, FitnessFunction fitnessFunction, int planSize, DateTime initialPhase, DateTime previousPhase, Plan patternPlan, int historySize) {
        this.experimentID = experimentID;
        this.plansLocation = plansLocation;
        this.planConfigurations = planConfigurations;
        this.treeStamp = treeStamp;
        this.agentMeterID = agentMeterID;
        this.currentPhase = initialPhase;
        this.plansFormat = plansFormat;
        this.fitnessFunction = fitnessFunction;
        this.planSize = planSize;
        this.previousPhase = previousPhase;
        this.patternPlan = patternPlan;
        this.historySize = historySize;
        this.phases = new ArrayList<>();
        this.phaseIndex = 0;
        this.possiblePlans = new ArrayList<>();
        this.combinationalPlans = new ArrayList<>();
        this.combinationalPlansMap = new HashMap<>();
        this.history = new TreeMap<>();
        this.messageBuffer = new HashMap<>();
        this.topologicalState = TopologicalState.DISCONNECTED;
    }

    /**
     * Intitializes the load management agent by creating the finger descriptor.
     *
     * @param peer the local peer
     */
    @Override
    public void init(Peer peer) {
        super.init(peer);
        this.myAgentDescriptor = new FingerDescriptor(getPeer().getFinger());
        this.loadCoordinationPhases();
    }

    /**
     * Starts the load management agent by scheduling the epoch measurements and
     * defining its network state
     */
    @Override
    public void start() {
        this.runBootstrap();
        scheduleMeasurements();
    }

    /**
     * Stops the load management agent
     */
    @Override
    public void stop() {

    }

    /**
     * The scheduling of the active state. Computes the output load and sends
     * the load to the network. It is executed periodically.
     */
    private void runBootstrap() {
        Timer loadAgentTimer = getPeer().getClock().createNewTimer();
        loadAgentTimer.addTimerListener(new TimerListener() {
            public void timerExpired(Timer timer) {
                runActiveState();
            }
        });
        loadAgentTimer.schedule(Time.inMilliseconds(2000));
    }

    boolean hasBroadcast = false;

    /**
     * The scheduling of the active state. Computes the output load and sends
     * the load to the network. It is executed periodically.
     */
    private void runActiveState() {
        Timer loadAgentTimer = getPeer().getClock().createNewTimer();
        loadAgentTimer.addTimerListener(new TimerListener() {
            public void timerExpired(Timer timer) {
//                System.out.println(coordinationPhaseIndex);
                if (phaseIndex < phases.size()) {
                    clearCoordinationPhase();
                    if (topologicalState == TopologicalState.LEAF) {
                        plan();
                        informParent();
                    }
                    runActiveState();
                }
            }
        });
        loadAgentTimer.schedule(Time.inMilliseconds(1000));
    }

    private void clearCoordinationPhase() {
        if (this.history.size() > this.historySize) {
            this.history.remove(this.history.firstKey());
        }
        currentPhase = phases.get(phaseIndex);
        if (phaseIndex > 0) {
            previousPhase = phases.get(phaseIndex - 1);
        }
        phaseIndex++;
        this.robustness = 0.0;
        this.possiblePlans.clear();
        this.selectedPlan = new PossiblePlan(this);
        this.aggregatePlan = new AggregatePlan(this);
        this.globalPlan = new GlobalPlan(this);
        this.historicSelectedPlan = new PossiblePlan(this);
        this.historicAggregatePlan = new AggregatePlan(this);
        this.historicGlobalPlan = new GlobalPlan(this);
        this.combinationalPlans.clear();
        this.combinationalPlansMap.clear();
        this.selectedCombinationalPlan = new CombinationalPlan(this);
        this.messageBuffer.clear();
    }

    private void loadCoordinationPhases() {
        File agentDirectory = new File(this.plansLocation + "/" + this.planConfigurations + "/" + this.agentMeterID);
        File[] dates = agentDirectory.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isHidden()) {
                    return false;
                }
                return pathname.isFile();
            }
        });
        for (int i = 0; i < dates.length; i++) {
            StringTokenizer dateTokenizer = new StringTokenizer(dates[i].getName(), ".");
            this.phases.add(DateTime.parse(dateTokenizer.nextToken()));
        }
    }

    public void plan() {
        try {
            File file = new File(this.plansLocation + "/" + this.planConfigurations + "/" + this.agentMeterID + "/" + this.currentPhase.toString("yyyy-MM-dd") + this.plansFormat);
            Scanner scanner = new Scanner(file);
            scanner.useLocale(Locale.US);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                this.possiblePlans.add(new PossiblePlan(this, line));
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void informParent() {
        EPOSRequest request = new EPOSRequest();
        request.child = getPeer().getFinger();
        request.possiblePlans = this.possiblePlans;
        request.aggregatePlan = this.aggregatePlan;
        HistoricPlans historicPlans = history.get(previousPhase);
        if (historicPlans != null) {
            //if (this.historicEnergyPlans.size() != 0) {
            //    Map<HistoricEnergyPlans, Plan> historicPlans = this.historicEnergyPlans.get(this.historicAggregationPhase);
            request.aggregateHistoryPlan = historicPlans.aggregatedPlan;
        } else {
            request.aggregateHistoryPlan = null;
        }
        this.getPeer().sendMessage(this.parent.getNetworkAddress(), request);
    }

    public void preProcessing() {
        int complexity = 1;
        ArrayList<Integer> inputPossiblePlanIndices = new ArrayList<Integer>();
        ArrayList<Integer> numberOfInputPossiblePlans = new ArrayList<Integer>();
        for (Finger child : children) {
            this.aggregatePlan.add(this.messageBuffer.get(child).aggregatePlan);
            inputPossiblePlanIndices.add(0);
            numberOfInputPossiblePlans.add(this.messageBuffer.get(child).possiblePlans.size());
            complexity *= this.messageBuffer.get(child).possiblePlans.size();
        }
        for (int i = 0; i < complexity; i++) {
            Plan combinationalPlan = new CombinationalPlan(this);
            HashMap<Finger, Plan> inputPossiblePlans = new HashMap<>();
            for (int c = 0; c < this.children.size(); c++) {
                Finger child = this.children.get(c);
                List<Plan> possiblePlans = this.messageBuffer.get(child).possiblePlans;
                Plan inputPossiblePlan = possiblePlans.get(inputPossiblePlanIndices.get(c));
                combinationalPlan.add(inputPossiblePlan);
                double discomfort = combinationalPlan.getDiscomfort() + inputPossiblePlan.getDiscomfort();
                combinationalPlan.setDiscomfort(discomfort);
                inputPossiblePlans.put(child, inputPossiblePlan);
            }
            this.combinationalPlans.add(combinationalPlan);
            this.combinationalPlansMap.put(combinationalPlan, inputPossiblePlans);
            int lastInputPossiblePlanIndex = inputPossiblePlanIndices.size() - 1;
            inputPossiblePlanIndices.set(lastInputPossiblePlanIndex, inputPossiblePlanIndices.get(lastInputPossiblePlanIndex) + 1);
            for (int j = lastInputPossiblePlanIndex; j > 0; j--) {
                if (inputPossiblePlanIndices.get(j) >= numberOfInputPossiblePlans.get(j)) {
                    inputPossiblePlanIndices.set(j, 0);
                    inputPossiblePlanIndices.set(j - 1, inputPossiblePlanIndices.get(j - 1) + 1);
                }
            }
        }
    }

    public void select() {
        HistoricPlans historicPlans = null;
        if (this.phaseIndex > 1) {
            historicPlans = this.history.get(this.previousPhase);
        }
        this.selectedCombinationalPlan = fitnessFunction.select(this, aggregatePlan, combinationalPlans, patternPlan, historicPlans);
    }

    public void update() {
        this.aggregatePlan.add(this.selectedCombinationalPlan);
        this.historicAggregatePlan.set(aggregatePlan);
    }

    public void informChildren() {
        for (Finger child : children) {
            Plan selectedPlan = this.combinationalPlansMap.get(selectedCombinationalPlan).get(child);
            EPOSResponse response = new EPOSResponse();
            response.selectedPlan = selectedPlan;
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
                this.plan();
                if (this.topologicalState == TopologicalState.ROOT) {
                    HistoricPlans historicPlans = null;
                    if (this.phaseIndex > 1) {
                        historicPlans = history.get(this.previousPhase);
                    }
                    Plan selectedPlan = fitnessFunction.select(this, aggregatePlan, possiblePlans, globalPlan, historicPlans);
                    this.selectedPlan.set(selectedPlan);
                    this.selectedPlan.setDiscomfort(selectedPlan.getDiscomfort());

                    this.aggregatePlan.add(selectedPlan);
                    this.globalPlan.add(aggregatePlan);

                    historicPlans = new HistoricPlans(globalPlan, aggregatePlan, selectedPlan);
                    this.history.put(this.currentPhase, historicPlans);

                    this.robustness = fitnessFunction.getRobustness(globalPlan, patternPlan, historicPlans);

                    System.out.print(globalPlan.getNumberOfStates() + "," + currentPhase.toString("yyyy-MM-dd") + ",");
                    for (ArithmeticState value : globalPlan.getArithmeticStates()) {
                        System.out.print(value.getValue() + ",");
                    }
                    System.out.print(this.robustness);
                    System.out.println();
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

            this.robustness = fitnessFunction.getRobustness(globalPlan, patternPlan, historicPlans);

            if (this.topologicalState == TopologicalState.LEAF) {
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
    public void setParent(Finger parent) {
        if (parent != null) {
            this.parent = parent;
        }
        this.computeTopologicalState();
    }

    @Override
    public void setChildren(List<Finger> children) {
        for (Finger child : children) {
            if (child != null) {
                this.children.add(child);
            }
        }
        this.computeTopologicalState();
    }

    @Override
    public void setTreeView(Finger parent, List<Finger> children) {
        this.setParent(parent);
        this.setChildren(children);
        this.computeTopologicalState();
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

    public TopologicalState getTopologicalState() {
        return this.topologicalState;
    }

    //****************** MEASUREMENTS ******************
    /**
     * Scheduling the measurements for the load management agent
     */
    private void scheduleMeasurements() {
        this.measurementDumper = new MeasurementFileDumper("peersLog/" + this.experimentID + getPeer().getIdentifier().toString());
        getPeer().getMeasurementLogger().addMeasurementLoggerListener(new MeasurementLoggerListener() {
            @Override
            public void measurementEpochEnded(MeasurementLog log, int epochNumber) {
                if (epochNumber == 2) {
                    if (topologicalState == TopologicalState.ROOT) {
                        log.log(epochNumber, globalPlan, 1.0);
                        log.log(epochNumber, EPOSMeasures.PLAN_SIZE, planSize);
                        log.log(epochNumber, EPOSMeasures.ROBUSTNESS, robustness);
                    }
                    log.log(epochNumber, selectedPlan, 1.0);
                    log.log(epochNumber, EPOSMeasures.DISCOMFORT, selectedPlan.getDiscomfort());
//                    log.log(epochNumber, Measurements.SELECTED_PLAN_VALUE, selectedPlan.getArithmeticState(0).getValue());
                    writeGraphData(epochNumber);
                }
                measurementDumper.measurementEpochEnded(log, epochNumber);
                log.shrink(epochNumber, epochNumber + 1);
            }
        });
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

    private boolean isEqual(ArithmeticListState planA, ArithmeticListState planB) {
        for (int i = 0; i < planA.getArithmeticStates().size(); i++) {
            if (planA.getArithmeticState(i).getValue() != planB.getArithmeticState(i).getValue()) {
                return false;
            }
        }
        return true;
    }

    private int findSelectedPlan() {
        for (int i = 0; i < possiblePlans.size(); i++) {
            if (isEqual(possiblePlans.get(i), selectedPlan)) {
                return i;
            }
        }
        return -1;
    }

    public void initPlan(Plan plan) {
        for (int i = 0; i < planSize; i++) {
            plan.addArithmeticState(new ArithmeticState(0.0));
        }
        plan.setCoordinationPhase(currentPhase);
        plan.setDiscomfort(0.0);
        plan.setAgentMeterID(agentMeterID);
        plan.setConfiguration(planConfigurations + "-" + treeStamp);
    }

    public void initPlan(Plan plan, String planStr) {
        plan.setCoordinationPhase(currentPhase);

        Scanner scanner = new Scanner(planStr);
        scanner.useLocale(Locale.US);
        scanner.useDelimiter(":");
        double score = scanner.nextDouble();
        plan.setDiscomfort(1.0 - score);

        scanner.useDelimiter(",");
        scanner.skip(":");
        while (scanner.hasNextDouble()) {
            plan.addArithmeticState(new ArithmeticState(scanner.nextDouble()));
        }
    }
}
