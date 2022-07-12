package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.legend;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.FormTag;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Langs;
import play.mvc.Http;
import play.twirl.api.Content;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.admin.TranslationFormView;
import views.components.FieldWithLabel;
import views.components.ToastMessage;
import views.style.Styles;

/** Renders a list of languages to select from, and a form for updating program information. */
public class ProgramTranslationView extends TranslationFormView {
  private final AdminLayout layout;

  @Inject
  public ProgramTranslationView(AdminLayoutFactory layoutFactory, Langs langs) {
    super(langs);
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
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
    String formAction =
        controllers.admin.routes.AdminProgramTranslationsController.update(
                programId, locale.toLanguageTag())
            .url();
    FormTag form =
        renderTranslationForm(
            request, locale, formAction, formFields(localizedName, localizedDescription));

    String title = "Manage program translations";

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(renderHeader(title), renderLanguageLinks(programId, locale), form);

    errors.ifPresent(s -> htmlBundle.addToastMessages(ToastMessage.error(s).setDismissible(false)));

    return layout.renderCentered(htmlBundle);
  }

  @Override
  protected String languageLinkDestination(long programId, Locale locale) {
    return routes.AdminProgramTranslationsController.edit(programId, locale.toLanguageTag()).url();
  }

  private ImmutableList<FieldsetTag> formFields(
      Optional<String> localizedName, Optional<String> localizedDescription) {
    return ImmutableList.of(
        fieldset()
            .withClasses(Styles.MY_4, Styles.PT_1, Styles.PB_2, Styles.PX_2, Styles.BORDER)
            .with(
                legend("Program details (visible to applicants)"),
                FieldWithLabel.input()
                    .setId("localize-display-name")
                    .setFieldName("displayName")
                    .setLabelText("Program name")
                    .setScreenReaderText("Program display name")
                    .setValue(localizedName)
                    .getInputTag(),
                FieldWithLabel.input()
                    .setId("localize-display-description")
                    .setFieldName("displayDescription")
                    .setLabelText("Program description")
                    .setScreenReaderText("Program description")
                    .setValue(localizedDescription)
                    .getInputTag()));
  }
}
