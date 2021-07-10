package org.kynosarges.tektosyne.demo;

import java.io.File;
import java.io.IOException;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.kynosarges.tektosyne.geometry.*;
import org.kynosarges.tektosyne.subdivision.PolygonGridMap;

/**
 * Provides a dialog for printing and saving {@link PolygonGrid} instances.
 * Allows the user to print or save a {@link PolygonGrid} of an arbitrary size,
 * based on a user-defined {@link RegularPolygon}.
 * 
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class MakeGridDialog extends JDialog {
    private static final long serialVersionUID = 0L;

    private final DrawPanel _output = new DrawPanel();

    private final JSpinner _elementSize = new JSpinner();
    private final JSpinner _columns = new JSpinner();
    private final JSpinner _rows = new JSpinner();

    private final JRadioButton _elementSquare, _elementHexagon;
    private final JRadioButton _elementOnEdge, _elementOnVertex;
    private final JRadioButton _shiftNone, _shiftColumnUp,
            _shiftColumnDown, _shiftRowLeft, _shiftRowRight;

    private final JLabel _widthLabel = new JLabel("0");
    private final JLabel _heightLabel = new JLabel("0");

    // current element and grid
    private RegularPolygon _element;
    private PolygonGrid _grid;

    // ignore input while updating grid
    private boolean _updating;
    
    /**
     * Creates a {@link MakeGridDialog}.
     * @param owner the {@link Window} that owns the dialog
     */
    public MakeGridDialog(Window owner) {
        super(owner);
        setModal(true);

        final JPanel panel = new JPanel();
        setContentPane(panel);

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        final JLabel message = new JLabel(
                "<html>Specify element size (= polygon side length), number of columns & rows, and grid geometry.<br>" +
                "Output size is total size of resulting grid. Element and output size are in units of 1/72 inch.</html>");

        final JPanel sizePanel = new JPanel();
        sizePanel.setBorder(BorderFactory.createTitledBorder("Grid Size "));
        sizePanel.setLayout(new GridLayout(3, 2, 0, 2));

        final JLabel sizeLabel = initSpinner(_elementSize,
                new SpinnerNumberModel(10, 1, 999, 1),
                "Element", "Set side length of grid elements (Alt+M)", KeyEvent.VK_M);
        final JLabel columnsLabel = initSpinner(_columns,
                new SpinnerNumberModel(10, 1, 999, 1),
                "Columns", "Set number of grid columns (Alt+C)", KeyEvent.VK_C);
        final JLabel rowsLabel = initSpinner(_rows,
                new SpinnerNumberModel(10, 1, 999, 1),
                "Rows", "Set number of grid rows (Alt+W)", KeyEvent.VK_W);

        sizePanel.add(sizeLabel);
        sizePanel.add(_elementSize);
        sizePanel.add(columnsLabel);
        sizePanel.add(_columns);
        sizePanel.add(rowsLabel);
        sizePanel.add(_rows);

        final ButtonGroup shiftGroup = new ButtonGroup();
        final JPanel shiftPanel = new JPanel();
        shiftPanel.setBorder(BorderFactory.createTitledBorder("Grid Shift "));
        shiftPanel.setLayout(new GridLayout(5, 1, 0, 2));

        _shiftNone = createRadioButton("None", "No grid shift (Alt+O)",
                KeyEvent.VK_O, shiftGroup, shiftPanel);
        _shiftColumnUp = createRadioButton("Column Up", "Shift column up (Alt+U)",
                KeyEvent.VK_U, shiftGroup, shiftPanel);
        _shiftColumnDown = createRadioButton("Column Down", "Shift column down (Alt+D)",
                KeyEvent.VK_D, shiftGroup, shiftPanel);
        _shiftRowLeft = createRadioButton("Row Left", "Shift row left (Alt+L)",
                KeyEvent.VK_L, shiftGroup, shiftPanel);
        _shiftRowRight = createRadioButton("Row Right", "Shift row right (Alt+R)",
                KeyEvent.VK_R, shiftGroup, shiftPanel);
        _shiftNone.setSelected(true);

        final GroupLayout.Group gridH = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addComponent(sizePanel).
                addComponent(shiftPanel);

        final GroupLayout.Group gridV = layout.createSequentialGroup().
                addComponent(sizePanel).
                addComponent(shiftPanel);

        final ButtonGroup elementGroup = new ButtonGroup();
        final JPanel elementPanel = new JPanel();
        elementPanel.setBorder(BorderFactory.createTitledBorder("Element "));
        elementPanel.setLayout(new GridLayout(2, 1, 0, 2));

        _elementSquare = createRadioButton("Square", "Square elements (Alt+S)",
                KeyEvent.VK_S, elementGroup, elementPanel);
        _elementHexagon = createRadioButton("Hexagon", "Hexagonal elements (Alt+H)",
                KeyEvent.VK_H, elementGroup, elementPanel);
        _elementSquare.setSelected(true);

        final ButtonGroup orientationGroup = new ButtonGroup();
        final JPanel orientationPanel = new JPanel();
        orientationPanel.setBorder(BorderFactory.createTitledBorder("Orientation "));
        orientationPanel.setLayout(new GridLayout(2, 1, 0, 2));

        _elementOnEdge = createRadioButton("On Edge", "Elements on edge (Alt+E)",
                KeyEvent.VK_E, orientationGroup, orientationPanel);
        _elementOnVertex = createRadioButton("On Vertex", "Elements on vertex (Alt+V)",
                KeyEvent.VK_V, orientationGroup, orientationPanel);
        _elementOnEdge.setSelected(true);
        
        final JPanel outputPanel = new JPanel();
        outputPanel.setBorder(BorderFactory.createTitledBorder("Output Size "));
        outputPanel.setLayout(new GridLayout(2, 2, 0, 2));

        _widthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        _widthLabel.setToolTipText("Current total width of grid");
        outputPanel.add(new JLabel("Width"));
        outputPanel.add(_widthLabel);

        _heightLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        _heightLabel.setToolTipText("Current total height of grid");
        outputPanel.add(new JLabel("Height"));
        outputPanel.add(_heightLabel);

        final GroupLayout.Group elementH = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addComponent(elementPanel).
                addComponent(orientationPanel).
                addComponent(outputPanel);

        final GroupLayout.Group elementV = layout.createSequentialGroup().
                addComponent(elementPanel).
                addComponent(orientationPanel).
                addComponent(outputPanel);

        _output.setBackground(Color.WHITE);
        final JScrollPane outputView = new JScrollPane(_output);
        final int viewSize = 280;

        final GroupLayout.Group inputH = layout.createSequentialGroup().
                addGroup(gridH).
                addGroup(elementH).
                addComponent(outputView, viewSize, viewSize, viewSize);

        final GroupLayout.Group inputV = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addGroup(gridV).
                addGroup(elementV).
                addComponent(outputView, viewSize, viewSize, viewSize);

        final JButton save = new JButton("Save Grid…");
        save.addActionListener(t -> save());
        save.setMnemonic(KeyEvent.VK_A);
        save.setToolTipText("Save grid to PNG file (Alt+A)");

        final JButton print = new JButton("Print Grid…");
        print.addActionListener(t -> print());
        print.setMnemonic(KeyEvent.VK_P);
        print.setToolTipText("Select printer and print grid (Alt+P)");

        final JButton close = CloseAction.createButton(this, panel);

        final GroupLayout.Group controlsH = layout.createSequentialGroup().
                addContainerGap(0, Short.MAX_VALUE).
                addComponent(save).
                addComponent(print).
                addComponent(close).
                addContainerGap(0, Short.MAX_VALUE);

        final GroupLayout.Group controlsV = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addComponent(save).
                addComponent(print).
                addComponent(close);

        layout.setHorizontalGroup(layout.createParallelGroup().
                addComponent(message).
                addGroup(inputH).
                addGroup(controlsH));

        layout.setVerticalGroup(layout.createSequentialGroup().
                addComponent(message).
                addGroup(inputV).
                addGroup(controlsV));

        setLocationByPlatform(true);
        setResizable(false);
        pack();
        setTitle("Save & Print Grid");
        SwingUtilities.invokeLater(_output::draw);

        updateElement();
        _elementSize.addChangeListener(e -> updateElement());
        _columns.addChangeListener(e -> _output.draw());
        _rows.addChangeListener(e -> _output.draw());

        Global.addGroupListener(elementGroup,
                e -> { if (!_updating) updateElement(); });
        Global.addGroupListener(orientationGroup,
                e -> { if (!_updating) updateElement(); });
        Global.addGroupListener(shiftGroup, e -> _output.draw());

        // HACK: Window.setMinimumSize ignores high DPI scaling
        addComponentListener(new ResizeListener(this));
        setVisible(true);
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

    private static JLabel initSpinner(JSpinner spinner,
            SpinnerNumberModel model, String label, String tip, int shortcut) {

        spinner.setModel(model);
        spinner.setToolTipText(tip);

        final JLabel spinnerLabel = new JLabel(label);
        spinnerLabel.setLabelFor(spinner);
        spinnerLabel.setDisplayedMnemonic(shortcut);
        spinnerLabel.setToolTipText(tip);

        return spinnerLabel;
    }

    private void save() {

        final JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose Save File");
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Files", "png"));
        chooser.setSelectedFile(new File("grid.png"));

        final int choice = chooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) return;
        final File file = chooser.getSelectedFile();

        try {
            final BufferedImage image = _output.createImage();
            ImageIO.write(image, "PNG", file);
        } catch (IOException e) {
            Global.showError(this, "An error occurred while saving the image file.", e);
        }
    }

    private void print() {

        final PrinterJob job = PrinterJob.getPrinterJob();
        if (job.getPrintService() == null) {
            Global.showError(this, "Java found no supported printers.", "");
            return;
        }

        job.setPrintable(_output);
        if (!job.printDialog()) return;

        try {
            job.print();
        } catch (PrinterException e) {
            Global.showError(this, "An error occurred while printing the grid.", e);
        }
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

    private void updateControls(int sides, PolygonOrientation orientation) {
        _updating = true;

        final boolean onEdge = (orientation == PolygonOrientation.ON_EDGE);
        if (sides == 4) {
            setGridShift(onEdge ? PolygonGridShift.NONE : PolygonGridShift.ROW_RIGHT);
            _shiftNone.setEnabled(onEdge);
            _shiftColumnUp.setEnabled(!onEdge);
            _shiftColumnDown.setEnabled(!onEdge);
        } else {
            setGridShift(onEdge ? PolygonGridShift.COLUMN_DOWN : PolygonGridShift.ROW_RIGHT);
            _shiftNone.setEnabled(false);
            _shiftColumnUp.setEnabled(onEdge);
            _shiftColumnDown.setEnabled(onEdge);
        }

        _shiftRowLeft.setEnabled(!onEdge);
        _shiftRowRight.setEnabled(!onEdge);

        _updating = false;
    }

    private void updateElement() {

        final int sides = (_elementSquare.isSelected() ? 4 : 6);
        final PolygonOrientation orientation = (_elementOnEdge.isSelected() ?
            PolygonOrientation.ON_EDGE : PolygonOrientation.ON_VERTEX);
        _element = new RegularPolygon((Integer) _elementSize.getValue(), sides, orientation);

        updateControls(sides, orientation);
        _output.draw();
    }

    /**
     * Provides the custom drawing {@link JPanel} for the {@link MakeGridDialog}.
     */
    private class DrawPanel extends JPanel implements Printable {
        private static final long serialVersionUID = 0L;

        private int _pixelWidth, _pixelHeight;

        /**
         * Computes and draws the currently configured {@link PolygonGrid}.
         */
        void draw() {
            if (_element == null) return;

            _grid = new PolygonGrid(_element, getGridShift());
            _grid.setSize(new SizeI((Integer) _columns.getValue(), (Integer) _rows.getValue()));

            // show resulting grid size in pixels, including right edges
            _pixelWidth = (int) Math.round(_grid.worldBounds().width()) + 1;
            _pixelHeight = (int) Math.round(_grid.worldBounds().height()) + 1;

            _widthLabel.setText(Long.toString(_pixelWidth));
            _heightLabel.setText(Long.toString(_pixelHeight));

            // JScrollPane listens to PreferredSize, not Size!
            setPreferredSize(new Dimension(_pixelWidth, _pixelHeight));
            setSize(_pixelWidth, _pixelHeight);
            repaint();
        }

        /**
         * Draws the current {@link PolygonGrid} to a {@link BufferedImage}.
         * The background is set to transparent for this operation.
         *
         * @return the {@link BufferedImage} containing the {@link PolygonGrid}
         */
        BufferedImage createImage() {
            final BufferedImage image = getGraphicsConfiguration().createCompatibleImage(
                    _pixelWidth, _pixelHeight, Transparency.BITMASK);

            final Color oldBackground = getBackground();
            setBackground(new Color(1, 1, 1, 0));
            paint(image.getGraphics());
            setBackground(oldBackground);

            return image;
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

            // uUse Subdivision edges to avoid overlapping polygon edges
            final PolygonGridMap map = new PolygonGridMap(_grid, PointD.EMPTY, 0);
            for (LineD edge: map.source().toLines()) {
                final Shape line = Global.drawLine(edge);
                g2.draw(line);
            }
        }

        /**
         * Prints the page at the specified index into the specified {@link Graphics}
         * context in the specified {@link PageFormat}.
         *
         * @param graphics   the {@link Graphics2D} context into which to print
         * @param pageFormat the size and orientation of the page being drawn
         * @param pageIndex  the zero based index of the page to be drawn
         * @return PAGE_EXISTS if the page is rendered successfully, or
         *         NO_SUCH_PAGE if {@code pageIndex} specifies a non-existent page
         * @throws PrinterException thrown when the print job is terminated.
         */
        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
                throws PrinterException {

            // we only print single pages
            if (pageIndex > 0) return NO_SUCH_PAGE;

            // correct for physical printer margins
            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            // render page and signal success
            paint(graphics);
            return PAGE_EXISTS;
        }
    }
}
