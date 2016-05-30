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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JFrame;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.xy.*;
import protopeer.measurement.Aggregate;

/**
 *
 * @author Peter
 */
public class JFreeChartEvaluator extends IEPOSEvaluator {

    @Override
    void evaluate(int id, String title, List<IEPOSMeasurement> configMeasurements, PrintStream out) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
        
        for(IEPOSMeasurement config : configMeasurements) {
            YIntervalSeries series = new YIntervalSeries(config.label);
           // OHLCSeries series = new OHLCSeries(config.label);
            for(int i = 0; i < config.globalMeasurements.size(); i++) {
                Aggregate aggregate = config.globalMeasurements.get(i);
                double avg = aggregate.getAverage();
                double std = aggregate.getStdDev();
                series.add(i+1, avg, avg - std, avg + std);
            }
            dataset.addSeries(series);
        }
        
        //JFreeChart chart = ChartFactory.createXYAreaChart(title, getXLabel(), getYLabel(configMeasurements.stream().map(x -> x.globalMeasure)), dataset, PlotOrientation.VERTICAL, true, true, true);
        
        CombinedDomainXYPlot combPlot = new CombinedDomainXYPlot();
        DeviationRenderer renderer = new DeviationRenderer(true, false);
        
        XYPlot plot = new XYPlot(dataset, new NumberAxis(getXLabel()), new NumberAxis(getYLabel(configMeasurements.stream().map(x -> x.globalMeasure))), renderer);
        
        renderer.setAlpha(0.3f);
        for(int i = 0; i < plot.getSeriesCount(); i++) {
            renderer.setSeriesFillPaint(i, renderer.lookupSeriesPaint(i));
        }
        
        combPlot.add(plot);
        JFreeChart chart = new JFreeChart(title, combPlot);
        
        // show plot
        ChartPanel panel = new ChartPanel(chart);
        frame.setContentPane(panel);
        frame.setSize(512, 512);
        frame.setVisible(true);
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
        return "iteration";
    }

    private String getYLabel(Stream<String> configMeasurements) {
        return configMeasurements.collect(Collectors.toSet()).stream().reduce((a, b) -> a + "," + b).get();
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
