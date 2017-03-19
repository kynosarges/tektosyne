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
 * Provides a dialog for testing the {@link RegularPolygon} class.
 * Draws a resizable polygon with user-defined side count and orientation.
 * The drawing includes the center of the polygon, the inscribed and circumscribed
 * circles in red, and the bounding rectangle in green.
 * <p>
 * Additionally, the user may adjust a delta value to draw another polygon
 * in blue that is identical to the first polygon except for an inflated
 * or deflated circumcircle radius.</p>
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class RegularPolygonDialog extends Stage {

    private final Pane _output = new Pane();

    private final Spinner<Integer> _sides = new Spinner<>(3, 12, 3, 1);
    private final Spinner<Integer> _delta = new Spinner<>(-100, 100, 0, 10);
    private final RadioButton _onEdge = new RadioButton("On _Edge");
    private final RadioButton _onVertex = new RadioButton("On _Vertex");

    /**
     * Creates a {@link RegularPolygonDialog}.
     */    
    public RegularPolygonDialog() {
        initOwner(Global.primaryStage());
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);

        final Label message = new Label(
                "Polygon and center point are black, optional delta-inflated clone is blue.\n" +
                "Inscribed and circumscribed circles are red, circumscribed rectangle is green.");
        message.setMinHeight(36); // reserve space for two lines
        message.setWrapText(true);

        _sides.getEditor().setAlignment(Pos.CENTER_RIGHT);
        _sides.setEditable(true);
        _sides.setPrefWidth(60);
        IntegerStringConverter.createFor(_sides);
        _sides.setTooltip(new Tooltip("Set number of sides for the polygon (Alt+S)"));
        _sides.getValueFactory().valueProperty().addListener((ov, oldValue, newValue) -> draw());

        final Label sidesLabel = new Label("_Sides ");
        sidesLabel.setLabelFor(_sides);
        sidesLabel.setMnemonicParsing(true);

        final HBox sidesBox = new HBox(sidesLabel, _sides);
        sidesBox.setAlignment(Pos.CENTER_LEFT);

        _delta.getEditor().setAlignment(Pos.CENTER_RIGHT);
        _delta.setEditable(true);
        _delta.setPrefWidth(80);
        IntegerStringConverter.createFor(_delta);
        _delta.setTooltip(new Tooltip("Set inflation or deflation for cloned polygon (Alt+D)"));
        _delta.getValueFactory().valueProperty().addListener((ov, oldValue, newValue) -> draw());

        final Label deltaLabel = new Label("_Delta ");
        deltaLabel.setLabelFor(_delta);
        deltaLabel.setMnemonicParsing(true);

        final HBox deltaBox = new HBox(deltaLabel, _delta);
        deltaBox.setAlignment(Pos.CENTER_LEFT);

        final ToggleGroup group = new ToggleGroup();
        _onEdge.selectedProperty().addListener((ov, oldValue, newValue) -> draw());
        _onEdge.setToggleGroup(group);
        _onEdge.setTooltip(new Tooltip("Stand polygon on an edge (Alt+E)"));

        _onVertex.selectedProperty().addListener((ov, oldValue, newValue) -> draw());
        _onVertex.setToggleGroup(group);
        _onVertex.setTooltip(new Tooltip("Stand polygon on a vertex (Alt+V)"));
        
        final VBox orientation = new VBox(_onEdge, _onVertex);
        orientation.setSpacing(4);

        final HBox input = new HBox(sidesBox, new Separator(Orientation.VERTICAL),
                orientation, new Separator(Orientation.VERTICAL), deltaBox);
        input.setAlignment(Pos.CENTER_LEFT);
        input.setSpacing(8);

        Global.clipChildren(_output);
        _output.setPrefSize(400, 300);
        _output.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, new CornerRadii(4), BorderWidths.DEFAULT)));

        final Button close = new Button("Close");
        close.setCancelButton(true);
        close.setOnAction(t -> close());
        close.setTooltip(new Tooltip("Close dialog (Escape, Alt+F4)"));

        final HBox controls = new HBox(close);
        controls.setAlignment(Pos.CENTER);
        controls.setSpacing(8);

        final VBox root = new VBox(message, new Separator(), input, _output, controls);
        root.setPadding(new Insets(8));
        root.setSpacing(8);
        VBox.setVgrow(_output, Priority.ALWAYS);
        
        setResizable(true);
        setScene(new Scene(root));
        setTitle("Regular Polygon Test");
        sizeToScene();

        setOnShown(t -> _onEdge.setSelected(true));
    }

    /**
     * Draws the {@link RegularPolygon} shapes specified in the dialog.
     */
    private void draw() {

        // determine side count and orientation
        final int sides = _sides.getValue();
        final PolygonOrientation orientation = (_onEdge.isSelected() ?
            PolygonOrientation.ON_EDGE : PolygonOrientation.ON_VERTEX);

        // compute side length based on layout size and side count
        final double layout = Math.min(_output.getWidth(), _output.getHeight());
        final double length = 2.5 * layout / sides;
        final RegularPolygon standard = new RegularPolygon(length, sides, orientation);

        // determine inflation relative to standard polygon
        final int delta = _delta.getValue();
        final RegularPolygon inflated = standard.inflate(delta);

        _output.getChildren().clear();

        final double centerX = _output.getWidth() / 2;
        final double centerY = _output.getHeight() / 2;

        // draw inscribed circle in red
        final Circle innerCircle = new Circle(centerX, centerY, standard.innerRadius);
        innerCircle.setFill(null);
        innerCircle.setStroke(Color.RED);
        _output.getChildren().add(innerCircle);

        // draw circumscribed circle in red
        final Circle outerCircle = new Circle(centerX, centerY, standard.outerRadius);
        outerCircle.setFill(null);
        outerCircle.setStroke(Color.RED);
        _output.getChildren().add(outerCircle);

        // draw circumscribed rectangle in green
        final Rectangle outerRect = new Rectangle(
                standard.bounds.min.x, standard.bounds.min.y,
                standard.bounds.width(), standard.bounds.height());
        outerRect.setTranslateX(centerX);
        outerRect.setTranslateY(centerY);
        outerRect.setFill(null);
        outerRect.setStroke(Color.GREEN);
        _output.getChildren().add(outerRect);

        // draw basic polygon in black
        final Polygon standardShape = new Polygon(PointD.toDoubles(standard.vertices));
        standardShape.setTranslateX(centerX);
        standardShape.setTranslateY(centerY);
        standardShape.setFill(null);
        standardShape.setStroke(Color.BLACK);
        _output.getChildren().add(standardShape);

        // draw center point of polygon
        final Circle centerShape = new Circle(centerX, centerY, 2);
        centerShape.setFill(Color.BLACK);
        centerShape.setStroke(Color.BLACK);
        _output.getChildren().add(centerShape);
        
        // draw inflated polygon in blue, if set
        if (delta != 0) {
            final Polygon inflatedShape = new Polygon(PointD.toDoubles(inflated.vertices));
            inflatedShape.setTranslateX(centerX);
            inflatedShape.setTranslateY(centerY);
            inflatedShape.setFill(null);
            inflatedShape.setStroke(Color.BLUE);
            _output.getChildren().add(inflatedShape);
        }
    }
}
