package org.kynosarges.tektosyne.demo;

import java.util.*;
import java.util.function.Predicate;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.*;

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
 * @version 6.0.0
 */
public class GraphDialog extends Stage {

    private final Pane _output = new Pane();
    private final Pane _outputUnderlay = new Pane();

    private final Label _message = new Label("Select a graph type and algorithm.");
    private final ComboBox<Choice> _graphChoice = new ComboBox<>();
    private final CheckBox _vertexNeighbors;

    private final ComboBox<Choice> _algorithmChoice = new ComboBox<>();
    private final Button _randomSource;
    private final Spinner<Double> _threshold = new Spinner<>(0, 1, 0.33, 0.1);

    // current Graph: either PolygonGrid or Delaunay Subdivision
    private GraphManager<?> _graphManager;
    private PointD _border = PointD.EMPTY;

    /**
     * Creates a {@link GraphDialog}.
     */    
    public GraphDialog() {
        initOwner(Global.primaryStage());
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);

        _message.setMinHeight(36); // reserve space for two lines
        _message.setWrapText(true);

        Global.clipChildren(_output);
        _output.setPrefSize(400, 300);
        _output.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, new CornerRadii(4), BorderWidths.DEFAULT)));

        Global.clipChildren(_outputUnderlay);
        _outputUnderlay.setPrefSize(400, 300);

        _graphChoice.getItems().addAll(Choice.SQUARE_EDGE, Choice.SQUARE_VERTEX,
                Choice.HEXAGON_EDGE, Choice.HEXAGON_VERTEX, Choice.VORONOI);
        _graphChoice.getEditor().setEditable(false);
        _graphChoice.setTooltip(new Tooltip("Select graph type (Alt+G)"));
        _graphChoice.getSelectionModel().select(0);
        _graphChoice.getSelectionModel().selectedItemProperty().addListener(
                (ov, oldValue, newValue) -> drawGraph());

        final Label graphLabel = new Label("_Graph");
        graphLabel.setLabelFor(_graphChoice);
        graphLabel.setMnemonicParsing(true);

        _vertexNeighbors = new CheckBox("_Vertex Neighbors");
        _vertexNeighbors.setTooltip(new Tooltip("Connect squares across vertices as well as edges (Alt+V)"));
        _vertexNeighbors.selectedProperty().addListener((ov, oldValue, newValue) -> {
            if (_graphManager.setVertexNeighbors(newValue))
                drawAlgorithm(false);
        });

        _algorithmChoice.getItems().addAll(
                Choice.A_STAR, Choice.COVERAGE, Choice.FLOOD_FILL, Choice.VISIBILITY);
        _algorithmChoice.getEditor().setEditable(false);
        _algorithmChoice.getSelectionModel().select(0);
        _algorithmChoice.setTooltip(new Tooltip("Select algorithm to run (Alt+A)"));
        _algorithmChoice.getSelectionModel().selectedItemProperty().addListener(
                (ov, oldValue, newValue) -> drawAlgorithm(false));

        final Label algorithmLabel = new Label("_Algorithm");
        algorithmLabel.setLabelFor(_algorithmChoice);
        algorithmLabel.setMnemonicParsing(true);
        
        _randomSource = new Button("_Random Source");
        _randomSource.setTooltip(new Tooltip("Re-run algorithm with new random source node (Alt+R)"));
        _randomSource.setOnAction(t -> drawAlgorithm(true));

        _threshold.getEditor().setAlignment(Pos.CENTER_RIGHT);
        _threshold.getEditor().setText("0.33"); // correct formatting for initial value
        _threshold.setEditable(true);
        _threshold.setPrefWidth(70);
        DoubleStringConverter.createFor(_threshold);
        _threshold.setTooltip(new Tooltip("Set threshold for visibility algorithm (Alt+T)"));
        _threshold.getValueFactory().valueProperty().addListener(
                (ov, oldValue, newValue) -> drawAlgorithm(false));

        final Label thresholdLabel = new Label("Visibility _Threshold");
        thresholdLabel.setLabelFor(_threshold);
        thresholdLabel.setMnemonicParsing(true);
        
        final GridPane input = new GridPane();
        input.setPadding(new Insets(0, 0, 0, 4));
        input.setVgap(8);
        GridPane.setMargin(_vertexNeighbors, new Insets(0, 4, 0, 12));
        GridPane.setHalignment(_algorithmChoice, HPos.CENTER);
        GridPane.setMargin(thresholdLabel, new Insets(0, 4, 0, 0));
        GridPane.setHalignment(thresholdLabel, HPos.RIGHT);

        input.add(graphLabel, 0, 0);
        input.add(_graphChoice, 1, 0);
        input.add(_vertexNeighbors, 2, 0);
        input.add(_randomSource, 3, 0);

        input.add(algorithmLabel, 0, 1);
        input.add(_algorithmChoice, 1, 1);
        input.add(thresholdLabel, 2, 1);
        input.add(_threshold, 3, 1);

        final Button newGraph = new Button("_New");
        newGraph.setDefaultButton(true);
        newGraph.setOnAction(t -> drawGraph());
        newGraph.setTooltip(new Tooltip("Generate new random graph (Alt+N)"));

        final Button close = new Button("Close");
        close.setCancelButton(true);
        close.setOnAction(t -> close());
        close.setTooltip(new Tooltip("Close dialog (Escape, Alt+F4)"));

        final HBox controls = new HBox(newGraph, close);
        controls.setAlignment(Pos.CENTER);
        controls.setSpacing(8);

        // underlay hosts pathfinding decoration for main output pane
        final StackPane outputStack = new StackPane(_outputUnderlay, _output);
        VBox.setVgrow(outputStack, Priority.ALWAYS);
        
        final VBox root = new VBox(_message, input, outputStack, controls);
        root.setPadding(new Insets(8));
        root.setSpacing(8);
        VBox.setVgrow(_output, Priority.ALWAYS);
        
        setResizable(true);
        setScene(new Scene(root));
        setTitle("Graph Algorithm Tests");
        sizeToScene();

        setOnShown(t -> drawGraph());
    }

    private void drawGraph() {
        final Choice graphChoice = _graphChoice.getValue();
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

        _output.getChildren().clear();
        if (polygon != null) {
            _vertexNeighbors.setDisable(polygon.sides != 4);

            final PolygonGrid grid = new PolygonGrid(polygon);
            _border = PolygonGridDialog.sizeGrid(grid, _output);
            _graphManager = new GraphManager<>(grid, 4, _output);

            // draw edges from Subdivision to avoid overlapping polygon edges
            final PolygonGridMap map = new PolygonGridMap(grid, PointD.EMPTY, 0);
            drawEdges(map.source(), false);
        }
        else {
            _vertexNeighbors.setDisable(true);
            _border = PointD.EMPTY;

            final RectD output = new RectD(0, 0, _output.getWidth(), _output.getHeight());
            final RectD bounds = new RectD(8, 8, output.width() - 16, output.height() - 16);
            final PointD[] points = GeoUtils.randomPoints(40, bounds, new PointDComparatorX(0), 20);

            final VoronoiResults results = Voronoi.findAll(points, output);
            final Subdivision division = results.toDelaunaySubdivision(output, true);
            _graphManager = new GraphManager<>(division, 8, _output);

            // draw Voronoi edges with superimposed Delaunay edges
            drawEdges(new VoronoiMap(results).source(), false);
            drawEdges(division, true);
        }

        drawAlgorithm(true);
    }

    private void drawAlgorithm(boolean random) {
        final Choice algorithmChoice = _algorithmChoice.getValue();
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
                final double threshold = _threshold.getValue();
                success = _graphManager.runVisibility(random, threshold);
                break;
        }

        _message.setText("Source is blue circle, numbers indicate step costs, dashes are impassable.\n"
                + algorithmChoice.resultMessage(success));
        
        _randomSource.setDisable(algorithmChoice == Choice.A_STAR);
        _threshold.setDisable(algorithmChoice != Choice.VISIBILITY);
        
        _outputUnderlay.getChildren().clear();
        _graphManager.showNodes(_outputUnderlay, _border);
    }

    private void drawEdges(Subdivision division, boolean isDelaunay) {
        for (LineD edge: division.toLines()) {
            final Line line = new Line(
                    _border.x + edge.start.x, _border.y + edge.start.y,
                    _border.x + edge.end.x, _border.y + edge.end.y);

            if (isDelaunay) {
                line.setStroke(Color.GOLD);
                line.getStrokeDashArray().addAll(2.0, 4.0);

                // clear space around cost markers
                final double angle = edge.angle();
                final double clear = 11;
                line.setStartX(line.getStartX() + Math.cos(angle) * clear);
                line.setStartY(line.getStartY() + Math.sin(angle) * clear);
                line.setEndX(line.getEndX() - Math.cos(angle) * clear);
                line.setEndY(line.getEndY() - Math.sin(angle) * clear);
            } else
                line.setStroke(Color.BLACK);

            _output.getChildren().add(line);
        }
    }

    private static class GraphManager<T> implements GraphAgent<T> {

        private final static Color[] NODE_COLORS_4 = {
            Color.web("#edf8fb"), Color.web("#b2e2e2"), Color.web("#66c2a4"), Color.web("#238b45")
        };

        private final static Color[] NODE_COLORS_8 = {
            Color.web("#f7fcfd"), Color.web("#e5f5f9"), Color.web("#ccece6"), Color.web("#99d8c9"),
            Color.web("#66c2a4"), Color.web("#41ae76"), Color.web("#238b45"), Color.web("#005824")
        };
        
        private final Graph<T> _graph;
        private final int _maxCost;
        private final PointD _maxWorld;
        private final double _scaleCost;

        private final List<T> _highlights = new ArrayList<>(2);
        private List<T> _locations = new ArrayList<>();
        private final Map<T, Integer> _nodeCosts;
        private final Color[] _nodeColors;

        GraphManager(Graph<T> graph, int maxCost, Pane output) {
            _graph = graph;
            _maxCost = maxCost;
            _maxWorld = new PointD(output.getWidth(), output.getHeight());

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
                    for (int i = 0; i < maxCost; i++)
                        _nodeColors[i] = Color.gray(1 - i / (double) maxCost);
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

        void showNodes(Pane output, PointD offset) {
            for (T node: _graph.nodes()) {
                final int cost = _nodeCosts.get(node);

                final PointD[] region = _graph.getWorldRegion(node);
                if (region != null) {
                    final Polygon polygon = new Polygon(PointD.toDoubles(_graph.getWorldRegion(node)));
                    polygon.setFill(_nodeColors[cost - 1]);
                    polygon.setStroke(null);
                    polygon.setTranslateX(offset.x);
                    polygon.setTranslateY(offset.y);
                    output.getChildren().add(polygon);
                }
                
                final PointD center = _graph.getWorldLocation(node);
                final String costString = (cost < _maxCost ? Integer.toString(cost) : "—");
                final Color costColor = (cost <= _maxCost / 2 ? Color.BLACK : Color.WHITE);

                final Text costText = new Text(costString);
                costText.setX(center.x + offset.x - costText.getLayoutBounds().getWidth() / 2);
                costText.setY(center.y + offset.y + costText.getLayoutBounds().getHeight() / 4);
                costText.setFill(costColor);
                output.getChildren().add(costText);

                // indicate found nodes by red rectangles
                if (_locations.contains(node)) {
                    final Rectangle rect = new Rectangle(-7, -7, 14, 14);
                    rect.setFill(null);
                    rect.setStroke(Color.RED);
                    rect.setTranslateX(center.x + offset.x);
                    rect.setTranslateY(center.y + offset.y);
                    output.getChildren().add(rect);
                }

                // indicate highlights by blue circles
                if (_highlights.contains(node)) {
                    final Circle circle = new Circle(10);
                    circle.setFill(null);
                    circle.setStroke(Color.BLUE);
                    circle.setCenterX(center.x + offset.x);
                    circle.setCenterY(center.y + offset.y);
                    output.getChildren().add(circle);
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

        private static enum NodeLocation {
            TOP_LEFT,
            BOTTOM_RIGHT,
            RANDOM;
        }
    }
    
    private static enum Choice {

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
}
