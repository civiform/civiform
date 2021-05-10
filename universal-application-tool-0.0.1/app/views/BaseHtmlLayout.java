package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.document;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;
import java.util.ArrayList;
import java.util.Arrays;
import javax.inject.Inject;
import play.twirl.api.Content;
import views.components.ToastMessage;

/**
 * Base class for all layout classes.
 *
 * <p>A layout class should describe the DOM contents of the head, header, nav, and footer. It
 * should have a `render` method that takes the DOM contents for the main tag.
 */
public class BaseHtmlLayout extends BaseHtmlView {
  private static final String TAILWIND_COMPILED_FILENAME = "tailwind";

  private static final String BANNER_TEXT =
      "Do not enter actual or personal data in this demo site";

  public final ViewUtils viewUtils;

  @Inject
  public BaseHtmlLayout(ViewUtils viewUtils) {
    this.viewUtils = checkNotNull(viewUtils);
  }

  public Content htmlContent(HtmlBundle bundle) {
    // TODO: Need to add a priority to toast messages so that we can specify order.
    ToastMessage privacyBanner =
        ToastMessage.error(BANNER_TEXT).setId("warning-message").setIgnorable(true).setDuration(0);
    bundle.addToastMessages(privacyBanner);

    bundle
      .addStylesheets(TAILWIND_COMPILED_FILENAME)
      .addFooterScripts("toast")
      .addFooterScripts("radio");
    
    return new HtmlResponseContent(bundle.getContent());
  }

  protected static class HtmlResponseContent implements Content {
    private final DomContent[] domContents;

    protected HtmlResponseContent(DomContent... domContents) {
      this.domContents = checkNotNull(domContents);
    }

    @Override
    public String body() {
      return document(new ContainerTag("html").with(domContents));
    }

    @Override
    public String contentType() {
      return "text/html";
    }
  }
}
