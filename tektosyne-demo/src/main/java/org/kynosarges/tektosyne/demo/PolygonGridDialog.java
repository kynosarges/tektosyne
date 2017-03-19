package org.kynosarges.tektosyne.demo;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.*;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides a dialog for testing the {@link PolygonGrid} class.
 * Draws a resizable {@link PolygonGrid} based on a user-defined {@link RegularPolygon}.
 * The dialog tracks the current element and shows its coordinates.
 * <p>
 * Additionally, the user may change the {@link PolygonGridShift}, highlight all
 * neighbors within a given distance, and show all distances from the current element.</p>
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class PolygonGridDialog extends Stage {

    private final Pane _output = new Pane();
    private final Pane _outputUnderlay = new Pane();

    private final RadioButton _elementSquare, _elementHexagon;
    private final CheckBox _vertexNeighbors;
    private final RadioButton _elementOnEdge, _elementOnVertex;
    private final RadioButton _shiftNone, _shiftColumnUp,
            _shiftColumnDown, _shiftRowLeft, _shiftRowRight;

    private final Label _columnsLabel = new Label("0");
    private final Label _rowsLabel = new Label("0");
    private final Label _cursorLabel = new Label("—");

    // current element and grid
    private RegularPolygon _element;
    private PolygonGrid _grid;

    // auxiliary variables for drawing
    private PointD _border = PointD.EMPTY;
    private final Polygon _cursorShape = new Polygon();
    private double[] _insetDoubles;

    private final static Color[] NEIGHBOR_COLORS = {
        Color.GREEN,
        Color.YELLOWGREEN,
        Color.LIGHTGREEN,
    };

    // ignore input while updating grid
    private boolean _updating;
    
    /**
     * Creates a {@link PolygonGridDialog}.
     */    
    public PolygonGridDialog() {
        initOwner(Global.primaryStage());
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);

        final Label message = new Label(
                "Resize dialog to change grid size. Mouse over grid to highlight elements. Left-click to show\n" +
                "immediate neighbors. Middle-click to show third neighbors. Right-click to show distances.");
        message.setMinHeight(36); // reserve space for two lines
        message.setWrapText(true);

        Global.clipChildren(_output);
        _output.setPrefSize(400, 300);
        _output.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, new CornerRadii(4), BorderWidths.DEFAULT)));

        _output.setOnMouseClicked(this::onMouseClicked);
        _output.setOnMouseMoved(this::onMouseMoved);
        _output.setOnMouseExited(t -> clearCursor());

        Global.clipChildren(_outputUnderlay);
        _outputUnderlay.setPrefSize(400, 300);

        _cursorShape.setFill(Color.RED);
        _cursorShape.setStroke(null);
        _cursorShape.setVisible(false);

        final ToggleGroup elementGroup = new ToggleGroup();
        final VBox elementPane = new VBox();
        final TitledPane elementTitle = new TitledPane("Element", elementPane);
        elementTitle.setCollapsible(false);

        _elementSquare = createRadioButton("_Square",
                "Square elements (Alt+S)", elementGroup, elementPane);
        _elementHexagon = createRadioButton("_Hexagon",
                "Hexagonal elements (Alt+H)", elementGroup, elementPane);
        _elementSquare.setSelected(true);

        _vertexNeighbors = new CheckBox("V-_Neighbors");
        _vertexNeighbors.setTooltip(new Tooltip("Vertex neighbors (Alt+N)"));
        elementPane.getChildren().add(_vertexNeighbors);

        final ToggleGroup orientationGroup = new ToggleGroup();
        final VBox orientationPane = new VBox();
        final TitledPane orientationTitle = new TitledPane("Orientation", orientationPane);
        orientationTitle.setCollapsible(false);

        _elementOnEdge = createRadioButton("On _Edge",
                "Elements on edge (Alt+E)", orientationGroup, orientationPane);
        _elementOnVertex = createRadioButton("On _Vertex",
                "Elements on vertex (Alt+V)", orientationGroup, orientationPane);
        _elementOnEdge.setSelected(true);

        final ToggleGroup shiftGroup = new ToggleGroup();
        final VBox shiftPane = new VBox();
        final TitledPane shiftTitle = new TitledPane("Grid Shift", shiftPane);
        shiftTitle.setCollapsible(false);

        _shiftNone = createRadioButton("N_one", "No grid shift (Alt+O)", shiftGroup, shiftPane);
        _shiftColumnUp = createRadioButton("Column _Up", "Shift column up (Alt+U)", shiftGroup, shiftPane);
        _shiftColumnDown = createRadioButton("Column _Down", "Shift column down (Alt+D)", shiftGroup, shiftPane);
        _shiftRowLeft = createRadioButton("Row _Left", "Shift row left (Alt+L)", shiftGroup, shiftPane);
        _shiftRowRight = createRadioButton("Row _Right", "Shift row right (Alt+R)", shiftGroup, shiftPane);
        _shiftNone.setSelected(true);

        final GridPane sizePane = new GridPane();
        final TitledPane sizeTitle = new TitledPane("Grid Size", sizePane);
        sizeTitle.setCollapsible(false);

        _columnsLabel.setAlignment(Pos.CENTER_RIGHT);
        _columnsLabel.setPrefWidth(48);
        _columnsLabel.setTooltip(new Tooltip("Current number of grid columns"));
        sizePane.add(new Label("Columns"), 0, 0);
        sizePane.add(_columnsLabel, 1, 0);

        _rowsLabel.setAlignment(Pos.CENTER_RIGHT);
        _rowsLabel.setPrefWidth(48);
        _rowsLabel.setTooltip(new Tooltip("Current number of grid rows"));
        sizePane.add(new Label("Rows"), 0, 1);
        sizePane.add(_rowsLabel, 1, 1);

        _cursorLabel.setAlignment(Pos.CENTER_RIGHT);
        _cursorLabel.setPrefWidth(48);
        _cursorLabel.setTooltip(new Tooltip("Grid coordinates under mouse cursor"));
        sizePane.add(new Label("Cursor"), 0, 2);
        sizePane.add(_cursorLabel, 1, 2);

        final HBox input = new HBox(elementTitle, orientationTitle, shiftTitle, sizeTitle);
        for (Node node: input.getChildren()) {
            final Region region = (Region) ((TitledPane) node).getContent();

            // uniform sizing and spacing for all containers
            region.setPrefSize(120, 120);
            if (region instanceof VBox)
                ((VBox) region).setSpacing(4);
            else
                ((GridPane) region).setVgap(4);
        }

        final Button close = new Button("Close");
        close.setCancelButton(true);
        close.setOnAction(t -> close());
        close.setTooltip(new Tooltip("Close dialog (Escape, Alt+F4)"));

        final HBox controls = new HBox(close);
        controls.setAlignment(Pos.CENTER);
        controls.setSpacing(8);

        // underlay hosts mouse click decoration for main output pane
        final StackPane outputStack = new StackPane(_outputUnderlay, _output);
        VBox.setVgrow(outputStack, Priority.ALWAYS);
        
        final VBox root = new VBox(message, input, outputStack, controls);
        root.setPadding(new Insets(8));
        root.setSpacing(8);

        setResizable(true);
        setScene(new Scene(root));
        setTitle("Polygon Grid Test");
        sizeToScene();

        /*
         * Update element and element controls immediately without drawing.
         * Grid is calculated and drawn implicitly by first output sizing.
         */
        updateElement(false);

        elementGroup.selectedToggleProperty().addListener((ov, oldValue, newValue) -> {
            if (!_updating) updateElement(true);
        });
        _vertexNeighbors.selectedProperty().addListener((ov, oldValue, newValue) -> {
            if (!_updating) updateElement(true);
        });
        orientationGroup.selectedToggleProperty().addListener((ov, oldValue, newValue) -> {
            if (!_updating) updateElement(true);
        });
        shiftGroup.selectedToggleProperty().addListener((ov, oldValue, newValue) ->  {
            if (!_updating) draw();
        });

        _output.layoutBoundsProperty().addListener((ov, oldBounds, newBounds) -> draw());
    }

    /**
     * Resizes the specified {@link PolygonGrid} to the specified {@link Region}.
     * @param grid the {@link PolygonGrid} to resize
     * @param region the {@link Region} that should contain {@code grid}
     * @return a {@link PointD} containing the left and top offsets by which to
     *         shift {@code grid} so that it is centered within {@code region}
     * @throws NullPointerException if {@code grid} or {@code region} is {@code null}
     */
    static PointD sizeGrid(PolygonGrid grid, Region region) {
        /*
         * PolygonGrid default size (1,1) is too small for meaningful calculations.
         * We need multiple rows and columns to see the effect of PolygonGridShift.
         */
        grid.setSize(new SizeI(10, 10));

        // check for available space when using default size
        final double ratioX = region.getWidth() / grid.worldBounds().width();
        final double ratioY = region.getHeight() / grid.worldBounds().height();

        // increase grid size accordingly if possible
        grid.setSize(new SizeI(
                Math.max(1, (int) (grid.size().width * ratioX) - 1),
                Math.max(1, (int) (grid.size().height * ratioY) - 1)));

        // calculate left and top drawing offsets
        return new PointD(
                (region.getWidth() - grid.worldBounds().width()) / 2,
                (region.getHeight()- grid.worldBounds().height()) / 2);
    }

    private static RadioButton createRadioButton(
            String text, String tip, ToggleGroup actionGroup, Pane actionPane) {

        final RadioButton button = new RadioButton(text);
        button.setMnemonicParsing(true);
        button.setToggleGroup(actionGroup);
        button.setTooltip(new Tooltip(tip));

        actionPane.getChildren().add(button);
        return button;
    }
    
    private void draw() {
        if (_element == null) return;
        _grid = new PolygonGrid(_element, getGridShift());

        // resize grid and show actual grid size
        _border = sizeGrid(_grid, _output);
        _columnsLabel.setText(Integer.toString(_grid.size().width));
        _rowsLabel.setText(Integer.toString(_grid.size().height));

        _output.getChildren().clear();
        _outputUnderlay.getChildren().clear();
        final double[] standard = PointD.toDoubles(_element.vertices);

        for (int x = 0; x < _grid.size().width; x++)
            for (int y = 0; y < _grid.size().height; y++) {
                final PointD center = _grid.gridToWorld(x, y);
                final Polygon poly = new Polygon(standard);

                poly.setFill(null);
                poly.setStroke(Color.BLACK);
                poly.setTranslateX(_border.x + center.x);
                poly.setTranslateY(_border.y + center.y);

                _output.getChildren().add(poly);
            }

        _output.getChildren().add(_cursorShape);
    }

    private void drawInset(PointI location, Color color, int distance) {
        final Polygon shape = new Polygon(_insetDoubles);
        shape.setFill(color);
        shape.setStroke(null);

        final PointD center = _grid.gridToWorld(location);
        shape.setTranslateX(_border.x + center.x);
        shape.setTranslateY(_border.y + center.y);
        _outputUnderlay.getChildren().add(shape);

        if (distance >= 0) {
            final Text text = new Text(Integer.toString(distance));
            text.setX(_border.x + center.x - text.getLayoutBounds().getWidth() / 2);
            text.setY(_border.y + center.y + text.getLayoutBounds().getHeight() / 3);
            _outputUnderlay.getChildren().add(text);
        }
    }

    private void clearCursor() {
        _cursorLabel.setText("—");
        _cursorShape.setVisible(false);
    }

    private PointI cursorToGrid(MouseEvent event) {
        final PointD q = new PointD(event.getX(), event.getY());

        if (q.x < 0 || q.x >= _output.getWidth() ||
            q.y < 0 || q.y >= _output.getHeight())
            return PolygonGrid.INVALID_LOCATION;

        return _grid.worldToGrid(q.subtract(_border));
    }
    
    private PolygonGridShift getGridShift() {

        if (_shiftColumnUp.isSelected()) return PolygonGridShift.COLUMN_UP;
        if (_shiftColumnDown.isSelected()) return PolygonGridShift.COLUMN_DOWN;
        if (_shiftRowLeft.isSelected()) return PolygonGridShift.ROW_LEFT;
        if (_shiftRowRight.isSelected()) return PolygonGridShift.ROW_RIGHT;

        return PolygonGridShift.NONE;
    }
    
    private void setGridShift(PolygonGridShift gridShift) {
        if (gridShift == null)
            gridShift = PolygonGridShift.NONE;

        switch (gridShift) {
            case NONE:        _shiftNone.setSelected(true); break;
            case COLUMN_UP:   _shiftColumnUp.setSelected(true); break;
            case COLUMN_DOWN: _shiftColumnDown.setSelected(true); break;
            case ROW_LEFT:    _shiftRowLeft.setSelected(true); break;
            case ROW_RIGHT:   _shiftRowRight.setSelected(true); break;
        }
    }

    private void onMouseClicked(MouseEvent event) {
        if (_grid == null) return;
        _outputUnderlay.getChildren().clear();

        final PointI p = cursorToGrid(event);
        if (!_grid.isValid(p)) return;

        switch (event.getButton()) {
            case PRIMARY:
                // highlight immediate neighbors
                for (PointI neighbor: _grid.getNeighbors(p))
                    drawInset(neighbor, Color.YELLOW, -1);
                break;

            case MIDDLE:
                // highlight neighbors within three steps
                for (PointI neighbor: _grid.getNeighbors(p, 3))
                    drawInset(neighbor, Color.YELLOW, -1);
                break;
            
            case SECONDARY:
                // show color-coded step distances from cursor
                for (int x = 0; x < _grid.size().width; x++)
                    for (int y = 0; y < _grid.size().height; y++) {
                        final PointI target = new PointI(x, y);
                        if (!p.equals(target)) {
                            final int distance = _grid.getStepDistance(p, target);

                            // use different brushes to highlight distances
                            final int index = (distance - 1) % NEIGHBOR_COLORS.length;
                            drawInset(target, NEIGHBOR_COLORS[index], distance);
                        }
                    }
                break;
        }
    }

    private void onMouseMoved(MouseEvent event) {
        if (_grid == null) return;
        clearCursor();

        final PointI p = cursorToGrid(event);
        if (!_grid.isValid(p)) return;
        
        _cursorLabel.setText(String.format("%d/%d", p.x, p.y));
        final PointD center = _grid.gridToWorld(p);
        _cursorShape.setTranslateX(_border.x + center.x);
        _cursorShape.setTranslateY(_border.y + center.y);
        _cursorShape.setVisible(true);
    }

    private void updateControls(int sides, PolygonOrientation orientation) {
        _updating = true;

        final boolean onEdge = (orientation == PolygonOrientation.ON_EDGE);
        if (sides == 4) {
            setGridShift(onEdge ? PolygonGridShift.NONE : PolygonGridShift.ROW_RIGHT);
            _shiftNone.setDisable(!onEdge);
            _shiftColumnUp.setDisable(onEdge);
            _shiftColumnDown.setDisable(onEdge);
            _vertexNeighbors.setDisable(false);
        } else {
            setGridShift(onEdge ? PolygonGridShift.COLUMN_DOWN : PolygonGridShift.ROW_RIGHT);
            _shiftNone.setDisable(true);
            _shiftColumnUp.setDisable(!onEdge);
            _shiftColumnDown.setDisable(!onEdge);
            _vertexNeighbors.setDisable(true);
        }

        _shiftRowLeft.setDisable(onEdge);
        _shiftRowRight.setDisable(onEdge);

        _updating = false;
    }

    private void updateElement(boolean draw) {

        // determine side count and integral parameters
        final int sides = (_elementSquare.isSelected() ? 4 : 6);
        final PolygonOrientation orientation = (_elementOnEdge.isSelected() ?
            PolygonOrientation.ON_EDGE : PolygonOrientation.ON_VERTEX);
        final boolean vertexNeighbors = (sides <= 4 ? _vertexNeighbors.isSelected() : false);

        // adjust side length based on side count
        final double length = 160.0 / sides;
        _element = new RegularPolygon(length, sides, orientation, vertexNeighbors);
        final RegularPolygon inset = _element.inflate(-3.0);
        _insetDoubles = PointD.toDoubles(inset.vertices);

        // permanent inset polygon for cursor tracking
        _cursorShape.getPoints().clear();
        for (double coord: _insetDoubles)
            _cursorShape.getPoints().add(coord);

        updateControls(sides, orientation);
        if (draw) draw();
    }
}
