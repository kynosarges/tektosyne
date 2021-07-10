package org.kynosarges.tektosyne.demo;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides a dialog for testing the {@link Voronoi} algorithm.
 * Draws a random set of points and then superimposes its Voronoi
 * diagram and Delaunay triangulation.
 * 
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class VoronoiDialog extends JDialog {
    private static final long serialVersionUID = 0L;

    private PointD[] _points;

    /**
     * Creates a {@link VoronoiDialog}.
     * @param owner the {@link Window} that owns the dialog
     */
    public VoronoiDialog(Window owner) {
        super(owner);
        setModal(true);

        final JPanel panel = new JPanel();
        setContentPane(panel);

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        final JLabel message = new JLabel(
                "<html>Voronoi regions are shaded yellow, Voronoi edges appear as red solid lines.<br>" +
                "Edges of the Delaunay triangulation appear as blue dashed lines.</html>");

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
        setTitle("Voronoi & Delaunay Test");
        SwingUtilities.invokeLater(() -> output.draw(null));

        // HACK: Window.setMinimumSize ignores high DPI scaling
        addComponentListener(new ResizeListener(this));
        setVisible(true);
    }

    /**
     * Provides the custom drawing {@link JPanel} for the {@link VoronoiDialog}.
     */
    private class DrawPanel extends JPanel {
        private static final long serialVersionUID = 0L;

        // diameter of circles marking random points
        private final static double DIAMETER = 8;

        private final Color PALEGOLDENROD = Color.decode("#EEE8AA");
        private VoronoiResults _results;

        /**
         * Draws a Voronoi diagram for the specified {@link PointD} array.
         * Creates a new {@link PointD} array if {@code points} is {@code null}.
         *
         * @param points the {@link PointD} array whose Voronoi diagram to draw
         */
        void draw(PointD[] points) {

            // generate new random point set if desired
            if (points == null) {
                final double width = getWidth();
                final double height = getHeight();
                final RectD bounds = new RectD(0.1 * width, 0.1 * height, 0.9 * width, 0.9 * height);

                final int count = 4 + Global.RANDOM.nextInt(17);
                points = GeoUtils.randomPoints(count, bounds, new PointDComparatorY(0), DIAMETER);
            }

            _points = points;
            final RectD clip = new RectD(0, 0, getWidth(), getHeight());
            _results = Voronoi.findAll(points, clip);

            repaint();
        }

        /**
         * Invoked by Swing to draw the {@link DrawPanel}.
         * @param g the {@link Graphics2D} context in which to paint
         */
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (_points == null || _results == null)
                return;

            final Graphics2D g2 = (Graphics2D) g;
            final Stroke oldStroke = g2.getStroke();

            // draw interior of Voronoi regions
            g2.setStroke(new BasicStroke(6f));
            for (PointD[] region: _results.voronoiRegions()) {
                final Shape polygon = Global.drawPolygon(region);
                g2.setColor(PALEGOLDENROD); g2.fill(polygon);
                g2.setColor(Color.WHITE); g2.draw(polygon);
            }

            // draw edges of Voronoi diagram
            g2.setStroke(oldStroke);
            g2.setColor(Color.RED);
            for (VoronoiEdge edge: _results.voronoiEdges) {
                final PointD start = _results.voronoiVertices[edge.vertex1];
                final PointD end = _results.voronoiVertices[edge.vertex2];
                final Shape line = Global.drawLine(new LineD(start, end));
                g2.draw(line);
            }

            // draw edges of Delaunay triangulation
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(1f,
                    BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER,
                    10f, new float[] { 3f, 2f }, 0));

            for (LineD edge: _results.delaunayEdges()) {
                final Shape line = Global.drawLine(edge);
                g2.draw(line);
            }

            // draw generator points
            g2.setStroke(oldStroke);
            g2.setColor(Color.BLACK);
            for (PointD point: _points) {
                final Shape circle = Global.drawCircle(point, DIAMETER);
                g2.fill(circle);
                g2.draw(circle);
            }

            super.paintBorder(g2);
        }
    }
}
