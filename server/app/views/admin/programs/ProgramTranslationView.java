package views.admin.programs;

import static annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.legend;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.FormTag;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import javax.inject.Inject;
import play.i18n.Langs;
import play.mvc.Http;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.admin.TranslationFormView;
import views.components.FieldWithLabel;
import views.components.ToastMessage;
import views.style.Styles;

/** Renders a list of languages to select from, and a form for updating program information. */
public final class ProgramTranslationView extends TranslationFormView {
  private final AdminLayout layout;
  private final boolean statusTrackingEnabled;

  @Inject
  public ProgramTranslationView(
      AdminLayoutFactory layoutFactory,
      Langs langs,
      @ApplicationStatusTrackingEnabled boolean statusTrackingEnabled) {
    super(langs);
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.statusTrackingEnabled = statusTrackingEnabled;
  }

  public Content render(
      Http.Request request,
      Locale locale,
      ProgramDefinition program,
      Optional<String> localizedName,
      Optional<String> localizedDescription,
      Optional<String> errors) {
    String formAction =
        controllers.admin.routes.AdminProgramTranslationsController.update(
                program.id(), locale.toLanguageTag())
            .url();
    FormTag form =
        renderTranslationForm(
            request,
            locale,
            formAction,
            formFields(program, locale, localizedName, localizedDescription));

    String title = String.format("Manage program translations: %s", program.adminName());

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(renderHeader(title), renderLanguageLinks(program.id(), locale), form);

    errors.ifPresent(s -> htmlBundle.addToastMessages(ToastMessage.error(s).setDismissible(false)));

    return layout.renderCentered(htmlBundle);
  }

  @Override
  protected String languageLinkDestination(long programId, Locale locale) {
    return routes.AdminProgramTranslationsController.edit(programId, locale.toLanguageTag()).url();
  }

  private ImmutableList<FieldsetTag> formFields(
      ProgramDefinition program,
      Locale locale,
      Optional<String> localizedName,
      Optional<String> localizedDescription) {
    ImmutableList.Builder<FieldsetTag> result =
        ImmutableList.<FieldsetTag>builder()
            .add(
                fieldset()
                    .withClasses(Styles.MY_4, Styles.PT_1, Styles.PB_2, Styles.PX_2, Styles.BORDER)
                    .with(
                        legend("Program details (visible to applicants)"),
                        FieldWithLabel.input()
                            .setId("localize-display-name")
                            .setFieldName("displayName")
                            .setLabelText("Program name")
                            .setValue(localizedName)
                            .getInputTag(),
                        FieldWithLabel.input()
                            .setId("localize-display-description")
                            .setFieldName("displayDescription")
                            .setLabelText("Program description")
                            .setValue(localizedDescription)
                            .getInputTag()));
    if (statusTrackingEnabled) {
      // TODO(#2752): Use real statuses from the program.
      ImmutableList<ApplicationStatus> statusesWithEmail =
          ImmutableList.of(
              ApplicationStatus.create("Approved", "Some email content"),
              ApplicationStatus.create("Needs more information", "Other email content"));
      for (ApplicationStatus s : statusesWithEmail) {
        result.add(
            fieldset()
                .withClasses(Styles.MY_4, Styles.PT_1, Styles.PB_2, Styles.PX_2, Styles.BORDER)
                .with(
                    legend(String.format("Application status: %s", s.statusName())),
                    FieldWithLabel.input()
                        .setLabelText("Status name")
                        .setScreenReaderText("Status name")
                        .setValue(s.statusName())
                        .getInputTag(),
                    FieldWithLabel.textArea()
                        .setLabelText("Email content")
                        .setScreenReaderText("Email content")
                        .setValue(s.emailContent())
                        .setRows(OptionalLong.of(8))
                        .getTextareaTag()));
      }
    }
    return result.build();
  }

  // TODO(#2752): Use a domain-specific representation of an ApplicationStatus
  // rather than an auto-value.
  @AutoValue
  abstract static class ApplicationStatus {

    static ApplicationStatus create(String statusName, String emailContent) {
      return new AutoValue_ProgramTranslationView_ApplicationStatus(statusName, emailContent);
    }

    abstract String statusName();

    abstract String emailContent();
  }
}
