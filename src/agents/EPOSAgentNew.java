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
public class EPOSAgentNew extends BasePeerlet implements TreeApplicationInterface {

    private String experimentID;
    private String plansLocation;
    private String planConfigurations;
    private String treeStamp;
    private String agentMeterID;
    private DateTime aggregationPhase;
    private List<DateTime> coordinationPhases;
    private int coordinationPhaseIndex;
    private String plansFormat;
    private DateTime historicAggregationPhase;
    private MeasurementFileDumper measurementDumper;

    public static enum Measurements {
        COORDINATION_PHASE,
        PLAN_SIZE,
        SELECTED_PLAN_VALUE,
        DISCOMFORT,
        ROBUSTNESS,
        GLOBAL_PLAN
    }

    public static enum TopologicalState {
        ROOT,
        LEAF,
        IN_TREE,
        DISCONNECTED
    }

    public static enum HistoricEnergyPlans {
        SELECTED_PLAN,
        AGGREGATE_PLAN,
        GLOBAL_PLAN
    }
    private double robustness;
    private int energyPlanSize;
    private int historySize;
    private FingerDescriptor myAgentDescriptor;
    private Finger parent = null;
    private List<Finger> children = new ArrayList<Finger>();
    private TopologicalState topologicalState;
    private FitnessFunction fitnessFunction;
    private Plan patternEnergyPlan;
    private List<Plan> possiblePlans;
    private Plan selectedPlan;
    private Plan aggregatePlan;
    private Plan globalPlan;
    private Plan historicSelectedPlan;
    private Plan historicAggregatePlan;
    private Plan historicGlobalPlan;
    private List<Plan> combinationalPlans;
    private Map<Plan, Map<Finger, Plan>> combinationalPlansMap;
    private Plan selectedCombinationalPlan;
    private TreeMap<DateTime, Map<HistoricEnergyPlans, Plan>> historicEnergyPlans;
    private Map<Finger, EPOSRequest> messageBuffer;

    public EPOSAgentNew(String experimentID, String plansLocation, String planConfigurations, String treeStamp, String agentMeterID, String plansFormat, FitnessFunction fitnessFunction, int planSize, DateTime aggregationPhase, DateTime historicAggregationPhase, Plan patternEnergyPlan, int historySize) {
        this.experimentID = experimentID;
        this.plansLocation = plansLocation;
        this.planConfigurations = planConfigurations;
        this.treeStamp = treeStamp;
        this.agentMeterID = agentMeterID;
        this.aggregationPhase = aggregationPhase;
        this.plansFormat = plansFormat;
        this.fitnessFunction = fitnessFunction;
        this.energyPlanSize = planSize;
        this.historicAggregationPhase = historicAggregationPhase;
        this.patternEnergyPlan = patternEnergyPlan;
        this.historySize = historySize;
        this.coordinationPhases = new ArrayList<>();
        this.coordinationPhaseIndex = 0;
        this.possiblePlans = new ArrayList<>();
        this.combinationalPlans = new ArrayList<>();
        this.combinationalPlansMap = new HashMap<>();
        this.historicEnergyPlans = new TreeMap<>();
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
                if (coordinationPhaseIndex < coordinationPhases.size()) {
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
        if (this.historicEnergyPlans.size() > this.historySize) {
            this.historicEnergyPlans.remove(this.historicEnergyPlans.firstKey());
        }
        aggregationPhase = coordinationPhases.get(coordinationPhaseIndex);
        if (coordinationPhaseIndex != 0) {
            historicAggregationPhase = coordinationPhases.get(coordinationPhaseIndex - 1);
        }
        coordinationPhaseIndex++;
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
            this.coordinationPhases.add(DateTime.parse(dateTokenizer.nextToken()));
        }
    }

