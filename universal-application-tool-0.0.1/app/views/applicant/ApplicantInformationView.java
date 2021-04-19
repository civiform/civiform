package views.applicant;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.p;

import com.google.common.collect.ImmutableList;
import controllers.applicant.routes;
import j2html.tags.ContainerTag;
import java.util.AbstractMap;
import java.util.Locale;
import javax.inject.Inject;
import play.i18n.Lang;
import play.i18n.Langs;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.components.SelectWithLabel;

public class ApplicantInformationView extends BaseHtmlView {

  private final ApplicantLayout layout;
  private final ImmutableList<Locale> supportedLanguages;

  @Inject
  public ApplicantInformationView(ApplicantLayout layout, Langs langs) {
    this.layout = layout;
    this.supportedLanguages =
        langs.availables().stream().map(Lang::toLocale).collect(toImmutableList());
  }

  public Content render(Http.Request request, long applicantId) {
    String formAction = routes.ApplicantProgramsController.index(applicantId).url();
    return layout.render(
        form()
            .withAction(formAction)
            .withMethod(Http.HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(request))
            .with(selectLanguageDropdown())
            .with(submitButton("Submit")));
  }

  private ContainerTag selectLanguageDropdown() {
    SelectWithLabel languageSelect = new SelectWithLabel();
    languageSelect.setId("select-language");

    // An option consists of the language (localized to that language - for example, this would
    // display 'Español' for es-US), and the value is the ISO code.
    languageSelect.setOptions(
        this.supportedLanguages.stream()
            .map(
                locale ->
                    new AbstractMap.SimpleEntry<>(
                        locale.getDisplayLanguage(locale), locale.toLanguageTag()))
            .collect(toImmutableList()));

    return div()
        .with(p("Please select your preferred language from the following: "))
        .with(languageSelect.getContainer());
  }
}
