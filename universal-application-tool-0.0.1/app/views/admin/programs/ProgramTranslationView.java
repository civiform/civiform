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
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.Styles;

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
      Http.Request request, ProgramDefinition program, Locale locale, Optional<String> errors) {
    ContainerTag form = renderTranslationForm(request, program, locale);
    errors.ifPresent(s -> form.with(ToastMessage.error(s).setDismissible(false).getContainerTag()));
    return layout.render(
        renderHeader("Manage Translations"), renderLanguageButtons(program.id(), locale), form);
  }

  private ContainerTag renderLanguageButtons(long programId, Locale currentlySelected) {
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
            .setStyles(Styles.M_2)
            .setHref(
                routes.AdminProgramTranslationsController.edit(programId, locale.toLanguageTag())
                    .url())
            .setText(locale.getDisplayLanguage(Locale.US));

    if (isCurrentlySelected) {
      link.setStyles(Styles.M_2, Styles.BORDER_BLUE_400, Styles.BORDER_B_2);
    }

    return link.asAnchorText();
  }

  private ContainerTag renderTranslationForm(
      Http.Request request, ProgramDefinition program, Locale locale) {
    return form()
        .withMethod("POST")
        .with(makeCsrfTokenInputTag(request))
        .withAction(
            controllers.admin.routes.AdminProgramTranslationsController.update(
                    program.id(), locale.toLanguageTag())
                .url())
        .with(
            FieldWithLabel.input()
                .setFieldName("displayName")
                .setPlaceholderText("Program display name")
                .setValue(program.maybeGetLocalizedName(locale))
                .getContainer())
        .with(
            FieldWithLabel.input()
                .setFieldName("displayDescription")
                .setPlaceholderText("Program description")
                .setValue(program.maybeGetLocalizedDescription(locale))
                .getContainer())
        .with(submitButton("Save"));
  }
}
