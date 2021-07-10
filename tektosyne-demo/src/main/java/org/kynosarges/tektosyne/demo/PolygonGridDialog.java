package org.kynosarges.tektosyne.demo;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides a dialog for testing the {@link PolygonGrid} class.
 * Draws a resizable {@link PolygonGrid} based on a user-defined {@link RegularPolygon}.
 * The dialog tracks the current element and shows its coordinates.
 * <p>
 * Additionally, the user may change the {@link PolygonGridShift}, highlight all
 * neighbors within a given distance, and show all distances from the current element.</p>
 * 
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class PolygonGridDialog extends JDialog {
    private static final long serialVersionUID = 0L;

    private final DrawPanel _output = new DrawPanel();

    private final JRadioButton _elementSquare, _elementHexagon;
    private final JCheckBox _vertexNeighbors;
    private final JRadioButton _elementOnEdge, _elementOnVertex;
    private final JRadioButton _shiftNone, _shiftColumnUp,
            _shiftColumnDown, _shiftRowLeft, _shiftRowRight;

    private final JLabel _columnsLabel = new JLabel("0");
    private final JLabel _rowsLabel = new JLabel("0");
    private final JLabel _cursorLabel = new JLabel("—");

    // current element and grid
    private RegularPolygon _element;
    private PolygonGrid _grid;

    // highlight options for neighbors of cursor
    private enum NeighborHighlight {
        IMMEDIATE,
        THREE_STEPS,
        DISTANCES
    }

    // auxiliary variables for drawing
    private PointI _gridCursor;
    private NeighborHighlight _highlight;
    private PointD _border = PointD.EMPTY, _drawCursor;
    private PointD[] _insetVertices;

    // ignore input while updating grid
    private boolean _updating;
    
    /**
     * Creates a {@link PolygonGridDialog}.
     * @param owner the {@link Window} that owns the dialog
     */
    public PolygonGridDialog(Window owner) {
        super(owner);
        setModal(true);

        final JPanel panel = new JPanel();
        setContentPane(panel);

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        final JLabel message = new JLabel(
                "<html>Resize dialog to change grid size. Mouse over grid to highlight elements. Left-click to show<br>" +
                "immediate neighbors. Middle-click to show third neighbors. Right-click to show distances.</html>");

        _output.setBackground(Color.WHITE);
        _output.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, true));

        final MouseAdapter mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onMouseClicked(e);
            }
            @Override
            public void mouseMoved(MouseEvent e) {
                onMouseMoved(e);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                clearCursor();
            }
        };
        _output.addMouseListener(mouseListener);
        _output.addMouseMotionListener(mouseListener);

        final ButtonGroup elementGroup = new ButtonGroup();
        final JPanel elementPanel = new JPanel();
        elementPanel.setBorder(BorderFactory.createTitledBorder("Element "));
        elementPanel.setLayout(new GridLayout(5, 1, 0, 2));

        _elementSquare = createRadioButton("Square",
                "Square elements (Alt+S)", KeyEvent.VK_S, elementGroup, elementPanel);
        _elementHexagon = createRadioButton("Hexagon",
                "Hexagonal elements (Alt+H)", KeyEvent.VK_H, elementGroup, elementPanel);
        _elementSquare.setSelected(true);

        _vertexNeighbors = new JCheckBox("V-Neighbors");
        _vertexNeighbors.setToolTipText("Vertex neighbors (Alt+N)");
        _vertexNeighbors.setMnemonic(KeyEvent.VK_N);
        elementPanel.add(_vertexNeighbors);

        final ButtonGroup orientationGroup = new ButtonGroup();
        final JPanel orientationPanel = new JPanel();
        orientationPanel.setBorder(BorderFactory.createTitledBorder("Orientation "));
        orientationPanel.setLayout(new GridLayout(5, 1, 0, 2));

        _elementOnEdge = createRadioButton("On Edge",
                "Elements on edge (Alt+E)", KeyEvent.VK_E, orientationGroup, orientationPanel);
        _elementOnVertex = createRadioButton("On Vertex",
                "Elements on vertex (Alt+V)", KeyEvent.VK_V, orientationGroup, orientationPanel);
        _elementOnEdge.setSelected(true);

        final ButtonGroup shiftGroup = new ButtonGroup();
        final JPanel shiftPanel = new JPanel();
        shiftPanel.setBorder(BorderFactory.createTitledBorder("Grid Shift "));
        shiftPanel.setLayout(new GridLayout(5, 1, 0, 2));

        _shiftNone = createRadioButton("None",
                "No grid shift (Alt+O)", KeyEvent.VK_O, shiftGroup, shiftPanel);
        _shiftColumnUp = createRadioButton("Column Up",
                "Shift column up (Alt+U)", KeyEvent.VK_U, shiftGroup, shiftPanel);
        _shiftColumnDown = createRadioButton("Column Down",
                "Shift column down (Alt+D)", KeyEvent.VK_D, shiftGroup, shiftPanel);
        _shiftRowLeft = createRadioButton("Row Left",
                "Shift row left (Alt+L)", KeyEvent.VK_L, shiftGroup, shiftPanel);
        _shiftRowRight = createRadioButton("Row Right",
                "Shift row right (Alt+R)", KeyEvent.VK_R, shiftGroup, shiftPanel);
        _shiftNone.setSelected(true);

        final JPanel sizePanel = new JPanel();
        sizePanel.setBorder(BorderFactory.createTitledBorder("Grid Size "));
        sizePanel.setLayout(new GridLayout(5, 2));

        _columnsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        _columnsLabel.setToolTipText("Current number of grid columns");
        sizePanel.add(new JLabel("Columns"));
        sizePanel.add(_columnsLabel);

        _rowsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        _rowsLabel.setToolTipText("Current number of grid rows");
        sizePanel.add(new JLabel("Rows"));
        sizePanel.add(_rowsLabel);

        _cursorLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        _cursorLabel.setToolTipText("Grid coordinates under mouse cursor");
        sizePanel.add(new JLabel("Cursor"));
        sizePanel.add(_cursorLabel);

        final GroupLayout.Group inputH = layout.createSequentialGroup().
                addComponent(elementPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).
                addComponent(orientationPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).
                addComponent(shiftPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).
                addComponent(sizePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE);

        final GroupLayout.Group inputV = layout.createParallelGroup().
                addComponent(elementPanel).
                addComponent(orientationPanel).
                addComponent(shiftPanel).
                addComponent(sizePanel);

        layout.linkSize(SwingConstants.VERTICAL,
                elementPanel, orientationPanel, shiftPanel, sizePanel);

        final JButton close = CloseAction.createButton(this, panel);

        final GroupLayout.Group controlsH = layout.createSequentialGroup().
                addContainerGap(0, Short.MAX_VALUE).
                addComponent(close).
                addContainerGap(0, Short.MAX_VALUE);

        final GroupLayout.Group controlsV = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addComponent(close);

        layout.setHorizontalGroup(layout.createParallelGroup().
                addComponent(message).
                addGroup(inputH).
                addComponent(_output).
                addGroup(controlsH));

        layout.setVerticalGroup(layout.createSequentialGroup().
                addComponent(message).
                addGroup(inputV).
                addComponent(_output, 300, 300, Short.MAX_VALUE).
                addGroup(controlsV));

        setLocationByPlatform(true);
        setResizable(true);
        pack();
        setTitle("Polygon Grid Test");

        /*
         * Update element and element controls immediately without drawing.
         * Grid is calculated and drawn implicitly by first output sizing.
         */
        updateElement(false);

        Global.addGroupListener(elementGroup,
                e -> { if (!_updating) updateElement(true); });
        _vertexNeighbors.addActionListener(
                e -> { if (!_updating) updateElement(true); });
        Global.addGroupListener(orientationGroup,
                e -> { if (!_updating) updateElement(true); });
        Global.addGroupListener(shiftGroup,
                e -> { if (!_updating) _output.draw(); });

        // HACK: Window.setMinimumSize ignores high DPI scaling
        // For PolygonGridDialog, the listener also resizes the grid.
        addComponentListener(new GridResizeListener());
        setVisible(true);
    }

    private class GridResizeListener extends ResizeListener {
        /**
         * Creates a {@link GridResizeListener} for the {@link PolygonGridDialog}.
         */
        GridResizeListener() {
            super(PolygonGridDialog.this);
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
     * Resizes the specified {@link PolygonGrid} to the specified {@link JPanel}.
     * @param grid the {@link PolygonGrid} to resize
     * @param panel the {@link JPanel} that should contain {@code grid}
     * @return a {@link PointD} containing the left and top offsets by which to
     *         shift {@code grid} so that it is centered within {@code panel}
     * @throws NullPointerException if {@code grid} or {@code panel} is {@code null}
     */
    static PointD sizeGrid(PolygonGrid grid, JPanel panel) {
        /*
         * PolygonGrid default size (1,1) is too small for meaningful calculations.
         * We need multiple rows and columns to see the effect of PolygonGridShift.
         */
        grid.setSize(new SizeI(10, 10));

        // check for available space when using default size
        final double ratioX = panel.getWidth() / grid.worldBounds().width();
        final double ratioY = panel.getHeight() / grid.worldBounds().height();

        // increase grid size accordingly if possible
        grid.setSize(new SizeI(
                Math.max(1, (int) (grid.size().width * ratioX) - 1),
                Math.max(1, (int) (grid.size().height * ratioY) - 1)));

        // calculate left and top drawing offsets
        return new PointD(
                (panel.getWidth() - grid.worldBounds().width()) / 2,
                (panel.getHeight()- grid.worldBounds().height()) / 2);
    }

    private static JRadioButton createRadioButton(String text, String tip,
            int shortcut, ButtonGroup actionGroup, JPanel actionPanel) {

        final JRadioButton button = new JRadioButton(text);
        button.setMnemonic(shortcut);
        button.setToolTipText(tip);

        actionGroup.add(button);
        actionPanel.add(button);
        return button;
    }

    private void clearCursor() {
        _cursorLabel.setText("—");
        _drawCursor = null;
        _output.repaint();
    }

    private PointI cursorToGrid(MouseEvent event) {
        final PointD q = new PointD(event.getX(), event.getY());

        if (q.x < 0 || q.x >= _output.getWidth() ||
            q.y < 0 || q.y >= _output.getHeight())
            return PolygonGrid.INVALID_LOCATION;

        return _grid.worldToGrid(q.subtract(_border));
    }
    
    private PolygonGridShift getGridShift() {

        if (_shiftColumnUp.isSelected()) return PolygonGridShift.COLUMN_UP;
        if (_shiftColumnDown.isSelected()) return PolygonGridShift.COLUMN_DOWN;
        if (_shiftRowLeft.isSelected()) return PolygonGridShift.ROW_LEFT;
        if (_shiftRowRight.isSelected()) return PolygonGridShift.ROW_RIGHT;

        return PolygonGridShift.NONE;
    }
    
    private void setGridShift(PolygonGridShift gridShift) {
        if (gridShift == null)
            gridShift = PolygonGridShift.NONE;

        switch (gridShift) {
            case NONE:        _shiftNone.setSelected(true); break;
            case COLUMN_UP:   _shiftColumnUp.setSelected(true); break;
            case COLUMN_DOWN: _shiftColumnDown.setSelected(true); break;
            case ROW_LEFT:    _shiftRowLeft.setSelected(true); break;
            case ROW_RIGHT:   _shiftRowRight.setSelected(true); break;
        }
    }

    private void onMouseClicked(MouseEvent event) {
        if (_grid == null) return;
        _highlight = null;
        _gridCursor = cursorToGrid(event);

        if (_grid.isValid(_gridCursor)) {
            switch (event.getButton()) {
                case MouseEvent.BUTTON1:
                    _highlight = NeighborHighlight.IMMEDIATE;
                    break;
                case MouseEvent.BUTTON2:
                    _highlight = NeighborHighlight.THREE_STEPS;
                    break;
                case MouseEvent.BUTTON3:
                    _highlight = NeighborHighlight.DISTANCES;
                    break;
            }
        }
        _output.repaint();
    }

    private void onMouseMoved(MouseEvent event) {
        if (_grid == null) return;
        clearCursor();

        final PointI p = cursorToGrid(event);
        if (!_grid.isValid(p)) return;
        
        _cursorLabel.setText(String.format("%d/%d", p.x, p.y));
        final PointD center = _grid.gridToWorld(p);
        _drawCursor = new PointD(_border.x + center.x, _border.y + center.y);

        _output.repaint();
    }

    private void updateControls(int sides, PolygonOrientation orientation) {
        _updating = true;

        final boolean onEdge = (orientation == PolygonOrientation.ON_EDGE);
        if (sides == 4) {
            setGridShift(onEdge ? PolygonGridShift.NONE : PolygonGridShift.ROW_RIGHT);
            _shiftNone.setEnabled(onEdge);
            _shiftColumnUp.setEnabled(!onEdge);
            _shiftColumnDown.setEnabled(!onEdge);
            _vertexNeighbors.setEnabled(true);
        } else {
            setGridShift(onEdge ? PolygonGridShift.COLUMN_DOWN : PolygonGridShift.ROW_RIGHT);
            _shiftNone.setEnabled(false);
            _shiftColumnUp.setEnabled(onEdge);
            _shiftColumnDown.setEnabled(onEdge);
            _vertexNeighbors.setEnabled(false);
        }

        _shiftRowLeft.setEnabled(!onEdge);
        _shiftRowRight.setEnabled(!onEdge);

        _updating = false;
    }

    private void updateElement(boolean draw) {

        // determine side count and integral parameters
        final int sides = (_elementSquare.isSelected() ? 4 : 6);
        final PolygonOrientation orientation = (_elementOnEdge.isSelected() ?
            PolygonOrientation.ON_EDGE : PolygonOrientation.ON_VERTEX);
        final boolean vertexNeighbors = (sides <= 4 && _vertexNeighbors.isSelected());

        // adjust side length based on side count
        final double length = 160.0 / sides;
        _element = new RegularPolygon(length, sides, orientation, vertexNeighbors);
        final RegularPolygon inset = _element.inflate(-3.0);
        _insetVertices = inset.vertices;

        updateControls(sides, orientation);
        if (draw) _output.draw();
    }

    /**
     * Provides the custom drawing {@link JPanel} for the {@link PolygonGridDialog}.
     */
    private class DrawPanel extends JPanel {
        private static final long serialVersionUID = 0L;

        private final Color[] NEIGHBOR_COLORS = {
                Color.GREEN,
                Color.decode("#9ACD32"), // JavaFX YELLOWGREEN
                Color.decode("#90EE90")  // JavaFX LIGHTGREEN
        };

        /**
         * Computes and draws the currently configured {@link PolygonGrid}.
         */
        void draw() {
            if (_element == null) return;
            _highlight = null;
            _grid = new PolygonGrid(_element, getGridShift());

            // resize grid and show actual grid size
            _border = sizeGrid(_grid, this);
            _columnsLabel.setText(Integer.toString(_grid.size().width));
            _rowsLabel.setText(Integer.toString(_grid.size().height));

            repaint();
        }

        /**
         * Invoked by Swing to draw the {@link DrawPanel}.
         * @param g the {@link Graphics2D} context in which to paint
         */
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (_element == null || _grid == null)
                return;

            final Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.BLACK);

            // draw polygon grid outlines
            for (int x = 0; x < _grid.size().width; x++)
                for (int y = 0; y < _grid.size().height; y++) {
                    final PointD world = _grid.gridToWorld(x, y);
                    final PointD center = new PointD(
                            _border.x + world.x, _border.y + world.y);
                    final Path2D poly = Global.drawPolygon(center, _element.vertices);
                    g2.draw(poly);
                }

            // draw inset cursor if present
            if (_drawCursor != null) {
                final Path2D cursor = Global.drawPolygon(_drawCursor, _insetVertices);
                g2.setColor(Color.RED);
                g2.fill(cursor);
            }

            // draw inset highlights if requested
            if (_highlight != null) {
                switch (_highlight) {

                    case IMMEDIATE:
                        // highlight immediate neighbors
                        for (PointI neighbor: _grid.getNeighbors(_gridCursor))
                            drawInset(g2, neighbor, Color.YELLOW, -1);
                        break;

                    case THREE_STEPS:
                        // highlight neighbors within three steps
                        for (PointI neighbor: _grid.getNeighbors(_gridCursor, 3))
                            drawInset(g2, neighbor, Color.YELLOW, -1);
                        break;

                    case DISTANCES:
                        // show color-coded step distances from cursor
                        for (int x = 0; x < _grid.size().width; x++)
                            for (int y = 0; y < _grid.size().height; y++) {
                                final PointI target = new PointI(x, y);
                                if (!_gridCursor.equals(target)) {
                                    final int distance = _grid.getStepDistance(_gridCursor, target);

                                    // use different brushes to highlight distances
                                    final int index = (distance - 1) % NEIGHBOR_COLORS.length;
                                    drawInset(g2, target, NEIGHBOR_COLORS[index], distance);
                                }
                            }
                        break;
                }
            }
        }

        private void drawInset(Graphics2D g2, PointI location, Color color, int distance) {

            final PointD worldLocation = _grid.gridToWorld(location);
            final PointD center = new PointD(
                    _border.x + worldLocation.x,
                    _border.y + worldLocation.y);

            final Path2D shape = Global.drawPolygon(center, _insetVertices);
            g2.setColor(color);
            g2.fill(shape);

            if (distance >= 0) {
                final String text = Integer.toString(distance);
                final Rectangle2D bounds = g2.getFontMetrics().getStringBounds(text, g2);

                g2.setColor(Color.BLACK);
                g2.drawString(text,
                        (float) (center.x - bounds.getWidth() / 2),
                        (float) (center.y + bounds.getHeight() / 3));
            }
        }
    }
}
