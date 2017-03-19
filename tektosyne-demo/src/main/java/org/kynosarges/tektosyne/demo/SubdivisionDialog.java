package org.kynosarges.tektosyne.demo;

import java.text.DecimalFormat;
import java.util.*;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.*;

import org.kynosarges.tektosyne.MathUtils;
import org.kynosarges.tektosyne.geometry.*;
import org.kynosarges.tektosyne.subdivision.*;

/**
 * Provides a dialog for testing the {@link Subdivision} class.
 * Shows a planar subdivision created from a random Voronoi diagram.
 * All half-edges and faces are labeled with their keys.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class SubdivisionDialog extends Stage {

    private final Pane _output = new Pane();
    private final DecimalFormat _doubleFormat = new DecimalFormat("0.00");
    private final Circle _vertexCircle;

    private final RadioButton _addEdge, _removeEdge, _splitEdge;
    private final RadioButton _connectVertex, _moveVertex, _removeVertex;

    private final Label _nearestEdgeLabel = new Label("-1");
    private final Label _nearestFaceLabel = new Label("-1");
    private final Label _nearestDistanceLabel = new Label("0");

    // current subdivision & selections
    private Subdivision _division;
    private int _nearestEdge = -1, _selectedEdge = -1;
    private PointD _selectedVertex = null;

    // mapping of half-edge keys to paths
    private Map<Integer, Path> _edges = new HashMap<>();

    /**
     * Creates a {@link SubdivisionDialog}.
     */    
    public SubdivisionDialog() {
        initOwner(Global.primaryStage());
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);

        final Label message = new Label(
                "Half-edge keys are normal weight, face keys are bold, hooks show orientation.\n" +
                "Half-edge cycles run clockwise because y-coordinates increase downward.");
        message.setMinHeight(36); // reserve space for two lines
        message.setWrapText(true);

        Global.clipChildren(_output);
        _output.setPrefSize(400, 300);
        _output.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, new CornerRadii(4), BorderWidths.DEFAULT)));

        _output.setOnMouseClicked(this::onMouseClicked);
        _output.setOnMouseMoved(this::onMouseMoved);
        _output.setOnMouseExited(t -> clearInfo());

        _vertexCircle = new Circle(6);
        _vertexCircle.setFill(null);
        _vertexCircle.setStroke(Color.BLUE);
        _vertexCircle.setVisible(false);

        final Label actionLabel = new Label("Click Action ");
        final ToggleGroup actionGroup = new ToggleGroup();

        final TilePane actionPane = new TilePane(Orientation.HORIZONTAL);
        actionPane.setPrefColumns(3);
        actionPane.setVgap(4);
        actionPane.setTileAlignment(Pos.CENTER_LEFT);

        final HBox actions = new HBox(actionLabel, actionPane);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setSpacing(8);

        _addEdge = createActionButton("_Add Edge",
                "Add edge from highlighted vertex to cursor (Alt+A)", actionGroup, actionPane);
        _removeEdge = createActionButton("_Remove Edge",
                "Remove highlighted edge (Alt+R)", actionGroup, actionPane);
        _splitEdge = createActionButton("_Split Edge",
                "Split highlighted edge in half (Alt+S)", actionGroup, actionPane);
        _connectVertex = createActionButton("_Connect Vertex",
                "Connect highlighted vertex with random vertex, if possible (Alt+C)", actionGroup, actionPane);
        _moveVertex = createActionButton("_Move Vertex",
                "Move highlighted vertex to cursor (Alt+M)", actionGroup, actionPane);
        _removeVertex = createActionButton("_Remove Vertex",
                "Remove higlighted vertex, joining two edges (Alt+R)", actionGroup, actionPane);

        final Button renumberEdges = new Button("Renumber _Edges");
        renumberEdges.setOnAction(t -> {
            if (_division.renumberEdges())
                draw(_division);
        });
        renumberEdges.setTooltip(new Tooltip("Remove gaps from sequence of half-edge keys (Alt+E)"));

        final Button renumberFaces = new Button("Renumber _Faces");
        renumberFaces.setOnAction(t -> {
            if (_division.renumberFaces())
                draw(_division);
        });
        renumberFaces.setTooltip(new Tooltip("Remove gaps from sequence of face keys (Alt+F)"));

        _nearestFaceLabel.setAlignment(Pos.CENTER_RIGHT);
        _nearestFaceLabel.setPrefWidth(16);
        _nearestFaceLabel.setTooltip(new Tooltip("Key of face containing mouse cursor"));

        _nearestEdgeLabel.setAlignment(Pos.CENTER_RIGHT);
        _nearestEdgeLabel.setPrefWidth(16);
        _nearestEdgeLabel.setTooltip(new Tooltip("Key of half-edge nearest to mouse cursor"));

        _nearestDistanceLabel.setAlignment(Pos.CENTER_RIGHT);
        _nearestDistanceLabel.setPrefWidth(36);
        _nearestDistanceLabel.setTooltip(new Tooltip("Distance from nearest half-edge to mouse cursor"));

        final HBox info = new HBox(renumberEdges, renumberFaces,
                new Label("Face"), _nearestFaceLabel, new Label(" Edge"), _nearestEdgeLabel,
                new Label(" Distance"), _nearestDistanceLabel);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(renumberFaces, new Insets(0, 8, 0, 8));

        final Button newTest = new Button("_New");
        newTest.setDefaultButton(true);
        newTest.setOnAction(t -> draw(null));
        newTest.setTooltip(new Tooltip("Generate new random subdivision (Alt+N)"));

        final Button copy = new Button("_Copy");
        copy.setOnAction(t -> Global.copy(this, LineD.class, _division.toLines()));
        copy.setTooltip(new Tooltip("Copy current subdivision to clipboard (Alt+C)"));

        final Button paste = new Button("_Paste");
        paste.setOnAction(t -> {
            final LineD[] lines = Global.paste(this, LineD.class);
            if (lines != null) draw(Subdivision.fromLines(lines, 0));
        });
        paste.setTooltip(new Tooltip("Paste existing subdivision from clipboard (Alt+P)"));

        final Button close = new Button("Close");
        close.setCancelButton(true);
        close.setOnAction(t -> close());
        close.setTooltip(new Tooltip("Close dialog (Escape, Alt+F4)"));

        final HBox controls = new HBox(newTest, copy, paste, close);
        controls.setAlignment(Pos.CENTER);
        controls.setSpacing(8);

        final VBox root = new VBox(message, new Separator(), actions, new Separator(), info, _output, controls);
        root.setPadding(new Insets(8));
        root.setSpacing(8);
        VBox.setVgrow(_output, Priority.ALWAYS);

        setResizable(true);
        setScene(new Scene(root));
        setTitle("Planar Subdivision Test");
        sizeToScene();

        setOnShown(t -> {
            draw(null);
            _addEdge.setSelected(true);
        });
    }

    /**
     * Draws the specified {@link Subdivision}.
     * Creates a new {@link Subdivision} if {@code division} is {@code null}.
     * 
     * @param division the {@link Subdivision} to draw
     */
    private void draw(Subdivision division) {

        // generate new random subdivision if desired
        if (division == null) {
            final PointD offset = new PointD(_output.getWidth() * 0.2, _output.getHeight() * 0.2);
            final SizeD scale = new SizeD(_output.getWidth() * 0.6, _output.getHeight() * 0.6);

            final int count = 4 + Global.RANDOM.nextInt(9);
            final PointD[] points = new PointD[count];

            for (int i = 0; i < points.length; i++)
                points[i] = new PointD(
                    offset.x + Global.RANDOM.nextDouble() * scale.width,
                    offset.y + Global.RANDOM.nextDouble() * scale.height);

            // outer bounds for Voronoi pseudo-vertices
            final double margin = 3 * Global.fontSize();
            final RectD bounds = new RectD(margin, margin,
                    _output.getWidth() - margin, _output.getHeight() - margin);

            final VoronoiResults results = Voronoi.findAll(points, bounds);
            final VoronoiMap map = new VoronoiMap(results);
            division = map.source();
        }

        _division = division;
        _division.validate();
        _nearestEdge = -1;

        _edges = drawSubdivision(_output, division);
        _output.getChildren().add(_vertexCircle);
    }

    /**
     * Draws the specified {@link Subdivision} to the specified {@link Pane}.
     * @param pane the {@link Pane} to draw on
     * @param division the {@link Subdivision} to draw
     * @return a {@link Map} that associates the keys of all half-edges in {@code division}
     *         with the corresponding {@link Path} nodes in {@code pane}
     * @throws NullPointerException if {@code pane} or {@code division} is {@code null}
     */
    static Map<Integer, Path> drawSubdivision(Pane pane, Subdivision division) {
        final double fs = Global.fontSize();
        pane.getChildren().clear();
        final Map<Integer, Path> edges = new HashMap<>(division.edges().size());

        // draw vertices
        final double radius = 2;
        for (PointD vertex: division.vertices().keySet()) {
            final Circle circle = new Circle(vertex.x, vertex.y, radius);
            circle.setFill(Color.BLACK);
            circle.setStroke(Color.BLACK);
            pane.getChildren().add(circle);
        }

        // draw half-edges with keys
        for (Map.Entry<Integer, SubdivisionEdge> entry: division.edges().entrySet()) {
            final LineD line = entry.getValue().toLine();

            final double deltaX = line.end.x - line.start.x;
            final double deltaY = line.end.y - line.start.y;
            final double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            // slightly offset half-edge toward incident face
            final double offsetX = radius * deltaX / length;
            final double offsetY = radius * deltaY / length;

            final double x0 = line.start.x + offsetX - offsetY;
            final double y0 = line.start.y + offsetX + offsetY;
            final double x1 = line.end.x - offsetX - offsetY;
            final double y1 = line.end.y + offsetX - offsetY;

            // draw hook in direction of half-edge
            final double angle = line.angle() + 0.75 * Math.PI;
            final double hookX0 = x1 - deltaX / 4;
            final double hookY0 = y1 - deltaY / 4;
            final double hookX1 = hookX0 + Math.cos(angle) * 8;
            final double hookY1 = hookY0 + Math.sin(angle) * 8;

            final Path path = new Path(
                    new MoveTo(x0, y0), new LineTo(x1, y1),
                    new MoveTo(hookX0, hookY0), new LineTo(hookX1, hookY1));

            path.setStroke(Color.RED);
            pane.getChildren().add(path);
            edges.put(entry.getKey(), path);

            // draw key of current half-edge
            final double centerX = (line.start.x + line.end.x) / 2;
            final double fontSizeX = (entry.getKey() < 10 && deltaY > 0) ? fs / 2 : fs;
            final double textX = centerX - fontSizeX * deltaY / length - fs / 2;

            final double centerY = (line.start.y + line.end.y) / 2;
            final double textY = centerY + fs * deltaX / length + fs / 2.2;

            final Text text = new Text(textX, textY, entry.getKey().toString());
            pane.getChildren().add(text);
        }

        // draw keys of bounded faces
        for (Map.Entry<Integer, SubdivisionFace> entry: division.faces().entrySet()) {
            if (entry.getKey() == 0) continue;

            final PointD centroid = entry.getValue().outerEdge().cycleCentroid();
            final double x = centroid.x - fs / 2;
            final double y = centroid.y + fs / 2.2;

            final Text text = new Text(x, y, entry.getKey().toString());
            text.setFont(Global.boldFont(text.getFont().getSize()));
            pane.getChildren().add(text);
        }

        return edges;
    }

    private void clearInfo() {
        if (_nearestEdge >= 0) setNearestEdge(null, 0);
        if (_selectedEdge >= 0) setSelectedEdge(-1);
        if (_selectedVertex != null) setSelectedVertex(null);
    }

    private static RadioButton createActionButton(
            String text, String tip, ToggleGroup actionGroup, TilePane actionPane) {

        final RadioButton button = new RadioButton(text);
        button.setMnemonicParsing(true);
        button.setToggleGroup(actionGroup);
        button.setTooltip(new Tooltip(tip));

        actionPane.getChildren().add(button);
        return button;
    }

    private void onMouseClicked(MouseEvent event) {
        if (_division == null) return;

        // check if mouse cursor is over subdivision
        final PointD q = new PointD(event.getX(), event.getY());
        if (q.x < 0 || q.x >= _output.getWidth() ||
            q.y < 0 || q.y >= _output.getHeight())
            return;

        if (_addEdge.isSelected()) {
            if (_selectedVertex != null)
                _division.addEdge(_selectedVertex, q);
        }
        else if (_removeEdge.isSelected()) {
            if (_selectedEdge >= 0)
                _division.removeEdge(_selectedEdge);
        }
        else if (_splitEdge.isSelected()) {
            if (_selectedEdge >= 0)
                _division.splitEdge(_selectedEdge);
        }
        else if (_connectVertex.isSelected()) {
            if (_selectedVertex != null) {
                final PointD target = MathUtils.getAny(_division.vertices().keySet());
                _division.addEdge(_selectedVertex, target);
            }
        }
        else if (_moveVertex.isSelected()) {
            if (_selectedVertex != null)
                _division.moveVertex(_selectedVertex, q);
        }
        else if (_removeVertex.isSelected()) {
            if (_selectedVertex != null)
                _division.removeVertex(_selectedVertex);
        }
        else return;

        draw(_division);
        setNearestEdge(null, 0);
        setSelectedEdge(-1);
        setSelectedVertex(null);
    }
    
    private void onMouseMoved(MouseEvent event) {
        if (_division == null) return;
        clearInfo();

        // check if mouse cursor is over subdivision
        final PointD q = new PointD(event.getX(), event.getY());
        if (q.x < 0 || q.x >= _output.getWidth() ||
            q.y < 0 || q.y >= _output.getHeight())
            return;

        // show nearest half-edge, if any
        final FindEdgeResult result = _division.findNearestEdge(q);
        if (result.edge != null) {
            setNearestEdge(result.edge, result.distance);
            if (result.distance <= 30) setSelectedEdge(result.edge.key());
        }

        // show nearest vertex, if any
        final NavigableSet<PointD> vertices = _division.vertices().navigableKeySet();
        if (!vertices.isEmpty()) {
            final PointD vertex = ((PointDComparator) vertices.comparator()).findNearest(vertices, q);
            setSelectedVertex(vertex);
        }
    }

    private void setNearestEdge(SubdivisionEdge edge, double distance) {
        _nearestDistanceLabel.setText(_doubleFormat.format(distance));

        if (edge == null) {
            _nearestEdge = -1;
            _nearestEdgeLabel.setText("-1");
            _nearestFaceLabel.setText("-1");
        } else {
            _nearestEdge = edge.key();
            _nearestEdgeLabel.setText(Integer.toString(edge.key()));
            _nearestFaceLabel.setText(Integer.toString(edge.face().key()));
        }
    }

    private void setSelectedEdge(int edge) {
        Path path;
        if (_selectedEdge >= 0 && (path = _edges.get(_selectedEdge)) != null) {
            path.setStroke(Color.RED);
            path.setStrokeWidth(1);
        }

        _selectedEdge = edge;
        if (_selectedEdge >= 0 && (path = _edges.get(_selectedEdge)) != null) {
            path.setStroke(Color.BLACK);
            path.setStrokeWidth(2);
        }
    }

    private void setSelectedVertex(PointD vertex) {
        _selectedVertex = vertex;

        if (_selectedVertex != null) {
            _vertexCircle.setCenterX(vertex.x);
            _vertexCircle.setCenterY(vertex.y);
            _vertexCircle.setVisible(true);
        } else
            _vertexCircle.setVisible(false);
    }
}
