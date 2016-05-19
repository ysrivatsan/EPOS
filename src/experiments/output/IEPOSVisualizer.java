/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package experiments.output;

import agents.Agent;
import agents.IGreedyAgent;
import agents.dataset.SparseAgentDataset;
import edu.uci.ics.jung.algorithms.layout.BalloonLayout;
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
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import org.apache.commons.collections15.Transformer;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Tree;
import edu.uci.ics.jung.visualization.DefaultVisualizationModel;
import edu.uci.ics.jung.visualization.VisualizationModel;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.Finger;
import protopeer.Peer;
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
    private Map<Agent, float[]> values;
    
    private IEPOSVisualizer() {
    }
    
    public static IEPOSVisualizer create(MeasurementLog log) {
        IEPOSVisualizer visualizer = new IEPOSVisualizer();
        
        Forest<Node, Integer> graph = new DelegateForest<>();
        visualizer.graph = graph;
        
        int maxIter = log.getMaxEpochNumber();
        visualizer.numIterations = maxIter;
        
        Set<Object> peers = log.getTagsOfType(Peer.class);
        Map<NetworkAddress, Node> idx2Node = new HashMap<>();
        Map<Agent, float[]> values = new HashMap<>();
        
        for(Object o : peers) {
            Peer peer = (Peer) o;
            Agent agent = (Agent) peer.getPeerletOfType(Agent.class);
            
            Node node = new Node();
            node.agent = agent;
            
            idx2Node.put(peer.getNetworkAddress(), node);
            graph.addVertex(node);

            float[] agentValues = new float[maxIter];
            for(int i = 0; i <= maxIter; i++) {
                
                double selected = log.getAggregateByEpochNumber(i, peer, "selected").getAverage();
                double numPlans = log.getAggregateByEpochNumber(i, peer, "numPlans").getAverage();
                if(Double.isNaN(selected)) {
                    visualizer.numIterations = i;
                    break;
                }
                agentValues[i] = (float) (1.0 - selected / numPlans);
            }
            
            values.put(agent, agentValues);
        }
        
        int edge = 0;
        for(Node node : graph.getVertices()) {
            for(Finger f : node.agent.getChildren()) {
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
    
    private VisualizationViewer<Node, Integer> visualize(Forest<Node, Integer> graph) {
        return visualize(new DefaultVisualizationModel(getLayout(graph)));
    }
    
    private void initIteration(int iteration) {
        for(Node n : graph.getVertices()) {
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
                
                if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    if(curIteration < numIterations-1) {
                        curIteration++;
                        refresh = true;
                    }
                } else if(e.getKeyCode() == KeyEvent.VK_LEFT) {
                    if(curIteration > 0) {
                        curIteration--;
                        refresh = true;
                    }
                }
                
                if(refresh) {
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
    }

    public void captureImage(int id) {
        VisualizationViewer<Node, Integer> viewer = visualize(graph);
        
        BufferedImage img = new BufferedImage(viewer.getWidth(), viewer.getHeight(), ColorSpace.TYPE_CMYK);

        viewer.setDoubleBuffered(false);
        viewer.getRootPane().paintComponents(img.createGraphics());

        try {
            ImageIO.write(img, "png", new File("output-dir/capture" + id + ".png"));
        } catch (IOException ex) {
            Logger.getLogger(IEPOSVisualizer.class.getName()).log(Level.SEVERE, null, ex);
        }

        viewer.setDoubleBuffered(true);
    }
    
    
    private static class Node {
        public Agent agent;
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
