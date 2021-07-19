package org.kynosarges.tektosyne.demo;

import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionListener;
import java.awt.geom.*;
import javax.swing.*;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides global resources and helpers for the application.
 * @author Christoph Nahr
 * @version 6.3.0
 */
public final class Global {
    /**
     * The random number generator for the application.
     * Shared instance for use by various test dialogs.
     */
    public final static ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    /**
     * Creates a {@link Global} instance.
     * Private to prevent instantiation.
     */
    private Global() { }

    /**
     * Determines whether the specified array contains the specified value.
     * Uses {@link Objects#equals} to check for equality.
     *
     * @param array the array whose elements to search for {@code value}
     * @param value the value to find in {@code array}
     * @param <T> the type of {@code array} elements and {@code value}
     * @return {@code true} if {@code array} contains {@code value}, else {@code false}
     */
    public static <T> boolean contains(T[] array, T value) {
        for (T element: array)
            if (Objects.equals(element, value))
                return true;

        return false;
    }

    /**
     * Copies the specified array of geometric objects to the system clipboard.
     * Copies {@code items} as a sequence of {@link Double} values that represent the
     * coordinates of all specified geometric objects. Does nothing if {@code items} is
     * {@code null} or empty. Shows a dialog for any other error or exception.
     * Never throws exceptions.
     *
     * @param <T> the geometric type to convert from, which must be either
     *            {@link LineD}, {@link PointD}, {@link RectD}, or {@link SizeD}
     * @param parent the parent {@link Component} for any dialog that appears
     * @param type the {@link Class} object for <b>T</b>, required for method dispatch
     * @param items the <b>T</b> array to copy
     */
    public static <T> void copy(Component parent, Class<T> type, T[] items) {
        if (items == null || items.length == 0)
            return;

        try {
            final double[] dp = GeoUtils.toDoubles(type, items);
            final String[] sp = Arrays.stream(dp).mapToObj(Double::toString).toArray(String[]::new);
            final String output = String.join(" ", sp);
            Toolkit.getDefaultToolkit().getSystemClipboard().
                    setContents(new StringSelection(output), null);
        }
        catch (Exception e) {
            showError(parent, "Failed to copy to clipboard.", e);
        }
    }

    /**
     * Attempts to paste an array of geometric objects from the system clipboard.
     * Expects a {@link String} on the clipboard that contains a sequence of {@link Double}
     * values which represent coordinates for instances of the specified geometric type.
     * Shows a dialog for any other error or exception. Never throws exceptions.
     *
     * @param <T> the geometric type to convert to, which must be either
     *            {@link LineD}, {@link PointD}, {@link RectD}, or {@link SizeD}
     * @param parent the parent {@link Component} for any dialog that appears
     * @param type the {@link Class} object for <b>T</b>, required for method dispatch
     * @return a <b>T</b> array copied from the clipboard, or {@code null}
     *         if none was available or an error occurred
     */
    public static <T> T[] paste(Component parent, Class<T> type) {
        String input;
        try {
            final Transferable data = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(parent);

            if (data == null || !data.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                showError(parent, "Failed to paste from clipboard.",
                        "No valid coordinate set available to paste");
                return null;
            }
            input = (String) data.getTransferData(DataFlavor.stringFlavor);
        }
        catch (Exception e) {
            showError(parent, "Failed to paste from clipboard.", e);
            return null;
        }

        try {
            final String[] sp = input.split("\\p{javaWhitespace}+");
            final double[] dp = Arrays.stream(sp).mapToDouble(Double::valueOf).toArray();
            return GeoUtils.fromDoubles(type, dp);
        } catch (Exception e) {
            showError(parent, "Failed to paste from clipboard.",
                    "No valid coordinate set available to paste.", e);
            return null;
        }
    }

    /**
     * Adds the specified {@link ActionListener} to all elements
     * of the specified {@link ButtonGroup}.
     * @param group the {@link ButtonGroup} whose elements to process
     * @param listener the {@link ActionListener} to add to all elements of {@code group}
     * @throws NullPointerException if {@code group} or {@code listener} is {@code null}
     */
    public static void addGroupListener(ButtonGroup group, ActionListener listener) {
        if (listener == null)
            throw new NullPointerException("listener");

        for (Enumeration<AbstractButton> e = group.getElements(); e.hasMoreElements(); )
            e.nextElement().addActionListener(listener);
    }

    /**
     * Converts the specified circle into an {@link Ellipse2D} shape.
     * @param center the {@link PointD} coordinates of the center of the circle
     * @param diameter the diameter of the circle
     * @return the circular {@link Ellipse2D.Double} shape for {@code center} and {@code diameter}
     * @throws NullPointerException if {@code center} is {@code null}
     */
    public static Ellipse2D drawCircle(PointD center, double diameter) {
        return new Ellipse2D.Double(
                center.x - diameter / 2, center.y - diameter / 2, diameter, diameter);
    }

    /**
     * Converts the specified {@link LineD} into a {@link Line2D} shape.
     * @param line the {@link LineD} to draw
     * @return the {@link Line2D.Double} shape that represents {@code line}
     * @throws NullPointerException if {@code line} is {@code null}
     */
    public static Line2D drawLine(LineD line) {
        return new Line2D.Double(line.start.x, line.start.y, line.end.x, line.end.y);
    }

