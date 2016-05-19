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

import experiments.ConfigurableExperiment;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.measurement.Aggregate;
import protopeer.measurement.LogReplayer;
import protopeer.measurement.MeasurementLog;

/**
 *
 * @author Peter
 */
public abstract class IEPOSEvaluator {
    
    public final void evaluateLogs(int id, Map<String, MeasurementLog> experiments, PrintStream out) {
        String title = "#" + id;
        String measure = "";
        String localMeasure = null;
        List<String> labels = new ArrayList<>();
        List<List<Aggregate>> iterationAggregates = new ArrayList<>();
        
        LogReplayer replayer = new LogReplayer();
        for(String experiment : experiments.keySet()) {
            MeasurementLog log = experiments.get(experiment);
            
            int maxIteration = 0;
            for (Object tag : log.getTagsOfExactType(Integer.class)) {
                maxIteration = Math.max(maxIteration, (Integer) tag);
            }
            
            String localMeasureTag = null;
            String measureTag = null;
            String label = experiment;
            for(Object tag : log.getTagsOfType(String.class)) {
                String s = (String) tag;
                if(s.startsWith("local")) {
                    localMeasure = s.substring(6);
                    localMeasureTag = s;
                } else if(s.startsWith("global")) {
                    measure = s.substring(7);
                    measureTag = s;
                } else {
                    int split = s.indexOf('=');
                    switch(s.substring(0, split)) {
                        case "title":
                            title = s.substring(split+1);
                            break;
                        case "measure":
                            measure = s.substring(split+1);
                            break;
                        case "label":
                            label = s.substring(split+1);
                            break;
                    }
                }
            }
            labels.add(label);
            
            List<Aggregate> list = new ArrayList<>();
            for(int i = 0; i <= maxIteration; i++) {
                list.add(log.getAggregate(i, measureTag));
            }
            iterationAggregates.add(list);
        }
        
        evaluate(id, title, measure, labels, iterationAggregates, out);
    }
    
    abstract void evaluate(int id, String title, String measure, List<String> labels, List<List<Aggregate>> iterationAggregates, PrintStream out);
}
