package views.components;

import static j2html.TagCreator.div;

import j2html.tags.ContainerTag;
import java.util.ArrayList;

/** 
 *  A singleton class that renders an absolutely positioned div on the page. It contains a list of ToastMessages.
 *	The container starts out hidden and is controlled by toast.ts.
 */
public class ToastContainer {
    private static ToastContainer instance = new ToastContainer();

    private static final String BANNER_TEXT = "Do not enter actual or personal data in this demo site";

    private boolean showPrivacyBanner = true;

    private ArrayList<ToastMessage> messages = new ArrayList<ToastMessage>();

    private ToastContainer() {}
    
    public static void addMessage(ToastMessage message) {
        instance.messages.add(message);
    }
    
    public static ContainerTag render() {        
        ContainerTag container = div().withId("toast-container")
          .withClasses("absolute top-0 transform -translate-x-1/2 left-1/2 hidden");
    
        if (instance.showPrivacyBanner) {
            // Add privacy banner. (Remove before launch.)
            ToastMessage privacyBanner = 
                ToastMessage.error(ToastContainer.BANNER_TEXT)
                .setId("warning-message")
                .setDismissible(true).setIgnorable(true).setDuration(0);
                container.with(privacyBanner.getContainer());
        }
    
        for (ToastMessage message: instance.messages) {
            container.with(message.getContainer());
        }
    
        return container;
    }
}