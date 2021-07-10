package org.kynosarges.tektosyne.demo;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.kynosarges.tektosyne.geometry.*;

/**
 * Provides a dialog for testing the brute force {@link MultiLineIntersection} algorithm.
 * Draws a random set of lines and marks any points of intersection that were found.
 * 
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class LineIntersectionDialog extends JDialog {
    private static final long serialVersionUID = 0L;

    private final JLabel _linesCount = new JLabel("0/0");
    private final JSpinner _tolerance = new JSpinner();

    private LineD[] _lines;
    private MultiLinePoint[] _crossings;

    /**
     * Creates a {@link LineIntersectionDialog}.
     * @param owner the {@link Window} that owns the dialog
     */    
    public LineIntersectionDialog(Window owner) {
        super(owner);
        setModal(true);

        final JPanel panel = new JPanel();
        setContentPane(panel);

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        final JLabel message = new JLabel(
                "<html>Crossings (intersection points) are marked by hollow red circles.<br>" +
                "Use Tolerance to adjust intersection proximity matching.</html>");

        final JLabel linesLabel = new JLabel("Lines/Crossings");
        final DrawPanel output = new DrawPanel();
        output.setBackground(Color.WHITE);
        output.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, true));

        _tolerance.setModel(new SpinnerNumberModel(0.0, 0.0, 10.0, 0.1));
        _tolerance.getModel().addChangeListener(e -> output.draw(_lines));
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
        
        final JButton splitLines = new JButton("Split");
        splitLines.addActionListener(t -> {
            final LineD[] lines = MultiLineIntersection.split(_lines, _crossings);
            output.draw(lines);
        });
        splitLines.setMnemonic(KeyEvent.VK_S);
        splitLines.setToolTipText("Split lines on intersection points (Alt+S)");

        final GroupLayout.Group inputH = layout.createSequentialGroup().
                addComponent(linesLabel).
                addComponent(_linesCount, 40, 40, 40).
                addComponent(splitLines).
                addComponent(toleranceLabel).
                addComponent(_tolerance, 70, 70, 70).
                addComponent(maxTolerance).
                addComponent(minTolerance);

        final GroupLayout.Group inputV = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addComponent(linesLabel).
                addComponent(_linesCount).
                addComponent(splitLines).
                addComponent(toleranceLabel).
                addComponent(_tolerance).
                addComponent(maxTolerance).
                addComponent(minTolerance);

        final JButton newTest = new JButton("New");
        newTest.requestFocus();
        newTest.addActionListener(t -> output.draw(null));
        newTest.setMnemonic(KeyEvent.VK_N);
        newTest.setToolTipText("Generate new random line set (Alt+N)");

        final JButton copy = new JButton("Copy");
        copy.addActionListener(t -> Global.copy(this, LineD.class, _lines));
        copy.setMnemonic(KeyEvent.VK_C);
        copy.setToolTipText("Copy current line set to clipboard (Alt+C)");

        final JButton paste = new JButton("Paste");
        paste.addActionListener(t -> {
            final LineD[] lines = Global.paste(this, LineD.class);
            if (lines != null) output.draw(lines);
        });
        paste.setMnemonic(KeyEvent.VK_P);
        paste.setToolTipText("Paste existing line set from clipboard (Alt+P)");

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
                addComponent(output, 250, 250, Short.MAX_VALUE).
                addGroup(controlsV));

        setLocationByPlatform(true);
        setResizable(true);
        pack();
        setTitle("Line Intersection Test");
        SwingUtilities.invokeLater(() -> output.draw(null));

        // HACK: Window.setMinimumSize ignores high DPI scaling
        addComponentListener(new ResizeListener(this));
        setVisible(true);
    }

    /**
     * Provides the custom drawing {@link JPanel} for the {@link LineIntersectionDialog}.
     */
    private class DrawPanel extends JPanel {
        private static final long serialVersionUID = 0L;

        // diameter of circles marking random points
        private final static double DIAMETER = 8;

        /**
         * Draws all intersections for the specified {@link LineD} array.
         * Creates a new {@link LineD} array if {@code lines} is {@code null}.
         *
         * @param lines the {@link LineD} array whose intersections to draw
         */
        void draw(LineD[] lines) {

            // generate new random line set if desired
            if (lines == null) {
                final int count = 3 + Global.RANDOM.nextInt(18);
                lines = new LineD[count];

                final double width = getWidth() - 2 * DIAMETER;
                final double height = getHeight() - 2 * DIAMETER;
                for (int i = 0; i < lines.length; i++)
                    lines[i] = GeoUtils.randomLine(DIAMETER, DIAMETER, width, height);
            }

            _lines = lines;
            final double epsilon = (Double) _tolerance.getModel().getValue();
            _crossings = (epsilon > 0 ?
                    MultiLineIntersection.findSimple(lines, epsilon) :
                    MultiLineIntersection.findSimple(lines));

            repaint();
        }

        /**
         * Invoked by Swing to draw the {@link DrawPanel}.
         * @param g the {@link Graphics2D} context in which to paint
         */
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (_lines == null || _crossings == null)
                return;

            _linesCount.setText(String.format("%d/%d", _lines.length, _crossings.length));
            final Graphics2D g2 = (Graphics2D) g;

            // draw line set
            g2.setColor(Color.BLACK);
            for (LineD line: _lines)
                g2.draw(Global.drawLine(line));

            // draw intersections as hollow circles
            g2.setColor(Color.RED);
            for (MultiLinePoint crossing: _crossings)
                g2.draw(Global.drawCircle(crossing.shared, DIAMETER));
        }
    }
}