    public void plan() {
        try {
            File file = new File(this.plansLocation + "/" + this.planConfigurations + "/" + this.agentMeterID + "/" + this.aggregationPhase.toString("yyyy-MM-dd") + this.plansFormat);
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
        if (this.historicEnergyPlans.size() != 0) {
            Map<HistoricEnergyPlans, Plan> historicPlans = this.historicEnergyPlans.get(this.historicAggregationPhase);
            request.aggregateHistoryPlan = historicPlans.get(HistoricEnergyPlans.AGGREGATE_PLAN);
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
        HistoricPlans historic = null;
        if (this.coordinationPhaseIndex > 1) {
            historic = new HistoricPlans(
                    this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.GLOBAL_PLAN),
                    this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.AGGREGATE_PLAN),
                    this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.SELECTED_PLAN));
        }
        this.selectedCombinationalPlan = fitnessFunction.select(this, aggregatePlan, combinationalPlans, patternEnergyPlan, historic);
    }

    public void update() {
        this.aggregatePlan.add(this.selectedCombinationalPlan);
        this.historicAggregatePlan.add(aggregatePlan);
    }

    public void informChildren() {
        for (Finger child : children) {
            Plan selectedPlan = this.combinationalPlansMap.get(this.selectedCombinationalPlan).get(child);
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
                    HistoricPlans historic = null;
                    if (this.coordinationPhaseIndex > 1) {
                        historic = new HistoricPlans(
                                this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.GLOBAL_PLAN),
                                this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.AGGREGATE_PLAN),
                                this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.SELECTED_PLAN));
                    }
                    Plan rootSelectedPlanMinDis = fitnessFunction.select(this, aggregatePlan, possiblePlans, globalPlan, historic);
                    this.selectedPlan.add(rootSelectedPlanMinDis);
                    this.selectedPlan.setDiscomfort(rootSelectedPlanMinDis.getDiscomfort());
                    this.historicSelectedPlan.add(rootSelectedPlanMinDis);

                    this.aggregatePlan.add(selectedPlan);
                    this.globalPlan.add(aggregatePlan);
                    this.historicAggregatePlan.add(aggregatePlan);
                    this.historicGlobalPlan.add(globalPlan);
                    HashMap<HistoricEnergyPlans, Plan> historicPlans = new HashMap<HistoricEnergyPlans, Plan>();
                    historicPlans.put(HistoricEnergyPlans.GLOBAL_PLAN, historicGlobalPlan);
                    historicPlans.put(HistoricEnergyPlans.AGGREGATE_PLAN, historicAggregatePlan);
                    historicPlans.put(HistoricEnergyPlans.SELECTED_PLAN, historicSelectedPlan);
                    this.historicEnergyPlans.put(this.aggregationPhase, historicPlans);

                    historic = new HistoricPlans(historicGlobalPlan, historicAggregatePlan, historicSelectedPlan);
                    this.robustness = fitnessFunction.getRobustness(globalPlan, patternEnergyPlan, historic);

                    System.out.print(globalPlan.getNumberOfStates() + "," + aggregationPhase.toString("yyyy-MM-dd") + ",");
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
            this.selectedPlan.add(response.selectedPlan);
            this.selectedPlan.setDiscomfort(response.selectedPlan.getDiscomfort());
            this.historicSelectedPlan.add(selectedPlan);
        }
        if (message instanceof EPOSBroadcast) {
            EPOSBroadcast broadcast = (EPOSBroadcast) message;
            this.globalPlan.add(broadcast.globalPlan);
            this.historicGlobalPlan.add(globalPlan);
            HashMap<HistoricEnergyPlans, Plan> historicPlans = new HashMap<>();
            historicPlans.put(HistoricEnergyPlans.GLOBAL_PLAN, historicGlobalPlan);
            historicPlans.put(HistoricEnergyPlans.AGGREGATE_PLAN, historicAggregatePlan);
            historicPlans.put(HistoricEnergyPlans.SELECTED_PLAN, historicSelectedPlan);
            this.historicEnergyPlans.put(this.aggregationPhase, historicPlans);

            HistoricPlans historic = new HistoricPlans(historicGlobalPlan, historicAggregatePlan, historicSelectedPlan);
            this.robustness = fitnessFunction.getRobustness(globalPlan, patternEnergyPlan, historic);

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
                        log.log(epochNumber, Measurements.PLAN_SIZE, energyPlanSize);
                        log.log(epochNumber, Measurements.ROBUSTNESS, robustness);
                    }
                    log.log(epochNumber, selectedPlan, 1.0);
                    log.log(epochNumber, Measurements.DISCOMFORT, selectedPlan.getDiscomfort());
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
        for (int i = 0; i < energyPlanSize; i++) {
            plan.addArithmeticState(new ArithmeticState(0.0));
        }
        plan.setCoordinationPhase(aggregationPhase);
        plan.setDiscomfort(0.0);
        plan.setAgentMeterID(agentMeterID);
        plan.setConfiguration(planConfigurations + "-" + treeStamp);
    }

    public void initPlan(Plan plan, String planStr) {
        plan.setCoordinationPhase(aggregationPhase);

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