    /**
     * Converts the specified polygon into a {@link Path2D} shape.
     * @param polygon the array of {@link PointD} coordinates that defines the polygon
     * @return the closed {@link Path2D.Double} shape that represents {@code polygon}
     * @throws NullPointerException if {@code polygon} or any of its elements is {@code null}
     */
    public static Path2D drawPolygon(PointD[] polygon) {
        final Path2D path = new Path2D.Double();

        path.moveTo(polygon[0].x, polygon[0].y);
        for (int i = 1; i < polygon.length; i++)
                path.lineTo(polygon[i].x, polygon[i].y);

        path.closePath();
        return path;
    }

    /**
     * Converts the specified polygon into a {@link Path2D} shape with the specified center.
     * @param center the central {@link PointD} added to all {@code polygon} coordinates
     * @param polygon the array of {@link PointD} coordinates that defines the polygon
     * @return the closed {@link Path2D.Double} shape that represents {@code polygon}
     * @throws NullPointerException if {@code center} or {@code polygon}
     *                              or any of its elements is {@code null}
     */
    public static Path2D drawPolygon(PointD center, PointD[] polygon) {
        final Path2D path = new Path2D.Double();

        path.moveTo(center.x + polygon[0].x, center.y + polygon[0].y);
        for (int i = 1; i < polygon.length; i++)
            path.lineTo(center.x + polygon[i].x, center.y + polygon[i].y);

        path.closePath();
        return path;
    }

    /**
     * Converts the specified {@link RectD} into a {@link Rectangle2D} shape.
     * @param rectangle the {@link RectD} to draw
     * @return the {@link Rectangle2D.Double} shape that represents {@code rectangle}
     * @throws NullPointerException if {@code rectangle} is {@code null}
     */
    public static Rectangle2D drawRectangle(RectD rectangle) {
        return new Rectangle2D.Double(
                rectangle.min.x, rectangle.min.y, rectangle.width(), rectangle.height());
    }

    /**
     * Determines whether the specified {@link String} has visible content.
     * @param s the {@link String} to examine
     * @return {@code true} if {@code s} is not {@code null} and contains
     *         any characters other than whitespace, else {@code false}
     */
    public static boolean hasContent(String s) {
        return (s != null && !s.trim().isEmpty());
    }

    /**
     * Shows a modal dialog with the specified error message.
     * @param parent the parent {@link Component}
     * @param header the header text of the dialog
     * @param message the optional message text of the dialog
     */
    public static void showError(Component parent, String header, String message) {
        showError(parent, header, message, null);
    }

    /**
     * Shows a modal dialog with the specified {@link Throwable}.
     * @param parent the parent {@link Component}
     * @param header the header text of the dialog
     * @param e the optional {@link Throwable} whose stack trace to show
     */
    public static void showError(Component parent, String header, Throwable e) {
        showError(parent, header, null, e);
    }

    /**
     * Shows a modal dialog with the specified error message and {@link Throwable}.
     * @param parent the parent {@link Component}
     * @param header the header text of the dialog
     * @param message the optional message text of the dialog
     * @param e the optional {@link Throwable} whose stack trace to show
     */
    public static void showError(Component parent, String header, String message, Throwable e) {

        String trace = null;
        if (e != null) {
            final StringWriter writer = new StringWriter();
            try (PrintWriter print = new PrintWriter(writer)) {
                e.printStackTrace(print);
                trace = writer.toString();
            }
        }

        final JPanel panel = new JPanel();
        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        // JOptionPane provides its own outside gaps
        layout.setAutoCreateContainerGaps(false);
        panel.setLayout(layout);

        final GroupLayout.ParallelGroup groupH = layout.createParallelGroup();
        final GroupLayout.SequentialGroup groupV = layout.createSequentialGroup();

        if (hasContent(header)) {
            final JLabel headerLabel = new JLabel(header);
            headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
            groupH.addComponent(headerLabel);
            groupV.addComponent(headerLabel);

            if (hasContent(message)) {
                final JSeparator separator = new JSeparator();
                groupH.addComponent(separator);
                // JSeparator is by default resizable in separation direction!
                groupV.addComponent(separator, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE);
            }
        }
        if (hasContent(message)) {
            final JLabel contentLabel = new JLabel(message);
            contentLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
            groupH.addComponent(contentLabel);
            groupV.addComponent(contentLabel);
        }
        if (hasContent(trace)) {
            final JScrollPane tracePane = new JScrollPane(new JTextArea(trace));
            tracePane.setPreferredSize(new Dimension(300, 200));
            groupH.addComponent(tracePane);
            groupV.addComponent(tracePane);
        }

        layout.setHorizontalGroup(groupH);
        layout.setVerticalGroup(groupV);

        final JOptionPane optionPane = new JOptionPane(panel, JOptionPane.ERROR_MESSAGE);
        final JDialog dialog = optionPane.createDialog(parent, "Operation Failed");
        dialog.setResizable(true);
        dialog.setVisible(true);
    }
}
