package views.admin.programs;

import static annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.span;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.FormTag;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import javax.inject.Inject;
import play.mvc.Http;
import play.twirl.api.Content;
import services.LocalizedStrings;
import services.TranslationHelper;
import services.program.ProgramDefinition;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.admin.TranslationFormView;
import views.components.FieldWithLabel;
import views.components.LinkElement;
import views.components.ToastMessage;

/** Renders a list of languages to select from, and a form for updating program information. */
public final class ProgramTranslationView extends TranslationFormView {
  private final AdminLayout layout;
  private final boolean statusTrackingEnabled;

  @Inject
  public ProgramTranslationView(
      AdminLayoutFactory layoutFactory,
      TranslationHelper translationHelper,
      @ApplicationStatusTrackingEnabled boolean statusTrackingEnabled) {
    super(translationHelper);
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
            request, locale, formAction, formFields(program, localizedName, localizedDescription));

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

  private ImmutableList<DomContent> formFields(
      ProgramDefinition program,
      Optional<String> localizedName,
      Optional<String> localizedDescription) {
    String programDetailsLink =
        controllers.admin.routes.AdminProgramController.edit(program.id()).url();
    ImmutableList.Builder<DomContent> result =
        ImmutableList.<DomContent>builder()
            .add(
                fieldSetForFields(
                    legend()
                        .with(
                            span("Applicant-visible program details"),
                            new LinkElement()
                                .setText("(edit default)")
                                .setHref(programDetailsLink)
                                .setStyles(Styles.ML_2)
                                .asAnchorText()),
                    ImmutableList.of(
                        div()
                            .with(
                                FieldWithLabel.input()
                                    .setFieldName("displayName")
                                    .setLabelText("Program name")
                                    .setValue(localizedName)
                                    .getInputTag(),
                                defaultLocaleTextHint(program.localizedName())),
                        div()
                            .with(
                                FieldWithLabel.input()
                                    .setFieldName("displayDescription")
                                    .setLabelText("Program description")
                                    .setValue(localizedDescription)
                                    .getInputTag(),
                                defaultLocaleTextHint(program.localizedDescription())))));
    if (statusTrackingEnabled) {
      String programStatusesLink =
          controllers.admin.routes.AdminProgramStatusesController.index(program.id()).url();
      // TODO(#2752): Use real statuses from the program.
      ImmutableList<ApplicationStatus> statusesWithEmail =
          ImmutableList.of(
              ApplicationStatus.create("Approved", "Some email content"),
              ApplicationStatus.create("Needs more information", "Other email content"));

      for (ApplicationStatus s : statusesWithEmail) {
        result.add(
            fieldSetForFields(
                legend()
                    .with(
                        span(String.format("Application status: %s", s.statusName())),
                        new LinkElement()
                            .setText("(edit default)")
                            .setHref(programStatusesLink)
                            .setStyles(Styles.ML_2)
                            .asAnchorText()),
                ImmutableList.of(
                    div()
                        .with(
                            FieldWithLabel.input()
                                .setLabelText("Status name")
                                .setScreenReaderText("Status name")
                                .setValue(s.statusName())
                                .getInputTag(),
                            defaultLocaleTextHint(
                                LocalizedStrings.withDefaultValue(s.statusName()))),
                    div()
                        .with(
                            FieldWithLabel.textArea()
                                .setLabelText("Email content")
                                .setScreenReaderText("Email content")
                                .setValue(s.emailContent())
                                .setRows(OptionalLong.of(8))
                                .getTextareaTag(),
                            defaultLocaleTextHint(
                                LocalizedStrings.withDefaultValue(s.emailContent()))))));
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
