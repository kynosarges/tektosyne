package org.kynosarges.tektosyne.demo;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.*;
import javafx.print.PrinterJob;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.stage.*;

import org.kynosarges.tektosyne.geometry.*;
import org.kynosarges.tektosyne.subdivision.PolygonGridMap;

/**
 * Provides a dialog for printing and saving {@link PolygonGrid} instances.
 * Allows the user to print or save a {@link PolygonGrid} of an arbitrary size,
 * based on a user-defined {@link RegularPolygon}.
 * 
 * @author Christoph Nahr
 * @version 6.1.0
 */
public class MakeGridDialog extends Stage {

    private final Canvas _output = new Canvas();

    private final Spinner<Integer> _elementSize = new Spinner<>(1, 999, 10, 1);
    private final Spinner<Integer> _columns = new Spinner<>(1, 999, 10, 1);
    private final Spinner<Integer> _rows = new Spinner<>(1, 999, 10, 1);

    private final RadioButton _elementSquare, _elementHexagon;
    private final RadioButton _elementOnEdge, _elementOnVertex;
    private final RadioButton _shiftNone, _shiftColumnUp,
            _shiftColumnDown, _shiftRowLeft, _shiftRowRight;

    private final Label _widthLabel = new Label("0");
    private final Label _heightLabel = new Label("0");

    // current element and grid
    private RegularPolygon _element;
    private PolygonGrid _grid;

    // ignore input while updating grid
    private boolean _updating;
    
    /**
     * Creates a {@link MakeGridDialog}.
     */    
    public MakeGridDialog() {
        initOwner(Global.primaryStage());
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);

        final Label message = new Label(
                "Specify element size (= polygon side length), number of columns & rows, and grid geometry.\n" +
                "Output size is total size of resulting grid. Element and output size are in units of 1/72 inch.");
        message.setMinHeight(36); // reserve space for two lines
        message.setWrapText(true);

        final GridPane sizePane = new GridPane();
        final TitledPane sizeTitle = new TitledPane("Grid Size", sizePane);
        sizeTitle.setCollapsible(false);

        final Label sizeLabel = initSpinner(_elementSize, "Ele_ment", "Set side length of grid elements (Alt+M)");
        final Label columnsLabel = initSpinner(_columns, "_Columns", "Set number of grid columns (Alt+C)");
        final Label rowsLabel = initSpinner(_rows, "Ro_ws", "Set number of grid rows (Alt+W)");

        sizePane.add(sizeLabel, 0, 0);
        sizePane.add(_elementSize, 1, 0);
        sizePane.add(columnsLabel, 0, 1);
        sizePane.add(_columns, 1, 1);
        sizePane.add(rowsLabel, 0, 2);
        sizePane.add(_rows, 1, 2);

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

        final ToggleGroup elementGroup = new ToggleGroup();
        final VBox elementPane = new VBox();
        final TitledPane elementTitle = new TitledPane("Element", elementPane);
        elementTitle.setCollapsible(false);

        _elementSquare = createRadioButton("_Square",
                "Square elements (Alt+S)", elementGroup, elementPane);
        _elementHexagon = createRadioButton("_Hexagon",
                "Hexagonal elements (Alt+H)", elementGroup, elementPane);
        _elementSquare.setSelected(true);

        final ToggleGroup orientationGroup = new ToggleGroup();
        final VBox orientationPane = new VBox();
        final TitledPane orientationTitle = new TitledPane("Orientation", orientationPane);
        orientationTitle.setCollapsible(false);

        _elementOnEdge = createRadioButton("On _Edge",
                "Elements on edge (Alt+E)", orientationGroup, orientationPane);
        _elementOnVertex = createRadioButton("On _Vertex",
                "Elements on vertex (Alt+V)", orientationGroup, orientationPane);
        _elementOnEdge.setSelected(true);
        
        final GridPane outputPane = new GridPane();
        final TitledPane outputTitle = new TitledPane("Output Size", outputPane);
        outputTitle.setCollapsible(false);

        _widthLabel.setAlignment(Pos.CENTER_RIGHT);
        _widthLabel.setPrefWidth(48);
        _widthLabel.setTooltip(new Tooltip("Current total width of grid"));
        outputPane.add(new Label("Width"), 0, 0);
        outputPane.add(_widthLabel, 1, 0);

        _heightLabel.setAlignment(Pos.CENTER_RIGHT);
        _heightLabel.setPrefWidth(48);
        _heightLabel.setTooltip(new Tooltip("Current total height of grid"));
        outputPane.add(new Label("Height"), 0, 1);
        outputPane.add(_heightLabel, 1, 1);
        
        // uniform spacing for all containers
        for (TitledPane pane: new TitledPane[] {
            sizeTitle, shiftTitle, elementTitle, orientationTitle, outputTitle }) {

            final Region region = (Region) pane.getContent();
            if (region instanceof VBox)
                ((VBox) region).setSpacing(4);
            else
                ((GridPane) region).setVgap(4);
        }

        final Button saveButton = new Button("S_ave Grid…");
        saveButton.setTooltip(new Tooltip("Save grid to PNG file (Alt+A)"));
        saveButton.setOnAction(t -> save());

        final Button printButton = new Button("_Print Grid…");
        printButton.setTooltip(new Tooltip("Select printer and print grid (Alt+P)"));
        printButton.setOnAction(t -> print());

        final Button close = new Button("Close");
        close.setCancelButton(true);
        close.setOnAction(t -> close());
        close.setTooltip(new Tooltip("Close dialog (Escape, Alt+F4)"));

