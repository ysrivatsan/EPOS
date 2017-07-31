/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent.logging;

import agent.Agent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.swing.JFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.ui.RectangleInsets;
import protopeer.measurement.Aggregate;
import protopeer.measurement.MeasurementLog;
import util.JFreeChartCustomLegend;
import data.DataType;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * An AgentLogger that only implements output logic. It reads the global cost
 * and, if present, the average local cost in each iteration and plots this
 * information.
 *
 * @author Peter
 */
public class CostViewer<V extends DataType<V>> extends AgentLogger<Agent<V>> {

    private static File defaultSrcDir = new File(".");
    private static File defaultDstDir = new File(".");

    private Font font = new Font("Computer Modern", Font.PLAIN, 12);

    private ChartPanel panel;

    private boolean showFrame;
    private boolean writeToFile = false;
    private String output = null;

    /**
     * Plots global and local cost in a new frame.
     */
    public CostViewer() {
        this(true);
    }

    /**
     * Plots global and local cost.
     *
     * @param showFrame if true, a new frame is opened to show the plot.
     */
    public CostViewer(boolean showFrame) {
        this.showFrame = showFrame;
    }

    public CostViewer(boolean showFrame, boolean writeToFile, String output) {
        this.showFrame = showFrame;
        this.writeToFile = writeToFile;
        this.output = output;
    }

    @Override
    public void init(Agent agent) {
    }

    @Override
    public void log(MeasurementLog log, int epoch, Agent agent) {
    }

