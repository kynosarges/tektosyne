package org.kynosarges.tektosyne.demo;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.*;
import java.net.URI;

/**
 * Encapsulates AWT {@link Desktop} actions.
 * The AWT {@link Desktop} facility requires two separate tests before executing each
 * {@link Action} and throws exceptions for unsupported actions. {@link DesktopAction}
 * provides a single test per action, silently does nothing if an attempted action is
 * unsupported, and caches test results for all supported actions at startup.
 *
 * @author Christoph Nahr
 * @version 6.0.0
 */
public final class DesktopAction {

    private final static Desktop DESKTOP;
    private final static boolean[] ACTIONS;

    static {
        final Action[] actions = Action.values();
        ACTIONS = new boolean[actions.length];

        if (Desktop.isDesktopSupported()) {
            DESKTOP = Desktop.getDesktop();
            for (int i = 0; i < actions.length; i++)
                ACTIONS[i] = DESKTOP.isSupported(actions[i]);
        } else
            DESKTOP = null;
    }

    /**
     * Creates a {@link DesktopAction} instance.
     * Private to prevent instantiation.
     */
    private DesktopAction() { }

    /**
     * Indicates whether the {@link Action#BROWSE} action is supported.
     * @return {@code true} if a {@link Desktop} exists and supports
     *         {@link Action#BROWSE}, else {@code false}
     */
    public static boolean canBrowse() {
        return ACTIONS[Action.BROWSE.ordinal()];
    }

    /**
     * Indicates whether the {@link Action#EDIT} action is supported.
     * @return {@code true} if a {@link Desktop} exists and supports
     *         {@link Action#EDIT}, else {@code false}
     */
    public static boolean canEdit() {
        return ACTIONS[Action.EDIT.ordinal()];
    }

    /**
     * Indicates whether the {@link Action#MAIL} action is supported.
     * @return {@code true} if a {@link Desktop} exists and supports
     *         {@link Action#MAIL}, else {@code false}
     */
    public static boolean canMail() {
        return ACTIONS[Action.MAIL.ordinal()];
    }

    /**
     * Indicates whether the {@link Action#OPEN} action is supported.
     * @return {@code true} if a {@link Desktop} exists and supports
     *         {@link Action#OPEN}, else {@code false}
     */
    public static boolean canOpen() {
        return ACTIONS[Action.OPEN.ordinal()];
    }

    /**
     * Indicates whether the {@link Action#PRINT} action is supported.
     * @return {@code true} if a {@link Desktop} exists and supports
     *         {@link Action#PRINT}, else {@code false}
     */
    public static boolean canPrint() {
        return ACTIONS[Action.PRINT.ordinal()];
    }

    /**
     * Launches the default browser to display the specified {@link URI}.
     * Returns {@code false} and does nothing if {@link #canBrowse} fails.
     * See {@link Desktop#browse} for details and possible security exceptions.
     *
     * @param uri the {@link URI} to display in the default browser
     * @return {@code true} if the action was attempted, else {@code false}
     * @throws IOException if the default browser could not be launched
     * @throws NullPointerException if {@code uri} is {@code null}
     */
    public static boolean browse(URI uri) throws IOException {
        if (!canBrowse()) return false;
        DESKTOP.browse(uri);
        return true;
    }

    /**
     * Launches the associated application to edit the specified {@link File}.
     * Returns {@code false} and does nothing if {@link #canEdit} fails.
     * See {@link Desktop#edit} for details and possible security exceptions.
     *
     * @param file the {@link File} to edit with the associated application
     * @return {@code true} if the action was attempted, else {@code false}
     * @throws IOException if the associated application could not be launched
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public static boolean edit(File file) throws IOException {
        if (!canEdit()) return false;
        DESKTOP.edit(file);
        return true;
    }

    /**
     * Launches the default mail client with an optional {@code mailto:} {@link URI}.
     * Returns {@code false} and does nothing if {@link #canMail} fails.
     * See {@link Desktop#mail} for details and possible security exceptions.
     *
     * @param mailtoURI an optional {@code mailto:} {@link URI} to fill message fields
     * @return {@code true} if the action was attempted, else {@code false}
     * @throws IOException if the default mail client could not be launched
     */
    public static boolean mail(URI mailtoURI) throws IOException {
        if (!canMail()) return false;
        if (mailtoURI == null)
            DESKTOP.mail();
        else
            DESKTOP.mail(mailtoURI);
        return true;
    }

    /**
     * Launches the associated application to open the specified {@link File}.
     * Returns {@code false} and does nothing if {@link #canOpen} fails.
     * See {@link Desktop#open} for details and possible security exceptions.
     *
     * @param file the {@link File} to open with the associated application
     * @return {@code true} if the action was attempted, else {@code false}
     * @throws IOException if the associated application could not be launched
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public static boolean open(File file) throws IOException {
        if (!canOpen()) return false;
        DESKTOP.open(file);
        return true;
    }

    /**
     * Launches the associated application to print the specified {@link File}.
     * Returns {@code false} and does nothing if {@link #canPrint} fails.
     * See {@link Desktop#print} for details and possible security exceptions.
     *
     * @param file the {@link File} to print with the associated application
     * @return {@code true} if the action was attempted, else {@code false}
     * @throws IOException if the associated application could not be launched
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public static boolean print(File file) throws IOException {
        if (!canPrint()) return false;
        DESKTOP.print(file);
        return true;
    }
}
