package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.legend;

import controllers.applicant.routes;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.LegendTag;
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
      Optional<String> redirectTo,
      boolean isTrustedIntermediary) {
    String formAction = routes.ApplicantInformationController.update(applicantId).url();
    String redirectLink = null;
    if (isTrustedIntermediary) {
      redirectLink =
          redirectTo.orElse(
              controllers.ti.routes.TrustedIntermediaryController.dashboard(
                      /* nameQuery= */ Optional.empty(),
                      /* dateQuery= */ Optional.empty(),
                      /* page= */ Optional.of(1))
                  .url());
    } else {
      redirectLink = redirectTo.orElse(routes.ApplicantProgramsController.index(applicantId).url());
    }
    InputTag redirectInput = input().isHidden().withValue(redirectLink).withName("redirectLink");

    String questionText = messages.at(MessageKey.CONTENT_SELECT_LANGUAGE.getKeyName());
    LegendTag questionTextLegend =
        legend(questionText)
            .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT, ApplicantStyles.QUESTION_TEXT);
    String preferredLanguage = layout.languageSelector.getPreferredLangage(request).code();
    FieldsetTag languageSelectorFieldset =
        fieldset()
            // legend must be a direct child of fieldset for screenreaders to work properly
            .with(questionTextLegend)
            .with(layout.languageSelector.renderRadios(preferredLanguage));
    FormTag formContent =
        form()
            .withAction(formAction)
            .withMethod(Http.HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(request))
            .with(redirectInput)
            .with(languageSelectorFieldset);

    String submitText = messages.at(MessageKey.BUTTON_UNTRANSLATED_SUBMIT.getKeyName());
    ButtonTag formSubmit =
        submitButton(submitText).withClasses(ApplicantStyles.BUTTON_SELECT_LANGUAGE);
    formContent.with(formSubmit);

    // No translation needed since this appears before applicants select their preferred language,
    // so we always use the default.
    String title = "Select language";
    HtmlBundle bundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainStyles(ApplicantStyles.MAIN_APPLICANT_INFO)
            .addMainContent(h1(title).withClasses("sr-only"), formContent);

    // We probably don't want the nav bar here (or we need it somewhat different - no dropdown.)
    return layout.renderWithNav(request, userName, messages, bundle);
  }
}
