package org.kynosarges.tektosyne.demo;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Defines a Swing {@link Action} to close a {@link Window}.
 * Intended for a {@link JButton} whose shortcut is the Escape key.
 * This cannot be set directly on a {@link JButton} since the
 * {@link JButton#setMnemonic} methods always imply a modifier key.
 *
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class CloseAction extends AbstractAction {

    private static final long serialVersionUID = 0L;
    private final Window _owner;

    /**
     * Creates a {@link CloseAction}.
     * @param owner the {@link Window} to be closed
     * @throws NullPointerException if {@code owner} is {@code null}
     */
    private CloseAction(Window owner) {
        if (owner == null)
            throw new NullPointerException("owner");

        _owner = owner;
    }

    /**
     * Creates a {@link CloseAction}.
     * @param owner the {@link Window} to be closed
     * @param mapper the {@link JComponent} that maps the shortcut key
     * @return the {@link CloseAction} for {@code owner} and {@code mapper}
     * @throws NullPointerException if {@code owner} or {@code mapper} is {@code null}
     */
    public static CloseAction createFor(Window owner, JComponent mapper) {
        final CloseAction action = new CloseAction(owner);

        action.putValue(Action.NAME, "Close");
        action.putValue(Action.SHORT_DESCRIPTION, "Close dialog (Escape)");

        mapper.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
                put(KeyStroke.getKeyStroke("ESCAPE"), "Close");
        mapper.getActionMap().put("Close", action);

        return action;
    }

    /**
     * Creates a {@link JButton} with an associated {@link CloseAction}.
     * @param owner the {@link Window} to be closed
     * @param mapper the {@link JComponent} that maps the shortcut key
     * @return the {@link JButton} that triggers the {@link CloseAction}
     * @throws NullPointerException if {@code owner} or {@code mapper} is {@code null}
     */
    public static JButton createButton(Window owner, JComponent mapper) {
        return new JButton(createFor(owner, mapper));
    }

    /**
     * Invoked when the {@link CloseAction} occurs.
      * @param e the {@link ActionEvent} to process
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        _owner.dispatchEvent(new WindowEvent(_owner, WindowEvent.WINDOW_CLOSING));
    }
}
