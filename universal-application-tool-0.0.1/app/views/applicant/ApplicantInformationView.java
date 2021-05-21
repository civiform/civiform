package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;

import com.google.common.collect.ImmutableList;
import controllers.applicant.routes;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.AbstractMap;
import java.util.Locale;
import javax.inject.Inject;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.SelectWithLabel;
import views.style.ApplicantStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

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
    this.layout = checkNotNull(layout);
    this.supportedLanguages =
        langs.availables().stream().map(Lang::toLocale).collect(toImmutableList());
  }

  public Content render(
      Http.Request request, String userName, Messages messages, long applicantId) {
    String formAction = routes.ApplicantInformationController.update(applicantId).url();

    ContainerTag questionText =
        div()
            .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT, ApplicantStyles.QUESTION_TEXT)
            .withText(messages.at(MessageKey.CONTENT_SELECT_LANGUAGE.getKeyName()));
    ContainerTag formContent =
        form()
            .withAction(formAction)
            .withMethod(Http.HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(request))
            .with(questionText);

    boolean useRadio = true;
    if (useRadio) {
      formContent.with(selectLanguageRadios());
    } else {
      formContent.with(selectLanguageDropdown());
    }

    formContent.with(submitButton(messages.at(MessageKey.BUTTON_UNTRANSLATED_SUBMIT.getKeyName())));

    HtmlBundle bundle =
        layout.getBundle().setTitle("Applicant information").addMainContent(formContent);
    return layout.renderWithNav(request, userName, messages, bundle);
    // We probably don't want the nav bar here (or we need it somewhat different.)
  }

  private ContainerTag selectLanguageRadios() {
    ContainerTag options = div();
    this.supportedLanguages.stream()
        .forEach(
            locale ->
                options.with(
                    renderRadioOption(formatLabel(locale), locale.toLanguageTag(), false)));
    return options;
  }

  private Tag renderRadioOption(String text, String value, boolean checked) {
    ContainerTag labelTag =
        label()
            .withClasses(
                ReferenceClasses.RADIO_OPTION,
                BaseStyles.RADIO_LABEL,
                checked ? BaseStyles.BORDER_SEATTLE_BLUE : "")
            .with(
                input()
                    .withType("radio")
                    .withName("locale")
                    .withValue(value)
                    .condAttr(checked, Attr.CHECKED, "")
                    .withClasses(
                        StyleUtils.joinStyles(ReferenceClasses.RADIO_INPUT, BaseStyles.RADIO)))
            .withText(text);

    return div().withClasses(Styles.MY_2, Styles.RELATIVE).with(labelTag);
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

    return languageSelect.getContainer();
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
