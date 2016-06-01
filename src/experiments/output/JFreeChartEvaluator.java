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
import java.awt.Stroke;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        
        boolean printLocal = configMeasurements.get(0).localMeasure != null;

        xAxis = new NumberAxis(getXLabel());
        List<PlotInfo> plotInfos = new ArrayList<>();
        plotInfos.add(new PlotInfo()
                .dataset(toDataset(configMeasurements.stream().collect(Collectors.toMap(x -> x.label, x -> x.globalMeasurements))))
                .yLabel(getYLabel(configMeasurements.stream().map(x -> x.globalMeasure))));
        if(printLocal) {
            plotInfos.add(new PlotInfo()
                    .dataset(toDataset(configMeasurements.stream().collect(Collectors.toMap(x -> x.label, x -> x.localMeasurements))))
                    .yLabel(getYLabel(configMeasurements.stream().map(x -> x.localMeasure))));
        }
        
        XYPlot plot = new XYPlot();
        plot.setDomainAxis(0, xAxis);
        
        for(int i = 0; i < plotInfos.size(); i++) {
            PlotInfo plotInfo = plotInfos.get(i);
            plotInfo.addToPlot(plot, i);
        }
        
        JFreeChart chart = new JFreeChart(title, plot);
        chart.setBackgroundPaint(Color.WHITE);

        // show plot
        ChartPanel panel = new ChartPanel(chart);
        frame.setContentPane(panel);
        frame.setSize(512, 512);
        frame.setVisible(true);

        frame = frame;
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

    private YIntervalSeriesCollection toDataset(Map<String, List<Aggregate>> configMeasurements) {
        YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();

        for (Map.Entry<String, List<Aggregate>> config : configMeasurements.entrySet()) {
            YIntervalSeries series = new YIntervalSeries(config.getKey());
            for (int i = 0; i < config.getValue().size(); i++) {
                Aggregate aggregate = config.getValue().get(i);
                double avg = aggregate.getAverage();
                double std = aggregate.getStdDev();
                series.add(i + 1, avg, avg - std, avg + std);
            }
            dataset.addSeries(series);
        }

        return dataset;
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
        
    private ValueAxis xAxis;
    
    private class PlotInfo {
        private XYDataset dataset;
        private String yLabel;
        
        public PlotInfo dataset(XYDataset dataset) {
            this.dataset = dataset;
            return this;
        }
        
        public PlotInfo yLabel(String yLabel) {
            this.yLabel = yLabel;
            return this;
        }
        
        public void addToPlot(XYPlot plot, int idx) {
            DeviationRenderer renderer = new DeviationRenderer(true, false);
        
            plot.setRangeAxis(idx, new NumberAxis(yLabel));
            plot.setDataset(idx, dataset);
            plot.mapDatasetToRangeAxis(idx, idx);
            plot.setRenderer(idx, renderer);
            
            renderer.setAlpha(0.2f);
            for (int i = 0; i < plot.getSeriesCount(); i++) {
                Paint paint = ((DeviationRenderer)plot.getRenderer(0)).lookupSeriesPaint(i);
                renderer.setSeriesPaint(i, paint);
                renderer.setSeriesFillPaint(i, paint);
                if(idx > 0) {
                    renderer.setSeriesStroke(i, new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{2,4}, 0));
                    renderer.setAlpha(0.1f);
                    renderer.setSeriesVisibleInLegend(i, false);
                }
            }
        }
    }
}
