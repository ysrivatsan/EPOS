/*
 * Copyright (C) 2016 Peter Pilgerstorfer
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
import java.awt.Font;
import java.awt.Paint;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.*;
import org.jfree.ui.RectangleInsets;
import protopeer.measurement.Aggregate;

/**
 *
 * @author Peter
 */
public class JFreeChartEvaluator extends IEPOSEvaluator {
    
    private static File defaultSrcDir = new File("C:\\Users\\Peter\\OneDrive\\Dokumente\\Master Studium\\Master Thesis\\MyThesis\\fig\\Plot Files");
    private static File defaultDstDir = new File("C:\\Users\\Peter\\OneDrive\\Dokumente\\Master Studium\\Master Thesis\\MyThesis\\fig");
    
    private ChartPanel panel = null;
    
    public static void main(String[] args) throws IOException {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JFileChooser chooser = new JFileChooser(defaultSrcDir);
        if (chooser.showOpenDialog(f) == JFileChooser.APPROVE_OPTION) {
            try (BufferedReader br = new BufferedReader(new FileReader(chooser.getSelectedFile()))) {
                new JFreeChartEvaluator().readAndExecuteState(br);
            }
        }
    }

    @Override
    public void evaluate(int id, String title, List<IEPOSMeasurement> configMeasurements, PrintStream out) {
        Locale.setDefault(Locale.US);
        if (out != null) {
            Util.writeState(id, title, configMeasurements, out);
        }

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        boolean printLocal = configMeasurements.get(0).localMeasure != null;
        
        List<PlotInfo> plotInfos = new ArrayList<>();
        Map<String, List<Aggregate>> globalMeasurements = new LinkedHashMap<>();
        Map<String, List<Double>> timeMeasurements = new LinkedHashMap<>();
        for(IEPOSMeasurement m : configMeasurements) {
            globalMeasurements.put(m.label, m.globalMeasurements);
            timeMeasurements.put(m.label, m.timeMeasurements);
            if(m.iterationMeasurements != null && m.iterationMeasurements.size() > 0) {
                Aggregate a = m.iterationMeasurements.get(m.iterationMeasurements.size()-1);
                System.out.println(m.label + ": " + a.getAverage() + "+-" + a.getStdDev());
            }
        }
        plotInfos.add(new PlotInfo()
                .dataset(toDataset(globalMeasurements, timeMeasurements))
                .xLabel(configMeasurements.get(0).timeMeasure)
                .yLabel(configMeasurements.get(0).globalMeasure));
        if (printLocal) {
            Map<String, List<Aggregate>> localMeasurements = new LinkedHashMap<>();
            for(IEPOSMeasurement m : configMeasurements) {
                localMeasurements.put(m.label, m.localMeasurements);
            }
            plotInfos.add(new PlotInfo()
                    .dataset(toDataset(localMeasurements, timeMeasurements))
                    .xLabel(configMeasurements.get(0).timeMeasure)
                    .yLabel(configMeasurements.get(0).localMeasure));
        }
        
        XYPlot plot = new XYPlot();
        plot.setDrawingSupplier(new MyDrawingSupplier());

        for (int i = 0; i < plotInfos.size(); i++) {
            PlotInfo plotInfo = plotInfos.get(i);
            plotInfo.addToPlot(plot, i);
        }

        Font font = new Font("Computer Modern", Font.PLAIN, 12);
        
        // init legend
        LegendTitle legend = new LegendTitle(plot);
        legend.setItemFont(font);
        legend.setItemLabelPadding(new RectangleInsets(0, 0, 0, 10));
        legend.setFrame(new BlockBorder());
        legend.setBackgroundPaint(Color.WHITE);
        
        JFreeChartCustomLegend chart = new JFreeChartCustomLegend(null, font, plot, legend);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getXYPlot().getDomainAxis().setLabelFont(font);
        chart.getXYPlot().getDomainAxis().setTickLabelFont(font);
        chart.getXYPlot().getRangeAxis().setLabelFont(font);
        chart.getXYPlot().getRangeAxis().setTickLabelFont(font);
        
        // show plot
        panel = chart.getPanel();
        panel.setDefaultDirectoryForSaveAs(defaultDstDir);
        panel.setMinimumDrawHeight(1);
        panel.setMinimumDrawWidth(1);
        frame.setContentPane(panel);
        frame.setSize(256, 256);
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

    private YIntervalSeriesCollection toDataset(Map<String, List<Aggregate>> configMeasurements, Map<String, List<Double>> configTime) {
        YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();

        for (Map.Entry<String, List<Aggregate>> config : configMeasurements.entrySet()) {
            YIntervalSeries series = new YIntervalSeries(config.getKey());
            System.out.println(config.getKey());
            List<Double> time = configTime.get(config.getKey());
            if(!config.getValue().isEmpty()) {
                Aggregate c1 = config.getValue().get(0);
                Aggregate ct = config.getValue().get(config.getValue().size()-1);
                if(config.getValue().size()>=2) {
                    Aggregate ctm1 = config.getValue().get(config.getValue().size()-2);
                    if(ctm1.getAverage() < ct.getAverage()) {
                        ct = ctm1;
                    }
                }
                System.out.println("E^(1)=" + c1.getAverage() + "+-" + c1.getStdDev() + ", E^(t)=" + ct.getAverage() + "+-" + ct.getStdDev());
            }
            for (int i = 0; i < config.getValue().size(); i++) {
                Aggregate aggregate = config.getValue().get(i);
                double avg = aggregate.getAverage();
                double std = aggregate.getStdDev();
                if(time == null) {
                    series.add(i + 1, avg, avg - std, avg + std);
                } else {
                    series.add(time.get(i), avg, avg - std, avg + std);
                }
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

    private class PlotInfo {

        private YIntervalSeriesCollection dataset;
        private ValueAxis xAxis;
        private ValueAxis yAxis;

        public PlotInfo dataset(YIntervalSeriesCollection dataset) {
            this.dataset = dataset;
            return this;
        }

        public PlotInfo xLabel(String xLabelStr) {
            if(xLabelStr == null) {
                xLabelStr = "iteration";
            }
            xAxis = toAxis(xLabelStr);
            return this;
        }
        
        public PlotInfo yLabel(String yLabelStr) {
            yAxis = toAxis(yLabelStr);
            return this;
        }

        public void addToPlot(XYPlot plot, int idx) {
            YIntervalSeriesCollection continuousDS = new YIntervalSeriesCollection();
            YIntervalSeriesCollection discreteDS = new YIntervalSeriesCollection();
            for(int series = 0; series < dataset.getSeriesCount(); series++) {
                YIntervalSeries s = dataset.getSeries(series);
                if(s.getItemCount()>1) {
                    continuousDS.addSeries(s);
                } else {
                    discreteDS.addSeries(s);
                }
            }
            
            DeviationRenderer continuousRenderer = new DeviationRenderer(true, false);
            /*for(int series = 0; series < dataset.getSeriesCount(); series++) {
                int numItems = dataset.getItemCount(series);
                if(numItems == 1) {
                    continuousRenderer.setSeriesLinesVisible(series, false);
                    continuousRenderer.setSeriesShapesVisible(series, true);
                    continuousRenderer.setSeriesShapesFilled(series, false);
                }
            }*/
            XYErrorRenderer discreteRenderer = new XYErrorRenderer();
            
            
            plot.setDomainAxis(idx, xAxis);
            if(xAxis.isAutoRange() && "iteration".equals(xAxis.getLabel())) {
                xAxis.setRange(0, dataset.getItemCount(0));
            } else if(xAxis.isAutoRange()) {
                double max = Double.POSITIVE_INFINITY;
                for(int i = 0; i < dataset.getSeriesCount(); i++) {
                    max = Math.min(max, dataset.getXValue(i, dataset.getItemCount(i)-1));
                }
                xAxis.setRange(0, max);
            }
            plot.setRangeAxis(idx, yAxis);
            plot.setDataset(idx, continuousDS);
            plot.mapDatasetToRangeAxis(idx, idx);
            plot.setRenderer(idx, continuousRenderer);
            if(discreteDS.getSeriesCount() > 0) {
                plot.setDataset(idx+1, discreteDS);
                plot.mapDatasetToRangeAxis(idx+1, idx);
                plot.setRenderer(idx+1, discreteRenderer);
            }

            continuousRenderer.setAlpha(0.2f);
            for (int i = 0; i < plot.getSeriesCount(); i++) {
                Paint paint = ((DeviationRenderer) continuousRenderer).lookupSeriesPaint(i);
                continuousRenderer.setSeriesPaint(i, paint);
                continuousRenderer.setSeriesFillPaint(i, paint);
                if (idx > 0) {
                    continuousRenderer.setSeriesStroke(i, new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{2, 4}, 0));
                    continuousRenderer.setAlpha(0.1f);
                    continuousRenderer.setSeriesVisibleInLegend(i, false);
                }
            }
        }
    }
    
    private ValueAxis toAxis(String axisStr) {
        ValueAxis axis;
        
        axisStr = axisStr.trim();
        
        if(axisStr.startsWith("log_")) {
            axisStr = axisStr.substring(4);
            axis = new LatexLogAxis(axisStr);
        } else {
            axis = new NumberAxis(axisStr);
        }
        
        if(axisStr.endsWith(")")) {
            String rangeStr = axisStr.substring(axisStr.indexOf("("));
            rangeStr = rangeStr.substring(1,rangeStr.length()-1);
            String[] range = rangeStr.split("-");
            axis.setRange(Double.parseDouble(range[0]), Double.parseDouble(range[1]));
            axisStr = axisStr.substring(0,axisStr.indexOf("("));
            axis.setLabel(axisStr);
        }
        
        return axis;
    }

    private void readAndExecuteState(BufferedReader br) throws IOException {
        Util.PlotInfo pi = Util.readStates(br);
        evaluate(pi.id, pi.title, pi.measurements, null);
    }
    
    private static class LatexLogAxis extends LogarithmicAxis {
        public LatexLogAxis(String label) {
            super(label);
            setAllowNegativesFlag(true);
        }
    }
}
