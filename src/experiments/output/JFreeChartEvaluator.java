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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.InputMethodListener;
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
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.CenterArrangement;
import org.jfree.chart.block.ColorBlock;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.*;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;
import protopeer.measurement.Aggregate;

/**
 *
 * @author Peter
 */
public class JFreeChartEvaluator extends IEPOSEvaluator {
    
    private static File defaultSrcDir = new File("C:\\Users\\Peter\\OneDrive\\Dokumente\\Master Studium\\Master Thesis\\MyThesis\\fig\\Plot Files");
    private static File defaultDstDir = new File("C:\\Users\\Peter\\OneDrive\\Dokumente\\Master Studium\\Master Thesis\\MyThesis\\fig");

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

        xAxis = new NumberAxis(getXLabel());
        List<PlotInfo> plotInfos = new ArrayList<>();
        plotInfos.add(new PlotInfo()
                .dataset(toDataset(configMeasurements.stream().collect(Collectors.toMap(x -> x.label, x -> x.globalMeasurements))))
                .yLabel(getYLabel(configMeasurements.stream().map(x -> x.globalMeasure))));
        if (printLocal) {
            plotInfos.add(new PlotInfo()
                    .dataset(toDataset(configMeasurements.stream().collect(Collectors.toMap(x -> x.label, x -> x.localMeasurements))))
                    .yLabel(getYLabel(configMeasurements.stream().map(x -> x.localMeasure))));
        }
        xAxis.setRange(0,plotInfos.get(0).dataset.getItemCount(0));
        XYPlot plot = new XYPlot();
        plot.setDomainAxis(0, xAxis);

        for (int i = 0; i < plotInfos.size(); i++) {
            PlotInfo plotInfo = plotInfos.get(i);
            plotInfo.addToPlot(plot, i);
        }

        Font font = new Font("Computer Modern", Font.PLAIN, 12);
        JFreeChart chart = new JFreeChart(null, font,  plot, false);
        chart.setBackgroundPaint(Color.WHITE);
        
        chart.getXYPlot().getDomainAxis().setLabelFont(font);
        chart.getXYPlot().getDomainAxis().setTickLabelFont(font);
        chart.getXYPlot().getRangeAxis().setLabelFont(font);
        chart.getXYPlot().getRangeAxis().setTickLabelFont(font);
        
        LegendTitle legend = new LegendTitle(plot);
        legend.setItemFont(font);
        legend.setItemLabelPadding(new RectangleInsets(0, 0, 0, 5));
        legend.setFrame(new BlockBorder());
        legend.getItemContainer().add(new ColorBlock(Color.BLUE, 20, 20));
        Panel myPanel = new Panel()/* {
            @Override
            public void paintAll(Graphics g) {
                super.paintAll(g);
                paint(g);
            }

            @Override
            public void paint(Graphics g) {
                super.paint(g);
                legend.draw((Graphics2D)g, new Rectangle(100, 100, 200, 300));
            }
        }*/;
        legend.setPosition(RectangleEdge.BOTTOM);
        chart.addLegend(legend);
        
        // show plot
        ChartPanel panel = new ChartPanel(chart);
        panel.setDefaultDirectoryForSaveAs(defaultDstDir);
        myPanel.setLayout(new BorderLayout(0,0));
        myPanel.add(panel);
        frame.setContentPane(myPanel);
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
        private String range;
        private ValueAxis axis;

        public PlotInfo dataset(XYDataset dataset) {
            this.dataset = dataset;
            return this;
        }

        public PlotInfo yLabel(String yLabel) {
            yLabel = yLabel.trim();
            if(yLabel.endsWith(")")) {
                range = yLabel.substring(yLabel.indexOf("("));
                range = range.substring(1,range.length()-1);
                yLabel = yLabel.substring(0,yLabel.indexOf("("));
            }
            if(yLabel.startsWith("log_")) {
                axis = new LatexLogAxis(yLabel.substring(4));
            } else {
                axis = new NumberAxis(yLabel);
            }
            this.yLabel = yLabel;
            return this;
        }

        public void addToPlot(XYPlot plot, int idx) {
            DeviationRenderer renderer = new DeviationRenderer(true, false);
            
            plot.setRangeAxis(idx, axis);
            if(range != null) {
                String[] fromTo = range.split("-");
                plot.getRangeAxis().setRange(Double.parseDouble(fromTo[0]), Double.parseDouble(fromTo[1]));
            }
            
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
            if("null".equals(m.localMeasure)) {
                m.localMeasure = null;
            }
            m.globalMeasurements = (List<Aggregate>) convertFromBytes(decoder.decode(br.readLine()));
            m.localMeasurements = (List<Aggregate>) convertFromBytes(decoder.decode(br.readLine()));
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
