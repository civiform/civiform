package views;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.text;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.linkedin.urls.Url;
import com.linkedin.urls.detection.UrlDetector;
import com.linkedin.urls.detection.UrlDetectorOptions;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Messages;
import play.mvc.Http;
import services.applicant.ValidationErrorMessage;
import views.html.helper.CSRF;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

/**
 * Base class for all HTML views. Provides stateless convenience methods for generating HTML.
 *
 * <p>All derived view classes should inject the layout class(es) in whose context they'll be
 * rendered.
 */
public abstract class BaseHtmlView {
  private static final Logger LOG = LoggerFactory.getLogger(BaseHtmlView.class);

  public static Tag renderHeader(String headerText, String... additionalClasses) {
    return h1(headerText).withClasses(Styles.MB_4, StyleUtils.joinStyles(additionalClasses));
  }

  public static ContainerTag fieldErrors(
      Messages messages, ImmutableSet<ValidationErrorMessage> errors) {
    return div(each(errors, error -> div(error.getMessage(messages))))
        .withClasses(BaseStyles.FORM_ERROR_TEXT_BASE);
  }

  public static Tag button(String textContents) {
    return TagCreator.button(text(textContents)).withType("button");
  }

  public static Tag button(String id, String textContents) {
    return button(textContents).withId(id);
  }

  protected static Tag submitButton(String textContents) {
    return TagCreator.button(text(textContents)).withType("submit");
  }

  protected static Tag submitButton(String id, String textContents) {
    return submitButton(textContents).withId(id);
  }

  protected static Tag redirectButton(String id, String text, String redirectUrl) {
    return button(id, text)
        .attr("onclick", String.format("window.location = '%s';", redirectUrl))
        .withClasses(Styles.M_2);
  }

  /**
   * Generates a hidden HTML input tag containing a signed CSRF token. The token and tag must be
   * present in all UAT forms.
   */
  protected static Tag makeCsrfTokenInputTag(Http.Request request) {
    return input().isHidden().withValue(getCsrfToken(request)).withName("csrfToken");
  }

  private static String getCsrfToken(Http.Request request) {
    return CSRF.getToken(request.asScala()).value();
  }

  protected String renderDateTime(Instant time) {
    LocalDateTime datetime = LocalDateTime.ofInstant(time, ZoneId.of("America/Los_Angeles"));
    return datetime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd 'at' h:mm a"));
  }

  protected ImmutableList<DomContent> createLinksAndEscapeText(String content) {
    List<Url> urls = new UrlDetector(content, UrlDetectorOptions.Default).detect();
    ImmutableList.Builder<DomContent> contentBuilder = ImmutableList.builder();
    for (Url url : urls) {
      int index = content.indexOf(url.getOriginalUrl());
      // Find where this URL is in the text.
      if (index == -1) {
        LOG.error(
            String.format(
                "Detected URL %s not present in actual content, %s.",
                url.getOriginalUrl(), content));
        continue;
      }
      if (index > 0) {
        // If it's not at the beginning, add the text from before the URL.
        contentBuilder.add(text(content.substring(0, index)));
      }
      // Add the URL.
      contentBuilder.add(
          a().withText(url.getOriginalUrl())
              .withHref(url.getFullUrl())
              .withClasses(Styles.OPACITY_75));
      content = content.substring(index + url.getOriginalUrl().length());
    }
    // If there's content leftover, add it.
    if (!Strings.isNullOrEmpty(content)) {
      contentBuilder.add(text(content));
    }
    return contentBuilder.build();
  }
}
