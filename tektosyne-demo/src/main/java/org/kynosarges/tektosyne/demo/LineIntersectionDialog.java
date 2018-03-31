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
 * Provides a dialog for testing the brute force {@link MultiLineIntersection} algorithm.
 * Draws a random set of lines and marks any points of intersection that were found.
 * 
 * @author Christoph Nahr
 * @version 6.1.0
 */
public class LineIntersectionDialog extends Stage {

    private final Pane _output = new Pane();
    private final Label _linesCount = new Label("0/0");
    private final Spinner<Double> _tolerance = new Spinner<>(0, 10, 0, 0.1);

    private LineD[] _lines;
    private MultiLinePoint[] _crossings;

    /**
     * Creates a {@link LineIntersectionDialog}.
     */    
    public LineIntersectionDialog() {
        initOwner(Global.primaryStage());
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);

        final Label message = new Label(
                "Crossings (intersection points) are marked by hollow red circles.\n" +
                "Use Tolerance to adjust intersection proximity matching.");
        message.setMinHeight(36); // reserve space for two lines
        message.setWrapText(true);

        final Label linesLabel = new Label("Lines/Crossings");
        _linesCount.setAlignment(Pos.CENTER);
        _linesCount.setPrefWidth(40);

        _tolerance.getEditor().setAlignment(Pos.CENTER_RIGHT);
        _tolerance.getEditor().setText("0.00"); // correct formatting for initial value
        _tolerance.setEditable(true);
        _tolerance.setPrefWidth(70);
        DoubleStringConverter.createFor(_tolerance);
        Global.addTooltip(_tolerance, "Set tolerance for proximity matching (Alt+T)");
        _tolerance.getValueFactory().valueProperty().addListener((ov, oldValue, newValue) -> draw(_lines));

        final Label toleranceLabel = new Label("_Tolerance");
        toleranceLabel.setLabelFor(_tolerance);
        toleranceLabel.setMnemonicParsing(true);

        final Button maxTolerance = new Button("M_ax");
        maxTolerance.setOnAction(t -> _tolerance.getValueFactory().setValue(10.0));
        maxTolerance.setTooltip(new Tooltip("Set tolerance to maximum (Alt+A)"));

        final Button minTolerance = new Button("M_in");
        minTolerance.setOnAction(t -> _tolerance.getValueFactory().setValue(0.0));
        minTolerance.setTooltip(new Tooltip("Set tolerance to minimum (Alt+I)"));
        
        final Button splitLines = new Button("_Split");
        splitLines.setOnAction(t -> {
            final LineD[] lines = MultiLineIntersection.split(_lines, _crossings);
            draw(lines);
        });
        splitLines.setTooltip(new Tooltip("Split lines on intersection points (Alt+S)"));

        final HBox input = new HBox(linesLabel, _linesCount, splitLines,
                toleranceLabel, _tolerance, maxTolerance, minTolerance);
        input.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(splitLines, new Insets(0, 8, 0, 4));
        HBox.setMargin(maxTolerance, new Insets(0, 8, 0, 8));

        Global.clipChildren(_output);
        _output.setPrefSize(400, 300);
        _output.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, new CornerRadii(4), BorderWidths.DEFAULT)));

        final Button newTest = new Button("_New");
        newTest.setDefaultButton(true);
        newTest.setOnAction(t -> draw(null));
        newTest.setTooltip(new Tooltip("Generate new random line set (Alt+N)"));

        final Button copy = new Button("_Copy");
        copy.setOnAction(t -> Global.copy(this, LineD.class, _lines));
        copy.setTooltip(new Tooltip("Copy current line set to clipboard (Alt+C)"));

        final Button paste = new Button("_Paste");
        paste.setOnAction(t -> {
            final LineD[] lines = Global.paste(this, LineD.class);
            if (lines != null) draw(lines);
        });
        paste.setTooltip(new Tooltip("Paste existing line set from clipboard (Alt+P)"));

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
        setTitle("Line Intersection Test");
        sizeToScene();
        
        setOnShown(t -> draw(null));
    }

    /**
     * Draws all intersections for the specified {@link LineD} array.
     * Creates a new {@link LineD} array if {@code lines} is {@code null}.
     * 
     * @param lines the {@link LineD} array whose intersections to draw
     */
    private void draw(LineD[] lines) {
        final double diameter = 8;

        // generate new random line set if desired
        if (lines == null) {
            final int count = 3 + Global.RANDOM.nextInt(18);
            lines = new LineD[count];

            final double width = _output.getWidth() - 2 * diameter;
            final double height = _output.getHeight() - 2 * diameter;
            for (int i = 0; i < lines.length; i++)
                lines[i] = GeoUtils.randomLine(diameter, diameter, width, height);
        }

        _lines = lines;
        final double epsilon = _tolerance.getValue();
        _crossings = (epsilon > 0 ?
            MultiLineIntersection.findSimple(lines, epsilon) :
            MultiLineIntersection.findSimple(lines));

        _linesCount.setText(String.format("%d/%d", lines.length, _crossings.length));
        _output.getChildren().clear();

        // draw line set
        for (LineD line: lines) {
            final Line shape = new Line(line.start.x, line.start.y, line.end.x, line.end.y);
            shape.setStroke(Color.BLACK);
            _output.getChildren().add(shape);
        }

        // draw intersections as hollow circles
        for (MultiLinePoint crossing: _crossings) {
            final Circle circle = new Circle(crossing.shared.x, crossing.shared.y, diameter / 2);
            circle.setFill(null);
            circle.setStroke(Color.RED);
            _output.getChildren().add(circle);
        }
    }
}
