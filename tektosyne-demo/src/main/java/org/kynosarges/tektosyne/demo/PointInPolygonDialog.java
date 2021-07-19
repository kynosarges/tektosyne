package org.kynosarges.tektosyne.demo;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides a dialog for testing the {@link GeoUtils#pointInPolygon} algorithm.
 * Draws a random arbitrary polygon and displays the {@link PolygonLocation}
 * of the mouse cursor relative to that polygon.
 * 
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class PointInPolygonDialog extends JDialog {
    private static final long serialVersionUID = 0L;

    private final JLabel _location = new JLabel("OUTSIDE");
    private final JSpinner _tolerance = new JSpinner();

    private PointD[] _polygon;
    private PointD _cursor = PointD.EMPTY;

    /**
     * Creates a {@link PointInPolygonDialog}.
     * @param owner the {@link Window} that owns the dialog
     */
    public PointInPolygonDialog(Window owner) {
        super(owner);
        setModal(true);

        final JPanel panel = new JPanel();
        setContentPane(panel);

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        final JLabel message = new JLabel(
                "<html>Move mouse over polygon to display relative cursor location.<br>" +
                "Use Tolerance to adjust edge and vertex proximity matching.</html>");

        final JLabel locationLabel = new JLabel("Location:");
        final DrawPanel output = new DrawPanel();
        output.setBackground(Color.WHITE);
        output.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, true));
        output.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                _cursor = new PointD(e.getX(), e.getY());
                showLocation();
            }
        });

        _tolerance.setModel(new SpinnerNumberModel(0.0, 0.0, 10.0, 0.1));
        _tolerance.getModel().addChangeListener(e -> output.draw(_polygon));
        _tolerance.setEditor(new JSpinner.NumberEditor(_tolerance, "0.00"));
        _tolerance.setToolTipText("Set tolerance for proximity matching (Alt+T)");

        final JLabel toleranceLabel = new JLabel("Tolerance");
        toleranceLabel.setLabelFor(_tolerance);
        toleranceLabel.setDisplayedMnemonic(KeyEvent.VK_T);
        toleranceLabel.setToolTipText(_tolerance.getToolTipText());

        final JButton maxTolerance = new JButton("Max");
        maxTolerance.addActionListener(t -> _tolerance.getModel().setValue(10.0));
        maxTolerance.setMnemonic(KeyEvent.VK_A);
        maxTolerance.setToolTipText("Set tolerance to maximum (Alt+A)");

        final JButton minTolerance = new JButton("Min");
        minTolerance.addActionListener(t -> _tolerance.getModel().setValue(0.0));
        minTolerance.setMnemonic(KeyEvent.VK_I);
        minTolerance.setToolTipText("Set tolerance to minimum (Alt+I)");

        final GroupLayout.Group inputH = layout.createSequentialGroup().
                addComponent(locationLabel).
                addComponent(_location, 60, 60, 60).
                addComponent(toleranceLabel).
                addComponent(_tolerance, 70, 70, 70).
                addComponent(maxTolerance).
                addComponent(minTolerance);

        final GroupLayout.Group inputV = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addComponent(locationLabel).
                addComponent(_location).
                addComponent(toleranceLabel).
                addComponent(_tolerance).
                addComponent(maxTolerance).
                addComponent(minTolerance);

        final JButton newTest = new JButton("New");
        newTest.requestFocus();
        newTest.addActionListener(t -> output.draw(null));
        newTest.setMnemonic(KeyEvent.VK_N);
        newTest.setToolTipText("Generate new random polygon (Alt+N)");

        final JButton copy = new JButton("Copy");
        copy.addActionListener(t -> Global.copy(this, PointD.class, _polygon));
        copy.setMnemonic(KeyEvent.VK_C);
        copy.setToolTipText("Copy current polygon to clipboard (Alt+C)");

        final JButton paste = new JButton("Paste");
        paste.addActionListener(t -> {
            final PointD[] points = Global.paste(this, PointD.class);
            if (points != null) output.draw(points);
        });
        paste.setMnemonic(KeyEvent.VK_P);
        paste.setToolTipText("Paste existing polygon from clipboard (Alt+P)");

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

        final JSeparator separator = new JSeparator();

        layout.setHorizontalGroup(layout.createParallelGroup().
                addComponent(message).
                addComponent(separator).
                addGroup(inputH).
                addComponent(output).
                addGroup(controlsH));

        layout.setVerticalGroup(layout.createSequentialGroup().
                addComponent(message).
                addComponent(separator, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).
                addGroup(inputV).
                addComponent(output, 200, 200, Short.MAX_VALUE).
                addGroup(controlsV));

        setLocationByPlatform(true);
        setResizable(true);
        pack();
        setTitle("Point in Polygon Test");
        SwingUtilities.invokeLater(() -> output.draw(null));

        // HACK: Window.setMinimumSize ignores high DPI scaling
        addComponentListener(new ResizeListener(this));
        setVisible(true);
    }

    /**
     * Shows the {@link PolygonLocation} of the mouse cursor relative to the current polygon.
     */
    private void showLocation() {
        final double tolerance = (Double) _tolerance.getModel().getValue();

        // determine relative location of mouse cursor
        final PolygonLocation location = (tolerance == 0 ?
            GeoUtils.pointInPolygon(_cursor, _polygon) :
            GeoUtils.pointInPolygon(_cursor, _polygon, tolerance));

        _location.setText(location.toString());
    }

    /**
     * Provides the custom drawing {@link JPanel} for the {@link PointInPolygonDialog}.
     */
    private class DrawPanel extends JPanel {
        private static final long serialVersionUID = 0L;

        private final Color PALEGOLDENROD = Color.decode("#EEE8AA");

        /**
         * Draws the polygon represented the specified {@link PointD} array.
         * Creates a new {@link PointD} array if {@code polygon} is {@code null}.
         *
         * @param polygon the {@link PointD} array containing the polygon vertices
         */
        void draw(PointD[] polygon) {

            // generate new random polygon if desired
            if (polygon == null)
                polygon = GeoUtils.randomPolygon(0, 0, getWidth(), getHeight());

            _polygon = polygon;
            repaint();
        }

        /**
         * Invoked by Swing to draw the {@link DrawPanel}.
         * @param g the {@link Graphics2D} context in which to paint
         */
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (_polygon == null)
                return;

            // draw polygon
            final Graphics2D g2 = (Graphics2D) g;
            final Shape shape = Global.drawPolygon(_polygon);
            g2.setColor(PALEGOLDENROD); g2.fill(shape);
            g2.setColor(Color.BLACK); g2.draw(shape);

            // redraw border over filled polygon
            super.paintBorder(g2);
        }
    }
}
