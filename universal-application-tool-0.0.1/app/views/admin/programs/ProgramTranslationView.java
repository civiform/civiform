package views.admin.programs;

import static j2html.TagCreator.form;

import controllers.admin.routes;
import j2html.tags.ContainerTag;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Langs;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizationUtils;
import views.TranslationFormView;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.ToastMessage;

/** Renders a list of languages to select from, and a form for updating program information. */
public class ProgramTranslationView extends TranslationFormView {
  private final AdminLayout layout;

  @Inject
  public ProgramTranslationView(AdminLayout layout, Langs langs) {
    super(langs);
    this.layout = layout;
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
        renderHeader("Manage Translations"), renderLanguageLinks(programId, locale), form);
  }

  @Override
  protected String languageLinkDestination(long programId, Locale locale) {
    return routes.AdminProgramTranslationsController.edit(programId, locale.toLanguageTag()).url();
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
