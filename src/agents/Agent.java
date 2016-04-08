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

import agents.plan.Plan;
import agents.plan.PossiblePlan;
import dsutil.generic.state.ArithmeticState;
import dsutil.protopeer.services.topology.trees.TreeApplicationInterface;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.StringTokenizer;
import org.joda.time.DateTime;
import protopeer.BasePeerlet;
import protopeer.Finger;
import protopeer.Peer;
import protopeer.measurement.MeasurementFileDumper;
import protopeer.measurement.MeasurementLog;
import protopeer.measurement.MeasurementLogger;
import protopeer.measurement.MeasurementLoggerListener;
import protopeer.network.Message;
import protopeer.time.Timer;
import protopeer.time.TimerListener;
import protopeer.util.quantities.Time;

/**
 *
 * @author Peter
 */
public abstract class Agent extends BasePeerlet implements TreeApplicationInterface {
    Finger parent = null;
    final List<Finger> children = new ArrayList<>();
    private TopologicalState topologicalState = TopologicalState.DISCONNECTED;
    
    private final String plansLocation;
    private final String planConfigurations;
    private final String treeStamp;
    private final String agentMeterID;
    
    DateTime currentPhase;
    DateTime previousPhase = null;
    final List<DateTime> phases = new ArrayList<>();
    private int phaseIndex = 0;
    
    private final String plansFormat;
    private final int planSize;
    
    final List<Plan> possiblePlans = new ArrayList<>();
    
    private static enum TopologicalState {
        ROOT, LEAF, IN_TREE, DISCONNECTED
    }

    public Agent(String plansLocation, String planConfigurations, String treeStamp, String agentMeterID, DateTime initialPhase, String plansFormat, int planSize) {
        this.plansLocation = plansLocation;
        this.planConfigurations = planConfigurations;
        this.treeStamp = treeStamp;
        this.agentMeterID = agentMeterID;
        this.currentPhase = initialPhase;
        this.plansFormat = plansFormat;
        this.planSize = planSize;
    }
    
    @Override
    public void init(Peer peer) {
        super.init(peer);
        this.loadCoordinationPhases();
    }

    @Override
    public void start() {
        this.runBootstrap();
        scheduleMeasurements();
    }

    @Override
    public void stop() {
    }

    private void runBootstrap() {
        Timer loadAgentTimer = getPeer().getClock().createNewTimer();
        loadAgentTimer.addTimerListener(new TimerListener() {
            public void timerExpired(Timer timer) {
                runActiveState();
            }
        });
        loadAgentTimer.schedule(Time.inMilliseconds(2000));
    }
    
    private void runActiveState() {
        Timer loadAgentTimer = getPeer().getClock().createNewTimer();
        loadAgentTimer.addTimerListener(new TimerListener() {
            public void timerExpired(Timer timer) {
                if (phaseIndex < phases.size()) {
                    
                    currentPhase = phases.get(phaseIndex);
                    if (phaseIndex > 0) {
                        previousPhase = phases.get(phaseIndex - 1);
                    }
                    phaseIndex++;
                    
                    runPhase();
                    runActiveState();
                }
            }
        });
        loadAgentTimer.schedule(Time.inMilliseconds(1000));
    }
    
    abstract void runPhase();
    
    @Override
    public abstract void handleIncomingMessage(Message message);
    
    void readPlans() {
        possiblePlans.clear();
        File file = new File(this.plansLocation + "/" + this.planConfigurations + "/" + this.agentMeterID + "/" + this.currentPhase.toString("yyyy-MM-dd") + this.plansFormat);
        try (Scanner scanner = new Scanner(file)) {
            scanner.useLocale(Locale.US);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                this.possiblePlans.add(new PossiblePlan(this, line));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
    public void setChildren(List<Finger> list) {
        children.addAll(list);
        this.computeTopologicalState();
    }

    @Override
    public void setTreeView(Finger parent, List<Finger> children) {
        this.setParent(parent);
        this.setChildren(children);
        this.computeTopologicalState();
    }
    
    public boolean isRoot() {
        return topologicalState == TopologicalState.ROOT;
    }
    
    public boolean isLeaf() {
        return topologicalState == TopologicalState.LEAF;
    }
    
    public boolean isInnerNode() {
        return topologicalState == TopologicalState.IN_TREE;
    }
    
    public boolean isDisconnected() {
        return topologicalState == TopologicalState.DISCONNECTED;
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
    
        MeasurementFileDumper measurementDumper;
    private void scheduleMeasurements() {
        if(isRoot()) {
            //measurementDumper = new MeasurementFileDumper("peersLog/" + experimentID + getPeer().getIdentifier().toString());
        }
        getPeer().getMeasurementLogger().addMeasurementLoggerListener(new MeasurementLoggerListener() {
            @Override
            public void measurementEpochEnded(MeasurementLog log, int epochNumber) {
                measure(log, epochNumber);
                if(isRoot()) {
                   // measurementDumper.measurementEpochEnded(log, epochNumber);
                }
                log.shrink(epochNumber, epochNumber + 1);
            }
        });
    }
    
    abstract void measure(MeasurementLog log, int epochNumber);
    
    private void loadCoordinationPhases() {
        File agentDirectory = new File(this.plansLocation + "/" + this.planConfigurations + "/" + this.agentMeterID);
        File[] dates = agentDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isHidden() || pathname.getName().charAt(0)=='.') {
                    return false;
                }
                return pathname.isFile();
            }
        });
        for (File date : dates) {
            StringTokenizer dateTokenizer = new StringTokenizer(date.getName(), ".");
            this.phases.add(DateTime.parse(dateTokenizer.nextToken()));
        }
    }

    public void initPlan(Plan plan) {
        plan.init(planSize);
        plan.setCoordinationPhase(currentPhase);
        plan.setDiscomfort(0.0);
        plan.setAgentMeterID(agentMeterID);
        plan.setConfiguration(planConfigurations + "-" + treeStamp);
    }

    public void initPlan(Plan plan, String planStr) {
        plan.init(planSize);
        plan.setCoordinationPhase(currentPhase);

        Scanner scanner = new Scanner(planStr);
        scanner.useLocale(Locale.US);
        scanner.useDelimiter(":");
        double score = scanner.nextDouble();
        plan.setDiscomfort(1.0 - score);
        
        scanner.useDelimiter(",");
        scanner.skip(":");
        
        for (int i=0; scanner.hasNextDouble(); i++) {
            plan.setValue(i, scanner.nextDouble());
        }
    }
    
    public void broadcast(Message msg) {
        for(Finger c : children) {
            getPeer().sendMessage(c.getNetworkAddress(), msg);
        }
    }
}
