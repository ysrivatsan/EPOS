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
package experiments.output;

import agents.Agent;
import agents.TreeNode;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import protopeer.measurement.Aggregate;
import protopeer.measurement.MeasurementLog;

/**
 *
 * @author Peter
 */
public abstract class IEPOSEvaluator {
    
    public final void evaluateLogs(int id, Map<String, MeasurementLog> experiments, PrintStream out) {
        List<IEPOSMeasurement> configMeasurements = new ArrayList<>();
        String title = "#" + id;
        
        for(String experiment : experiments.keySet()) {
            IEPOSMeasurement configMeasurement = new IEPOSMeasurement();
            configMeasurement.label = experiment;
            
            MeasurementLog log = experiments.get(experiment);
            
            String localTag = null;
            String globalTag = null;
            for(Object tag : log.getTagsOfType(String.class)) {
                String s = (String) tag;
                if(s.startsWith("local")) {
                    configMeasurement.localMeasure = s.split("-", 2)[1];
                    localTag = s;
                } else if(s.startsWith("global")) {
                    configMeasurement.globalMeasure = s.substring(7);
                    globalTag = s;
                } else if(s.startsWith("title")) {
                    title = s.substring(6);
                } else if(s.startsWith("label")) {
                    configMeasurement.label = s.substring(6);
                }
            }
            
            Set<Object> agents = log.getTagsOfType(TreeNode.class);
            
            configMeasurement.localMeasurements = getMeasurements(log, localTag, agents);
            configMeasurement.globalMeasurements = getMeasurements(log, globalTag);
            
            configMeasurements.add(configMeasurement);
        }
        
        evaluate(id, title, configMeasurements, out);
    }
    
    abstract void evaluate(int id, String title, List<IEPOSMeasurement> configMeasurements, PrintStream out);
    
    private List<Aggregate> getMeasurements(MeasurementLog log, String tag, Iterable<Object> agents) {
        if(tag == null) {
            return null;
        }
        
        List<Aggregate> list = new ArrayList<>();
        OUTER:
        for(int i = 0; true; i++) {
            //Aggregate aggregate = null;
            Map<Integer, Aggregate> localCost = new HashMap<>();
            for(Object agent : agents) {
                Aggregate value = log.getAggregate(i, tag, agent);
                if(value.getNumValues() == 0) {
                    continue;
                }
                TreeNode node = (TreeNode) agent;
                if(!localCost.containsKey(node.expId)) {
                    localCost.put(node.expId, value);
                } else {
                    localCost.get(node.expId).mergeWith(value);
                }
                /*if(aggregate == null) {
                    aggregate = value;
                } else {
                    aggregate.mergeWith(value);
                }*/
            }
            if(localCost.isEmpty()) {
            //if(aggregate == null) {
                break;
            }
            
            MeasurementLog tmp = new MeasurementLog();
            for(Aggregate lc : localCost.values()) {
                tmp.log(0, "bla", lc.getAverage());
            }
            list.add(tmp.getAggregate("bla"));
            //list.add(aggregate);
        }
        return list;
    }
    
    private List<Aggregate> getMeasurements(MeasurementLog log, String tag) {
        if(tag == null) {
            return null;
        }
        
        List<Aggregate> list = new ArrayList<>();
        for(int i = 0; true; i++) {
            Aggregate value = log.getAggregate(i, tag);
            if(value.getNumValues() == 0) {
                break;
            }
            list.add(value);
        }
        return list;
    }
}
