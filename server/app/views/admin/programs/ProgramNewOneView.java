package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import forms.ProgramForm;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import models.ProgramNotificationPreference;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import repository.AccountRepository;
import repository.CategoryRepository;
import services.settings.SettingsManifest;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.Modal;
import views.components.ToastMessage;

/** Renders a page for adding a new program. */
public final class ProgramNewOneView extends ProgramFormBuilder {
  private final AdminLayout layout;

  @Inject
  public ProgramNewOneView(
      AdminLayoutFactory layoutFactory,
      Config configuration,
      SettingsManifest settingsManifest,
      AccountRepository accountRepository,
      CategoryRepository categoryRepository) {
    super(configuration, settingsManifest, accountRepository, categoryRepository);
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
  }

  /** Renders the create form. */
  public Content render(Request request) {
    ProgramForm programForm = new ProgramForm();
    // We set this default here, instead of in the ProgramForm constructor, because otherwise
    // the setting gets re-enabled when the form is reloaded after an error. This is because
    // an unset checkbox value is not included in the POST at all, so there's nothing to
    // override the value the constructor initializes and set it back to an empty array.
    programForm.setNotificationPreferences(ProgramNotificationPreference.getDefaultsForForm());
    return render(request, programForm, Optional.empty(), Optional.empty());
  }

  /**
   * Renders the create form with a toast containing the content of ToastMessage. Fields are
   * pre-populated based on the content of programForm.
   */
  public Content render(Request request, ProgramForm programForm, ToastMessage toastMessage) {
    return render(request, programForm, Optional.of(toastMessage), Optional.empty());
  }

  /**
   * Renders the create form with a modal that confirms whether or not the user wants to change
   * which program is set to be the common intake form. Fields are pre-populated based on the
   * content of programForm.
   */
  public Content renderChangeCommonIntakeConfirmation(
      Request request, ProgramForm programForm, String existingCommonIntakeFormDisplayName) {
    return render(
        request,
        programForm,
        Optional.empty(),
        Optional.of(buildConfirmCommonIntakeChangeModal(existingCommonIntakeFormDisplayName)));
  }

  private Content render(
      Request request,
      ProgramForm programForm,
      Optional<ToastMessage> toastMessage,
      Optional<Modal> modal) {
    String title = "New program information";

    DivTag contentDiv =
        div(
                renderHeader(title),
                buildProgramForm(request, programForm, ProgramEditStatus.CREATION)
                    .with(makeCsrfTokenInputTag(request))
                    .withAction(controllers.admin.routes.AdminProgramController.create().url()))
            .withClasses("mx-4", "my-12", "flex", "flex-col");
    HtmlBundle htmlBundle =
        layout.getBundle(request).setTitle(title).addMainContent(contentDiv).setAddAside(true);
    toastMessage.ifPresent(htmlBundle::addToastMessages);
    modal.ifPresent(htmlBundle::addModals);
    return layout.renderCentered(htmlBundle);
  }
}
