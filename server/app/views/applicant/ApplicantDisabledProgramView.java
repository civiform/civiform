package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
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
import services.applicant.ApplicantPersonalInfo;
import views.BaseHtmlView;
import views.HtmlBundle;

/** renders a info page for applicants trying to access a disabled program via its deep link */
public final class ApplicantDisabledProgramView extends BaseHtmlView {

  private final ApplicantLayout layout;

  @Inject
  public ApplicantDisabledProgramView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(
      Messages messages,
      Http.Request request,
      long applicantId,
      ApplicantPersonalInfo personalInfo) {
    HtmlBundle bundle = layout.getBundle(request);
    bundle.setTitle("Disabled Program");
    bundle.addMainContent(mainContent(messages));
    return layout.renderWithNav(request, personalInfo, messages, bundle, applicantId);
  }

  private DivTag mainContent(Messages messages) {
    // TODO: replace the text with translated messages once the text is confirmed by product side
    H1Tag headerText =
        renderHeader(messages.at(MessageKey.TITLE_PROGRAM_NOT_AVAILABLE.getKeyName()));
    PTag contentText =
        p().withClass("usa-intro")
            .withText(messages.at(MessageKey.CONTENT_DISABLED_PROGRAM_INFO.getKeyName()));
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
                                    .withId("visit-home-page-button")
                                    .withText(MessageKey.BUTTON_HOME_PAGE.getKeyName())
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
