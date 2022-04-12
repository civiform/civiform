package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;

import controllers.applicant.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.Styles;

/**
 * Provides a form for selecting an applicant's preferred language. Note that we cannot use Play's
 * {@link play.i18n.Messages}, since the applicant has no language set yet. Instead, we use English
 * since this is the CiviForm default language.
 */
public class ApplicantInformationView extends BaseHtmlView {

  private final ApplicantLayout layout;

  @Inject
  public ApplicantInformationView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(
      Http.Request request,
      Optional<String> userName,
      Messages messages,
      long applicantId,
      Optional<String> redirectTo) {
    String formAction = routes.ApplicantInformationController.update(applicantId).url();
    String redirectLink =
        redirectTo.orElse(routes.ApplicantProgramsController.index(applicantId).url());
    Tag redirectInput = input().isHidden().attr("value", redirectLink).attr("name", "redirectLink");

    String questionText = messages.at(MessageKey.CONTENT_SELECT_LANGUAGE.getKeyName());
    ContainerTag questionTextDiv =
        div(questionText)
            .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT, ApplicantStyles.QUESTION_TEXT);
    String preferredLanguage = layout.languageSelector.getPreferredLangage(request).code();
    ContainerTag formContent =
        form()
            .withAction(formAction)
            .withMethod(Http.HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(request))
            .with(redirectInput)
            .with(questionTextDiv)
            .with(layout.languageSelector.renderRadios(preferredLanguage));

    String submitText = messages.at(MessageKey.BUTTON_UNTRANSLATED_SUBMIT.getKeyName());
    Tag formSubmit = submitButton(submitText).withClasses(ApplicantStyles.BUTTON_SELECT_LANGUAGE);
    formContent.with(formSubmit);

    HtmlBundle bundle =
        layout
            .getBundle()
            .setTitle(messages.at(MessageKey.CONTENT_APPLICANT_INFORMATION.getKeyName()))
            .addMainStyles(ApplicantStyles.MAIN_APPLICANT_INFO)
            .addMainContent(formContent);
    bundle.addMainContent(
        h1(messages.at(MessageKey.CONTENT_APPLICANT_INFORMATION.getKeyName()))
            .withClasses(Styles.SR_ONLY));

    // We probably don't want the nav bar here (or we need it somewhat different - no dropdown.)
    return layout.renderWithNav(request, userName, messages, bundle);
  }
}
