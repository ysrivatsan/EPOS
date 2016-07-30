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
package experiments.log;

import agents.Agent;
import agents.fitnessFunction.costFunction.IterativeCostFunction;
import agents.fitnessFunction.costFunction.StdDevCostFunction;
import agents.plan.Plan;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.text.html.HTML;
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

    public DetailLogger(String dir) {
        this.dir = "output-data/" + dir;
    }

    @Override
    public void init(int agentId) {
        this.agentId = agentId;
    }

    @Override
    public void initRoot(Plan costSignal) {
        this.costSignal = costSignal;
    }

    @Override
    public void log(MeasurementLog log, int epoch, Agent agent) {
        Entry entry = new Entry();
        entry.agentId = agentId;
        entry.iteration = agent.getIteration();
        entry.selectedLocalPlan = agent.getSelectedPlan();

        log.log(epoch, agentId, entry, 0.0);
    }

    @Override
    public void logRoot(MeasurementLog log, int epoch, Agent agent, Plan global) {
        if (iterativeCost == null) {
            iterativeCost = global.clone();
        } else {
            iterativeCost = iterativeCost.clone();
            iterativeCost.add(global);
        }

        RootEntry entry = new RootEntry();
        entry.iteration = agent.getIteration();
        entry.cost = measure.calcCost(global, costSignal, 0, 0, true);
        entry.global = global;
        entry.iterativeCost = iterativeCost;

        log.log(epoch, entry, 0.0);
    }
    
    private String str2filename(String str) {
        return str.replace(' ', '_').replace('-','m').replace('.', '_');
    }

    @Override
    public void print(MeasurementLog log) {
        // title and label of the current execution is stored in the log as String tag: "label=..."
        String printDir = this.dir;
        Set<Object> info = (Set<Object>) log.getTagsOfType(String.class);
        String title = "";
        String label = "";
        for(Object o : info) {
            String str = (String)o;
            if(str.startsWith("label=")) {
                label =  str2filename(str.substring(6,Math.min(str.length(), 36)));
            }
            if(str.startsWith("title=")) {
                title =  str2filename(str.substring(6,Math.min(str.length(), 33)));
            }
        }
        printDir += "/" + title + "_" + label;
        
        new File(printDir).mkdirs();

        Map<Integer, PrintStream> outStreams = new HashMap<>();
        try {
            List<Entry> entries = log.getTagsOfType(Entry.class).stream().map(x -> (Entry) x).collect(Collectors.toList());
            entries.sort((a, b) -> Integer.compare(a.iteration, b.iteration));

            for (Entry entry : entries) {
                PrintStream out = outStreams.get(entry.agentId);
                if (out == null) {
                    out = new PrintStream(printDir + "/" + entry.agentId + ".txt");
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

        try (PrintStream outRoot = new PrintStream(printDir + "/root.txt")) {
            TreeSet<Object> entryObjs = new TreeSet<>((a, b) -> Integer.compare(((RootEntry) a).iteration, ((RootEntry) b).iteration));
            entryObjs.addAll(log.getTagsOfType(RootEntry.class));

            Plan prevIterCost = ((RootEntry) entryObjs.first()).global.clone();
            prevIterCost.set(0);

            for (Object entryObj : entryObjs) {
                RootEntry entry = (RootEntry) entryObj;

                if (prevIterCost.max() != 0.0) {
                    double avg = prevIterCost.avg() + 0.00001;
                    double std = prevIterCost.stdDeviation() + 0.00001;
                    prevIterCost.multiply(88 / Math.max(avg, std));
                    //prevIterCost.multiply(1/Math.max(1,Math.pow(10,Math.floor(Math.log10(prevIterCost.max()))-1)));
                }
                outRoot.println(entry.iteration + ": " + String.format(Locale.US, "%.3g", entry.cost) + "," + entry.global.toString("%.0f") + "," + prevIterCost.toString("%.0f"));
                prevIterCost = entry.iterativeCost;
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
