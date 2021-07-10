package org.kynosarges.tektosyne.demo;

import java.io.IOException;
import java.net.*;
import java.nio.file.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Shows a modal dialog with information about the application.
 * @author Christoph Nahr
 * @version 6.3.0
 */
public class AboutDialog {

    private final static String TITLE, VERSION, AUTHOR, DATE;
    private final static String MAIL = "webmaster@kynosarges.org", MAIL_URI;
    private final static String SITE = "kynosarges.org/Tektosyne.html";
    private final static String SITE_URI = "https://kynosarges.org/Tektosyne.html";

    static {
        final Package pack = AboutDialog.class.getPackage();

        TITLE = pack.getSpecificationTitle();
        VERSION = pack.getSpecificationVersion();
        AUTHOR = pack.getSpecificationVendor();
        DATE = pack.getImplementationVersion();

        MAIL_URI = String.format("mailto:%s?subject=%s %s", MAIL, TITLE, VERSION)
                .replace(" ", "%20"); // embedded space in title!
    }

    /**
     * Creates an {@link AboutDialog}.
     * Private to prevent instantiation.
     */
    private AboutDialog() { }

    /**
     * Shows a {@link JOptionPane} with information about the application.
     */
    public static void show() {

        final JLabel title = new JLabel(TITLE);
        final Font regular = title.getFont();
        title.setFont(regular.deriveFont(Font.BOLD, 2 * regular.getSize()));

        final JLabel version = new JLabel("Version " + VERSION);
        final JLabel date = new JLabel(DATE);

        final ActionListener listener = new HyperlinkListener();
        final String unsupported = "(action not supported on your platform)";

        final JHyperlink author = new JHyperlink();
        author.setLink("Copyright \u00a9 " + AUTHOR);
        if (DesktopAction.canMail()) {
            author.setToolTipText("Send email to author");
            author.setTarget(MAIL_URI);
            author.addActionListener(listener);
        } else
            author.setToolTipText(unsupported);

        final JHyperlink website = new JHyperlink();
        website.setLink(SITE);
        if (DesktopAction.canBrowse()) {
            website.setToolTipText("Visit official website");
            website.setTarget(SITE_URI);
            website.addActionListener(listener);
        } else
            website.setToolTipText(unsupported);

        final JHyperlink readme = new JHyperlink();
        readme.setLink("Technical Information");
        if (DesktopAction.canBrowse()) {
            readme.setToolTipText("Show ReadMe & WhatsNew files");
            readme.setTarget("ReadMe.html");
            readme.addActionListener(listener);
        } else
            readme.setToolTipText(unsupported);

        final Box message = Box.createVerticalBox();
        for (JComponent component: new JComponent[] {
                title, version, date, author, website, readme }) {

            // center all info components
            component.setAlignmentX(0.5f);
            message.add(component);
            if (component == date)
                message.add(Box.createRigidArea(new Dimension(0,12)));
        }

        JOptionPane.showMessageDialog(TektosyneDemo.INSTANCE,
                message, "About", JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Handles clicks on any {@link JHyperlink} control.
     * Shows a modal dialog if an error occurred.
     */
    private static class HyperlinkListener implements ActionListener {
        /**
         * Handles the specified {@link ActionEvent}.
         * @param t the {@link ActionEvent} to handle
         */
        @Override
        public void actionPerformed(ActionEvent t) {
            try {
                final JHyperlink source = (JHyperlink) t.getSource();
                final String target = (String) source.getTarget();

                if (target.startsWith("mailto:")) {
                    final URI uri = new URI(target);
                    DesktopAction.mail(uri);
                } else {
                    final URI uri = (target.contains("://") ?
                            new URI(target) : Paths.get(target).toUri());
                    DesktopAction.browse(uri);
                }
            }
            catch (IOException | URISyntaxException e) {
                Global.showError(TektosyneDemo.INSTANCE,
                        "Failed to navigate to desired address.", null, e);
            }
        }
    }
}
