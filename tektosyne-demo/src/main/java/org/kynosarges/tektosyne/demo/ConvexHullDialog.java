package org.kynosarges.tektosyne.demo;

import java.util.*;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.stage.*;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides a dialog for testing the {@link GeoUtils#convexHull} algorithm.
 * Draws a random set of points and then superimposes the polygon
 * that constitutes its convex hull.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class ConvexHullDialog extends Stage {

    private final Pane _output = new Pane();
    private PointD[] _points;

    /**
     * Creates a {@link ConvexHullDialog}.
     */    
    public ConvexHullDialog() {
        initOwner(Global.primaryStage());
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);

        final Label message = new Label(
                "Convex hull vertices appear as filled circles, interior points appear hollow.");
        message.setWrapText(true);

        Global.clipChildren(_output);
        _output.setPrefSize(400, 300);
        _output.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, new CornerRadii(4), BorderWidths.DEFAULT)));

        final Button newTest = new Button("_New");
        newTest.setDefaultButton(true);
        newTest.setOnAction(t -> draw(null));
        newTest.setTooltip(new Tooltip("Generate new random point set (Alt+N)"));

        final Button copy = new Button("_Copy");
        copy.setOnAction(t -> Global.copy(this, PointD.class, _points));
        copy.setTooltip(new Tooltip("Copy current point set to clipboard (Alt+C)"));

        final Button paste = new Button("_Paste");
        paste.setOnAction(t -> {
            final PointD[] points = Global.paste(this, PointD.class);
            if (points != null) draw(points);
        });
        paste.setTooltip(new Tooltip("Paste existing point set from clipboard (Alt+P)"));

        final Button close = new Button("Close");
        close.setCancelButton(true);
        close.setOnAction(t -> close());
        close.setTooltip(new Tooltip("Close dialog (Escape, Alt+F4)"));

        final HBox controls = new HBox(newTest, copy, paste, close);
        controls.setAlignment(Pos.CENTER);
        controls.setSpacing(8);

        final VBox root = new VBox(message, _output, controls);
        root.setPadding(new Insets(8));
        root.setSpacing(8);
        VBox.setVgrow(_output, Priority.ALWAYS);
        
        setResizable(true);
        setScene(new Scene(root));
        setTitle("Convex Hull Test");
        sizeToScene();
        
        setOnShown(t -> draw(null));
    }

    /**
     * Draws a convex hull for the specified {@link PointD} array.
     * Creates a new {@link PointD} array if {@code points} is {@code null}.
     * 
     * @param points the {@link PointD} array whose convex hull to draw
     */
    private void draw(PointD[] points) {
        final double diameter = 8;

        // generate new random point set if desired
        if (points == null) {
            final double width = _output.getWidth() - 2 * diameter;
            final double height = _output.getHeight() - 2 * diameter;
            final RectD bounds = new RectD(0, 0, width, height).offset(diameter, diameter);

            final int count = 4 + Global.RANDOM.nextInt(37);
            points = GeoUtils.randomPoints(count, bounds, new PointDComparatorY(0), diameter);
        }

        _points = points;
        final PointD[] polygon = GeoUtils.convexHull(points);
        final List<PointD> polygonList = Arrays.asList(polygon);
        _output.getChildren().clear();

        // draw hull vertices filled, other points hollow
        for (PointD point: points) {
            final boolean isVertex = polygonList.contains(point);
            final Circle vertex = new Circle(point.x, point.y, diameter / 2);
            vertex.setFill(isVertex ? Color.BLACK : null);
            vertex.setStroke(Color.BLACK);
            _output.getChildren().add(vertex);
        }

        // draw edges of convex hull
        final Polygon hull = new Polygon(PointD.toDoubles(polygon));
        hull.setFill(null);
        hull.setStroke(Color.RED);
        _output.getChildren().add(hull);
    }
}
