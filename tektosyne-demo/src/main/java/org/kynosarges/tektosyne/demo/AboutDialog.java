package org.kynosarges.tektosyne.demo;

import java.io.IOException;
import java.net.*;
import java.nio.file.*;

import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;

/**
 * Shows a modal dialog with information about the application.
 * @author Christoph Nahr
 * @version 2.0.0
 */
public class AboutDialog extends Stage {

    private final static String TITLE, VERSION, AUTHOR, DATE;
    private final static String MAIL = "webmaster@kynosarges.org", MAIL_URI;
    private final static String SITE = "kynosarges.org/Tektosyne.html";
    private final static String SITE_URI = "http://kynosarges.org/Tektosyne.html";

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
     */
    public AboutDialog() {
        initOwner(Global.primaryStage());
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.DECORATED);

        final Text title = new Text(TITLE);
        final Font regular = title.getFont();
        title.setFont(Font.font(regular.getFamily(), FontWeight.BOLD, 2 * regular.getSize()));

        final Text version = new Text(String.format("Version %s%n%s", VERSION, DATE));
        version.setTextAlignment(TextAlignment.CENTER);

        final HyperlinkHandler handler = new HyperlinkHandler();
        final Hyperlink author = new Hyperlink(String.format("Copyright \u00a9 %s", AUTHOR));
        if (DesktopAction.canMail()) {
            author.setTooltip(new Tooltip("Send email to author"));
            author.setUnderline(true);
            author.setUserData(MAIL_URI);
            author.setOnAction(handler);
        } else
            author.setTooltip(new Tooltip("(action not supported on your platform)"));

        final Hyperlink website = new Hyperlink(SITE);
        if (DesktopAction.canBrowse()) {
            website.setTooltip(new Tooltip("Visit official website"));
            website.setUnderline(true);
            website.setUserData(SITE_URI);
            website.setOnAction(handler);
        } else
            website.setTooltip(new Tooltip("(action not supported on your platform)"));

        final Hyperlink readme = new Hyperlink("Technical Information");
        if (DesktopAction.canBrowse()) {
            readme.setTooltip(new Tooltip("Show ReadMe & WhatsNew files"));
            readme.setUnderline(true);
            readme.setUserData("ReadMe.html");
            readme.setOnAction(handler);
        } else
            readme.setTooltip(new Tooltip("(action not supported on your platform)"));

        final Button btnOK = new Button("OK");
        btnOK.setCancelButton(true);
        btnOK.setDefaultButton(true);
        btnOK.setPrefWidth(56);
        btnOK.setOnAction(t -> close());

        final VBox box = new VBox(title, version, author, website, readme, btnOK);
        VBox.setMargin(btnOK, new Insets(8, 0, 0, 0));
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8));
        box.setSpacing(4);
        
        setResizable(false);
        setScene(new Scene(box));
        sizeToScene();
        setTitle("About");

        // must set after scene built
        btnOK.requestFocus();
    }

    /**
     * Handles clicks on any {@link Hyperlink} control.
     * Shows a modal dialog if an error occurred.
     */
    private class HyperlinkHandler implements EventHandler<ActionEvent> {
        /**
         * Handles the specified {@link ActionEvent}.
         * @param t the {@link ActionEvent} to handle
         */
        @Override public void handle(ActionEvent t) {
            try {
                final Hyperlink source = (Hyperlink) t.getSource();
                final String target = (String) source.getUserData();

                if (target.startsWith("mailto:")) {
                    final URI uri = new URI(target);
                    DesktopAction.mail(uri);
                } else {
                    final URI uri = (target.contains("://") ? new URI(target) : Paths.get(target).toUri());
                    DesktopAction.browse(uri);
                }
            } catch (IOException | URISyntaxException e) {
                Global.showError(AboutDialog.this, "Failed to navigate to desired address.", e);
            }
        }
    }
}
