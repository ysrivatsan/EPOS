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

import agents.fitnessFunction.FitnessFunction;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import protopeer.measurement.Aggregate;
import protopeer.measurement.LogReplayer;
import protopeer.measurement.MeasurementLog;
import protopeer.measurement.MeasurementLoggerListener;

/**
 *
 * @author Peter
 */
public class IEPOSEvaluator {

    public static void readLogs(String experimentID) {
        LogReplayer replayer = new LogReplayer();

        File folder = new File("peersLog/Experiment " + experimentID);
        if (!folder.isDirectory()) {
            System.err.println("No dictionary " + folder.getPath());
            return;
        }
        try {
            File[] listOfFiles = folder.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile() && !listOfFiles[i].isHidden()) {
                    MeasurementLog log = replayer.loadLogFromFile(folder.getPath() + "/" + listOfFiles[i].getName());
                    System.out.println(log.getTagsOfExactType(Integer.class).size());
                    replayer.mergeLog(log);
                }
            }
            evaluateLog(replayer);
        } catch (IOException | ClassNotFoundException ex) {
        }
    }

    public static void evaluateLog(LogReplayer replayer) {
        replayer.replayTo(new MeasurementLoggerListener() {
            public void measurementEpochEnded(MeasurementLog log, int epochNumber) {

            }
        });
    }

    public static void evaluateLogs(int id, String title, List<String> labels, String measure, List<MeasurementLog> logs, PrintStream out) {
        int maxIteration = 0;
        for (MeasurementLog log : logs) {
            for (Object tag : log.getTagsOfExactType(Integer.class)) {
                maxIteration = Math.max(maxIteration, (Integer) tag);
            }
        }

        printMatrix("XAvg" + id, logs, maxIteration, a -> a.getAverage(), out);
        printMatrix("XMax" + id, logs, maxIteration, a -> a.getMax(), out);
        printMatrix("XMin" + id, logs, maxIteration, a -> a.getMin(), out);
        printMatrix("XStd" + id, logs, maxIteration, a -> a.getStdDev(), out);

        out.println("figure(" + id + ");");
        out.println("plot(XAvg" + id + "');");
        out.println("xlabel('iteration');");
        out.println("ylabel('" + measure + "');");
        out.print("legend('" + toMatlabString(labels.get(0)) + "'");
        for (int i = 1; i < labels.size(); i++) {
            out.print(",'" + toMatlabString(labels.get(i)) + "'");
        }
        out.println(");");
        out.println("title('" + toMatlabString(title) + "');");
    }

    private static String toMatlabString(String str) {
        return str.replace("_", "\\_");
    }

    private static void printMatrix(String name, List<MeasurementLog> logs, int maxIteration, Function<Aggregate, Double> function, PrintStream out) {
        out.println(name + " = [");
        for (MeasurementLog log : logs) {
            out.print(function.apply(log.getAggregate(0)));
            for (int i = 1; i <= maxIteration; i++) {
                out.print(", " + function.apply(log.getAggregate(i)));
            }
            out.println(";");
        }
        out.println("];");
    }
}
