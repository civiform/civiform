package views.applicant;

import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.ul;

import controllers.routes;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.H1Tag;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;

/** lala temp doc placeholder */
public final class ApplicantDisabledProgramView extends BaseHtmlView {

  private final BaseHtmlLayout unauthenticatedlayout;

  @Inject
  public ApplicantDisabledProgramView(BaseHtmlLayout unauthenticatedlayout) {
    this.unauthenticatedlayout = unauthenticatedlayout;
  }

  /**
   * For each program in the list, render the program information along with an "Apply" button that
   * redirects the user to that program's application.
   *
   * @param messages the localized {@link Messages} for the current applicant
   * @return HTML content for rendering the list of available programs
   */
  public Content render(Messages messages, Http.Request request) {

    BaseHtmlLayout layout = unauthenticatedlayout;

    HtmlBundle bundle = layout.getBundle(request);
    bundle.setTitle("Disabled Program");

    bundle.addMainContent(mainContent());

    return layout.render(bundle);
  }

  private DivTag mainContent() {

    String h1Text;

    // "Page not found"
    h1Text = "Program disabled";
    H1Tag headerText = h1().withText(h1Text);
    String homeLink = routes.HomeController.index().url();

    DivTag button =
        div()
            .withClass("margin-y-5")
            .with(
                ul().withClass("usa-button-group")
                    .with(
                        li().withClass("usa-button-group__item")
                            .with(
                                a().withClass("usa-button")
                                    .withText("Visit" + " HomePage")
                                    .withHref(homeLink))));

    return div()
        .with(
            div()
                .withClass("usa-section")
                .with(
                    div()
                        .withClass("grid-container")
                        .with(
                            div()
                                .withClasses("grid-row", "grid-gap")
                                .with(
                                    div()
                                        .withId("main-content")
                                        .withClasses("usa-prose")
                                        .with(headerText)
                                        .with(
                                            p().withClass("usa-intro")
                                                .withText(
                                                    "We're sorry, the program you are"
                                                        + " trying to access has been"
                                                        + " disabled. "))
                                        .with(div().withClass("margin-y-5").with(button))))));
  }
}
