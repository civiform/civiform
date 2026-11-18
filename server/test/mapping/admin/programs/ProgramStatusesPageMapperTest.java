package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import forms.admin.ProgramStatusesForm;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import models.ApplicationStep;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import play.data.Form;
import play.data.validation.ValidationError;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.statuses.StatusDefinitions;
import views.admin.programs.ProgramStatusesPageViewModel;
import views.admin.programs.ProgramStatusesPageViewModel.StatusFormModal;
import views.admin.programs.ProgramStatusesPageViewModel.StatusItem;
import views.admin.programs.ProgramStatusesPageViewModel.Toast;

public final class ProgramStatusesPageMapperTest {

  private static final long PROGRAM_ID = 7L;

  private ProgramStatusesPageMapper mapper;
  private Messages messages;
  private ProgramDefinition program;

  @Before
  public void setup() {
    mapper = new ProgramStatusesPageMapper();

    Langs langs = new Langs(new play.api.i18n.DefaultLangs());
    Map<String, Map<String, String>> langMap =
        ImmutableMap.of("en", ImmutableMap.of("toast.errorMessageOutline", "Error: {0}"));
    MessagesApi messagesApi = play.test.Helpers.stubMessagesApi(langMap, langs);
    messages = messagesApi.preferred(ImmutableSet.of(Lang.defaultLang()));

    program =
        ProgramDefinition.builder()
            .setId(PROGRAM_ID)
            .setAdminName("utility-discount")
            .setAdminDescription("")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Utility Discount"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
            .setLocalizedShortDescription(LocalizedStrings.withDefaultValue("short"))
            .setExternalLink("")
            .setDisplayMode(DisplayMode.PUBLIC)
            .setProgramType(ProgramType.DEFAULT)
            .setEligibilityIsGating(true)
            .setLoginOnly(false)
            .setAcls(new ProgramAcls())
            .setCategories(ImmutableList.of())
            .setApplicationSteps(ImmutableList.of(new ApplicationStep("title", "description")))
            .setBridgeDefinitions(ImmutableMap.of())
            .build();
  }

  private static StatusDefinitions.Status status(String text) {
    return StatusDefinitions.Status.builder()
        .setStatusText(text)
        .setLocalizedStatusText(LocalizedStrings.withDefaultValue(text))
        .build();
  }

  private static StatusDefinitions.Status statusWithEmail(String text, String emailBody) {
    return status(text).toBuilder()
        .setLocalizedEmailBodyText(Optional.of(LocalizedStrings.withDefaultValue(emailBody)))
        .build();
  }

  private static StatusDefinitions.Status defaultStatus(String text) {
    return status(text).toBuilder().setDefaultStatus(Optional.of(true)).build();
  }

  /** A submitted form carrying the given values and errors, as Play would bind it. */
  @SuppressWarnings("unchecked")
  private static Form<ProgramStatusesForm> submittedForm(
      String configuredStatusText,
      String statusText,
      String emailBody,
      boolean defaultStatus,
      List<ValidationError> globalErrors,
      List<ValidationError> statusTextErrors) {
    ProgramStatusesForm data = new ProgramStatusesForm();
    data.setConfiguredStatusText(configuredStatusText);
    data.setStatusText(statusText);
    data.setEmailBody(emailBody);
    data.setDefaultStatus(Optional.of(defaultStatus));

    Form<ProgramStatusesForm> form = mock(Form.class);
    when(form.value()).thenReturn(Optional.of(data));
    when(form.globalErrors()).thenReturn(globalErrors);
    when(form.errors(ProgramStatusesForm.STATUS_TEXT_FORM_NAME)).thenReturn(statusTextErrors);
    when(form.errors(ProgramStatusesForm.EMAIL_BODY_FORM_NAME)).thenReturn(ImmutableList.of());
    return form;
  }

