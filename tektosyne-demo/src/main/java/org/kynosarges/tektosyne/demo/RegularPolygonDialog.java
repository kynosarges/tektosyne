package org.kynosarges.tektosyne.demo;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import javax.swing.*;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides a dialog for testing the {@link RegularPolygon} class.
 * Draws a resizable polygon with user-defined side count and orientation.
 * The drawing includes the center of the polygon, the inscribed and circumscribed
 * circles in red, and the bounding rectangle in green.
 * <p>
 * Additionally, the user may adjust a delta value to draw another polygon
 * in blue that is identical to the first polygon except for an inflated
 * or deflated circumcircle radius.</p>
 * 
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class RegularPolygonDialog extends JDialog {
    private static final long serialVersionUID = 0L;

    private final DrawPanel _output = new DrawPanel();

    private final JSpinner _sides = new JSpinner();
    private final JSpinner _delta = new JSpinner();
    private final JRadioButton _onEdge = new JRadioButton("On Edge");
    private final JRadioButton _onVertex = new JRadioButton("On Vertex");

    /**
     * Creates a {@link RegularPolygonDialog}.
     * @param owner the {@link Window} that owns the dialog
     */
    public RegularPolygonDialog(Window owner) {
        super(owner);
        setModal(true);

        final JPanel panel = new JPanel();
        setContentPane(panel);

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        final JLabel message = new JLabel(
                "<html>Polygon and center point are black, optional delta-inflated clone is blue.<br>" +
                "Inscribed and circumscribed circles are red, circumscribed rectangle is green.</html>");

        _output.setBackground(Color.WHITE);
        _output.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, true));

        _sides.setModel(new SpinnerNumberModel(3, 3, 12, 1));
        _sides.getModel().addChangeListener(e -> _output.draw());
        _sides.setEditor(new JSpinner.NumberEditor(_sides));
        _sides.setToolTipText("Set number of sides for the polygon (Alt+S)");

        final JLabel sidesLabel = new JLabel("Sides");
        sidesLabel.setLabelFor(_sides);
        sidesLabel.setDisplayedMnemonic(KeyEvent.VK_S);
        sidesLabel.setToolTipText(_sides.getToolTipText());

        _delta.setModel(new SpinnerNumberModel(0, -100, 100, 10));
        _delta.getModel().addChangeListener(e -> _output.draw());
        _delta.setEditor(new JSpinner.NumberEditor(_delta));
        _delta.setToolTipText("Set inflation or deflation for cloned polygon (Alt+D)");

        final JLabel deltaLabel = new JLabel("Delta");
        deltaLabel.setLabelFor(_delta);
        deltaLabel.setDisplayedMnemonic(KeyEvent.VK_D);
        deltaLabel.setToolTipText(_delta.getToolTipText());

        final ButtonGroup group = new ButtonGroup();
        group.add(_onEdge);
        group.add(_onVertex);

        _onEdge.setSelected(true);
        _onEdge.addActionListener(e -> _output.draw());
        _onEdge.setMnemonic(KeyEvent.VK_E);
        _onEdge.setToolTipText("Stand polygon on an edge (Alt+E)");

        _onVertex.addActionListener(e -> _output.draw());
        _onVertex.setMnemonic(KeyEvent.VK_V);
        _onVertex.setToolTipText("Stand polygon on a vertex (Alt+V)");

        final JSeparator leftOrientation = new JSeparator(SwingConstants.VERTICAL);
        final JSeparator rightOrientation = new JSeparator(SwingConstants.VERTICAL);

        final GroupLayout.Group inputH = layout.createSequentialGroup().
                addComponent(sidesLabel).
                addComponent(_sides, 60, 60, 60).
                addComponent(leftOrientation, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).
                addComponent(_onEdge).
                addComponent(_onVertex).
                addComponent(rightOrientation, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).
                addComponent(deltaLabel).
                addComponent(_delta, 60, 60, 60);

        final GroupLayout.Group inputV = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addComponent(sidesLabel).
                addComponent(_sides).
                addComponent(leftOrientation).
                addComponent(_onEdge).
                addComponent(_onVertex).
                addComponent(rightOrientation).
                addComponent(deltaLabel).
                addComponent(_delta);

        final JButton close = CloseAction.createButton(this, panel);

        final GroupLayout.Group controlsH = layout.createSequentialGroup().
                addContainerGap(0, Short.MAX_VALUE).
                addComponent(close).
                addContainerGap(0, Short.MAX_VALUE);

        final GroupLayout.Group controlsV = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addComponent(close);

        final JSeparator separator = new JSeparator();

        layout.setHorizontalGroup(layout.createParallelGroup().
                addComponent(message).
                addComponent(separator).
                addGroup(inputH).
                addComponent(_output).
                addGroup(controlsH));

        layout.setVerticalGroup(layout.createSequentialGroup().
                addComponent(message).
                addComponent(separator, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).
                addGroup(inputV).
                addComponent(_output, 250, 250, Short.MAX_VALUE).
                addGroup(controlsV));

        setLocationByPlatform(true);
        setResizable(true);
        pack();
        setTitle("Regular Polygon Test");
        SwingUtilities.invokeLater(_output::draw);

        // HACK: Window.setMinimumSize ignores high DPI scaling
        // For RegularPolygonDialog, the listener also resizes the grid.
        addComponentListener(new PolygonResizeListener());
        setVisible(true);
    }

    private class PolygonResizeListener extends ResizeListener {
        /**
         * Creates a {@link PolygonResizeListener} for the {@link RegularPolygonDialog}.
         */
        PolygonResizeListener() {
            super(RegularPolygonDialog.this);
        }

        /**
         * Invoked when the size of the {@link Component} changes.
         * Restores the minimum size obtained during construction if
         * the current size is smaller in either or both dimensions.
         *
         * @param e the {@link ComponentEvent} that occurred
         */
        @Override
        public void componentResized(ComponentEvent e) {
            super.componentResized(e);
            _output.draw();
        }
    }

    /**
     * Provides the custom drawing {@link JPanel} for the {@link RegularPolygonDialog}.
     */
    private class DrawPanel extends JPanel {
        private static final long serialVersionUID = 0L;

        private PointD _center;
        private Shape _innerCircle, _outerCircle, _outerRect;
        private Shape _centerPoint, _standardPoly, _inflatedPoly;

        /**
         * Draws the {@link RegularPolygon} shapes specified in the dialog.
         */
        void draw() {

            // determine side count and orientation
            final int sides = (Integer) _sides.getModel().getValue();
            final PolygonOrientation orientation = (_onEdge.isSelected() ?
                    PolygonOrientation.ON_EDGE : PolygonOrientation.ON_VERTEX);

            // compute side length based on layout size and side count
            final double layout = Math.min(getWidth(), getHeight());
            final double length = 2.5 * layout / sides;
            final RegularPolygon standard = new RegularPolygon(length, sides, orientation);

            // determine inflation relative to standard polygon
            final int delta = (Integer) _delta.getModel().getValue();
            final RegularPolygon inflated = standard.inflate(delta);

            // inscribed circle, circumscribed circle and rectangle
            _center = new PointD(getWidth() / 2.0, getHeight() / 2.0);
            _innerCircle = Global.drawCircle(_center, 2 * standard.innerRadius);
            _outerCircle = Global.drawCircle(_center, 2 * standard.outerRadius);
            _outerRect = Global.drawRectangle(standard.bounds);

            // draw center point, basic and inflated polygon
            _centerPoint = Global.drawCircle(_center, 2);
            _standardPoly = Global.drawPolygon(standard.vertices);
            _inflatedPoly = (delta == 0 ? null : Global.drawPolygon(inflated.vertices));

            repaint();
        }

        /**
         * Invoked by Swing to draw the {@link DrawPanel}.
         * @param g the {@link Graphics2D} context in which to paint
         */
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (_standardPoly == null)
                return;

            // draw inscribed and circumscribed circle in red
            final Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.RED);
            g2.draw(_innerCircle);
            g2.draw(_outerCircle);

            // draw center point of polygon
            _centerPoint = Global.drawCircle(_center, 4);
            g2.setColor(Color.BLACK);
            g2.draw(_centerPoint);
            g2.fill(_centerPoint);

            // move to center point of the nested shapes
            final AffineTransform saveAT = g2.getTransform();
            g2.translate(_center.x, _center.y);

            // draw basic polygon in black
            g2.draw(_standardPoly);

            // draw circumscribed rectangle in green
            g2.setColor(Color.GREEN);
            g2.draw(_outerRect);

            // draw inflated polygon in blue, if set
            if (_inflatedPoly != null) {
                g2.setColor(Color.BLUE);
                g2.draw(_inflatedPoly);
            }

            // redraw border over filled polygon
            g2.setTransform(saveAT);
            super.paintBorder(g2);
        }
    }
}
