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
package experiments;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
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
    
    public final void evaluateLogs(int id, List<String> experiments, PrintStream out) {
        String title = "#" + id;
        String measure = "";
        List<String> labels = new ArrayList<>();
        List<List<Aggregate>> iterationAggregates = new ArrayList<>();
        
        LogReplayer replayer = new LogReplayer();
        for(String experiment : experiments) {
            try {
                MeasurementLog log = new MeasurementLog();
                for(File f : new File(experiment).listFiles()) {
                    MeasurementLog l = replayer.loadLogFromFile(f.getPath());
                    log.mergeWith(l);
                }
        
                int maxIteration = 0;
                for (Object tag : log.getTagsOfExactType(Integer.class)) {
                    maxIteration = Math.max(maxIteration, (Integer) tag);
                }
                
                List<Aggregate> list = new ArrayList<>();
                for(int i = 0; i <= maxIteration; i++) {
                    list.add(log.getAggregate(i));
                }
                iterationAggregates.add(list);
                
                String label = experiment;
                for(Object o : log.getTagsOfType(String.class)) {
                    String s = (String)o;
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
                labels.add(label);
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(ConfigurableExperiment.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        evaluate(id, title, measure, labels, iterationAggregates, out);
    }
    
    abstract void evaluate(int id, String title, String measure, List<String> labels, List<List<Aggregate>> iterationAggregates, PrintStream out);
}