  private ProgramStatusesPageViewModel map(
      StatusDefinitions statusDefinitions, Optional<Form<ProgramStatusesForm>> maybeStatusForm) {
    return mapper.map(
        program,
        statusDefinitions,
        maybeStatusForm,
        messages,
        /* hasTranslatableLocales= */ true,
        /* successMessage= */ Optional.empty(),
        /* errorMessage= */ Optional.empty());
  }

  @Test
  public void map_setsProgramDisplayName() {
    ProgramStatusesPageViewModel model =
        map(new StatusDefinitions(), /* maybeStatusForm= */ Optional.empty());

    assertThat(model.getProgramDisplayName()).isEqualTo("Utility Discount");
  }

  @Test
  public void map_setsFormActionUrls() {
    ProgramStatusesPageViewModel model =
        map(new StatusDefinitions(), /* maybeStatusForm= */ Optional.empty());

    assertThat(model.getCreateOrUpdateUrl()).isEqualTo("/admin/programs/7/statuses");
    assertThat(model.getDeleteUrl()).isEqualTo("/admin/programs/7/statuses/delete");
  }

  @Test
  public void map_setsManageTranslationsUrlWhenLocalesAreTranslatable() {
    ProgramStatusesPageViewModel model =
        map(new StatusDefinitions(), /* maybeStatusForm= */ Optional.empty());

    assertThat(model.getManageTranslationsUrl())
        .isEqualTo("/admin/programs/utility-discount/translations/edit");
  }

  @Test
  public void map_omitsManageTranslationsUrlWithoutTranslatableLocales() {
    ProgramStatusesPageViewModel model =
        mapper.map(
            program,
            new StatusDefinitions(),
            /* maybeStatusForm= */ Optional.empty(),
            messages,
            /* hasTranslatableLocales= */ false,
            /* successMessage= */ Optional.empty(),
            /* errorMessage= */ Optional.empty());

    assertThat(model.getManageTranslationsUrl()).isNull();
  }

  @Test
  public void map_setsSuccessAndErrorToasts() {
    ProgramStatusesPageViewModel model =
        mapper.map(
            program,
            new StatusDefinitions(),
            /* maybeStatusForm= */ Optional.empty(),
            messages,
            /* hasTranslatableLocales= */ true,
            /* successMessage= */ Optional.of("Status created"),
            /* errorMessage= */ Optional.of("Something went wrong"));

    assertThat(model.getToasts()).hasSize(2);

    Toast success = model.getToasts().get(0);
    assertThat(success.getMessage()).isEqualTo("Status created");
    assertThat(success.getType()).isEqualTo("SUCCESS");
    assertThat(success.isCanDismiss()).isTrue();
    assertThat(success.getId()).isNotBlank();

    Toast error = model.getToasts().get(1);
    assertThat(error.getMessage()).isEqualTo("Error: Something went wrong");
    assertThat(error.getType()).isEqualTo("ERROR");
    assertThat(error.isCanDismiss()).isFalse();
  }

  @Test
  public void map_noFlash_hasNoToasts() {
    ProgramStatusesPageViewModel model =
        map(new StatusDefinitions(), /* maybeStatusForm= */ Optional.empty());

    assertThat(model.getToasts()).isEmpty();
  }

  @Test
  public void map_setsCurrentDefaultStatusText() {
    StatusDefinitions statuses =
        new StatusDefinitions(ImmutableList.of(status("Approved"), defaultStatus("Rejected")));

    ProgramStatusesPageViewModel model = map(statuses, /* maybeStatusForm= */ Optional.empty());

    assertThat(model.getCurrentDefaultStatusText()).isEqualTo("Rejected");
  }

  @Test
  public void map_noDefaultStatus_currentDefaultStatusTextIsNull() {
    StatusDefinitions statuses = new StatusDefinitions(ImmutableList.of(status("Approved")));

    ProgramStatusesPageViewModel model = map(statuses, /* maybeStatusForm= */ Optional.empty());

    assertThat(model.getCurrentDefaultStatusText()).isNull();
  }

