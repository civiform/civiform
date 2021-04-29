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
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.components.SelectWithLabel;

/**
 * Provides a form for selecting an applicant's preferred language. Note that we cannot use Play's
 * {@link play.i18n.Messages}, since the applicant has no language set yet. Instead, we use English
 * since this is the CiviForm default language.
 */
public class ApplicantInformationView extends BaseHtmlView {

  private final ApplicantLayout layout;
  private final ImmutableList<Locale> supportedLanguages;

  @Inject
  public ApplicantInformationView(ApplicantLayout layout, Langs langs) {
    this.layout = layout;
    this.supportedLanguages =
        langs.availables().stream().map(Lang::toLocale).collect(toImmutableList());
  }

  public Content render(Http.Request request, Messages messages, long applicantId) {
    String formAction = routes.ApplicantInformationController.update(applicantId).url();
    return layout.render(
        messages,
        form()
            .withAction(formAction)
            .withMethod(Http.HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(request))
            .with(selectLanguageDropdown())
            .with(submitButton("Submit")));
  }

  private ContainerTag selectLanguageDropdown() {
    SelectWithLabel languageSelect =
        new SelectWithLabel()
            .setId("select-language")
            .setFieldName("locale")
            .setOptions(
                // An option consists of the language (localized to that language - for example,
                // this would display 'Español' for es-US), and the value is the ISO code.
                this.supportedLanguages.stream()
                    .map(
                        locale ->
                            new AbstractMap.SimpleEntry<>(
                                formatLabel(locale), locale.toLanguageTag()))
                    .collect(toImmutableList()));

    return div()
        .with(p("Please select your preferred language from the following: "))
        .with(languageSelect.getContainer());
  }

  /**
   * The dropdown option label should be the language name localized to that language - for example,
   * "español" for "es-US". We capitalize the first letter, since some locales do not capitalize
   * languages.
   */
  private String formatLabel(Locale locale) {
    // The default for Java is 中文, but the City of Seattle prefers 繁體中文
    if (locale.equals(Locale.TRADITIONAL_CHINESE)) {
      return "繁體中文";
    }
    String language = locale.getDisplayLanguage(locale);
    return language.substring(0, 1).toUpperCase(locale) + language.substring(1);
  }
}
