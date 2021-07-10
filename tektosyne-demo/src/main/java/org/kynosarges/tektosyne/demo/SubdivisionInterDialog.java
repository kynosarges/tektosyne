package org.kynosarges.tektosyne.demo;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.kynosarges.tektosyne.geometry.*;
import org.kynosarges.tektosyne.subdivision.*;

/**
 * Provides a dialog for testing the {@link Subdivision#intersection}
 * algorithm of the {@link Subdivision} class.
 * Intersects an existing planar subdivision with a user-defined diamond or rectangle.
 * All half-edges and faces are labeled with their keys.
 * 
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class SubdivisionInterDialog extends JDialog {
    private static final long serialVersionUID = 0L;

    private final DrawPanel _output = new DrawPanel();

    private final JSpinner _left = new JSpinner();
    private final JSpinner _top = new JSpinner();
    private final JSpinner _width = new JSpinner();
    private final JSpinner _height = new JSpinner();
    
    private final JLabel _currentFaceLabel = new JLabel("-1");
    private final JLabel _previousFaceLabel = new JLabel("-1");
    private final JLabel _intersectFaceLabel = new JLabel("-1");

    // current subdivision & intersection results
    private Subdivision _division;
    private SubdivisionIntersection _intersection;

    /**
     * Creates a {@link SubdivisionInterDialog}.
     * @param owner the {@link Window} that owns the dialog
     */
    public SubdivisionInterDialog(Window owner) {
        super(owner);
        setModal(true);

        final JPanel panel = new JPanel();
        setContentPane(panel);

        final GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        panel.setLayout(layout);

        final JLabel message = new JLabel(
                "<html>Half-edge keys are normal weight, face keys are bold, hooks show orientation.<br>" +
                "Half-edge cycles run clockwise because y-coordinates increase downward.</html>");

        _output.setBackground(Color.WHITE);
        _output.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1, true));

        final MouseAdapter mouseListener = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (_division != null) {
                    final PointD q = new PointD(e.getX(), e.getY());
                    showFace(_division.findFace(q));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                showFace(null);
            }
        };
        _output.addMouseListener(mouseListener);
        _output.addMouseMotionListener(mouseListener);

        final JLabel intersectWithLabel = new JLabel("Intersect with specified");

        final JButton rectangle = new JButton("Rectangle");
        rectangle.addActionListener(t -> onIntersect(false));
        rectangle.setMnemonic(KeyEvent.VK_R);
        rectangle.setToolTipText("Intersect subdivision with specified rectangle (Alt+R)");

        final JButton diamond = new JButton("Diamond");
        diamond.addActionListener(t -> onIntersect(true));
        diamond.setMnemonic(KeyEvent.VK_D);
        diamond.setToolTipText("Intersect subdivision with diamond inscribed in specified rectangle (Alt+D)");

        final JLabel leftLabel = initSpinner(_left,
                new SpinnerNumberModel(0, 0, 300, 10),
                "Left", "Set left border of rectangle (Alt+L)", KeyEvent.VK_L);
        final JLabel topLabel = initSpinner(_top,
                new SpinnerNumberModel(0, 0, 300, 10),
                " Top", "Set top border of rectangle (Alt+T)", KeyEvent.VK_T);
        final JLabel widthLabel = initSpinner(_width,
                new SpinnerNumberModel(100, 10, 400, 10),
                " Width", "Set width of rectangle (Alt+W)", KeyEvent.VK_W);
        final JLabel heightLabel = initSpinner(_height,
                new SpinnerNumberModel(100, 10, 400, 10),
                " Height", "Set height of rectangle (Alt+H)", KeyEvent.VK_H);

        final GroupLayout.Group actionsH = layout.createParallelGroup().
                addGroup(layout.createSequentialGroup().
                        addComponent(intersectWithLabel).
                        addComponent(rectangle).
                        addComponent(diamond)
                ).
                addGroup(layout.createSequentialGroup().
                        addComponent(leftLabel).
                        addComponent(_left, 60, 60, 60).
                        addComponent(topLabel).
                        addComponent(_top, 60, 60, 60).
                        addComponent(widthLabel).
                        addComponent(_width, 60, 60, 60).
                        addComponent(heightLabel).
                        addComponent(_height, 60, 60, 60)
                );

        final GroupLayout.Group actionsV = layout.createSequentialGroup().
                addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                        addComponent(intersectWithLabel).
                        addComponent(rectangle).
                        addComponent(diamond)
                ).
                addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                        addComponent(leftLabel).
                        addComponent(_left).
                        addComponent(topLabel).
                        addComponent(_top).
                        addComponent(widthLabel).
                        addComponent(_width).
                        addComponent(heightLabel).
                        addComponent(_height)
                );

        final JLabel currentLabel = new JLabel("Current Face");
        _currentFaceLabel.setToolTipText("Key of face containing mouse cursor");
        final JLabel previousLabel = new JLabel("Previous Face");
        _previousFaceLabel.setToolTipText("Key of face in previous subdivision overlapping current face");
        final JLabel intersectLabel = new JLabel("Intersecting Face");
        _intersectFaceLabel.setToolTipText("Key of face in intersecting subdivision overlapping current face");

        final GroupLayout.Group infoH = layout.createSequentialGroup().
                addComponent(currentLabel).
                addComponent(_currentFaceLabel, 30, 30, 30).
                addComponent(previousLabel).
                addComponent(_previousFaceLabel, 30, 30, 30).
                addComponent(intersectLabel).
                addComponent(_intersectFaceLabel, 30, 30, 30);

        final GroupLayout.Group infoV = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addComponent(currentLabel).
                addComponent(_currentFaceLabel).
                addComponent(previousLabel).
                addComponent(_previousFaceLabel).
                addComponent(intersectLabel).
                addComponent(_intersectFaceLabel);

        final JButton newTest = new JButton("New");
        newTest.requestFocus();
        newTest.addActionListener(t -> _output.draw(null));
        newTest.setMnemonic(KeyEvent.VK_N);
        newTest.setToolTipText("Create new empty subdivision (Alt+N)");

        final JButton copy = new JButton("Copy");
        copy.addActionListener(t -> Global.copy(this, LineD.class, _division.toLines()));
        copy.setMnemonic(KeyEvent.VK_C);
        copy.setToolTipText("Copy current subdivision to clipboard (Alt+C)");

        final JButton paste = new JButton("Paste");
        paste.addActionListener(t -> {
            final LineD[] lines = Global.paste(this, LineD.class);
            if (lines != null) _output.draw(Subdivision.fromLines(lines, 0));
        });
        paste.setMnemonic(KeyEvent.VK_P);
        paste.setToolTipText("Paste existing subdivision from clipboard (Alt+P)");

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

        final JSeparator separator1 = new JSeparator();
        final JSeparator separator2 = new JSeparator();

        layout.setHorizontalGroup(layout.createParallelGroup().
                addComponent(message).
                addComponent(separator1).
                addGroup(actionsH).
                addComponent(separator2).
                addGroup(infoH).
                addComponent(_output).
                addGroup(controlsH));

        layout.setVerticalGroup(layout.createSequentialGroup().
                addComponent(message).
                addComponent(separator1, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).
                addGroup(actionsV).
                addComponent(separator2, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).
                addGroup(infoV).
                addComponent(_output, 250, 250, Short.MAX_VALUE).
                addGroup(controlsV));


        setLocationByPlatform(true);
        setResizable(true);
        pack();
        setTitle("Subdivision Intersection Test");
        SwingUtilities.invokeLater(() -> _output.draw(null));

        // HACK: Window.setMinimumSize ignores high DPI scaling
        addComponentListener(new ResizeListener(this));
        setVisible(true);
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

    private void onIntersect(boolean isDiamond) {
        final double margin = 2 * getFont().getSize();

        final double x = margin + (Integer) _left.getModel().getValue();
        final double y = margin + (Integer) _top.getModel().getValue();
        final double dx = (Integer) _width.getModel().getValue();
        final double dy = (Integer) _height.getModel().getValue();

        Subdivision rectangle;
        if (!isDiamond)
            rectangle = Subdivision.fromLines(new LineD[] {
                new LineD(x, y, x + dx, y),
                new LineD(x + dx, y, x + dx, y + dy),
                new LineD(x + dx, y + dy, x, y + dy),
                new LineD(x, y + dy, x, y)
            }, 0);
        else
            rectangle = Subdivision.fromLines(new LineD[] {
                new LineD(x + dx/2, y, x + dx, y + dy/2),
                new LineD(x + dx, y + dy/2, x + dx/2, y + dy),
                new LineD(x + dx/2, y + dy, x, y + dy/2),
                new LineD(x, y + dy/2, x + dx/2, y)
            }, 0);

        rectangle.validate();
        _intersection = Subdivision.intersection(_division, rectangle);
        _output.draw(_intersection.division);
    }

    private void showFace(SubdivisionFace face) {
        if (face == null) {
            _currentFaceLabel.setText("-1");
            _previousFaceLabel.setText("-1");
            _intersectFaceLabel.setText("-1");
        } else {
            _currentFaceLabel.setText(Integer.toString(face.key()));
            if (_intersection != null) {
                _previousFaceLabel.setText(Integer.toString(_intersection.faceKeys1[face.key()]));
                _intersectFaceLabel.setText(Integer.toString(_intersection.faceKeys2[face.key()]));
            }
        }
    }

    /**
     * Provides the custom drawing {@link JPanel} for the {@link SubdivisionDialog}.
     */
    private class DrawPanel extends JPanel {
        private static final long serialVersionUID = 0L;

        /**
         * Draws the specified {@link Subdivision}.
         * Creates a new empty {@link Subdivision} if {@code division} is {@code null}.
         *
         * @param division the {@link Subdivision} to draw
         */
        void draw(Subdivision division) {

            // default to empty subdivision
            if (division == null) {
                division = new Subdivision(0);
                _intersection = null;
            }

            _division = division;
            _division.validate();
            repaint();
        }

        /**
         * Invoked by Swing to draw the {@link DrawPanel}.
         * @param g the {@link Graphics2D} context in which to paint
         */
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (_division == null)
                return;

            // draw current subdivision
            final Graphics2D g2 = (Graphics2D) g;
            SubdivisionDialog.drawSubdivision(g2, _division, -1, null);
        }
    }
}
