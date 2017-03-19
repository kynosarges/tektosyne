package org.kynosarges.tektosyne.demo;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.stage.*;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides a dialog for testing the {@link GeoUtils#pointInPolygon} algorithm.
 * Draws a random arbitrary polygon and displays the {@link PolygonLocation}
 * of the mouse cursor relative to that polygon.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class PointInPolygonDialog extends Stage {

    private final Pane _output = new Pane();
    private final Label _location = new Label("INSIDE");
    private final Spinner<Double> _tolerance = new Spinner<>(0, 10, 0, 0.1);

    private PointD[] _polygon;
    private PointD _cursor = PointD.EMPTY;

    /**
     * Creates a {@link PointInPolygonDialog}.
     */    
    public PointInPolygonDialog() {
        initOwner(Global.primaryStage());
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);

        final Label message = new Label(
                "Move mouse over polygon to display relative cursor location.\n" +
                "Use Tolerance to adjust edge and vertex proximity matching.");
        message.setMinHeight(36); // reserve space for two lines
        message.setWrapText(true);

        final Label locationLabel = new Label("Location: ");
        _location.setAlignment(Pos.CENTER_LEFT);
        _location.setPrefWidth(60);

        _tolerance.getEditor().setAlignment(Pos.CENTER_RIGHT);
        _tolerance.getEditor().setText("0.00"); // correct formatting for initial value
        _tolerance.setEditable(true);
        _tolerance.setPrefWidth(70);
        DoubleStringConverter.createFor(_tolerance);
        _tolerance.setTooltip(new Tooltip("Set tolerance for proximity matching (Alt+T)"));
        _tolerance.getValueFactory().valueProperty().addListener((ov, oldValue, newValue) -> showLocation());

        final Label toleranceLabel = new Label("_Tolerance");
        toleranceLabel.setLabelFor(_tolerance);
        toleranceLabel.setMnemonicParsing(true);

        final Button maxTolerance = new Button("M_ax");
        maxTolerance.setOnAction(t -> _tolerance.getValueFactory().setValue(10.0));
        maxTolerance.setTooltip(new Tooltip("Set tolerance to maximum (Alt+A)"));

        final Button minTolerance = new Button("M_in");
        minTolerance.setOnAction(t -> _tolerance.getValueFactory().setValue(0.0));
        minTolerance.setTooltip(new Tooltip("Set tolerance to minimum (Alt+I)"));
        
        final HBox input = new HBox(locationLabel, _location,
                toleranceLabel, _tolerance, maxTolerance, minTolerance);
        input.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(maxTolerance, new Insets(0, 8, 0, 8));

        Global.clipChildren(_output);
        _output.setPrefSize(400, 300);
        _output.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, new CornerRadii(4), BorderWidths.DEFAULT)));

        _output.setOnMouseMoved(t -> {
            _cursor = new PointD(t.getX(), t.getY());
            showLocation();
        });

        final Button newTest = new Button("_New");
        newTest.setDefaultButton(true);
        newTest.setOnAction(t -> draw(null));
        newTest.setTooltip(new Tooltip("Generate new random polygon (Alt+N)"));

        final Button copy = new Button("_Copy");
        copy.setOnAction(t -> Global.copy(this, PointD.class, _polygon));
        copy.setTooltip(new Tooltip("Copy current polygon to clipboard (Alt+C)"));

        final Button paste = new Button("_Paste");
        paste.setOnAction(t -> {
            final PointD[] points = Global.paste(this, PointD.class);
            if (points != null) draw(points);
        });
        paste.setTooltip(new Tooltip("Paste existing polygon from clipboard (Alt+P)"));

        final Button close = new Button("Close");
        close.setCancelButton(true);
        close.setOnAction(t -> close());
        close.setTooltip(new Tooltip("Close dialog (Escape, Alt+F4)"));

        final HBox controls = new HBox(newTest, copy, paste, close);
        controls.setAlignment(Pos.CENTER);
        controls.setSpacing(8);

        final VBox root = new VBox(message, new Separator(), input, _output, controls);
        root.setPadding(new Insets(8));
        root.setSpacing(8);
        VBox.setVgrow(_output, Priority.ALWAYS);
        
        setResizable(true);
        setScene(new Scene(root));
        setTitle("Point in Polygon Test");
        sizeToScene();
        
        setOnShown(t -> draw(null));
    }

    /**
     * Draws the polygon represented the specified {@link PointD} array.
     * Creates a new {@link PointD} array if {@code polygon} is {@code null}.
     * 
     * @param polygon the {@link PointD} array containing the polygon vertices
     */
    private void draw(PointD[] polygon) {

        // generate new random polygon if desired
        if (polygon == null)
            polygon = GeoUtils.randomPolygon(0, 0, _output.getWidth(), _output.getHeight());

        _polygon = polygon;
        _output.getChildren().clear();

        // draw polygon
        final Polygon shape = new Polygon(PointD.toDoubles(polygon));
        shape.setFill(Color.PALEGOLDENROD);
        shape.setStroke(Color.BLACK);
        _output.getChildren().add(shape);
    }

    /**
     * Shows the {@link PolygonLocation} of the mouse cursor relative to the current polygon.
     */
    private void showLocation() {
        final double tolerance = _tolerance.getValue();

        // determine relative location of mouse cursor
        final PolygonLocation location = (tolerance == 0 ?
            GeoUtils.pointInPolygon(_cursor, _polygon) :
            GeoUtils.pointInPolygon(_cursor, _polygon, tolerance));

        _location.setText(location.toString());
    }
}
