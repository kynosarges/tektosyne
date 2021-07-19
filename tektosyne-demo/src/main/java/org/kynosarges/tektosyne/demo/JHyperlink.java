package org.kynosarges.tektosyne.demo;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Provides an {@link AbstractButton} styled as an HTML hyperlink.
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class JHyperlink extends AbstractButton {
    private static final long serialVersionUID = 0L;

    private final Border _focusBorder, _emptyBorder;
    private Object _target;

    /**
     * Creates a {@link JHyperlink}.
     * Creates an {@link AbstractButton} with default behavior,
     * showing as plain text with a {@link Cursor#HAND_CURSOR}.
     */
    public JHyperlink() {
        super();

        _focusBorder = BorderFactory.createDashedBorder(getForeground());
        Insets insets = _focusBorder.getBorderInsets(this);
        _emptyBorder = BorderFactory.createEmptyBorder(
                insets.top, insets.left, insets.bottom, insets.right);
        setBorder(_emptyBorder);

        addFocusListener(new JHyperlinkFocusListener());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setModel(new DefaultButtonModel());
        setUI(new BasicButtonUI());
    }

    /**
     * Gets the target of the {@link JHyperlink}.
     * Provided for convenience. May return {@code null} and defaults to {@code null}.
     * The {@link JHyperlink} class does not use this value.
     *
     * @return the target of the {@link JHyperlink}
     */
    public Object getTarget() {
        return _target;
    }

    /**
     * Sets the target of the {@link JHyperlink}.
     * Provided for convenience. Arbitrary objects and {@code null} are acceptable.
     * The {@link JHyperlink} class does not use this value.
     *
     * @param target the target of the {@link JHyperlink}
     */
    public void setTarget(Object target) {
        _target = target;
    }

    /**
     * Sets the link text of the {@link JHyperlink}.
     * An empty {@code link} or one that starts with "&lt;" is forwarded to {@link #setText}.
     * Otherwise, {@code link} is first wrapped in "&lt;html&gt;&lt;u&gt;…&lt;/u&gt;&lt;/html&gt;".
     *
     * @param link the link of the {@link JHyperlink}
     */
    public void setLink(String link) {
        if (link != null && !link.startsWith("<"))
            setText("<html><u>" + link + "</u></html>");
        else
            setText(link);
    }

    /**
     * Handles keyboard focus changes for the {@link JHyperlink}.
     */
    private class JHyperlinkFocusListener implements FocusListener {
        /**
         * Invoked when a component gains the keyboard focus.
         * @param e the {@link FocusEvent} to be processed
         */
        @Override
        public void focusGained(FocusEvent e) {
            JHyperlink.this.setBorder(_focusBorder);
        }

        /**
         * Invoked when a component loses the keyboard focus.
         * @param e the {@link FocusEvent} to be processed
         */
        @Override
        public void focusLost(FocusEvent e) {
            JHyperlink.this.setBorder(_emptyBorder);
        }
    }
}
