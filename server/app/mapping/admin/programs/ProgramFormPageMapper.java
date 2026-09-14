package mapping.admin.programs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import forms.ProgramForm;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import models.CategoryModel;
import models.DisplayMode;
import models.ProgramNotificationPreference;
import modules.MainModule;
import repository.AccountRepository;
import repository.CategoryRepository;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramFormPageViewModel;

/** Maps data to the ProgramFormPageViewModel for the new/edit program details page. */
public final class ProgramFormPageMapper {

  // The legacy form always rendered exactly five application steps.
  private static final int APPLICATION_STEP_COUNT = 5;

  /** Maps data for creating a new program. */
  public ProgramFormPageViewModel mapNew(
      ProgramForm programForm,
      CategoryRepository categoryRepository,
      AccountRepository accountRepository,
      String defaultConfirmationMessage,
      Optional<String> preScreenerFormDisplayName,
      Optional<String> errorMessage) {
    return commonBuilder(
            programForm,
            ProgramEditStatus.CREATION,
            categoryRepository,
            accountRepository,
            defaultConfirmationMessage,
            preScreenerFormDisplayName,
            errorMessage)
        .editMode(false)
        .slugEditable(true)
        .saveAndContinue(true)
        .build();
  }

  /** Maps data for editing an existing program. */
  public ProgramFormPageViewModel mapEdit(
      ProgramDefinition existingProgram,
      ProgramEditStatus programEditStatus,
      Optional<ProgramForm> programForm,
      String baseUrl,
      CategoryRepository categoryRepository,
      AccountRepository accountRepository,
      String defaultConfirmationMessage,
      Optional<String> preScreenerFormDisplayName,
      Optional<String> errorMessage) {
    // Re-renders after a failed update bind the submitted form; the initial
    // GET builds the form data from the stored program, exactly like the
    // legacy view's two buildProgramForm overloads.
    ProgramForm form = programForm.orElseGet(() -> toProgramForm(existingProgram));

    boolean slugEditable = programEditStatus == ProgramEditStatus.CREATION;
    ProgramType formProgramType = form.getProgramType();

    String slugFieldText = null;
    if (!slugEditable) {
      slugFieldText =
          formProgramType.equals(ProgramType.EXTERNAL)
              ? form.getAdminName()
              : baseUrl
                  + controllers.applicant.routes.ApplicantProgramsController.show(
                          MainModule.SLUGIFIER.slugify(form.getAdminName()))
                      .url();
    }

    // The "Manage questions" link depends on the stored program's type, not
    // the submitted form's.
    ProgramType storedProgramType = existingProgram.programType();
    boolean showManageQuestionsLink =
        storedProgramType.equals(ProgramType.DEFAULT)
            || storedProgramType.equals(ProgramType.PRE_SCREENER_FORM);

    return commonBuilder(
            form,
            programEditStatus,
            categoryRepository,
            accountRepository,
            defaultConfirmationMessage,
            preScreenerFormDisplayName,
            errorMessage)
        .editMode(true)
        .programId(existingProgram.id())
        .programEditStatusName(programEditStatus.name())
        .titleProgramName(existingProgram.localizedName().getDefault())
        .slugEditable(slugEditable)
        .saveAndContinue(
            programEditStatus == ProgramEditStatus.CREATION
                || programEditStatus == ProgramEditStatus.CREATION_EDIT)
        .slugFieldText(slugFieldText)
        .showManageQuestionsLink(showManageQuestionsLink)
        .build();
  }

