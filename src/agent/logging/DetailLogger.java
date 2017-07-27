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
package agent.logging;

import data.Plan;
import data.Vector;
import func.CostFunction;
import func.StdDevCostFunction;
import agent.Agent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import protopeer.measurement.MeasurementLog;

/**
 * stores the selected plan for each agent for each iteration in one file per
 * agent. stores the global cost, the global response and the cumulated global
 * response
 *
 * @author Peter
 */
public class DetailLogger extends AgentLogger<Agent<Vector>> {

    private final String outputDir;
    private int agentId;

    private Vector cumulatedResponse;

    private final CostFunction globalCostFunction = new StdDevCostFunction();

    public DetailLogger(String dir) {
        this.outputDir = dir;
    }

    @Override
    public void init(Agent<Vector> agent) {
        this.agentId = agent.getPeer().getIndexNumber();
    }

    @Override
    public void log(MeasurementLog log, int epoch, Agent<Vector> agent) {
        Entry entry = new Entry();
        entry.agentId = agentId;
        entry.iteration = agent.getIteration();
        entry.numIteration = agent.getNumIterations(); 
        entry.selectedPlan = agent.getSelectedPlan();
        log.log(epoch, agentId, entry, 0.0);

        if (agent.isRepresentative()) {
            if (cumulatedResponse == null) {
                cumulatedResponse = agent.getGlobalResponse().cloneThis();
            } else {
                cumulatedResponse = cumulatedResponse.cloneThis();
                cumulatedResponse.add(agent.getGlobalResponse());
            }

            RootEntry rootEntry = new RootEntry();
            rootEntry.iteration = agent.getIteration();
            rootEntry.globalCost = globalCostFunction.calcCost(agent.getGlobalResponse());
            rootEntry.globalResponse = agent.getGlobalResponse();
            rootEntry.cumulatedResponse = cumulatedResponse;

            log.log(epoch, rootEntry, 0.0);
        }
    }

    private String str2filename(String str) {
        return str.replace(' ', '_').replace('-', 'm').replace('.', '_');
    }

    @Override
    public void print(MeasurementLog log) {
        // get title and label (of the output plots) according to the log
        // title and label of the current execution is stored in the log as String tag: "label=..."
        Set<Object> info = (Set<Object>) log.getTagsOfType(String.class);
        String title = "Min";
        String label = "Peak";
        for (Object o : info) {
            String str = (String) o;
            if (str.startsWith("label=")) {
                label = str2filename(str.substring(6, Math.min(str.length(), 36)));
            }
            if (str.startsWith("title=")) {
                title = str2filename(str.substring(6, Math.min(str.length(), 33)));
            }
        }

        // compute output directory
        String printDir = outputDir + "/" + title + "_" + label;
        new File(printDir).mkdirs();

        // output the selected plan at each iteration for each agent
        Map<Integer, PrintStream> outStreams = new HashMap<>();
        Map<Integer, PrintStream> outStreams2 = new HashMap<>();
        
        try {
            List<Entry> entries = log.getTagsOfType(Entry.class).stream().map(x -> (Entry) x).collect(Collectors.toList());
            entries.sort((a, b) -> Integer.compare(a.iteration, b.iteration));
            PrintStream out3 = new PrintStream(new FileOutputStream(outputDir+"/Plan_Selections.txt",false));
            for (Entry entry : entries) {
                PrintStream out = outStreams.get(entry.agentId);
                PrintStream out2 = outStreams2.get(entry.agentId);
                if (out == null) {
                    out = new PrintStream(printDir + "/" + "Agent_"+entry.agentId + "_all_iterations.txt");
                    outStreams.put(entry.agentId, out);
                }
                if (out2 == null) {
                    out2 = new PrintStream(printDir + "/" + "Agent_"+entry.agentId + "_final_iteration_selected_plan.txt");
                    outStreams2.put(entry.agentId, out2);
                }
//                Vector out2 = (Vector)entry.selectedPlan.getValue();
//                String plan2 = out2.toString();
                out.println(entry.iteration +":"+ entry.selectedPlan.getValue());
                
                if(entry.iteration == entry.numIteration-1)
                {
                out2.println("Index:" +entry.selectedPlan.getIndex()+"\tPlan:"+entry.selectedPlan.getValue());
                out3.println(entry.agentId+":"+entry.selectedPlan.getIndex());
                //System.out.println("Im here");
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DetailLogger.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            for (PrintStream out : outStreams.values()) {
                out.close();
            }
            for (PrintStream out : outStreams2.values()) {
                out.close();
            }
        }

        // output the global cost, the global response and the cumulated global response
        try (PrintStream outRoot = new PrintStream(printDir + "/root.txt")) {
            TreeSet<Object> entryObjs = new TreeSet<>((a, b) -> Integer.compare(((RootEntry) a).iteration, ((RootEntry) b).iteration));
            entryObjs.addAll(log.getTagsOfType(RootEntry.class));

            Vector prevIterCost = ((RootEntry) entryObjs.first()).globalResponse.cloneThis();
            prevIterCost.set(0);

            for (Object entryObj : entryObjs) {
                RootEntry entry = (RootEntry) entryObj;

                if (prevIterCost.max() != 0.0) {
                    double avg = prevIterCost.avg() + 0.00001;
                    double std = prevIterCost.std() + 0.00001;
                    prevIterCost.multiply(88 / Math.max(avg, std));
                }
                outRoot.println(entry.iteration + ": " + String.format(Locale.US, "%.3g", entry.globalCost) + "," + entry.globalResponse.toString("%.0f") + "," + prevIterCost.toString("%.0f"));
                prevIterCost = entry.cumulatedResponse;
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DetailLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private class Entry implements Serializable {

        public int agentId;
        public int iteration;
        public Plan selectedPlan;
        public int numIteration;
    }

    private class RootEntry implements Serializable {

        public int iteration;
        public double globalCost;
        public Vector globalResponse;
        public Vector cumulatedResponse;
    }
}
