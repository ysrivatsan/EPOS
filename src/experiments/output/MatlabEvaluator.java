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

import java.io.PrintStream;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import protopeer.measurement.Aggregate;

/**
 *
 * @author Peter
 */
public class MatlabEvaluator extends IEPOSEvaluator {

    @Override
    void evaluate(int id, String title, List<IEPOSMeasurement> configMeasurements, PrintStream out) {
        int num = configMeasurements.size();
        boolean printLocal = configMeasurements.get(0).localMeasure != null;
        String xavg = "XAvg" + id;
        String xmax = "XMax" + id;
        String xmin = "XMin" + id;
        String xstd = "XStd" + id;
        printMatrix(xavg, configMeasurements.stream().map(x -> x.globalMeasurements), a -> a.getAverage(), out);
        printMatrix(xmax, configMeasurements.stream().map(x -> x.globalMeasurements), a -> a.getMax(), out);
        printMatrix(xmin, configMeasurements.stream().map(x -> x.globalMeasurements), a -> a.getMin(), out);
        printMatrix(xstd, configMeasurements.stream().map(x -> x.globalMeasurements), a -> a.getStdDev(), out);
        
        String xavgLocal = "XAvgL" + id;
        String xmaxLocal = "XMaxL" + id;
        String xminLocal = "XMinL" + id;
        String xstdLocal = "XStdL" + id;
        if(printLocal) {
            printMatrix(xavgLocal, configMeasurements.stream().map(x -> x.localMeasurements), a -> a.getAverage(), out);
            printMatrix(xmaxLocal, configMeasurements.stream().map(x -> x.localMeasurements), a -> a.getMax(), out);
            printMatrix(xminLocal, configMeasurements.stream().map(x -> x.localMeasurements), a -> a.getMin(), out);
            printMatrix(xstdLocal, configMeasurements.stream().map(x -> x.localMeasurements), a -> a.getStdDev(), out);
        }

        String xcon = "XCon" + id;
        printConvergence(xcon, configMeasurements.stream().map(x -> x.globalMeasurements), out);

        out.println("figure(" + id + ");");
        if(printLocal) {
            out.println("[hAx,hLine1,hLine2] = plotyy(1:size(" + xavg + ",2)," + xavg + ",1:size(" + xavgLocal + ",2),"+xavgLocal+");");
            out.println("for i = 1:length(hLine2)");
            out.println("   hLine2(i).Color = hLine1(i).Color;");
            out.println("   hLine2(i).LineStyle = '--';");
            out.println("end");
        } else {
            out.println("plot(" + xavg + "');");
        }
        out.println("hold on; plot(" + getConvergencePoints(xcon, xavg, num) + "),'ko'); hold off;");
        out.println("xlabel(" + getXLabel() + ");");
        if(printLocal) {
            out.println("ylabel(hAx(1)," + getYLabel(configMeasurements.stream().map(x -> x.globalMeasure)) + ");");
            out.println("ylabel(hAx(2),['local ' " + getYLabel(configMeasurements.stream().map(x -> x.localMeasure)) + "]);");
        } else {
            out.println("ylabel(" + getYLabel(configMeasurements.stream().map(x -> x.globalMeasure)) + ");");
        }
        out.println("legend(" + getLegend(configMeasurements) + ");");
        out.println("title('" + toMatlabString(title) + "');");
    }

    private String toMatlabString(String str) {
        return str.replace("_", "\\_");
    }

    private String getConvergencePoints(String conVar, String valVar, int num) {
        return conVar + "," + valVar + "(" + rowIdxToIdx(conVar, num);
    }

    private String rowIdxToIdx(String var, int varRows) {
        StringBuilder offset = new StringBuilder();
        for (int i = varRows - 1; i > 0; i--) {
            offset.append('-');
            offset.append(i);
            offset.append(';');
        }
        offset.append(0);

        return var + "*" + varRows + "+[" + offset + "]";
    }

    private String getXLabel() {
        return "'iteration'";
    }

    private String getYLabel(Stream<String> configMeasurements) {
        return "'" + configMeasurements.collect(Collectors.toSet()).stream().reduce((a, b) -> a + "," + b).get() + "'";
    }

    private String getLegend(List<IEPOSMeasurement> configMeasurements) {
        return configMeasurements.stream().map(x -> "'" + toMatlabString(x.label) + "'").reduce((a, b) -> a + "," + b).get();
    }

    private void printMatrix(String name, Stream<List<Aggregate>> iterationAggregates, Function<Aggregate, Double> function, PrintStream out) {
        out.println(name + " = [");
        iterationAggregates.forEachOrdered((List<Aggregate> log) -> {
            out.print(function.apply(log.get(0)));
            for (int i = 1; i < log.size(); i++) {
                double val = function.apply(log.get(i));
                if (val == Double.POSITIVE_INFINITY) {
                    out.print(", inf");
                } else if (val == Double.NEGATIVE_INFINITY) {
                    out.print(", -inf");
                } else {
                    out.print(", " + val);
                }
            }
            out.println(";");
        });
        out.println("];");
    }

    private void printConvergence(String name, Stream<List<Aggregate>> iterationAggregates, PrintStream out) {
        Convergence con = new EpsilonConvergence(0.1);
        out.print(name + " = [");
        iterationAggregates.forEachOrdered((List<Aggregate> log) -> {
            double[] signal = new double[log.size()];
            for (int i = 0; i < signal.length; i++) {
                signal[i] = log.get(i).getAverage();
            }
            int convergence = con.convergence(signal);
            out.print(convergence + " ");
        });
        out.println("]';");
    }
}
