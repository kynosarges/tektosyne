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
 * Provides a dialog for testing the {@link Voronoi} algorithm.
 * Draws a random set of points and then superimposes its Voronoi
 * diagram and Delaunay triangulation.
 * 
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class VoronoiDialog extends Stage {

    private final Pane _output = new Pane();
    private PointD[] _points;

    /**
     * Creates a {@link VoronoiDialog}.
     */    
    public VoronoiDialog() {
        initOwner(Global.primaryStage());
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);

        final Label message = new Label(
                "Voronoi regions are shaded yellow, Voronoi edges appear as red solid lines.\n" +
                "Edges of the Delaunay triangulation appear as blue dashed lines.");
        message.setMinHeight(36); // reserve space for two lines
        message.setWrapText(true);

        // no border as Voronoi diagram has no fixed maximum size
        Global.clipChildren(_output);
        _output.setPrefSize(400, 300);

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
        setTitle("Voronoi & Delaunay Test");
        sizeToScene();
        
        setOnShown(t -> draw(null));
    }

    /**
     * Draws a Voronoi diagram for the specified {@link PointD} array.
     * Creates a new {@link PointD} array if {@code points} is {@code null}.
     * 
     * @param points the {@link PointD} array whose Voronoi diagram to draw
     */
    private void draw(PointD[] points) {
        final double diameter = 4;

        // generate new random point set if desired
        if (points == null) {
            final double width = _output.getWidth();
            final double height = _output.getHeight();
            final RectD bounds = new RectD(0.1 * width, 0.1 * height, 0.9 * width, 0.9 * height);

            final int count = 4 + Global.RANDOM.nextInt(17);
            points = GeoUtils.randomPoints(count, bounds, new PointDComparatorY(0), diameter);
        }

        _points = points;
        final RectD clip = new RectD(0, 0, _output.getWidth(), _output.getHeight());
        final VoronoiResults results = Voronoi.findAll(points, clip);
        _output.getChildren().clear();

        // draw interior of Voronoi regions
        for (PointD[] region: results.voronoiRegions()) {
            final Polygon polygon = new Polygon(PointD.toDoubles(region));
            polygon.setFill(Color.PALEGOLDENROD);
            polygon.setStroke(Color.WHITE);
            polygon.setStrokeWidth(6);
            _output.getChildren().add(polygon);
        }

        // draw edges of Voronoi diagram
        for (VoronoiEdge edge: results.voronoiEdges) {
            final PointD start = results.voronoiVertices[edge.vertex1];
            final PointD end = results.voronoiVertices[edge.vertex2];

            final Line line = new Line(start.x, start.y, end.x, end.y);
            line.setStroke(Color.RED);
            _output.getChildren().add(line);
        }

        // draw edges of Delaunay triangulation
        for (LineD edge: results.delaunayEdges()) {
            final Line line = new Line(edge.start.x, edge.start.y, edge.end.x, edge.end.y);
            line.getStrokeDashArray().addAll(3.0, 2.0);
            line.setStroke(Color.BLUE);
            _output.getChildren().add(line);
        }

        // draw generator points
        for (PointD point: points) {
            final Circle shape = new Circle(point.x, point.y, diameter / 2);
            shape.setFill(Color.BLACK);
            shape.setStroke(Color.BLACK);
            _output.getChildren().add(shape);
        }
    }
}