        final HBox controls = new HBox(saveButton, printButton, close);
        controls.setAlignment(Pos.CENTER);
        controls.setSpacing(8);

        final ScrollPane outputView = new ScrollPane(_output);
        outputView.setPannable(true);
        outputView.setPrefSize(240, 200);

        final VBox gridBox = new VBox(sizeTitle, shiftTitle);
        final VBox elementBox = new VBox(elementTitle, orientationTitle, outputTitle);
        final HBox input = new HBox(gridBox, elementBox, outputView);
        input.setSpacing(8);

        final VBox root = new VBox(message, input, controls);
        root.setPadding(new Insets(8));
        root.setSpacing(8);

        setResizable(false);
        setScene(new Scene(root));
        setTitle("Save & Print Grid");
        sizeToScene();

        updateElement();
        _elementSize.getValueFactory().valueProperty().addListener((ov, oldValue, newValue) -> updateElement());
        elementGroup.selectedToggleProperty().addListener((ov, oldValue, newValue) -> {
            if (!_updating) updateElement();
        });
        orientationGroup.selectedToggleProperty().addListener((ov, oldValue, newValue) -> {
            if (!_updating) updateElement();
        });

        shiftGroup.selectedToggleProperty().addListener((ov, oldValue, newValue) -> updateGrid());
        _columns.getValueFactory().valueProperty().addListener((ov, oldValue, newValue) -> updateGrid());
        _rows.getValueFactory().valueProperty().addListener((ov, oldValue, newValue) -> updateGrid());
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

    private static Label initSpinner(Spinner<Integer> spinner, String text, String tip) {

        spinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
        spinner.setEditable(true);
        spinner.setPrefWidth(60);
        IntegerStringConverter.createFor(spinner);
        Global.addTooltip(spinner, tip);

        final Label label = new Label(text);
        label.setLabelFor(spinner);
        label.setMnemonicParsing(true);
        return label;
    }

    private void save() {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Save File");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.setInitialFileName("grid.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));

        final File file = chooser.showSaveDialog(this);
        if (file == null) return;

        // ensure that transparent background remains transparent
        final SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        final Image image = _output.snapshot(params, null);
        final BufferedImage img = SwingFXUtils.fromFXImage(image, null);

        try {
            ImageIO.write(img, "PNG", file);
        } catch (IOException e) {
            Global.showError(this, "An error occurred while saving the image file.", e);
        }
    }

    private void print() {
        final PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            Global.showError(this, "JavaFX found no supported printers.", "");
            return;
        }

        // fails if user cancels
        if (!job.showPrintDialog(this))
            return;

        /*
         * NOTE: printPage creates a flood of JavaFX assertion failures when run with -ea
         * ("*** unexpected PG access"). This is a known internal JavaFX bug that does not
         * affect successful completion of print output or overall application stability.
         */
        if (!job.printPage(_output) || job.getJobStatus() == PrinterJob.JobStatus.ERROR)
            Global.showError(this, "JavaFX reported a printing error.", "");
        else
            job.endJob();
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

    private void updateControls(int sides, PolygonOrientation orientation) {
        _updating = true;

        final boolean onEdge = (orientation == PolygonOrientation.ON_EDGE);
        if (sides == 4) {
            setGridShift(onEdge ? PolygonGridShift.NONE : PolygonGridShift.ROW_RIGHT);
            _shiftNone.setDisable(!onEdge);
            _shiftColumnUp.setDisable(onEdge);
            _shiftColumnDown.setDisable(onEdge);
        } else {
            setGridShift(onEdge ? PolygonGridShift.COLUMN_DOWN : PolygonGridShift.ROW_RIGHT);
            _shiftNone.setDisable(true);
            _shiftColumnUp.setDisable(!onEdge);
            _shiftColumnDown.setDisable(!onEdge);
        }

        _shiftRowLeft.setDisable(onEdge);
        _shiftRowRight.setDisable(onEdge);

        _updating = false;
    }

    private void updateElement() {

        final int sides = (_elementSquare.isSelected() ? 4 : 6);
        final PolygonOrientation orientation = (_elementOnEdge.isSelected() ?
            PolygonOrientation.ON_EDGE : PolygonOrientation.ON_VERTEX);
        _element = new RegularPolygon(_elementSize.getValue(), sides, orientation);

        updateControls(sides, orientation);
        updateGrid();
    }
    
    private void updateGrid() {
        if (_element == null) return;

        _grid = new PolygonGrid(_element, getGridShift());
        _grid.setSize(new SizeI(_columns.getValue(), _rows.getValue()));

        // show resulting grid size in pixels
        _widthLabel.setText(Long.toString(Math.round(_grid.worldBounds().width())));
        _heightLabel.setText(Long.toString(Math.round(_grid.worldBounds().height())));
        
        // increase dimensions by one for 0.5 pixel shifting
        _output.setWidth(_grid.worldBounds().width() + 1);
        _output.setHeight(_grid.worldBounds().height() + 1);

        // set background to transparent
        final GraphicsContext context = _output.getGraphicsContext2D();
        context.clearRect(0, 0, _output.getWidth(), _output.getHeight());
        context.setStroke(Color.BLACK);

        /*
         * Use Subdivision edges to avoid overlapping polygon edges.
         * Shift all coordinates by 0.5 to minimize blended pixels.
         */
        final PolygonGridMap map = new PolygonGridMap(_grid, PointD.EMPTY, 0);
        for (LineD edge: map.source().toLines())
            context.strokeLine(edge.start.x + 0.5, edge.start.y + 0.5, edge.end.x + 0.5, edge.end.y + 0.5);
    }
}
