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
import j2html.tags.specialized.PTag;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;

/** renders a info page for applicants trying to access a disabled program via its deep link */
public final class ApplicantDisabledProgramView extends BaseHtmlView {

  private final BaseHtmlLayout layout;

  @Inject
  public ApplicantDisabledProgramView(BaseHtmlLayout layout) {
    this.layout = layout;
  }

  public Content render(Messages messages, Http.Request request) {
    HtmlBundle bundle = layout.getBundle(request);
    bundle.setTitle("Disabled Program");
    bundle.addMainContent(mainContent());
    return layout.render(bundle);
  }

  private DivTag mainContent() {
    // TODO: replace the text with translated messages
    String h1Text = "Program disabled";
    H1Tag headerText = h1().withText(h1Text);
    String pText = "We're sorry, the program you are trying to access has been disabled.";
    PTag contentText = p().withClass("usa-intro").withText(pText);
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
                                        .with(contentText)
                                        .with(div().withClass("margin-y-5").with(button))))));
  }
}
