package org.kynosarges.tektosyne.demo;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides a dialog for testing the {@link GeoUtils#convexHull} algorithm.
 * Draws a random set of points and then superimposes the polygon
 * that constitutes its convex hull.
 * 
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class ConvexHullDialog extends JDialog {
    private static final long serialVersionUID = 0L;

    private PointD[] _points, _polygon;

    /**
     * Creates a {@link ConvexHullDialog}.
     * @param owner the {@link Window} that owns the dialog
     */    
    public ConvexHullDialog(Window owner) {
        super(owner);
        setModal(true);

        final JPanel panel = new JPanel();
        setContentPane(panel);

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        final JLabel message = new JLabel(
                "<html>Convex hull vertices appear as filled circles, interior points appear hollow.</html>");

        final DrawPanel output = new DrawPanel();
        output.setBackground(Color.WHITE);
        output.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, true));

        final JButton newTest = new JButton("New");
        newTest.requestFocus();
        newTest.addActionListener(t -> output.draw(null));
        newTest.setMnemonic(KeyEvent.VK_N);
        newTest.setToolTipText("Generate new random point set (Alt+N)");

        final JButton copy = new JButton("Copy");
        copy.addActionListener(t -> Global.copy(this, PointD.class, _points));
        copy.setMnemonic(KeyEvent.VK_C);
        copy.setToolTipText("Copy current point set to clipboard (Alt+C)");

        final JButton paste = new JButton("Paste");
        paste.addActionListener(t -> {
            final PointD[] points = Global.paste(this, PointD.class);
            if (points != null) output.draw(points);
        });
        paste.setMnemonic(KeyEvent.VK_P);
        paste.setToolTipText("Paste existing point set from clipboard (Alt+P)");

        final JButton close = CloseAction.createButton(this, panel);

        final GroupLayout.Group controlsH = layout.createSequentialGroup().
                addContainerGap(0, Short.MAX_VALUE).
                addComponent(newTest).
                addComponent(copy).
                addComponent(paste).
                addComponent(close).
                addContainerGap(0, Short.MAX_VALUE);

        final GroupLayout.Group controlsV = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addComponent(newTest).
                addComponent(copy).
                addComponent(paste).
                addComponent(close);

        layout.setHorizontalGroup(layout.createParallelGroup().
                addComponent(message).
                addComponent(output).
                addGroup(controlsH));

        layout.setVerticalGroup(layout.createSequentialGroup().
                addComponent(message).
                addComponent(output, 250, 250, Short.MAX_VALUE).
                addGroup(controlsV));

        setLocationByPlatform(true);
        setResizable(true);
        pack();
        setTitle("Convex Hull Test");
        SwingUtilities.invokeLater(() -> output.draw(null));

        // HACK: Window.setMinimumSize ignores high DPI scaling
        addComponentListener(new ResizeListener(this));
        setVisible(true);
    }

    /**
     * Provides the custom drawing {@link JPanel} for the {@link ConvexHullDialog}.
     */
    private class DrawPanel extends JPanel {
        private static final long serialVersionUID = 0L;

        // diameter of circles marking random points
        private final static double DIAMETER = 8;

        /**
         * Draws a convex hull for the specified {@link PointD} array.
         * Creates a new {@link PointD} array if {@code points} is {@code null}.
         *
         * @param points the {@link PointD} array whose convex hull to draw
         */
        void draw(PointD[] points) {

            // generate new random point set if desired
            if (points == null) {
                final double width = getWidth() - 2 * DIAMETER;
                final double height = getHeight() - 2 * DIAMETER;
                final RectD bounds = new RectD(0, 0, width, height).offset(DIAMETER, DIAMETER);

                final int count = 4 + Global.RANDOM.nextInt(37);
                points = GeoUtils.randomPoints(count, bounds, new PointDComparatorY(0), DIAMETER);
            }

            _points = points;
            _polygon = GeoUtils.convexHull(points);
            repaint();
        }

        /**
         * Invoked by Swing to draw the {@link DrawPanel}.
         * @param g the {@link Graphics2D} context in which to paint
         */
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (_points == null || _polygon == null)
                return;

            final Graphics2D g2 = (Graphics2D) g;

            // draw hull vertices filled, other points hollow
            g2.setColor(Color.BLACK);
            for (PointD point: _points) {
                final Shape circle = Global.drawCircle(point, DIAMETER);
                g2.draw(circle);
                if (Global.contains(_polygon, point))
                    g2.fill(circle);
            }

            // draw edges of convex hull
            g2.setColor(Color.RED);
            g2.draw(Global.drawPolygon(_polygon));
        }
    }
}
