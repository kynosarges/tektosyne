package org.kynosarges.tektosyne.demo;

import java.util.*;
import java.util.List; // clash with AWT List class
import java.util.function.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

import org.kynosarges.tektosyne.geometry.*;
import org.kynosarges.tektosyne.graph.*;
import org.kynosarges.tektosyne.subdivision.*;

/**
 * Provides a dialog for testing {@link Graph} algorithms.
 * Tests all available {@link Graph} algorithms on all available implementations.
 * This includes six different {@link PolygonGrid} configurations, and a planar
 * {@link Subdivision} that represents a Delaunay triangulation with its dual
 * {@link Voronoi} diagram.
 * <p>
 * Each graph node is associated with a random step cost, represented on the screen
 * by shades of grey. Darker shades equal higher costs, black nodes are inaccessible
 * and opaque. The actual step cost is printed on every accessible node. If a graph
 * algorithm succeeds, all result nodes are marked as well.</p>
 * 
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class GraphDialog extends JDialog {
    private static final long serialVersionUID = 0L;

    private final DrawPanel _output = new DrawPanel();

    private final JLabel _message = new JLabel("<html>Select a graph type and algorithm.<br>" +
            "(Messages about the algorithm appear here.)</html>");
    private final JComboBox<Choice> _graphChoice = new JComboBox<>();
    private final JCheckBox _vertexNeighbors;

    private final JComboBox<Choice> _algorithmChoice = new JComboBox<>();
    private final JButton _randomSource;
    private final JSpinner _threshold = new JSpinner();

    // current Graph: either PolygonGrid or Delaunay Subdivision
    private GraphManager<?> _graphManager;
    private PointD _border = PointD.EMPTY;

    /**
     * Creates a {@link GraphDialog}.
     * @param owner the {@link Window} that owns the dialog
     */
    public GraphDialog(Window owner) {
        super(owner);
        setModal(true);

        final JPanel panel = new JPanel();
        setContentPane(panel);

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        _output.setBackground(Color.WHITE);
        _output.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, true));

        _graphChoice.addItem(Choice.SQUARE_EDGE);
        _graphChoice.addItem(Choice.SQUARE_VERTEX);
        _graphChoice.addItem(Choice.HEXAGON_EDGE);
        _graphChoice.addItem(Choice.HEXAGON_VERTEX);
        _graphChoice.addItem(Choice.VORONOI);

        _graphChoice.setEditable(false);
        _graphChoice.setSelectedIndex(0);
        _graphChoice.setToolTipText("Select graph type (Alt+G)");
        _graphChoice.addItemListener(e -> _output.drawGraph());

        final JLabel graphLabel = new JLabel("Graph");
        graphLabel.setDisplayedMnemonic(KeyEvent.VK_G);
        graphLabel.setLabelFor(_graphChoice);

        _vertexNeighbors = new JCheckBox("Vertex Neighbors");
        _vertexNeighbors.setMnemonic(KeyEvent.VK_V);
        _vertexNeighbors.setToolTipText("Connect squares across vertices as well as edges (Alt+V)");
        _vertexNeighbors.addActionListener(e -> {
            if (_graphManager.setVertexNeighbors(_vertexNeighbors.isSelected()))
                _output.drawAlgorithm(false);
        });

        _algorithmChoice.addItem(Choice.A_STAR);
        _algorithmChoice.addItem(Choice.COVERAGE);
        _algorithmChoice.addItem(Choice.FLOOD_FILL);
        _algorithmChoice.addItem(Choice.VISIBILITY);

        _algorithmChoice.setEditable(false);
        _algorithmChoice.setSelectedIndex(0);
        _algorithmChoice.setToolTipText("Select algorithm to run (Alt+A)");
        _algorithmChoice.addItemListener(e -> _output.drawAlgorithm(false));

        final JLabel algorithmLabel = new JLabel("Algorithm");
        algorithmLabel.setDisplayedMnemonic(KeyEvent.VK_A);
        algorithmLabel.setLabelFor(_algorithmChoice);
        
        _randomSource = new JButton("Random Source");
        _randomSource.setMnemonic(KeyEvent.VK_R);
        _randomSource.setToolTipText("Re-run algorithm with new random source node (Alt+R)");
        _randomSource.addActionListener(e -> _output.drawAlgorithm(true));

        _threshold.setModel(new SpinnerNumberModel(0.33, 0, 1, 0.1));
        _threshold.setToolTipText("Set threshold for visibility algorithm (Alt+T)");
        _threshold.addChangeListener(e -> _output.drawAlgorithm(false));

        final JLabel thresholdLabel = new JLabel("Visibility Threshold");
        thresholdLabel.setDisplayedMnemonic(KeyEvent.VK_T);
        thresholdLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        thresholdLabel.setLabelFor(_threshold);

        final GroupLayout.Group inputH1 = layout.createSequentialGroup().
                addComponent(graphLabel).
                addComponent(_graphChoice).
                addComponent(_vertexNeighbors).
                addComponent(_randomSource).
                addContainerGap(0, Short.MAX_VALUE);

        final GroupLayout.Group inputV1 = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addComponent(graphLabel).
                addComponent(_graphChoice).
                addComponent(_vertexNeighbors).
                addComponent(_randomSource);

        final GroupLayout.Group inputH2 = layout.createSequentialGroup().
                addComponent(algorithmLabel).
                addComponent(_algorithmChoice).
                addComponent(thresholdLabel).
                addComponent(_threshold).
                addContainerGap(0, Short.MAX_VALUE);

        final GroupLayout.Group inputV2 = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addComponent(algorithmLabel).
                addComponent(_algorithmChoice).
                addComponent(thresholdLabel).
                addComponent(_threshold);

        layout.linkSize(SwingConstants.HORIZONTAL, graphLabel, algorithmLabel);
        layout.linkSize(SwingConstants.HORIZONTAL, _graphChoice, _algorithmChoice);
        layout.linkSize(SwingConstants.HORIZONTAL, _vertexNeighbors, thresholdLabel);
        layout.linkSize(SwingConstants.HORIZONTAL, _randomSource, _threshold);

        final JButton newGraph = new JButton("New");
        newGraph.requestFocus();
        newGraph.addActionListener(t -> _output.drawGraph());
        newGraph.setMnemonic(KeyEvent.VK_N);
        newGraph.setToolTipText("Generate new random graph (Alt+N)");

        final JButton close = CloseAction.createButton(this, panel);

        final GroupLayout.Group controlsH = layout.createSequentialGroup().
                addContainerGap(0, Short.MAX_VALUE).
                addComponent(newGraph).
                addComponent(close).
                addContainerGap(0, Short.MAX_VALUE);

        final GroupLayout.Group controlsV = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addComponent(newGraph).
                addComponent(close);

        layout.setHorizontalGroup(layout.createParallelGroup().
                addComponent(_message).
                addGroup(inputH1).
                addGroup(inputH2).
                addComponent(_output).
                addGroup(controlsH));

        layout.setVerticalGroup(layout.createSequentialGroup().
                addComponent(_message).
                addGroup(inputV1).
                addGroup(inputV2).
                addComponent(_output, 320, 320, Short.MAX_VALUE).
                addGroup(controlsV));

        setLocationByPlatform(true);
        setResizable(true);
        pack();
        setTitle("Graph Algorithm Test");
        SwingUtilities.invokeLater(_output::drawGraph);

        // HACK: Window.setMinimumSize ignores high DPI scaling
        addComponentListener(new ResizeListener(this));
        setVisible(true);
    }

    private enum Choice {

        SQUARE_EDGE("Square on Edge", null, null),
        SQUARE_VERTEX("Square on Vertex", null, null),
        HEXAGON_EDGE("Hexagon on Edge", null, null),
        HEXAGON_VERTEX("Hexagon on Vertex", null, null),
        VORONOI("Voronoi Regions", null, null),

        A_STAR("A* Pathfinding",
                "Red squares indicate best path from source to marked target node.",
                "Could find no connecting path due to impassable random terrain."),

        COVERAGE("Path Coverage",
                "Red squares indicate reachable nodes with a maximum path cost of 10.",
                "Could find no reachable locations due to impassable random terrain."),

        FLOOD_FILL("Flood Fill",
                "Red squares indicate connected nodes with up to 1/2 maximum node cost.",
                "Could find no matching locations due to impassable random terrain."),

        VISIBILITY("Visibility",
                "Red squares indicate visible nodes. Impassable nodes block the view.",
                "Could find no visible locations due to obscuring random terrain.");

        private final String _text, _success, _failure;

        Choice(String text, String success, String failure) {
            _text = text;
            _success = success;
            _failure = failure;
        }

        String resultMessage(boolean success) {
            return (success ? _success : _failure);
        }

        @Override
        public String toString() {
            return _text;
        }
    }

    private static class GraphManager<T> implements GraphAgent<T> {

        private final static Color[] NODE_COLORS_4 = {
            Color.decode("#edf8fb"), Color.decode("#b2e2e2"),
            Color.decode("#66c2a4"), Color.decode("#238b45")
        };

        private final static Color[] NODE_COLORS_8 = {
            Color.decode("#f7fcfd"), Color.decode("#e5f5f9"),
            Color.decode("#ccece6"), Color.decode("#99d8c9"),
            Color.decode("#66c2a4"), Color.decode("#41ae76"),
            Color.decode("#238b45"), Color.decode("#005824")
        };

        private final Graph<T> _graph;
        private final int _maxCost;
        private final PointD _maxWorld;
        private final double _scaleCost;

        private final List<T> _highlights = new ArrayList<>(2);
        private List<T> _locations = new ArrayList<>();
        private final Map<T, Integer> _nodeCosts;
        private final Color[] _nodeColors;

        GraphManager(Graph<T> graph, int maxCost, PointD maxWorld) {
            _graph = graph;
            _maxCost = maxCost;
            _maxWorld = maxWorld;

            // set random step costs for all nodes
            _nodeCosts = new HashMap<>(graph.nodeCount());
            for (T node: graph.nodes())
                _nodeCosts.put(node, 1 + Global.RANDOM.nextInt(maxCost));

            // scaling factor for Subdivision (see getStepCost)
            if (graph instanceof Subdivision)
                _scaleCost = _maxWorld.x + _maxWorld.y;
            else
                _scaleCost = 1;

            switch (maxCost) {
                case 4: _nodeColors = NODE_COLORS_4; break;
                case 8: _nodeColors = NODE_COLORS_8; break;

                default:
                    // create darkening shades for increasing costs
                    _nodeColors = new Color[maxCost];
                    for (int i = 0; i < maxCost; i++) {
                        final float gray = 1 - i / (float) maxCost;
                        _nodeColors[i] = new Color(gray, gray, gray);
                    }
                    break;
            }
        }

        private T findNode(NodeLocation location) {
            switch (location) {
                case TOP_LEFT:
                    return _graph.findNearestNode(PointD.EMPTY);

                case BOTTOM_RIGHT:
                    return _graph.findNearestNode(_maxWorld);

                default:
                    return _graph.findNearestNode(new PointD(
                        _maxWorld.x * Global.RANDOM.nextDouble(),
                        _maxWorld.y * Global.RANDOM.nextDouble()));
            }
        }

        private T findSource(boolean random) {
            T source;
            if (!random && !_highlights.isEmpty())
                source = _highlights.get(0);
            else
                source = findNode(NodeLocation.RANDOM);

            _highlights.clear();
            _highlights.add(source);

            return source;
        }

        boolean runAStar() {
            final T source = findNode(NodeLocation.TOP_LEFT);
            final T target = findNode(NodeLocation.BOTTOM_RIGHT);

            _highlights.clear();
            _highlights.add(source);
            _highlights.add(target);

            // find best path from source to target
            final AStar<T> aStar = new AStar<>(_graph);
            aStar.useWorldDistance = true;
            final boolean success = aStar.findBestPath(this, source, target);
            _locations = aStar.nodes();

            return success;
        }

        boolean runCoverage(boolean random) {
            final T source = findSource(random);

            // find all nodes reachable from source
            // (note: scaling maximum step cost for Subdivision)
            final Coverage<T> coverage = new Coverage<>(_graph);
            final boolean success = coverage.findReachable(this, source, _scaleCost * 10);
            _locations = coverage.nodes();

            return success;
        }

        boolean runFloodFill(boolean random) {
            final T source = findSource(random);

            // find all nodes reachable from source node
            final FloodFill<T> floodFill = new FloodFill<>(_graph);
            final Predicate<T> match = (p -> _nodeCosts.get(p) <= _maxCost / 2);
            final boolean success = floodFill.findMatching(match, source);
            _locations = floodFill.nodes();

            return success;
        }

        boolean runVisibility(boolean random, double threshold) {
            final T source = findSource(random);

            // find all nodes visible from source node
            final Visibility<T> visibility = new Visibility<>(_graph);
            visibility.setThreshold(threshold);
            final Predicate<T> isOpaque = (p -> _nodeCosts.get(p) >= _maxCost);
            final boolean success = visibility.findVisible(isOpaque, source, 0);
            _locations = visibility.nodes();

            return success;
        }

        boolean setVertexNeighbors(boolean value) {
            if (!(_graph instanceof PolygonGrid))
                return false;

            final PolygonGrid grid = (PolygonGrid) _graph;
            final RegularPolygon element = grid.element();
            if (element.sides != 4 || element.vertexNeighbors == value)
                return false;

            grid.setElement(new RegularPolygon(element.length, 4, element.orientation, value));
            return true;
        }

        void showNodes(Graphics2D g2, PointD offset) {
            for (T node: _graph.nodes()) {
                final int cost = _nodeCosts.get(node);

                final PointD[] region = _graph.getWorldRegion(node);
                if (region != null) {
                    final PointD[] polygon = _graph.getWorldRegion(node);
                    final Path2D path = Global.drawPolygon(offset, polygon);
                    g2.setColor(_nodeColors[cost - 1]);
                    g2.fill(path);
                }

                final PointD center = _graph.getWorldLocation(node);
                final String costString = (cost < _maxCost ? Integer.toString(cost) : "—");
                final Color costColor = (cost <= _maxCost / 2 ? Color.BLACK : Color.WHITE);

                final Rectangle2D bounds = g2.getFontMetrics().getStringBounds(costString, g2);
                g2.setColor(costColor);
                g2.drawString(costString,
                        (float) (center.x + offset.x - bounds.getWidth() / 2),
                        (float) (center.y + offset.y + bounds.getHeight() / 4));

                // indicate found nodes by red rectangles
                if (_locations.contains(node)) {
                    final Rectangle2D rect = new Rectangle2D.Double(
                            center.x + offset.x - 7, center.y + offset.y - 7, 14, 14);
                    g2.setColor(Color.RED);
                    g2.draw(rect);
                }

                // indicate highlights by blue circles
                if (_highlights.contains(node)) {
                    final Ellipse2D circle = Global.drawCircle(center.add(offset), 20);
                    g2.setColor(Color.BLUE);
                    g2.draw(circle);
                }
            }
        }

        /// GraphAgent<T> Members

        @Override
        public boolean relaxedRange() {
            return false;
        }

        @Override
        public boolean canMakeStep(T source, T target) {
            return (_nodeCosts.get(target) < _maxCost);
        }

        @Override
        public boolean canOccupy(T target) {
            return true;
        }

        @Override
        public double getStepCost(T source, T target) {
            /*
             * Subdivision graphs must scale step costs by world distance because Graph<T>
             * requires that the step cost is never less than the getDistance result. Step costs
             * must be multiplied with the scaling factor (and not added) so that multiple cheap
             * steps are preferred to a single, more expensive step.
             * 
             * 1. Using the current distance makes pathfinding sensitive to both world distance
             *    and step cost. For best results, we would average out the step costs of source
             *    and target. This corresponds exactly to the visible Voronoi region shading,
             *    as Delaunay edges are always halved by region boundaries.
             * 
             * 2. Using a fixed value that equals or exceeds the maximum possible distance
             *    between any two nodes makes pathfinding sensitive only to assigned step costs.
             *    This effectively replicates the behavior on a PolygonGrid.
             */
            //double distance = _graph.getDistance(source, target);
            //return (distance * (nodeCosts.get(source) + nodeCosts.get(target)) / 2);

            return _scaleCost * _nodeCosts.get(target);
        }

        @Override
        public boolean isNearTarget(T source, T target, double distance) {
            return (distance == 0);
        }

        private enum NodeLocation {
            TOP_LEFT,
            BOTTOM_RIGHT,
            RANDOM
        }
    }

    /**
     * Provides the custom drawing {@link JPanel} for the {@link GraphDialog}.
     */
    private class DrawPanel extends JPanel {
        private static final long serialVersionUID = 0L;

        // call(s) to draw graph edges during repaint
        private Consumer<Graphics2D> _drawGraphCall;

        /**
         * Computes and draws the currently configured {@link Graph}.
         */
        void drawGraph() {
            final Choice graphChoice = (Choice) _graphChoice.getSelectedItem();
            final boolean vertexNeighbors = _vertexNeighbors.isSelected();
            RegularPolygon polygon = null;

            switch (graphChoice) {
                case SQUARE_EDGE:
                    polygon = new RegularPolygon(24, 4, PolygonOrientation.ON_EDGE, vertexNeighbors);
                    break;

                case SQUARE_VERTEX:
                    polygon = new RegularPolygon(24, 4, PolygonOrientation.ON_VERTEX, vertexNeighbors);
                    break;

                case HEXAGON_EDGE:
                    polygon = new RegularPolygon(16, 6, PolygonOrientation.ON_EDGE);
                    break;

                case HEXAGON_VERTEX:
                    polygon = new RegularPolygon(16, 6, PolygonOrientation.ON_VERTEX);
                    break;
            }

            if (polygon != null) {
                _vertexNeighbors.setEnabled(polygon.sides == 4);

                final PolygonGrid grid = new PolygonGrid(polygon);
                _border = PolygonGridDialog.sizeGrid(grid, this);
                final PointD maxWorld = new PointD(getWidth(), getHeight());
                _graphManager = new GraphManager<>(grid, 4, maxWorld);

                // draw edges from Subdivision to avoid overlapping polygon edges
                final PolygonGridMap map = new PolygonGridMap(grid, PointD.EMPTY, 0);
                _drawGraphCall = g2 -> drawEdges(g2, map.source(), false);
            }
            else {
                _vertexNeighbors.setEnabled(false);
                _border = PointD.EMPTY;

                final RectD output = new RectD(0, 0, getWidth(), getHeight());
                final RectD bounds = new RectD(8, 8, getWidth() - 16, getHeight() - 16);
                final PointD[] points = GeoUtils.randomPoints(40, bounds, new PointDComparatorX(0), 20);

                final VoronoiResults results = Voronoi.findAll(points, output);
                final Subdivision division = results.toDelaunaySubdivision(output, true);
                _graphManager = new GraphManager<>(division, 8, output.max);

                // draw Voronoi edges with superimposed Delaunay edges
                final VoronoiMap map = new VoronoiMap(results);
                _drawGraphCall = g2 -> {
                    drawEdges(g2, map.source(), false);
                    drawEdges(g2, division, true);
                };
            }

            drawAlgorithm(true);
        }

        /**
         * Runs and draws the currently configured {@link Graph} algorithm.
         * @param random {@code true} for a new random starting location, else {@code false}
         */
        private void drawAlgorithm(boolean random) {
            final Choice algorithmChoice = (Choice) _algorithmChoice.getSelectedItem();
            boolean success = false;

            switch (algorithmChoice) {
                case A_STAR:
                    success = _graphManager.runAStar();
                    break;

                case COVERAGE:
                    success = _graphManager.runCoverage(random);
                    break;

                case FLOOD_FILL:
                    success = _graphManager.runFloodFill(random);
                    break;

                case VISIBILITY:
                    final double threshold = (Double) _threshold.getValue();
                    success = _graphManager.runVisibility(random, threshold);
                    break;
            }

            _message.setText("<html>Source is blue circle, numbers indicate step costs, dashes are impassable.<br>" + algorithmChoice.resultMessage(success) + "</html>");

            _randomSource.setEnabled(algorithmChoice != Choice.A_STAR);
            _threshold.setEnabled(algorithmChoice == Choice.VISIBILITY);

            repaint();
        }

        private void drawEdges(Graphics2D g2, Subdivision division, boolean isDelaunay) {
            for (LineD edge: division.toLines()) {
                double x1 = _border.x + edge.start.x;
                double y1 = _border.y + edge.start.y;
                double x2 = _border.x + edge.end.x;
                double y2 = _border.y + edge.end.y;

                final Stroke oldStroke = g2.getStroke();
                if (isDelaunay) {
                    g2.setColor(Color.decode("#FFD700")); // JavaFX GOLD
                    g2.setStroke(new BasicStroke(1f,
                            BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER,
                            10f, new float[] { 2f, 4f }, 0));

                    // clear space around cost markers
                    final double angle = edge.angle();
                    final double clear = 11;
                    x1 += Math.cos(angle) * clear;
                    y1 += Math.sin(angle) * clear;
                    x2 -= Math.cos(angle) * clear;
                    y2 -= Math.sin(angle) * clear;
                } else
                    g2.setColor(Color.BLACK);

                final Line2D line = new Line2D.Double(x1, y1, x2, y2);
                g2.draw(line);
                g2.setStroke(oldStroke);
            }
        }

        /**
         * Invoked by Swing to draw the {@link DrawPanel}.
         * @param g the {@link Graphics2D} context in which to paint
         */
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (_graphManager == null)
                return;

            final Graphics2D g2 = (Graphics2D) g;
            _graphManager.showNodes(g2, _border);
            _drawGraphCall.accept(g2);
        }
    }
}