    @Override
    public void print(MeasurementLog log) {
        try {
            print(Arrays.asList(log));
        } catch (IOException ex) {
            Logger.getLogger(CostViewer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void print(List<MeasurementLog> logs) throws IOException {
        Locale.setDefault(Locale.US);

        MyPlot plot = new MyPlot();
        ValueAxis xAxis = getAxis(getProperty(logs.get(0), "yLabel", "iteration"));

        // add global data
        YIntervalSeriesCollection globalDataset = getDataset(GlobalCostLogger.class.getName(), logs);
        ValueAxis globalYAxis = getAxis(getProperty(logs.get(0), "globalXLabel", "global cost"));
        plot.add(globalDataset, xAxis, globalYAxis);

        // add local data
        YIntervalSeriesCollection localDataset = getDataset(LocalCostLogger.class.getName(), logs);
        ValueAxis localYAxis = getAxis(getProperty(logs.get(0), "localXLabel", "local cost"));
        plot.add(localDataset, xAxis, localYAxis);

        LegendTitle legend = createLegend(plot);
        panel = createChartPanel(plot, legend);

        if (showFrame) {
            createAndShowFrame(logs.get(0), panel);
        }
        if (writeToFile) {
            BufferedImage Global_Cost_Img = getPlotImage(500, 250);
            ImageIO.write(Global_Cost_Img, "jpg", new File(output + "/Global_Cost.jpg"));
        }
    }

    public BufferedImage getPlotImage(int width, int height) {
        BufferedImage outputImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        panel.setSize(width, height);
        panel.setVisible(true);
        panel.paint(outputImg.getGraphics());
        panel.setVisible(false);
        return outputImg;
    }

    private String getProperty(MeasurementLog log, String propertyName, String defaultValue) {
        return (String) log.getTagsOfType(String.class).stream()
                .filter(tag -> ((String) tag).startsWith(propertyName + "="))
                .findFirst().orElse(defaultValue);
    }

    private YIntervalSeriesCollection getDataset(Object datasetTag, List<MeasurementLog> logs) {
        YIntervalSeriesCollection dataset = new YIntervalSeriesCollection();
        for (MeasurementLog log : logs) {
            String label = getProperty(log, "label", "LABEL");
            YIntervalSeries series = new YIntervalSeries(label);
            for (int i = 0; true; i++) {
                Aggregate aggregate = log.getAggregate(datasetTag, i);
                if (aggregate == null || aggregate.getNumValues() < 1) {
                    break;
                }
                double avg = aggregate.getAverage();
                double std = aggregate.getStdDev();
                series.add(i + 1, avg, avg - std, avg + std);
            }
            dataset.addSeries(series);
        }
        return dataset;
    }

    private ValueAxis getAxis(String axisStr) {
        ValueAxis axis;

        axisStr = axisStr.trim();

        if (axisStr.startsWith("log_")) {
            axisStr = axisStr.substring(4);
            axis = new LatexLogAxis(axisStr);
        } else {
            axis = new NumberAxis(axisStr);
        }

        if (axisStr.endsWith(")")) {
            String rangeStr = axisStr.substring(axisStr.indexOf("("));
            rangeStr = rangeStr.substring(1, rangeStr.length() - 1);
            String[] range = rangeStr.split("-");
            axis.setRange(Double.parseDouble(range[0]), Double.parseDouble(range[1]));
            axisStr = axisStr.substring(0, axisStr.indexOf("("));
            axis.setLabel(axisStr);
        }

        return axis;
    }

    private LegendTitle createLegend(Plot plot) {
        LegendTitle legend = new LegendTitle(plot);
        legend.setItemFont(font);
        legend.setItemLabelPadding(new RectangleInsets(0, 0, 0, 10));
        legend.setFrame(new BlockBorder());
        legend.setBackgroundPaint(Color.WHITE);
        return legend;
    }

    private ChartPanel createChartPanel(XYPlot plot, LegendTitle legend) {
        JFreeChartCustomLegend chart = new JFreeChartCustomLegend(null, font, plot, legend);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getXYPlot().getDomainAxis().setLabelFont(font);
        chart.getXYPlot().getDomainAxis().setTickLabelFont(font);
        chart.getXYPlot().getRangeAxis().setLabelFont(font);
        chart.getXYPlot().getRangeAxis().setTickLabelFont(font);

        ChartPanel panel = chart.getPanel();
        panel.setDefaultDirectoryForSaveAs(defaultDstDir);
        panel.setMinimumDrawHeight(1);
        panel.setMinimumDrawWidth(1);

        return panel;
    }

    private void createAndShowFrame(MeasurementLog log, ChartPanel panel) {
        String title = getProperty(log, "title", "TITLE");

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        frame.setSize(256, 256);
        frame.setVisible(true);
    }

    private static class LatexLogAxis extends LogarithmicAxis {

        public LatexLogAxis(String label) {
            super(label);
            setAllowNegativesFlag(true);
        }
    }

    private static class MyPlot extends XYPlot {

        private int idx = 0;

        public MyPlot() {
            setDrawingSupplier(new MyDrawingSupplier());
        }

        public void add(YIntervalSeriesCollection dataset, ValueAxis xAxis, ValueAxis yAxis) {
            YIntervalSeriesCollection continuousDS = new YIntervalSeriesCollection();
            YIntervalSeriesCollection discreteDS = new YIntervalSeriesCollection();
            for (int series = 0; series < dataset.getSeriesCount(); series++) {
                YIntervalSeries s = dataset.getSeries(series);
                if (s.getItemCount() > 1) {
                    continuousDS.addSeries(s);
                } else if (s.getItemCount() == 1) {
                    discreteDS.addSeries(s);
                } else {
                    return;
                }
            }

            DeviationRenderer continuousRenderer = new DeviationRenderer(true, false);
            XYErrorRenderer discreteRenderer = new XYErrorRenderer();

            setDomainAxis(0, xAxis);
            if (xAxis.isAutoRange() && "iteration".equals(xAxis.getLabel())) {
                xAxis.setRange(0, dataset.getItemCount(0));
            } else if (xAxis.isAutoRange()) {
                double max = Double.POSITIVE_INFINITY;
                for (int i = 0; i < dataset.getSeriesCount(); i++) {
                    max = Math.min(max, dataset.getXValue(i, dataset.getItemCount(i) - 1));
                }
                xAxis.setRange(0, max);
            }
            setRangeAxis(idx, yAxis);
            setDataset(idx, continuousDS);
            mapDatasetToRangeAxis(idx, idx);
            setRenderer(idx, continuousRenderer);
            if (discreteDS.getSeriesCount() > 0) {
                setDataset(idx + 1, discreteDS);
                mapDatasetToRangeAxis(idx + 1, idx);
                setRenderer(idx + 1, discreteRenderer);
            }

            continuousRenderer.setAlpha(0.2f);
            for (int i = 0; i < getSeriesCount(); i++) {
                Paint paint = ((DeviationRenderer) continuousRenderer).lookupSeriesPaint(i);
                continuousRenderer.setSeriesPaint(i, paint);
                continuousRenderer.setSeriesFillPaint(i, paint);
                if (idx > 0) {
                    continuousRenderer.setSeriesStroke(i, new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{2, 4}, 0));
                    continuousRenderer.setAlpha(0.1f);
                    continuousRenderer.setSeriesVisibleInLegend(i, false);
                }
            }

            idx++;
        }
    }

    private static class MyDrawingSupplier extends DefaultDrawingSupplier {

        private static Stroke solid = new BasicStroke();
        private static Stroke dashed = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1, new float[]{4.0f, 4.0f}, 1);

        private static Paint[] paints = {Color.RED, Color.GREEN, Color.BLUE, Color.BLACK};
        private static Stroke[] strokes = {solid};
        private static Shape[] shapes = {new Ellipse2D.Double()};

        public MyDrawingSupplier() {
            super(paints, paints, strokes, strokes, shapes);
        }

        private DefaultDrawingSupplier dds = new DefaultDrawingSupplier();

        @Override
        public Shape getNextShape() {
            return dds.getNextShape();
        }
    }
}
