package org.kynosarges.tektosyne.demo;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.stage.*;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides global resources for the application.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class Global {

    private static double _fontSize;
    private static Stage _primaryStage;
    
    /**
     * The random number generator for the application.
     * Shared instance for use by various test dialogs.
     */
    public final static Random RANDOM = new Random();

    /**
     * Creates a {@link Global} instance.
     * Private to prevent instantiation.
     */
    private Global() { }

    /**
     * Gets the primary {@link Stage} of the application.
     * @return the primary {@link Stage} of the application
     */
    public static Stage primaryStage() {
        return _primaryStage;
    }

    /**
     * Sets the primary {@link Stage} of the application.
     * @param stage the primary {@link Stage} of the application
     * @throws IllegalStateException if the method has already been called
     * @throws NullPointerException if {@code primaryStage} is {@code null}
     */
    static void setPrimaryStage(Stage stage) {
        if (_primaryStage != null)
            throw new IllegalStateException("primaryStage != null");
        if (stage == null)
            throw new NullPointerException("stage");

        _primaryStage = stage;
    }

    /**
     * Clips the children of the specified {@link Region} to its current size.
     * This requires attaching a change listener to the region’s layout bounds,
     * as JavaFX does not currently provide any built-in way to clip children.
     * 
     * @param region the {@link Region} whose children to clip
     * @throws NullPointerException if {@code region} is {@code null}
     */
    public static void clipChildren(Region region) {

        final Rectangle outputClip = new Rectangle();
        region.setClip(outputClip);

        region.layoutBoundsProperty().addListener((ov, oldValue, newValue) -> {
            outputClip.setWidth(newValue.getWidth());
            outputClip.setHeight(newValue.getHeight());
        });        
    }

    /**
     * Gets a bold {@link Font} using the default family and specified size.
     * @param size the size of the {@link Font}, in points
     * @return a bold {@link Font} using the default family and specified {@code size}
     */
    public static Font boldFont(double size) {
        return Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, size);
    }
    
    /**
     * Copies the specified array of geometric objects to the system clipboard.
     * Copies {@code items} as a sequence of {@link Double} values that represent the
     * coordinates of all specified geometric objects. Does nothing if {@code items} is
     * {@code null} or empty. Shows an {@link Alert} for any other error or exception.
     * Never throws exceptions.
     * 
     * @param <T> the geometric type to convert from, which must be either
     *            {@link LineD}, {@link PointD}, {@link RectD}, or {@link SizeD}
     * @param owner the {@link Window} that owns any {@link Alert} that appears
     * @param type the {@link Class} object for <b>T</b>, required for method dispatch
     * @param items the <b>T</b> array to copy
     */
    public static <T> void copy(Window owner, Class<T> type, T[] items) {
        if (items == null || items.length == 0)
            return;

        try {
            final double[] dp = GeoUtils.toDoubles(type, items);
            final String[] sp = Arrays.stream(dp).mapToObj(Double::toString).toArray(String[]::new);
            final String output = String.join(" ", sp);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().
                    setContents(new StringSelection(output), null);
        }
        catch (Exception e) {
            showError(owner, "Failed to copy to clipboard.", e.toString());
        }
    }

    /**
     * Gets the default {@link Font} size for the application.
     * @return the default {@link Font} size for the application
     */
    public static double fontSize() {
        if (_fontSize == 0)
            _fontSize = Font.getDefault().getSize();

        return _fontSize;
    }
    
    /**
     * Determines whether the specified {@link String} has any content.
     * @param s the {@link String} to examine
     * @return {@code true} if {@code s} is neither {@code null} nor empty
     */
    public static boolean hasContent(String s) {
        return (s != null && !s.isEmpty());
    }

    /**
     * Attempts to paste an array of geometric objects from the system clipboard.
     * Expects a {@link String} on the clipboard that contains a sequence of {@link Double}
     * values which represent coordinates for instances of the specified geometric type.
     * Shows an {@link Alert} for any other error or exception. Never throws exceptions.
     * 
     * @param <T> the geometric type to convert to, which must be either
     *            {@link LineD}, {@link PointD}, {@link RectD}, or {@link SizeD}
     * @param owner the {@link Window} that owns any {@link Alert} that appears
     * @param type the {@link Class} object for <b>T</b>, required for method dispatch
     * @return a <b>T</b> array copied from the clipboard, or {@code null}
     *         if none was available or an error occurred
     */
    public static <T> T[] paste(Window owner, Class<T> type) {
        String input = null;
        try {
            final Transferable data = java.awt.Toolkit.
                    getDefaultToolkit().getSystemClipboard().getContents(owner);

            if (data == null || !data.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                showError(owner, "Failed to paste from clipboard.",
                        "No valid coordinate set available to paste");
                return null;
            }
            input = (String) data.getTransferData(DataFlavor.stringFlavor);
        }
        catch (Exception e) {
            showError(owner, "Failed to paste from clipboard.", e.toString());
            return null;
        }

        try {
            final String[] sp = input.split("\\p{javaWhitespace}+");
            final double[] dp = Arrays.stream(sp).mapToDouble(Double::valueOf).toArray();
            return GeoUtils.fromDoubles(type, dp);
        } catch (Exception e) {
            showError(owner, "Failed to paste from clipboard.",
                    "No valid coordinate set available to paste\n" + e.toString());
            return null;
        }
    }

    /**
     * Shows a modal {@link Alert} with the specified error message.
     * @param owner the {@link Window} that owns the {@link Alert}
     * @param header the header text of the {@link Alert}
     * @param content the content text of the {@link Alert}
     */
    public static void showError(Window owner, String header, String content) {

        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner == null ? primaryStage() : owner);
        alert.setHeaderText(header);
        alert.setContentText(content);

        alert.setResizable(true);
        alert.setTitle("Operation Failed");
        alert.showAndWait();
    }

    /**
     * Shows a modal {@link Alert} with the specified {@link Exception}.
     * @param owner the {@link Window} that owns the {@link Alert}
     * @param header the header text of the {@link Alert}
     * @param e the {@link Exception} providing content for the {@link Alert}
     */
    public static void showError(Window owner, String header, Exception e) {

        String content = "Unspecified error.";
        String stackTrace = "No stack trace available.";
        if (e != null) {
            content = e.toString();
            final StringWriter writer = new StringWriter();
            try (PrintWriter print = new PrintWriter(writer)) {
                e.printStackTrace(print);
                stackTrace = writer.toString();
            }
        }

        final TextArea textArea = new TextArea(stackTrace);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner == null ? primaryStage() : owner);
        alert.setTitle("Operation Failed");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setExpandableContent(new StackPane(textArea));
        alert.showAndWait();
    }
}