  /** Populates the fields common to new and edit mode. */
  private ProgramFormPageViewModel.ProgramFormPageViewModelBuilder commonBuilder(
      ProgramForm form,
      ProgramEditStatus programEditStatus,
      CategoryRepository categoryRepository,
      AccountRepository accountRepository,
      String defaultConfirmationMessage,
      Optional<String> preScreenerFormDisplayName,
      Optional<String> errorMessage) {
    ProgramType programType = form.getProgramType();
    boolean isPreScreenerForm = programType.equals(ProgramType.PRE_SCREENER_FORM);
    boolean isExternalProgram = programType.equals(ProgramType.EXTERNAL);
    boolean isDefaultProgram = programType.equals(ProgramType.DEFAULT);

    boolean disableProgramEligibility = isPreScreenerForm || isExternalProgram;
    boolean disableLongDescription = isPreScreenerForm || isExternalProgram;
    boolean disableExternalLink = isDefaultProgram || isPreScreenerForm;
    boolean disableEmailNotifications = isExternalProgram;
    boolean disableApplicationSteps = isPreScreenerForm || isExternalProgram;
    boolean disableLoginOnly = isExternalProgram;
    boolean disableConfirmationMessage = isExternalProgram;

    // When creating a program, program type fields (if visible) are never
    // disabled. When editing:
    //   - the external program field is disabled when the program type is
    //     default or pre-screener form, since a program can be changed to
    //     external only after creation.
    //   - the pre-screener and default program fields are disabled when the
    //     program type is external, since an external program cannot change
    //     type after creation.
    boolean defaultProgramFieldDisabled = false;
    boolean preScreenerFieldDisabled = false;
    boolean externalProgramFieldDisabled = false;
    if (programEditStatus.equals(ProgramEditStatus.EDIT)) {
      switch (programType) {
        case DEFAULT, PRE_SCREENER_FORM -> externalProgramFieldDisabled = true;
        case EXTERNAL -> {
          defaultProgramFieldDisabled = true;
          preScreenerFieldDisabled = true;
        }
      }
    }

    ImmutableList<ProgramFormPageViewModel.CategoryOption> categoryOptions =
        categoryRepository.listCategories().stream()
            .map(
                category ->
                    ProgramFormPageViewModel.CategoryOption.builder()
                        .name(category.getDefaultName())
                        .value(String.valueOf(category.getId()))
                        .checked(
                            form.getCategories().contains(category.getId()) && !isPreScreenerForm)
                        .build())
            .collect(ImmutableList.toImmutableList());

    ImmutableSet<Long> selectedTi = ImmutableSet.copyOf(form.getTiGroups());
    ImmutableList<ProgramFormPageViewModel.TiGroupOption> tiGroups =
        accountRepository.listTrustedIntermediaryGroups().stream()
            .map(
                tiGroup ->
                    ProgramFormPageViewModel.TiGroupOption.builder()
                        .value(tiGroup.id.toString())
                        .name(tiGroup.getName())
                        .checked(selectedTi.contains(tiGroup.id))
                        .build())
            .collect(ImmutableList.toImmutableList());

    ImmutableList.Builder<ProgramFormPageViewModel.ApplicationStepRow> stepsBuilder =
        ImmutableList.builder();
    for (int i = 0; i < APPLICATION_STEP_COUNT; i++) {
      String title = "";
      String description = "";
      if (i < form.getApplicationSteps().size()) {
        Map<String, String> step = form.getApplicationSteps().get(i);
        title = step.get("title");
        description = step.get("description");
      }
      stepsBuilder.add(
          ProgramFormPageViewModel.ApplicationStepRow.builder()
              .index(i)
              .title(title)
              .description(description)
              .required(i == 0 && !disableApplicationSteps)
              .build());
    }

    boolean emailNotificationChecked =
        form.getNotificationPreferences()
                .contains(
                    ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS.getValue())
            && !disableEmailNotifications;

    Optional<String> errorToastMessage = errorMessage.map(message -> "Error: " + message);

    return ProgramFormPageViewModel.builder()
        .adminName(form.getAdminName())
        .adminDescription(form.getAdminDescription())
        .displayName(form.getLocalizedDisplayName())
        .displayDescription(form.getLocalizedDisplayDescription())
        .shortDescription(form.getLocalizedShortDescription())
        .externalLink(form.getExternalLink())
        .confirmationMessage(form.getLocalizedConfirmationMessage())
        .displayMode(form.getDisplayMode())
        .programTypeValue(programType.getValue())
        .emailNotificationChecked(emailNotificationChecked)
        .loginOnlyChecked(form.getLoginOnly())
        .eligibilityGatingChecked(form.getEligibilityIsGating() && !disableProgramEligibility)
        .eligibilityNotGatingChecked(!form.getEligibilityIsGating() && !disableProgramEligibility)
        .isExternalProgram(isExternalProgram)
        .disableProgramEligibility(disableProgramEligibility)
        .disableLongDescription(disableLongDescription)
        .disableExternalLink(disableExternalLink)
        .disableEmailNotifications(disableEmailNotifications)
        .disableApplicationSteps(disableApplicationSteps)
        .disableLoginOnly(disableLoginOnly)
        .disableConfirmationMessage(disableConfirmationMessage)
        .categoriesDisabled(isPreScreenerForm)
        .defaultProgramFieldDisabled(defaultProgramFieldDisabled)
        .preScreenerFieldDisabled(preScreenerFieldDisabled)
        .externalProgramFieldDisabled(externalProgramFieldDisabled)
        .categoryOptions(categoryOptions)
        .tiGroups(tiGroups)
        .tiListVisible(form.getDisplayMode().equals(DisplayMode.SELECT_TI.getValue()))
        .applicationSteps(stepsBuilder.build())
        .defaultConfirmationMessage(defaultConfirmationMessage)
        .preScreenerFormDisplayName(preScreenerFormDisplayName.orElse(null))
        .errorMessage(errorToastMessage)
        .errorToastId(errorToastMessage.isPresent() ? UUID.randomUUID().toString() : null);
  }

  /**
   * Builds form data from a stored program definition, mirroring the legacy view's
   * ProgramDefinition-based buildProgramForm overload.
   */
  private ProgramForm toProgramForm(ProgramDefinition program) {
    ProgramForm form = new ProgramForm();

    form.setAdminName(program.adminName());
    form.setAdminDescription(program.adminDescription());
    form.setLocalizedDisplayName(program.localizedName().getDefault());
    form.setLocalizedDisplayDescription(program.localizedDescription().getDefault());
    form.setLocalizedShortDescription(program.localizedShortDescription().getDefault());
    form.setLocalizedConfirmationMessage(program.localizedConfirmationMessage().getDefault());
    form.setExternalLink(program.externalLink());
    form.setDisplayMode(program.displayMode().getValue());
    form.setNotificationPreferences(
        program.notificationPreferences().stream()
            .map(ProgramNotificationPreference::getValue)
            .collect(ImmutableList.toImmutableList()));
    form.setEligibilityIsGating(program.eligibilityIsGating());
    form.setLoginOnly(program.loginOnly());
    form.setProgramTypeValue(program.programType().getValue());
    form.setTiGroups(ImmutableList.copyOf(program.acls().getTiProgramViewAcls()));
    form.setCategories(
        program.categories().stream()
            .map(CategoryModel::getId)
            .collect(ImmutableList.toImmutableList()));
    form.setApplicationSteps(
        program.applicationSteps().stream()
            .map(
                step ->
                    Map.of(
                        /* k1= */ "title",
                        /* v1= */ step.getTitle().getDefault(),
                        /* k2= */ "description",
                        /* v2= */ step.getDescription().getDefault()))
            .collect(ImmutableList.toImmutableList()));

    return form;
  }
}
