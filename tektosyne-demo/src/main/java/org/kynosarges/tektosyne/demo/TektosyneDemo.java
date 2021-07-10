package org.kynosarges.tektosyne.demo;

import java.util.function.Consumer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Defines the Tektosyne Demo application.
 *
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class TektosyneDemo extends JFrame {
    private static final long serialVersionUID = 0L;

    /**
     * The single instance of the {@link TektosyneDemo} class.
     */
    public static TektosyneDemo INSTANCE;

    /**
     * Creates a {@link TektosyneDemo} frame.
     * @param args the command line arguments
     */
    private TektosyneDemo(String[] args) {
        INSTANCE = this;
        setJMenuBar(createMenuBar());

        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        getContentPane().add(panel);

        final JLabel caption = new JLabel("Tektosyne Demo Application");
        caption.setFont(caption.getFont().deriveFont(Font.BOLD, 16));
        caption.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        panel.add(caption);

        final JLabel message = new JLabel("Select a menu item to demonstrate Tektosyne features.");
        message.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        panel.add(message);

        setTitle("Tektosyne Demo");
        setSize(400, 300);
        setResizable(false);
        addWindowListener(new WindowCloser());
        getToolkit().setDynamicLayout(true);
    }

    /**
     * Starts the Tektosyne Demo application.
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // default to cross-platform Nimbus L&F if present
        final String name = getLookAndFeelClassName("NimbusLookAndFeel");

        if (name != null) try {
            UIManager.setLookAndFeel(name);
            /*
             * HACK: Nimbus surrounds focusable controls with an empty border that fills in
             * only when focused. This has the effect of making labels appear left-shifted.
             * We add some right-shift to visually align labels with non-focused controls.
             */
            if (name.contains("Nimbus"))
                UIManager.getDefaults().put("Label.contentMargins", new Insets(0, 4, 0, 0));
        } catch (Exception e) {
        }

        EventQueue.invokeLater(() -> {
            final TektosyneDemo frame = new TektosyneDemo(args);
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        });
    }

    /**
     * Gets the full Look &amp; Feel class name for the specified name.
     * Finds first installed {@link UIManager.LookAndFeelInfo} whose name
     * either matches the specified {@code name} or whose class name contains
     * the specified {@code name}. The second option allows disambiguation.
     *
     * @param name a Look &amp; Feel name or (partial) class name
     * @return the first Look &amp; Feel class name matching {@code name},
     * as described above, or {@code null} for no match
     * @throws NullPointerException if {@code name} is {@code null}
     */
    private static String getLookAndFeelClassName(String name) {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
            if (info.getName().equals(name) || info.getClassName().contains(name))
                return info.getClassName();

        return null;
    }

    /**
     * Creates the {@link JMenuBar} for the Tektosyne Demo application.
     * @return the {@link JMenuBar} for the Tektosyne Demo application
     */
    private static JMenuBar createMenuBar() {
        final JMenuBar menu = new JMenuBar();

        final JMenu fileMenu = createMenu("File", KeyEvent.VK_F,
                createMenuItem("About", KeyEvent.VK_A, null, e -> AboutDialog.show()),
                createMenuItem("Benchmarks", KeyEvent.VK_B,
                        KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK),
                        e -> new BenchmarkDialog(INSTANCE)),
                null, // separator
                createMenuItem("Exit", KeyEvent.VK_X, null,
                        e -> INSTANCE.dispatchEvent(new WindowEvent(INSTANCE, WindowEvent.WINDOW_CLOSING)))
        );

        final JMenu geoMenu = createMenu("Geometry", KeyEvent.VK_G,
                createMenuItem("Convex Hull", KeyEvent.VK_H,
                        KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK),
                        e -> new ConvexHullDialog(INSTANCE)),
                createMenuItem("Line Intersection", KeyEvent.VK_I,
                        KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK),
                        e -> new LineIntersectionDialog(INSTANCE)),
                createMenuItem("Point in Polygon", KeyEvent.VK_P,
                        KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK),
                        e -> new PointInPolygonDialog(INSTANCE)),
                null, // separator
                createMenuItem("Subdivision", KeyEvent.VK_S,
                        KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
                        e -> new SubdivisionDialog(INSTANCE)),
                createMenuItem("Subdivision Intersection", KeyEvent.VK_T,
                        KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK),
                        e -> new SubdivisionInterDialog(INSTANCE)),
                createMenuItem("Voronoi & Delaunay", KeyEvent.VK_V,
                        KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK),
                        e -> new VoronoiDialog(INSTANCE))
        );

        final JMenu graphMenu = createMenu("Polygon & Graph", KeyEvent.VK_P,
                createMenuItem("Regular Polygon", KeyEvent.VK_R, KeyStroke.getKeyStroke(
                        KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                        e -> new RegularPolygonDialog(INSTANCE)),
                createMenuItem("Polygon Grid", KeyEvent.VK_G, KeyStroke.getKeyStroke(
                        KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                        e -> new PolygonGridDialog(INSTANCE)),
                createMenuItem("Save & Print Grid", KeyEvent.VK_S, KeyStroke.getKeyStroke(
                        KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                        e -> new MakeGridDialog(INSTANCE)),
                null, // separator
                createMenuItem("Graph Algorithms", KeyEvent.VK_A, KeyStroke.getKeyStroke(
                        KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                        e -> new GraphDialog(INSTANCE))
        );

        menu.add(fileMenu);
        menu.add(geoMenu);
        menu.add(graphMenu);
        return menu;
    }

    private static JMenu createMenu(String title, int mnemonic, JMenuItem... items) {
        final JMenu menu = new JMenu(title);
        menu.setMnemonic(mnemonic);
        for (JComponent item: items)
            if (item == null)
                menu.addSeparator();
            else
                menu.add(item);
        return menu;
    }

    private static JMenuItem createMenuItem(String text, int mnemonic,
            KeyStroke accelerator, Consumer<ActionEvent> onAction) {

        final JMenuItem item = new JMenuItem(text);
        item.setMnemonic(mnemonic);
        if (accelerator != null) item.setAccelerator(accelerator);
        item.addActionListener(onAction::accept);
        return item;
    }

    /**
     * Closes the {@link TektosyneDemo} window.
     */
    private class WindowCloser extends WindowAdapter {
        /**
         * Invoked when the user attempts to close the window.
         * Stops the background thread of the {@link BenchmarkDialog}
         * and closes the {@link TektosyneDemo} window.
         *
         * @param e the {@link WindowEvent} to process
         */
        @Override
        public void windowClosing(WindowEvent e) {
            BenchmarkDialog.EXECUTOR.shutdownNow();
            TektosyneDemo.this.dispose();
        }
    }
}
