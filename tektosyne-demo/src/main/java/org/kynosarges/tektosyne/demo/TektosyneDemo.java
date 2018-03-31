package org.kynosarges.tektosyne.demo;

import javafx.application.Application;
import javafx.event.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Defines the Tektosyne Demo application for JavaFX.
 * @author Christoph Nahr
 * @version 6.1.0
 */
public class TektosyneDemo extends Application {
    /**
     * Starts the {@link TektosyneDemo} application.
     * @param primaryStage the primary {@link Stage} for the application
     */
    @Override
    public void start(Stage primaryStage) {
        Global.setPrimaryStage(primaryStage);

        final Label caption = new Label("Tektosyne Demo Application");
        caption.setFont(Global.boldFont(16));
        caption.setPadding(new Insets(8));
        final Label message = new Label("Select a menu item to demonstrate Tektosyne features.");
        message.setPadding(new Insets(8));

        final VBox root = new VBox(createMenuBar(), caption, message);
        final Scene scene = new Scene(root, 400, 300);

        // background thread executor must be shut down manually
        primaryStage.setOnCloseRequest(t -> BenchmarkDialog.EXECUTOR.shutdownNow());

        primaryStage.setTitle("Tektosyne Demo");
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    private static MenuBar createMenuBar() {
        final MenuBar menu = new MenuBar();

        final Menu fileMenu = createMenu("_File",
            createMenuItem("_About", t -> new AboutDialog().showAndWait(), null),
            createMenuItem("_Benchmarks", t -> new BenchmarkDialog().showAndWait(),
                new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN)),
            new SeparatorMenuItem(),
            createMenuItem("E_xit", t -> Global.primaryStage().close(), null)
        );

        final Menu geoMenu = createMenu("_Geometry", 
            createMenuItem("Convex _Hull", t -> new ConvexHullDialog().showAndWait(),
                new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN)),
            createMenuItem("Line _Intersection", t -> new LineIntersectionDialog().showAndWait(),
                new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN)),
            createMenuItem("_Point in Polygon", t -> new PointInPolygonDialog().showAndWait(),
                new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN)),
            new SeparatorMenuItem(),
            createMenuItem("_Subdivision", t -> new SubdivisionDialog().showAndWait(),
                new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN)),
            createMenuItem("Subdivision In_tersection", t -> new SubdivisionInterDialog().showAndWait(),
                new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN)),
            createMenuItem("_Voronoi & Delaunay", t -> new VoronoiDialog().showAndWait(),
                new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN))
        );

        final Menu graphMenu = createMenu("_Polygon & Graph",
            createMenuItem("_Regular Polygon", t -> new RegularPolygonDialog().showAndWait(),
                new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)),
            createMenuItem("Polygon _Grid", t -> new PolygonGridDialog().showAndWait(),
                new KeyCodeCombination(KeyCode.G, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)),
            createMenuItem("_Save & Print Grid", t -> new MakeGridDialog().showAndWait(),
                new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)),
            new SeparatorMenuItem(),
            createMenuItem("Graph _Algorithms", t -> new GraphDialog().showAndWait(),
                new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN))
        );

        menu.getMenus().addAll(fileMenu, geoMenu, graphMenu);
        return menu;
    }

    private static Menu createMenu(String title, MenuItem... items) {
        final Menu menu = new Menu(title);
        menu.setMnemonicParsing(true);
        menu.getItems().addAll(items);
        return menu;
    }
    
    private static MenuItem createMenuItem(String text,
            EventHandler<ActionEvent> onAction, KeyCombination key) {

        final MenuItem item = new MenuItem(text);
        item.setMnemonicParsing(true);
        item.setOnAction(onAction);
        if (key != null) item.setAccelerator(key);
        return item;
    }

    /**
     * Launches the {@link TektosyneDemo} application.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