  @Test
  public void map_createModal_isEmptyCreationFormWhenNothingWasSubmitted() {
    ProgramStatusesPageViewModel model =
        map(new StatusDefinitions(), /* maybeStatusForm= */ Optional.empty());

    StatusFormModal modal = model.getCreateStatusModal();
    assertThat(modal.getId()).startsWith("uuid-");
    assertThat(modal.isCreation()).isTrue();
    assertThat(modal.isDisplayOnLoad()).isFalse();
    assertThat(modal.isShowEmailDeletionWarning()).isFalse();
    assertThat(modal.getConfiguredStatusText()).isEmpty();
    assertThat(modal.getStatusText()).isEmpty();
    assertThat(modal.getEmailBody()).isEmpty();
    assertThat(modal.isDefaultStatus()).isFalse();
    assertThat(modal.getGlobalErrors()).isEmpty();
    assertThat(modal.getStatusTextErrors()).isEmpty();
    assertThat(modal.getEmailBodyErrors()).isEmpty();
  }

  @Test
  public void map_rejectedCreation_createModalShowsValuesAndErrorsOnLoad() {
    Form<ProgramStatusesForm> form =
        submittedForm(
            /* configuredStatusText= */ "",
            /* statusText= */ "",
            /* emailBody= */ "An email",
            /* defaultStatus= */ true,
            /* globalErrors= */ ImmutableList.of(),
            /* statusTextErrors= */ ImmutableList.of(
                new ValidationError("statusText", "This field is required.")));

    ProgramStatusesPageViewModel model = map(new StatusDefinitions(), Optional.of(form));

    StatusFormModal modal = model.getCreateStatusModal();
    assertThat(modal.isCreation()).isTrue();
    assertThat(modal.isDisplayOnLoad()).isTrue();
    assertThat(modal.getEmailBody()).isEqualTo("An email");
    assertThat(modal.isDefaultStatus()).isTrue();
    assertThat(modal.getStatusTextErrors()).containsExactly("Error: This field is required.");
    assertThat(modal.getEmailBodyErrors()).isEmpty();
  }

  @Test
  public void map_rejectedForm_formatsGlobalErrorsWithoutOutline() {
    Form<ProgramStatusesForm> form =
        submittedForm(
            /* configuredStatusText= */ "",
            /* statusText= */ "Approved",
            /* emailBody= */ "",
            /* defaultStatus= */ false,
            /* globalErrors= */ ImmutableList.of(new ValidationError("", "Something global")),
            /* statusTextErrors= */ ImmutableList.of());

    ProgramStatusesPageViewModel model = map(new StatusDefinitions(), Optional.of(form));

    assertThat(model.getCreateStatusModal().getGlobalErrors()).containsExactly("Something global");
  }

  @Test
  public void map_statusItems_setsTextEmailAndDefaultFlags() {
    StatusDefinitions statuses =
        new StatusDefinitions(
            ImmutableList.of(
                statusWithEmail("Approved", "You are approved"), defaultStatus("Rejected")));

    ProgramStatusesPageViewModel model = map(statuses, /* maybeStatusForm= */ Optional.empty());

    assertThat(model.getStatusItems()).hasSize(2);

    StatusItem approved = model.getStatusItems().get(0);
    assertThat(approved.getStatusText()).isEqualTo("Approved");
    assertThat(approved.isHasEmail()).isTrue();
    assertThat(approved.isDefaultStatus()).isFalse();
    assertThat(approved.getDeleteModalId()).startsWith("uuid-");

    StatusItem rejected = model.getStatusItems().get(1);
    assertThat(rejected.getStatusText()).isEqualTo("Rejected");
    assertThat(rejected.isHasEmail()).isFalse();
    assertThat(rejected.isDefaultStatus()).isTrue();
  }

