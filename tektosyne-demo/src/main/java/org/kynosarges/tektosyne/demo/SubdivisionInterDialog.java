package org.kynosarges.tektosyne.demo;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.stage.*;

import org.kynosarges.tektosyne.geometry.*;
import org.kynosarges.tektosyne.subdivision.*;

/**
 * Provides a dialog for testing the {@link Subdivision#intersection}
 * algorithm of the {@link Subdivision} class.
 * Intersects an existing planar subdivision with a user-defined diamond or rectangle.
 * All half-edges and faces are labeled with their keys.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class SubdivisionInterDialog extends Stage {

    private final Pane _output = new Pane();

    private final Spinner<Integer> _left = new Spinner<>(0, 300, 0, 10);
    private final Spinner<Integer> _top = new Spinner<>(0, 300, 0, 10);
    private final Spinner<Integer> _width = new Spinner<>(10, 400, 100, 10);
    private final Spinner<Integer> _height = new Spinner<>(10, 400, 100, 10);
    
    private final Label _currentFaceLabel = new Label("-1");
    private final Label _previousFaceLabel = new Label("-1");
    private final Label _intersectFaceLabel = new Label("-1");

    // current subdivision & intersection results
    private Subdivision _division;
    private SubdivisionIntersection _intersection;

    /**
     * Creates a {@link SubdivisionInterDialog}.
     */    
    public SubdivisionInterDialog() {
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

        _output.setOnMouseMoved(this::onMouseMoved);
        _output.setOnMouseExited(t -> showFace(null));

        final Button rectangle = new Button("_Rectangle");
        rectangle.setOnAction(t -> onIntersect(false));
        rectangle.setTooltip(new Tooltip("Intersect subdivision with specified rectangle (Alt+R)"));

        final Button diamond = new Button("_Diamond");
        diamond.setOnAction(t -> onIntersect(true));
        diamond.setTooltip(new Tooltip(
                "Intersect subdivision with diamond inscribed in specified rectangle (Alt+D)"));

        final Label leftLabel = initSpinner(_left, "_Left", "Set left border of rectangle (Alt+L)");
        final Label topLabel = initSpinner(_top, "  _Top", "Set top border of rectangle (Alt+T)");
        final Label widthLabel = initSpinner(_width, "  _Width", "Set width of rectangle (Alt+W)");
        final Label heightLabel = initSpinner(_height, "  _Height", "Set height of rectangle (Alt+H)");

        final HBox buttonBox = new HBox(new Label("Intersect with specified"), rectangle, diamond);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setSpacing(8);

        final HBox sizeBox = new HBox(leftLabel, _left, topLabel, _top, widthLabel, _width, heightLabel, _height);
        sizeBox.setAlignment(Pos.CENTER_LEFT);

        final VBox actions = new VBox(buttonBox, sizeBox);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setSpacing(8);

        _currentFaceLabel.setAlignment(Pos.CENTER_RIGHT);
        _currentFaceLabel.setPrefWidth(16);
        _currentFaceLabel.setTooltip(new Tooltip("Key of face containing mouse cursor"));

        _previousFaceLabel.setAlignment(Pos.CENTER_RIGHT);
        _previousFaceLabel.setPrefWidth(16);
        _previousFaceLabel.setTooltip(new Tooltip("Key of face in previous subdivision overlapping current face"));

        _intersectFaceLabel.setAlignment(Pos.CENTER_RIGHT);
        _intersectFaceLabel.setPrefWidth(16);
        _intersectFaceLabel.setTooltip(new Tooltip("Key of face in intersecting subdivision overlapping current face"));

        final HBox info = new HBox(
                new Label("Current Face"), _currentFaceLabel,
                new Label("  Previous Face"), _previousFaceLabel,
                new Label("  Intersecting Face"), _intersectFaceLabel);
        info.setAlignment(Pos.CENTER_LEFT);

        final Button newTest = new Button("_New");
        newTest.setDefaultButton(true);
        newTest.setOnAction(t -> draw(null));
        newTest.setTooltip(new Tooltip("Create new empty subdivision (Alt+N)"));

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
        setTitle("Subdivision Intersection Test");
        sizeToScene();

        setOnShown(t -> draw(null));
    }

    /**
     * Draws the specified {@link Subdivision}.
     * Creates a new empty {@link Subdivision} if {@code division} is {@code null}.
     * 
     * @param division the {@link Subdivision} to draw
     */
    private void draw(Subdivision division) {

        // default to empty subdivision
        if (division == null) {
            division = new Subdivision(0);
            _intersection = null;
        }

        _division = division;
        _division.validate();

        SubdivisionDialog.drawSubdivision(_output, division);
    }

    private static Label initSpinner(Spinner<Integer> spinner, String label, String tip) {
        spinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
        spinner.setEditable(true);
        spinner.setPrefWidth(60);
        IntegerStringConverter.createFor(spinner);
        spinner.setTooltip(new Tooltip(tip));

        final Label spinnerLabel = new Label(label);
        spinnerLabel.setLabelFor(spinner);
        spinnerLabel.setMnemonicParsing(true);
        return spinnerLabel;
    }

    private void onIntersect(boolean isDiamond) {
        final double margin = 2 * Global.fontSize();

        final double x = margin + _left.getValue();
        final double y = margin + _top.getValue();
        final double dx = _width.getValue();
        final double dy = _height.getValue();

        Subdivision rectangle;
        if (!isDiamond)
            rectangle = Subdivision.fromLines(new LineD[] {
                new LineD(x, y, x + dx, y),
                new LineD(x + dx, y, x + dx, y + dy),
                new LineD(x + dx, y + dy, x, y + dy),
                new LineD(x, y + dy, x, y)
            }, 0);
        else
            rectangle = Subdivision.fromLines(new LineD[] {
                new LineD(x + dx/2, y, x + dx, y + dy/2),
                new LineD(x + dx, y + dy/2, x + dx/2, y + dy),
                new LineD(x + dx/2, y + dy, x, y + dy/2),
                new LineD(x, y + dy/2, x + dx/2, y)
            }, 0);

        rectangle.validate();
        _intersection = Subdivision.intersection(_division, rectangle);
        draw(_intersection.division);
    }
    
    private void onMouseMoved(MouseEvent event) {
        if (_division == null) return;
        SubdivisionFace face = null;

        // check if mouse cursor is over subdivision
        final PointD q = new PointD(event.getX(), event.getY());
        if (q.x >= 0 && q.x < _output.getWidth() &&
            q.y >= 0 && q.y < _output.getHeight())
            face = _division.findFace(q);

        showFace(face);
    }

    private void showFace(SubdivisionFace face) {
        if (face == null) {
            _currentFaceLabel.setText("-1");
            _previousFaceLabel.setText("-1");
            _intersectFaceLabel.setText("-1");
        } else {
            _currentFaceLabel.setText(Integer.toString(face.key()));
            if (_intersection != null) {
                _previousFaceLabel.setText(Integer.toString(_intersection.faceKeys1[face.key()]));
                _intersectFaceLabel.setText(Integer.toString(_intersection.faceKeys2[face.key()]));
            }
        }
    }
}
