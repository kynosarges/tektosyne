package org.kynosarges.tektosyne.demo;

import java.awt.*;
import java.awt.event.*;

/**
 * Prevents resizing a {@link Component} below its initial size.
 * Works around {@link Window#setMinimumSize(Dimension)} not respecting high DPI scaling.
 *
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class ResizeListener extends ComponentAdapter {

    private final Component _component;
    private final int _width, _height;

    /**
     * Creates a {@link ResizeListener} for the specified {@link Component}.
     * The minimum width and height are set to the current width and height
     * of the specified {@code component}.
     *
     * @param component the {@link Component} whose resize events to track
     * @throws NullPointerException if {@code component} is {@code null}
     */
    public ResizeListener(Component component) {
        _component = component;
        _width = component.getWidth();
        _height = component.getHeight();
    }

    /**
     * Creates a {@link ResizeListener} for the specified {@link Component}
     * with the specified minimum width and height.
     *
     * @param component the {@link Component} whose resize events to track
     * @param width the minimum width for {@code component}
     * @param height the minimum height for {@code component}
     * @throws NullPointerException if {@code component} is {@code null}
     */
    public ResizeListener(Component component, int width, int height) {
        if (component == null)
            throw new NullPointerException("component");

        _component = component;
        _width = width;
        _height = height;
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
        final int width = _component.getWidth();
        final int height = _component.getHeight();
        if (width < _width || height < _height)
            _component.setSize(Math.max(width, _width), Math.max(height, _height));
    }
}
