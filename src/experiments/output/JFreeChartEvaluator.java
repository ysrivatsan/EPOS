/*
 * Copyright (C) 2016 Evangelos Pournaras
                     @Override
                    public double getX() {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }

                    @Override
                    public double getY() {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }

                    @Override
                    public void setLocation(double x, double y) {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }
                }
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
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.block.ColorBlock;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.*;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.Size2D;
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
    void evaluate(int id, String title, List<IEPOSMeasurement> configMeasurements, PrintStream out) {
        Locale.setDefault(Locale.US);
        if (out != null) {
            writeState(id, title, configMeasurements, out);
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
        legend.getItemContainer().add(new ColorBlock(Color.BLUE, 20, 20));
        legend.setPosition(RectangleEdge.BOTTOM);
        legend.setBackgroundPaint(Color.WHITE);
        
        JFreeChart chart;
        chart = new JFreeChart(null, font,  plot, false) {
            @Override
            public void draw(Graphics2D g2, Rectangle2D chartArea, Point2D anchor, ChartRenderingInfo info) {
                panel.getChartRenderingInfo().setChartArea(panel.getScreenDataArea());
                super.draw(g2, chartArea, anchor, info);
                
                // arrange legend and compute width and height
                double w = 0;
                double h = 0;
                
                Size2D size = legend.arrange(g2, new RectangleConstraint(0, Double.POSITIVE_INFINITY));
                h = size.height;
                for(Block item : (List<Block>) legend.getItemContainer().getBlocks()) {
                    Size2D itemSize = item.arrange(g2, RectangleConstraint.NONE);
                    w = Math.max(w, itemSize.width);
                }
                
                //compute content rectangle
                double scaleX = 1;
                double scaleY = 1;
                double offsetX = 0;
                double offsetY = 0;
                
                Rectangle2D plotBounds;
                if(info != null) {
                    plotBounds = info.getPlotInfo().getPlotArea();
                    //System.out.println(chartArea + "," + panel.getScreenDataArea() + "," + plotBounds);
                } else {
                    plotBounds = panel.getChartRenderingInfo().getPlotInfo().getPlotArea();
                    //System.out.println(chartArea + "," + panel.getScreenDataArea());
                    
                    scaleX = panel.getScaleX();
                    scaleY = panel.getScaleY();
                    offsetX = 2;
                    offsetY = 0;
                }
                
                // get unscaled content rectangle
                Rectangle2D contentRectRaw = null;
                for(ChartEntity entity : (Collection<ChartEntity>)panel.getChartRenderingInfo().getEntityCollection().getEntities()) {
                    if(entity instanceof PlotEntity) {
                        contentRectRaw = entity.getArea().getBounds2D();
                    }
                }
                
                //panel.getScaleX() returns the effective scale (input size -> output size), however the border (8px) has constant width (is not scaled!)
                double plotXRaw = plotBounds.getX();
                double plotYRaw = plotBounds.getY();
                double plotWRaw = plotBounds.getWidth();
                double plotHRaw = plotBounds.getHeight();
                double plotW = (2*plotXRaw + plotWRaw)*scaleX - 2*plotXRaw;
                double plotH = (2*plotYRaw + plotHRaw)*scaleY - 2*plotYRaw;
                double newScaleX = plotW / plotWRaw;
                double newScaleY = plotH / plotHRaw;
                
                double border = ((BasicStroke)plot.getRangeGridlineStroke()).getLineWidth()/2;
                
                System.out.println(plotBounds + "," + contentRectRaw);
                /*double contentX = plotBounds.getX()-border;
                double contentY = plotBounds.getY()-border;
                */
                
                double ox = 0;
                double oy = 0;
                if(plot.getRangeAxisCount() > 1 || plot.getRangeAxisLocation() == AxisLocation.TOP_OR_LEFT) {
                    ox = offsetX;
                    oy = offsetY;
                }
                boolean leftAxis = plot.getRangeAxisCount() > 1 || plot.getRangeAxisLocation() == AxisLocation.TOP_OR_LEFT;
                boolean rightAxis = plot.getRangeAxisCount() > 1 || plot.getRangeAxisLocation() == AxisLocation.TOP_OR_RIGHT;
                double contentX = contentRectRaw.getX() - (leftAxis?offsetX:0) - border;
                double contentY = contentRectRaw.getY() - (leftAxis?offsetY:0) - border;
                double contentW = contentRectRaw.getWidth()*newScaleX + (rightAxis?offsetX:0) + 2*border;
                double contentH = contentRectRaw.getHeight()*newScaleY + (rightAxis?offsetY:0) + 2*border;
                
                // compute legend position
                double x = contentX + contentW - w;
                double y = contentY;
                
                //System.out.println(x + "/" + y + ", " + w + "/" + h);
                legend.draw(g2, new Rectangle2D.Double(x, y, w, h));
            }

            @Override
            public void draw(Graphics2D g2, Rectangle2D area, ChartRenderingInfo info) {
                super.draw(g2, area, info);
            }

            @Override
            public void draw(Graphics2D g2, Rectangle2D area) {
                super.draw(g2, area);
            }
            
        };
        chart.setBackgroundPaint(Color.WHITE);
        
        chart.getXYPlot().getDomainAxis().setLabelFont(font);
        chart.getXYPlot().getDomainAxis().setTickLabelFont(font);
        chart.getXYPlot().getRangeAxis().setLabelFont(font);
        chart.getXYPlot().getRangeAxis().setTickLabelFont(font);
        
        //chart.addLegend(legend);
        
        // show plot
        panel = new ChartPanel(chart);
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

        private XYDataset dataset;
        private ValueAxis xAxis;
        private ValueAxis yAxis;

        public PlotInfo dataset(XYDataset dataset) {
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
            DeviationRenderer renderer = new DeviationRenderer(true, false);
            
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
            plot.setDataset(idx, dataset);
            plot.mapDatasetToRangeAxis(idx, idx);
            plot.setRenderer(idx, renderer);

            renderer.setAlpha(0.2f);
            for (int i = 0; i < plot.getSeriesCount(); i++) {
                Paint paint = ((DeviationRenderer) plot.getRenderer(0)).lookupSeriesPaint(i);
                renderer.setSeriesPaint(i, paint);
                renderer.setSeriesFillPaint(i, paint);
                if (idx > 0) {
                    renderer.setSeriesStroke(i, new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{2, 4}, 0));
                    renderer.setAlpha(0.1f);
                    renderer.setSeriesVisibleInLegend(i, false);
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

    private void writeState(int id, String title, List<IEPOSMeasurement> configMeasurements, PrintStream out) {
        Base64.Encoder encoder = Base64.getEncoder();

        out.println(id);
        out.println(title);
        for (IEPOSMeasurement m : configMeasurements) {
            out.println(m.label);
            out.println(m.globalMeasure);
            out.println(m.localMeasure);
            out.println(encoder.encodeToString(convertToBytes(m.globalMeasurements)));
            out.println(encoder.encodeToString(convertToBytes(m.localMeasurements)));
        }
    }

    private void readAndExecuteState(BufferedReader br) throws IOException {
        Base64.Decoder decoder = Base64.getDecoder();

        int id = Integer.parseInt(br.readLine());
        String title = br.readLine();
        List<IEPOSMeasurement> configMeasurements = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            IEPOSMeasurement m = new IEPOSMeasurement();
            m.label = line;
            m.globalMeasure = br.readLine();
            m.localMeasure = br.readLine();
            line = br.readLine();
            boolean withTime = line.length() < 50;
            if(withTime) {
                m.timeMeasure = line;
                line = br.readLine();
            }
            if("null".equals(m.localMeasure)) {
                m.localMeasure = null;
            }
            m.globalMeasurements = (List<Aggregate>) convertFromBytes(decoder.decode(line));
            m.localMeasurements = (List<Aggregate>) convertFromBytes(decoder.decode(br.readLine()));
            if(withTime) {
                m.timeMeasurements = (List<Double>) convertFromBytes(decoder.decode(br.readLine()));
            }
            configMeasurements.add(m);
        }
        evaluate(id, title, configMeasurements, null);
    }

    private byte[] convertToBytes(Object object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        } catch (IOException ex) {
            Logger.getLogger(JFreeChartEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Object convertFromBytes(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                ObjectInputStream in = new ObjectInputStream(bis)) {
            return in.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(JFreeChartEvaluator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    private static class LatexLogAxis extends LogarithmicAxis {
        public LatexLogAxis(String label) {
            super(label);
            setAllowNegativesFlag(true);
        }
    }
}
