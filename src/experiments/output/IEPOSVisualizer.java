/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package experiments.output;

import agents.TreeNode;
import edu.uci.ics.jung.algorithms.layout.RadialTreeLayout;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.DirectionalEdgeArrowTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Shape;
import java.io.File;
import javax.swing.JFrame;
import org.apache.commons.collections15.Transformer;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.visualization.DefaultVisualizationModel;
import edu.uci.ics.jung.visualization.VisualizationModel;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import protopeer.Finger;
import protopeer.measurement.MeasurementLog;
import protopeer.network.NetworkAddress;

/**
 *
 * @author Evangelos
 */
public class IEPOSVisualizer {

    private final Dimension size = new Dimension(1920, 1080);
    private Forest<Node, Integer> graph;
    private int numIterations;
    private int curIteration;
    private Map<TreeNode, float[]> values;

    public static IEPOSVisualizer create(MeasurementLog log) {
        IEPOSVisualizer visualizer = new IEPOSVisualizer();

        Forest<Node, Integer> graph = new DelegateForest<>();
        visualizer.graph = graph;
        visualizer.numIterations = log.getMaxEpochNumber();;

        Map<NetworkAddress, Node> idx2Node = new HashMap<>();
        Map<TreeNode, float[]> values = new HashMap<>();
        
        String measureTag = null;
        for(Object tagObj : log.getTagsOfType(String.class)) {
            String tag = (String) tagObj;
            if(tag.startsWith("local")) {
                measureTag = tag;
                break;
            }
        }
        
        if(measureTag == null) {
            throw new IllegalArgumentException("no local measurements available");
        }

        for (Object agentObj : log.getTagsOfType(TreeNode.class)) {
            TreeNode agent = (TreeNode) agentObj;
            
            Node node = new Node();
            node.agent = agent;

            idx2Node.put(agent.id.getNetworkAddress(), node);
            graph.addVertex(node);

            float[] agentValues = new float[visualizer.numIterations];
            for (int i = 0; i < visualizer.numIterations; i++) {
                double localError = log.getAggregate(i, measureTag, agent).getAverage();
                if (Double.isNaN(localError)) {
                    visualizer.numIterations = i;
                    break;
                }
                agentValues[i] = 1.0f - (float) localError;
            }

            values.put(agent, agentValues);
        }

        int edge = 0;
        for (Node node : graph.getVertices()) {
            for (Finger f : node.agent.children) {
                Node child = idx2Node.get(f.getNetworkAddress());
                graph.addEdge(edge++, node, child);
            }
        }

        visualizer.values = values;
        return visualizer;
    }

    private VisualizationViewer<Node, Integer> visualize(VisualizationModel<Node, Integer> model) {
        VisualizationViewer<Node, Integer> viewer = new VisualizationViewer<>(model);
        viewer.setPreferredSize(size);
        viewer.setBackground(Color.white);
        viewer.getRenderContext().setEdgeShapeTransformer(getEdgeShapeTransformer());
        viewer.getRenderContext().setEdgeArrowTransformer(getEdgeArrowTransformer());
        viewer.getRenderContext().setVertexFillPaintTransformer(getVertexFillPaintTransformer());
        viewer.setGraphMouse(getGraphMouse());
        return viewer;
    }

    private void initIteration(int iteration) {
        for (Node n : graph.getVertices()) {
            n.val = values.get(n.agent)[iteration];
        }
    }

    private <V, E> Layout<V, E> getLayout(Forest<V, E> graph) {
        Layout<V, E> layout = new RadialTreeLayout<>(graph);
        layout.setSize(size);
        return layout;
    }

    private <V, E> Transformer<Context<Graph<V, E>, E>, Shape> getEdgeShapeTransformer() {
        return new EdgeShape.Line();
    }

    private <V, E> Transformer<Context<Graph<V, E>, E>, Shape> getEdgeArrowTransformer() {
        return new DirectionalEdgeArrowTransformer<>(0, 0, 0);
    }

    private Transformer<Node, Paint> getVertexFillPaintTransformer() {
        return (Node vertex) -> {
            return new Color(vertex.val, vertex.val, vertex.val);
        };
    }

    private VisualizationViewer.GraphMouse getGraphMouse() {
        DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();
        graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
        return graphMouse;
    }

    public void showGraph() {
        JFrame frame = new JFrame("IEPOS");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        curIteration = 0;
        initIteration(curIteration);
        frame.setTitle("IEPOS - iteration " + (curIteration+1));
        VisualizationModel<Node, Integer> model = new DefaultVisualizationModel(getLayout(graph));
        VisualizationViewer<Node, Integer> viewer = visualize(model);

        KeyListener keyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                boolean refresh = false;

                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    if (curIteration < numIterations - 1) {
                        curIteration++;
                        refresh = true;
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    if (curIteration > 0) {
                        curIteration--;
                        refresh = true;
                    }
                }
                frame.setTitle("IEPOS - iteration " + (curIteration+1));

                if (refresh) {
                    initIteration(curIteration);
                    model.setGraphLayout(model.getGraphLayout());
                    //model.setGraphLayout(getLayout((Forest<Node, Integer>) tree));
                    viewer.invalidate();
                    viewer.validate();
                }
            }
        };

        viewer.addKeyListener(keyListener);
        frame.getContentPane().add(viewer);
        frame.pack();
        frame.setVisible(true);

        frame.setMenuBar(new MenuBar());
        Menu menu = new Menu("File");
        MenuItem saveas = new MenuItem("Save As...");
        saveas.addActionListener((ActionEvent e) -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.addChoosableFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.getName().endsWith(".png");
                }

                @Override
                public String getDescription() {
                    return ".png files";
                }
            });
            fileChooser.addChoosableFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.getName().endsWith(".svg");
                }

                @Override
                public String getDescription() {
                    return ".svg files";
                }
            });
            int returnVal = fileChooser.showSaveDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                
                ImageFile img = null;
                if(file.getName().endsWith(".png")) {
                    img = new PngFile(file, viewer.getWidth(), viewer.getHeight());
                } else if(file.getName().endsWith(".svg")) {
                    img = new SvgFile(file, viewer.getWidth(), viewer.getHeight());
                }
                
                viewer.setDoubleBuffered(false);
                viewer.getRootPane().paintComponents(img.createGraphics());
                viewer.setDoubleBuffered(true);

                img.write();
            }
        });
        menu.add(saveas);
        frame.getMenuBar().add(menu);
    }

    private static class Node {

        public TreeNode agent;
        public float val;

        @Override
        public int hashCode() {
            int hash = 3;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Node other = (Node) obj;
            if (!Objects.equals(this.agent, other.agent)) {
                return false;
            }
            return true;
        }

    }
}