  @Test
  public void map_statusItems_editModalIsFilledFromTheStatus() {
    StatusDefinitions statuses =
        new StatusDefinitions(
            ImmutableList.of(
                statusWithEmail("Approved", "You are approved").toBuilder()
                    .setDefaultStatus(Optional.of(true))
                    .build()));

    ProgramStatusesPageViewModel model = map(statuses, /* maybeStatusForm= */ Optional.empty());

    StatusFormModal modal = model.getStatusItems().get(0).getEditModal();
    assertThat(modal.getId()).startsWith("uuid-");
    assertThat(modal.isCreation()).isFalse();
    assertThat(modal.isDisplayOnLoad()).isFalse();
    assertThat(modal.getConfiguredStatusText()).isEqualTo("Approved");
    assertThat(modal.getStatusText()).isEqualTo("Approved");
    assertThat(modal.getEmailBody()).isEqualTo("You are approved");
    assertThat(modal.isDefaultStatus()).isTrue();
    assertThat(modal.getStatusTextErrors()).isEmpty();
  }

  @Test
  public void map_statusItems_emailDeletionWarningOnlyWhenStatusHasEmail() {
    StatusDefinitions statuses =
        new StatusDefinitions(
            ImmutableList.of(statusWithEmail("Approved", "You are approved"), status("Rejected")));

    ProgramStatusesPageViewModel model = map(statuses, /* maybeStatusForm= */ Optional.empty());

    assertThat(model.getStatusItems().get(0).getEditModal().isShowEmailDeletionWarning()).isTrue();
    assertThat(model.getStatusItems().get(1).getEditModal().isShowEmailDeletionWarning()).isFalse();
  }

  @Test
  public void map_rejectedEdit_onlyTheMatchingEditModalShowsErrorsOnLoad() {
    StatusDefinitions statuses =
        new StatusDefinitions(ImmutableList.of(status("Approved"), status("Rejected")));
    Form<ProgramStatusesForm> form =
        submittedForm(
            /* configuredStatusText= */ "rejected",
            /* statusText= */ "Approved",
            /* emailBody= */ "",
            /* defaultStatus= */ false,
            /* globalErrors= */ ImmutableList.of(),
            /* statusTextErrors= */ ImmutableList.of(
                new ValidationError("statusText", "A status with name Approved already exists")));

    ProgramStatusesPageViewModel model = map(statuses, Optional.of(form));

    // The create modal and the non-matching edit modal are untouched.
    assertThat(model.getCreateStatusModal().isDisplayOnLoad()).isFalse();
    StatusFormModal approvedModal = model.getStatusItems().get(0).getEditModal();
    assertThat(approvedModal.isDisplayOnLoad()).isFalse();
    assertThat(approvedModal.getStatusText()).isEqualTo("Approved");

    // The matching status is found case-insensitively and re-renders the submission.
    StatusFormModal rejectedModal = model.getStatusItems().get(1).getEditModal();
    assertThat(rejectedModal.isDisplayOnLoad()).isTrue();
    assertThat(rejectedModal.isCreation()).isFalse();
    assertThat(rejectedModal.getConfiguredStatusText()).isEqualTo("rejected");
    assertThat(rejectedModal.getStatusText()).isEqualTo("Approved");
    assertThat(rejectedModal.getStatusTextErrors())
        .containsExactly("Error: A status with name Approved already exists");
  }

  @Test
  public void map_generatesDistinctModalIds() {
    StatusDefinitions statuses = new StatusDefinitions(ImmutableList.of(status("Approved")));

    ProgramStatusesPageViewModel model = map(statuses, /* maybeStatusForm= */ Optional.empty());

    StatusItem item = model.getStatusItems().get(0);
    assertThat(
            ImmutableSet.of(
                model.getCreateStatusModal().getId(),
                item.getEditModal().getId(),
                item.getDeleteModalId()))
        .hasSize(3);
  }
}
