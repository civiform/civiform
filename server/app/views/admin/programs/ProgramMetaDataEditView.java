package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import forms.ProgramForm;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.FormTag;
import java.util.Optional;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import repository.AccountRepository;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.LinkElement;
import views.components.Modal;
import views.components.ToastMessage;

/** Renders a page for editing the name and description of a program. */
public final class ProgramMetaDataEditView extends ProgramFormBuilder {
  private final AdminLayout layout;

  @Inject
  public ProgramMetaDataEditView(
      AdminLayoutFactory layoutFactory,
      Config configuration,
      SettingsManifest settingsManifest,
      AccountRepository accountRepository) {
    super(configuration, settingsManifest, accountRepository);
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
  }

  /** Renders the edit form. Fields are pre-populated based on the content of existingProgram. */
  public Content render(
      Request request, ProgramDefinition existingProgram, boolean isInCreationFlow) {
    return render(
        request,
        existingProgram,
        isInCreationFlow,
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  /**
   * Renders the edit form with a toast containing the content of ToastMessage. Fields are
   * pre-populated based on the content of programForm.
   */
  public Content render(
      Request request,
      ProgramDefinition existingProgram,
      boolean isInCreationFlow,
      ProgramForm programForm,
      ToastMessage message) {
    return render(
        request,
        existingProgram,
        isInCreationFlow,
        Optional.of(programForm),
        Optional.of(message),
        Optional.empty());
  }

  /**
   * Renders the edit form with a modal that confirms whether or not the user wants to change which
   * program is set to be the common intake form. Fields are pre-populated based on the content of
   * programForm.
   */
  public Content renderChangeCommonIntakeConfirmation(
      Request request,
      ProgramDefinition existingProgram,
      boolean isInCreationFlow,
      ProgramForm programForm,
      String existingCommonIntakeFormDisplayName) {
    return render(
        request,
        existingProgram,
        isInCreationFlow,
        Optional.of(programForm),
        Optional.empty(),
        Optional.of(buildConfirmCommonIntakeChangeModal(existingCommonIntakeFormDisplayName)));
  }

  private Content render(
      Request request,
      ProgramDefinition existingProgram,
      boolean isInCreationFlow,
      Optional<ProgramForm> programForm,
      Optional<ToastMessage> toastMessage,
      Optional<Modal> modal) {
    String title = String.format("Edit program: %s", existingProgram.localizedName().getDefault());

    FormTag formTag =
        programForm.isPresent()
            ? buildProgramForm(
                request, programForm.get(), /* editExistingProgram= */ !isInCreationFlow)
            : buildProgramForm(
                request, existingProgram, /* editExistingProgram= */ !isInCreationFlow);

    HtmlBundle htmlBundle =
        layout
            .getBundle(request)
            .setTitle(title)
            .addMainContent(
                div(
                        renderHeader(title),
                        formTag
                            .with(makeCsrfTokenInputTag(request))
                            .with(buildManageQuestionLink(existingProgram.id()))
                            .withAction(
                                controllers.admin.routes.AdminProgramController.update(
                                        existingProgram.id(), isInCreationFlow)
                                    .url()))
                    .withClasses("mx-4", "my-12", "flex", "flex-col"));
    toastMessage.ifPresent(htmlBundle::addToastMessages);
    modal.ifPresent(htmlBundle::addModals);
    return layout.renderCentered(htmlBundle);
  }

  private ATag buildManageQuestionLink(long programId) {
    String manageQuestionLink =
        controllers.admin.routes.AdminProgramBlocksController.index(programId).url();
    return new LinkElement()
        .setId("manage-questions-link")
        .setHref(manageQuestionLink)
        .setText("Manage Questions →")
        .setStyles("mx-4", "float-right")
        .asAnchorText();
  }
}
