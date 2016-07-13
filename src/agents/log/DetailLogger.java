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
package agents.log;

import agents.fitnessFunction.costFunction.IterativeCostFunction;
import agents.fitnessFunction.costFunction.StdDevCostFunction;
import agents.plan.Plan;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import protopeer.measurement.MeasurementLog;

/**
 *
 * @author Peter
 */
public class DetailLogger extends AgentLogger {

    private String dir = "output-data/detail";
    private int agentId;

    private Plan iterativeCost;
    private Plan costSignal;
    
    private IterativeCostFunction measure = new StdDevCostFunction();

    @Override
    public void init(int agentId) {
        this.agentId = agentId;
    }

    @Override
    public void initRoot(Plan costSignal) {
        this.costSignal = costSignal;
    }

    @Override
    public void log(MeasurementLog log, int epoch, int iteration, Plan selectedLocalPlan) {
        Entry entry = new Entry();
        entry.agentId = agentId;
        entry.iteration = iteration;
        entry.selectedLocalPlan = selectedLocalPlan;
        
        log.log(epoch, agentId, entry, 0.0);
    }

    @Override
    public void logRoot(MeasurementLog log, int epoch, int iteration, Plan global, int numIterations) {
        if (iterativeCost == null) {
            iterativeCost = measure.calcGradient(global, costSignal);
        } else {
            iterativeCost = iterativeCost.clone();
            iterativeCost.add(measure.calcGradient(global, costSignal));
        }

        RootEntry entry = new RootEntry();
        entry.iteration = iteration;
        entry.cost = measure.calcCost(global, costSignal, 0, 0);
        entry.global = global;
        entry.iterativeCost = iterativeCost;
        
        log.log(epoch, entry, 0.0);
    }

    @Override
    public void print(MeasurementLog log) {
        new File(dir).mkdir();

        Map<Integer, PrintStream> outStreams = new HashMap<>();
        try {
            List<Entry> entries = log.getTagsOfType(Entry.class).stream().map(x -> (Entry) x).collect(Collectors.toList());
            entries.sort((a,b) -> Integer.compare(a.iteration, b.iteration));
            
            for (Entry entry : entries) {
                PrintStream out = outStreams.get(entry.agentId);
                if (out == null) {
                    out = new PrintStream(dir + "/" + entry.agentId + ".txt");
                    outStreams.put(entry.agentId, out);
                }
                out.println(entry.iteration + ": " + entry.selectedLocalPlan);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DetailLogger.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            for (PrintStream out : outStreams.values()) {
                out.close();
            }
        }

        try (PrintStream outRoot = new PrintStream(dir + "/root.txt")) {
            for (Object entryObj : log.getTagsOfType(RootEntry.class)) {
                RootEntry entry = (RootEntry) entryObj;
                outRoot.println(entry.iteration + ": " + entry.cost + "," + entry.global + "," + entry.iterativeCost);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DetailLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private class Entry {

        public int agentId;
        public int iteration;
        public Plan selectedLocalPlan;
    }

    private class RootEntry {

        public int iteration;
        public double cost;
        public Plan global;
        public Plan iterativeCost;
    }
}
