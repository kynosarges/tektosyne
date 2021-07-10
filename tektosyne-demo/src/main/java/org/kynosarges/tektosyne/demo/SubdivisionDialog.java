package org.kynosarges.tektosyne.demo;

import java.text.DecimalFormat;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

import org.kynosarges.tektosyne.MathUtils;
import org.kynosarges.tektosyne.geometry.*;
import org.kynosarges.tektosyne.subdivision.*;

/**
 * Provides a dialog for testing the {@link Subdivision} class.
 * Shows a planar subdivision created from a random Voronoi diagram.
 * All half-edges and faces are labeled with their keys.
 * 
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class SubdivisionDialog extends JDialog {
    private static final long serialVersionUID = 0L;

    private final DecimalFormat _doubleFormat = new DecimalFormat("0.00");
    private final DrawPanel _output = new DrawPanel();

    private final JRadioButton _addEdge, _removeEdge, _splitEdge;
    private final JRadioButton _connectVertex, _moveVertex, _removeVertex;

    private final JLabel _nearestEdgeLabel = new JLabel("-1");
    private final JLabel _nearestFaceLabel = new JLabel("-1");
    private final JLabel _nearestDistanceLabel = new JLabel("0");

    // current subdivision & selections
    private Subdivision _division;
    private int _selectedEdge = -1;
    private PointD _selectedVertex = null;

    /**
     * Creates a {@link SubdivisionDialog}.
     * @param owner the {@link Window} that owns the dialog
     */    
    public SubdivisionDialog(Window owner) {
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
            public void mouseClicked(MouseEvent e) {
                onMouseClicked(e);
            }
            @Override
            public void mouseMoved(MouseEvent e) {
                onMouseMoved(e);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                setNearestEdge(null, 0);
                _selectedEdge = -1;
                _selectedVertex = null;
                _output.repaint();
            }
        };
        _output.addMouseListener(mouseListener);
        _output.addMouseMotionListener(mouseListener);

        final JLabel actionLabel = new JLabel("Click Action ");
        final ButtonGroup actionGroup = new ButtonGroup();
        final JPanel actionPanel = new JPanel(new GridLayout(2, 3, 4, 4));

        final GroupLayout.Group actionsH = layout.createSequentialGroup().
                addComponent(actionLabel).
                addComponent(actionPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE);

        final GroupLayout.Group actionsV = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addComponent(actionLabel).
                addComponent(actionPanel);

        _addEdge = createRadioButton("Add Edge",
                "Add edge from highlighted vertex to cursor (Alt+A)",
                KeyEvent.VK_A, actionGroup, actionPanel);
        _removeEdge = createRadioButton("Remove Edge",
                "Remove highlighted edge (Alt+R)",
                KeyEvent.VK_R, actionGroup, actionPanel);
        _splitEdge = createRadioButton("Split Edge",
                "Split highlighted edge in half (Alt+S)",
                KeyEvent.VK_S, actionGroup, actionPanel);
        _connectVertex = createRadioButton("Connect Vertex",
                "Connect highlighted vertex with random vertex, if possible (Alt+O)",
                KeyEvent.VK_O, actionGroup, actionPanel);
        _moveVertex = createRadioButton("Move Vertex",
                "Move highlighted vertex to cursor (Alt+M)",
                KeyEvent.VK_M, actionGroup, actionPanel);
        _removeVertex = createRadioButton("Remove Vertex",
                "Remove highlighted vertex, joining two edges (Alt+V)",
                KeyEvent.VK_V, actionGroup, actionPanel);

        final JButton renumberEdges = new JButton("Renumber Edges");
        renumberEdges.addActionListener(t -> {
            if (_division.renumberEdges())
                _output.draw(_division);
        });
        renumberEdges.setMnemonic(KeyEvent.VK_E);
        renumberEdges.setToolTipText("Remove gaps from sequence of half-edge keys (Alt+E)");

        final JButton renumberFaces = new JButton("Renumber Faces");
        renumberFaces.addActionListener(t -> {
            if (_division.renumberFaces())
                _output.draw(_division);
        });
        renumberFaces.setMnemonic(KeyEvent.VK_F);
        renumberFaces.setToolTipText("Remove gaps from sequence of face keys (Alt+F)");

        final JLabel faceLabel = new JLabel("Face");
        _nearestFaceLabel.setToolTipText("Key of face containing mouse cursor");
        final JLabel edgeLabel = new JLabel("Edge");
        _nearestEdgeLabel.setToolTipText("Key of half-edge nearest to mouse cursor");
        final JLabel distLabel = new JLabel("Distance");
        _nearestDistanceLabel.setToolTipText("Distance from nearest half-edge to mouse cursor");

        final GroupLayout.Group infoH = layout.createSequentialGroup().
                addComponent(renumberEdges).
                addComponent(renumberFaces).
                addComponent(faceLabel).
                addComponent(_nearestFaceLabel, 20, 20, 20).
                addComponent(edgeLabel).
                addComponent(_nearestEdgeLabel, 20, 20, 20).
                addComponent(distLabel).
                addComponent(_nearestDistanceLabel, 40, 40, 40);

        final GroupLayout.Group infoV = layout.createParallelGroup(GroupLayout.Alignment.CENTER, false).
                addComponent(renumberEdges).
                addComponent(renumberFaces).
                addComponent(faceLabel).
                addComponent(_nearestFaceLabel).
                addComponent(edgeLabel).
                addComponent(_nearestEdgeLabel).
                addComponent(distLabel).
                addComponent(_nearestDistanceLabel);

        final JButton newTest = new JButton("New");
        newTest.requestFocus();
        newTest.addActionListener(t -> _output.draw(null));
        newTest.setMnemonic(KeyEvent.VK_N);
        newTest.setToolTipText("Generate new random subdivision (Alt+N)");

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
                addComponent(_output, 360, 360, Short.MAX_VALUE).
                addGroup(controlsV));

        setLocationByPlatform(true);
        setResizable(true);
        pack();
        setTitle("Planar Subdivision Test");
        SwingUtilities.invokeLater(() -> {
            _output.draw(null);
            _addEdge.setSelected(true);
        });

        // HACK: Window.setMinimumSize ignores high DPI scaling
        addComponentListener(new ResizeListener(this));
        setVisible(true);
    }

    /**
     * Draws the specified {@link Subdivision} to the specified {@link Graphics2D} context.
     * @param g2 the {@link Graphics2D} context in which to paint
     * @param division the {@link Subdivision} to draw
     * @param selectedEdge the index of a selected edge, or -1 for none
     * @param selectedVertex the coordinates of a selected vertex, or {@code null} for none
     * @throws NullPointerException if {@code g2} or {@code division} is {@code null}
     */
    static void drawSubdivision(Graphics2D g2, Subdivision division,
            int selectedEdge, PointD selectedVertex) {

        final double fs = g2.getFont().getSize2D();
        final double radius = 2;

        // draw vertices
        for (PointD vertex: division.vertices().keySet()) {
            final Shape circle = Global.drawCircle(vertex, 2 * radius);
            g2.setColor(Color.BLACK);
            g2.fill(circle); g2.draw(circle);

            if (vertex.equals(selectedVertex)) {
                final Shape superCircle = Global.drawCircle(vertex, 12);
                g2.setColor(Color.BLUE);
                g2.draw(superCircle);
            }
        }

        // draw half-edges with keys
        for (Map.Entry<Integer, SubdivisionEdge> entry: division.edges().entrySet()) {
            final LineD line = entry.getValue().toLine();

            final double deltaX = line.end.x - line.start.x;
            final double deltaY = line.end.y - line.start.y;
            final double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

            // slightly offset half-edge toward incident face
            final double offsetX = radius * deltaX / length;
            final double offsetY = radius * deltaY / length;

            final double x0 = line.start.x + offsetX - offsetY;
            final double y0 = line.start.y + offsetX + offsetY;
            final double x1 = line.end.x - offsetX - offsetY;
            final double y1 = line.end.y + offsetX - offsetY;

            // draw hook in direction of half-edge
            final double angle = line.angle() + 0.75 * Math.PI;
            final double hookX0 = x1 - deltaX / 4;
            final double hookY0 = y1 - deltaY / 4;
            final double hookX1 = hookX0 + Math.cos(angle) * 8;
            final double hookY1 = hookY0 + Math.sin(angle) * 8;

            final Path2D path = new Path2D.Double();
            path.moveTo(x0, y0);
            path.lineTo(x1, y1);
            path.moveTo(hookX0, hookY0);
            path.lineTo(hookX1, hookY1);

            if (entry.getKey() == selectedEdge) {
                g2.setColor(Color.BLACK);
                final Stroke stroke = g2.getStroke();
                g2.setStroke(new BasicStroke(2));
                g2.draw(path);
                g2.setStroke(stroke);
            } else {
                g2.setColor(Color.RED);
                g2.draw(path);
            }

            // draw key of current half-edge
            final double centerX = (line.start.x + line.end.x) / 2;
            final double fontSizeX = (entry.getKey() < 10 && deltaY > 0) ? fs / 2 : fs;
            final double textX = centerX - fontSizeX * deltaY / length - fs / 2;

            final double centerY = (line.start.y + line.end.y) / 2;
            final double textY = centerY + fs * deltaX / length + fs / 2.2;

            g2.setColor(Color.BLACK);
            g2.drawString(entry.getKey().toString(), (float) textX, (float) textY);
        }

        // draw keys of bounded faces
        final Font font = g2.getFont();
        g2.setColor(Color.BLACK);
        g2.setFont(font.deriveFont(Font.BOLD));

        for (Map.Entry<Integer, SubdivisionFace> entry: division.faces().entrySet()) {
            if (entry.getKey() == 0) continue;

            final PointD centroid = entry.getValue().outerEdge().cycleCentroid();
            final double x = centroid.x - fs / 2;
            final double y = centroid.y + fs / 2.2;

            g2.drawString(entry.getKey().toString(), (float) x, (float) y);
        }
        g2.setFont(font);
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

    private void onMouseClicked(MouseEvent event) {
        if (_division == null) return;
        final PointD q = new PointD(event.getX(), event.getY());

        if (_addEdge.isSelected()) {
            if (_selectedVertex != null)
                _division.addEdge(_selectedVertex, q);
        }
        else if (_removeEdge.isSelected()) {
            if (_selectedEdge >= 0)
                _division.removeEdge(_selectedEdge);
        }
        else if (_splitEdge.isSelected()) {
            if (_selectedEdge >= 0)
                _division.splitEdge(_selectedEdge);
        }
        else if (_connectVertex.isSelected()) {
            if (_selectedVertex != null) {
                final PointD target = MathUtils.getAny(_division.vertices().keySet());
                _division.addEdge(_selectedVertex, target);
            }
        }
        else if (_moveVertex.isSelected()) {
            if (_selectedVertex != null)
                _division.moveVertex(_selectedVertex, q);
        }
        else if (_removeVertex.isSelected()) {
            if (_selectedVertex != null)
                _division.removeVertex(_selectedVertex);
        }
        else return;

        _selectedEdge = -1;
        _selectedVertex = null;
        _output.draw(_division);
        setNearestEdge(null, 0);
    }

    private void onMouseMoved(MouseEvent event) {
        if (_division == null) return;

        final PointD q = new PointD(event.getX(), event.getY());
        _selectedEdge = -1;
        _selectedVertex = null;

        // show nearest half-edge, if any
        final FindEdgeResult result = _division.findNearestEdge(q);
        if (result.edge != null) {
            setNearestEdge(result.edge, result.distance);
            if (result.distance <= 30)
                _selectedEdge = result.edge.key();
        } else
            setNearestEdge(null, 0);

        // show nearest vertex, if any
        final NavigableSet<PointD> vertices = _division.vertices().navigableKeySet();
        if (!vertices.isEmpty())
            _selectedVertex = ((PointDComparator) vertices.comparator()).findNearest(vertices, q);

        _output.repaint();
    }

    private void setNearestEdge(SubdivisionEdge edge, double distance) {
        _nearestDistanceLabel.setText(_doubleFormat.format(distance));

        if (edge == null) {
            _nearestEdgeLabel.setText("-1");
            _nearestFaceLabel.setText("-1");
        } else {
            _nearestEdgeLabel.setText(Integer.toString(edge.key()));
            _nearestFaceLabel.setText(Integer.toString(edge.face().key()));
        }
    }

    /**
     * Provides the custom drawing {@link JPanel} for the {@link SubdivisionDialog}.
     */
    private class DrawPanel extends JPanel {
        private static final long serialVersionUID = 0L;

        /**
         * Draws the specified {@link Subdivision}.
         * Creates a new {@link Subdivision} if {@code division} is {@code null}.
         *
         * @param division the {@link Subdivision} to draw
         */
        void draw(Subdivision division) {

            // generate new random subdivision if desired
            if (division == null) {
                final PointD offset = new PointD(getWidth() * 0.2, getHeight() * 0.2);
                final SizeD scale = new SizeD(getWidth() * 0.6, getHeight() * 0.6);

                final int count = 4 + Global.RANDOM.nextInt(9);
                final PointD[] points = new PointD[count];

                for (int i = 0; i < points.length; i++)
                    points[i] = new PointD(
                            offset.x + Global.RANDOM.nextDouble() * scale.width,
                            offset.y + Global.RANDOM.nextDouble() * scale.height);

                // outer bounds for Voronoi pseudo-vertices
                final double margin = 3 * getFont().getSize();
                final RectD bounds = new RectD(margin, margin,
                        getWidth() - margin, getHeight() - margin);

                final VoronoiResults results = Voronoi.findAll(points, bounds);
                final VoronoiMap map = new VoronoiMap(results);
                division = map.source();
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
            drawSubdivision(g2, _division, _selectedEdge, _selectedVertex);
        }
    }
}
