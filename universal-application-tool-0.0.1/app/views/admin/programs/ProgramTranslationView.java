package views.admin.programs;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import j2html.tags.ContainerTag;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Lang;
import play.i18n.Langs;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizationUtils;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.Styles;

/** Renders a list of languages to select from, and a form for updating program information. */
public class ProgramTranslationView extends BaseHtmlView {
  private final AdminLayout layout;
  private final ImmutableList<Locale> supportedLanguages;

  @Inject
  public ProgramTranslationView(AdminLayout layout, Langs langs) {
    this.layout = layout;
    this.supportedLanguages =
        langs.availables().stream().map(Lang::toLocale).collect(toImmutableList());
  }

  public Content render(
      Http.Request request,
      Locale locale,
      long programId,
      String localizedName,
      String localizedDescription,
      Optional<String> errors) {
    return render(
        request,
        locale,
        programId,
        Optional.of(localizedName),
        Optional.of(localizedDescription),
        errors);
  }

  public Content render(
      Http.Request request,
      Locale locale,
      long programId,
      Optional<String> localizedName,
      Optional<String> localizedDescription,
      Optional<String> errors) {
    ContainerTag form =
        renderTranslationForm(request, locale, programId, localizedName, localizedDescription);
    errors.ifPresent(s -> form.with(ToastMessage.error(s).setDismissible(false).getContainerTag()));
    return layout.render(
        renderHeader("Manage Program Translations"), renderLanguageLinks(programId, locale), form);
  }

  /** Render a list of languages, with the currently selected language underlined. */
  private ContainerTag renderLanguageLinks(long programId, Locale currentlySelected) {
    return div()
        .withClasses(Styles.M_2)
        .with(
            each(
                supportedLanguages,
                language ->
                    renderLanguageLink(programId, language, language.equals(currentlySelected))));
  }

  private ContainerTag renderLanguageLink(
      long programId, Locale locale, boolean isCurrentlySelected) {
    LinkElement link =
        new LinkElement()
            .setStyles("language-link", Styles.M_2)
            .setHref(
                routes.AdminProgramTranslationsController.edit(programId, locale.toLanguageTag())
                    .url())
            .setText(locale.getDisplayLanguage(LocalizationUtils.DEFAULT_LOCALE));

    if (isCurrentlySelected) {
      link.setStyles(Styles.M_2, Styles.BORDER_BLUE_400, Styles.BORDER_B_2);
    }

    return link.asAnchorText();
  }

  private ContainerTag renderTranslationForm(
      Http.Request request,
      Locale locale,
      long programId,
      Optional<String> localizedName,
      Optional<String> localizedDescription) {
    return form()
        .withMethod("POST")
        .with(makeCsrfTokenInputTag(request))
        .withAction(
            controllers.admin.routes.AdminProgramTranslationsController.update(
                    programId, locale.toLanguageTag())
                .url())
        .with(
            FieldWithLabel.input()
                .setId("localize-display-name")
                .setFieldName("displayName")
                .setPlaceholderText("Program display name")
                .setValue(localizedName)
                .getContainer())
        .with(
            FieldWithLabel.input()
                .setId("localize-display-description")
                .setFieldName("displayDescription")
                .setPlaceholderText("Program description")
                .setValue(localizedDescription)
                .getContainer())
        .with(
            submitButton(
                    String.format(
                        "Save %s updates",
                        locale.getDisplayLanguage(LocalizationUtils.DEFAULT_LOCALE)))
                .withId("update-localizations-button"));
  }
}
