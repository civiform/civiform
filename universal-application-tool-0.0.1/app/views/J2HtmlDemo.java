package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.body;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;

import javax.inject.Inject;
import play.mvc.Http.Request;
import play.twirl.api.Content;

public final class J2HtmlDemo extends BaseHtmlView {

  private final ViewUtils viewUtils;
  private final BaseHtmlLayout layout;

  /** Views needing access to stateful dependencies can inject {@link ViewUtils} */
  @Inject
  public J2HtmlDemo(ViewUtils viewUtils, BaseHtmlLayout layout) {
    this.viewUtils = checkNotNull(viewUtils);
    this.layout = checkNotNull(layout);
  }

  /**
   * All view classes should implement a `render` method that has params for whatever state is
   * needed from the internal services.
   */
  public Content render(String greeting, Request request) {
    return layout.htmlContent(
        head(viewUtils.makeLocalJsTag("hello"), layout.getCommonCssTag()),
        body(
            h1(greeting),
            form(
                    makeCsrfTokenInputTag(request),
                    textField("firstName", "What is your first name?"),
                    submitButton("Enter"))
                .withAction("/demo")
                .withMethod("post")
                .withId("demo-id")));
  }
}
