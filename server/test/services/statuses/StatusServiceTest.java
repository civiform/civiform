package services.statuses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import models.ProgramModel;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import repository.ApplicationStatusesRepository;
import repository.ResetPostgres;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.program.LocalizationUpdate;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import support.ProgramBuilder;

public class StatusServiceTest extends ResetPostgres {
  private ApplicationStatusesRepository applicationStatusesRepo;
  private StatusService service;
  private ProgramService ps;

  @Before
  public void setup() {
    applicationStatusesRepo = instanceOf(ApplicationStatusesRepository.class);
    service = instanceOf(StatusService.class);
    ps = instanceOf(ProgramService.class);
  }

  private static final StatusDefinitions.Status APPROVED_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Approved")
          .setLocalizedStatusText(LocalizedStrings.of(Locale.US, "Approved"))
          .setLocalizedEmailBodyText(Optional.of(LocalizedStrings.of(Locale.US, "I'm a US email!")))
          .setDefaultStatus(Optional.of(false))
          .build();

  private static final StatusDefinitions.Status APPROVED_DEFAULT_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Approved")
          .setLocalizedStatusText(LocalizedStrings.of(Locale.US, "Approved"))
          .setLocalizedEmailBodyText(Optional.of(LocalizedStrings.of(Locale.US, "I'm a US email!")))
          .setDefaultStatus(Optional.of(true))
          .build();

  private static final StatusDefinitions.Status REJECTED_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Rejected")
          .setLocalizedStatusText(LocalizedStrings.of(Locale.US, "Rejected"))
          .setLocalizedEmailBodyText(
              Optional.of(LocalizedStrings.of(Locale.US, "I'm a US rejection email!")))
          .setDefaultStatus(Optional.of(false))
          .build();

  private static final StatusDefinitions.Status REJECTED_DEFAULT_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Rejected")
          .setLocalizedStatusText(LocalizedStrings.of(Locale.US, "Rejected"))
          .setLocalizedEmailBodyText(
              Optional.of(LocalizedStrings.of(Locale.US, "I'm a US rejection email!")))
          .setDefaultStatus(Optional.of(true))
          .build();

  @Test
  public void appendStatus() throws Exception {
    // Also tests unsetDefaultStatus

    ProgramModel program = ProgramBuilder.newDraftProgram().build();
    String programName = program.getProgramDefinition().adminName();
    assertThat(program.getStatusDefinitions().getStatuses()).isEmpty();
    assertThat(applicationStatusesRepo.lookupActiveStatusDefinitions(programName).getStatuses())
        .isEmpty();

    final ErrorAnd<StatusDefinitions, CiviFormError> firstResult =
        service.appendStatus(programName, APPROVED_DEFAULT_STATUS);

    assertThat(firstResult.isError()).isFalse();
    assertThat(firstResult.getResult().getStatuses()).containsExactly(APPROVED_DEFAULT_STATUS);
    assertThat(applicationStatusesRepo.lookupActiveStatusDefinitions(programName).getStatuses())
        .containsExactly(APPROVED_DEFAULT_STATUS);

    // Ensure that appending to a non-empty list actually appends.
    ErrorAnd<StatusDefinitions, CiviFormError> secondResult =
        service.appendStatus(programName, REJECTED_DEFAULT_STATUS);

    assertThat(secondResult.isError()).isFalse();
    assertThat(secondResult.getResult().getStatuses())
        .containsExactly(APPROVED_STATUS, REJECTED_DEFAULT_STATUS);
    assertThat(applicationStatusesRepo.lookupActiveStatusDefinitions(programName).getStatuses())
        .containsExactly(APPROVED_STATUS, REJECTED_DEFAULT_STATUS);
  }

  @Test
  public void appendStatus_programNotFound_throws() throws Exception {
    assertThatThrownBy(() -> service.appendStatus("random_program", APPROVED_STATUS))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("No active status found for program random_program");
  }

  @Test
  public void appendStatus_duplicateStatus_throws() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .build();

    var newApprovedStatus =
        StatusDefinitions.Status.builder()
            .setStatusText(APPROVED_STATUS.statusText())
            .setLocalizedStatusText(LocalizedStrings.withDefaultValue(APPROVED_STATUS.statusText()))
            .setLocalizedEmailBodyText(
                Optional.of(LocalizedStrings.withDefaultValue("A new US email")))
            .build();

    DuplicateStatusException exc =
        Assertions.catchThrowableOfType(
            DuplicateStatusException.class,
            () -> {
              service.appendStatus(program.getProgramDefinition().adminName(), newApprovedStatus);
            });

    assertThat(exc.userFacingMessage()).contains("A status with name Approved already exists");
  }

  @Test
  public void editStatus() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .build();
    String programName = program.getProgramDefinition().adminName();
    assertThat(applicationStatusesRepo.lookupActiveStatusDefinitions(programName).getStatuses())
        .containsExactly(APPROVED_STATUS);

    var editedStatus =
        StatusDefinitions.Status.builder()
            .setStatusText("New status text")
            .setLocalizedStatusText(LocalizedStrings.withDefaultValue("New status text"))
            .setLocalizedEmailBodyText(
                Optional.of(LocalizedStrings.withDefaultValue("A new US email")))
            .build();

    ErrorAnd<StatusDefinitions, CiviFormError> result =
        service.editStatus(
            programName,
            APPROVED_STATUS.statusText(),
            (existing) -> {
              assertThat(existing).isEqualTo(APPROVED_STATUS);
              return editedStatus;
            });
    assertThat(result.isError()).isFalse();
    assertThat(result.getResult().getStatuses()).containsExactly(editedStatus);
    assertThat(applicationStatusesRepo.lookupActiveStatusDefinitions(programName).getStatuses())
        .containsExactly(editedStatus);
  }

  @Test
  public void editStatus_updatedStatusIsDuplicate_throws() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withStatusDefinitions(
                new StatusDefinitions(ImmutableList.of(APPROVED_STATUS, REJECTED_STATUS)))
            .build();

    // We update the "rejected" status entry so that it's text is the same as the
    // "approved" status entry.
    DuplicateStatusException exc =
        Assertions.catchThrowableOfType(
            DuplicateStatusException.class,
            () ->
                service.editStatus(
                    program.getProgramDefinition().adminName(),
                    REJECTED_STATUS.statusText(),
                    (existingStatus) -> {
                      return StatusDefinitions.Status.builder()
                          .setStatusText(APPROVED_STATUS.statusText())
                          .setLocalizedStatusText(
                              LocalizedStrings.withDefaultValue("New status text"))
                          .setLocalizedEmailBodyText(
                              Optional.of(LocalizedStrings.withDefaultValue("A new US email")))
                          .build();
                    }));
    assertThat(exc.userFacingMessage()).contains("A status with name Approved already exists");
  }

  @Test
  public void editStatus_missingStatus_returnsError() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .build();

    ErrorAnd<StatusDefinitions, CiviFormError> result =
        service.editStatus(
            program.getProgramDefinition().adminName(),
            REJECTED_STATUS.statusText(),
            (existingStatus) -> {
              fail("unexpected edit entry found");
              throw new RuntimeException("unexpected edit entry found");
            });
    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors())
        .containsExactly(
            CiviFormError.of(
                "The status being edited no longer exists and may have been modified in a"
                    + " separate window."));
  }

  @Test
  public void deleteStatus() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withStatusDefinitions(
                new StatusDefinitions(ImmutableList.of(APPROVED_STATUS, REJECTED_STATUS)))
            .build();

    ErrorAnd<StatusDefinitions, CiviFormError> result =
        service.deleteStatus(
            program.getProgramDefinition().adminName(), APPROVED_STATUS.statusText());
    assertThat(result.isError()).isFalse();
    assertThat(result.getResult().getStatuses()).isEqualTo(ImmutableList.of(REJECTED_STATUS));
    assertThat(
            applicationStatusesRepo
                .lookupActiveStatusDefinitions(program.getProgramDefinition().adminName())
                .getStatuses())
        .isEqualTo(ImmutableList.of(REJECTED_STATUS));
  }

  @Test
  public void deleteStatus_programNotFound_throws() throws Exception {
    assertThatThrownBy(() -> service.deleteStatus("random", APPROVED_STATUS.statusText()))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("No active status found for program random");
  }

  @Test
  public void deleteStatus_missingStatus_returnsError() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .build();
    ErrorAnd<StatusDefinitions, CiviFormError> result =
        service.deleteStatus(
            program.getProgramDefinition().adminName(), REJECTED_STATUS.statusText());
    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors())
        .containsExactly(
            CiviFormError.of(
                "The status being deleted no longer exists and may have been deleted in a"
                    + " separate window."));
    assertThat(
            applicationStatusesRepo
                .lookupActiveStatusDefinitions(program.getProgramDefinition().adminName())
                .getStatuses())
        .isEqualTo(ImmutableList.of(APPROVED_STATUS));
  }

  private static final String STATUS_WITH_EMAIL_ENGLISH_NAME = "status-with-email";
  private static final String STATUS_WITH_EMAIL_ENGLISH_EMAIL = "some email";
  private static final String STATUS_WITH_EMAIL_FRENCH_NAME = "status-with-email-french";
  private static final String STATUS_WITH_EMAIL_FRENCH_EMAIL = "some email in French";

  private static final StatusDefinitions.Status STATUS_WITH_EMAIL =
      StatusDefinitions.Status.builder()
          .setStatusText(STATUS_WITH_EMAIL_ENGLISH_NAME)
          .setLocalizedStatusText(
              LocalizedStrings.withDefaultValue(STATUS_WITH_EMAIL_ENGLISH_NAME)
                  .updateTranslation(Locale.FRENCH, STATUS_WITH_EMAIL_FRENCH_NAME))
          .setLocalizedEmailBodyText(
              Optional.of(
                  LocalizedStrings.withDefaultValue(STATUS_WITH_EMAIL_ENGLISH_EMAIL)
                      .updateTranslation(Locale.FRENCH, STATUS_WITH_EMAIL_FRENCH_EMAIL)))
          .build();

  private static final String STATUS_WITH_NO_EMAIL_ENGLISH_NAME = "status-with-no-email";
  private static final String STATUS_WITH_NO_EMAIL_FRENCH_NAME = "status-with-no-email-french";

  private static final StatusDefinitions.Status STATUS_WITH_NO_EMAIL =
      StatusDefinitions.Status.builder()
          .setStatusText(STATUS_WITH_NO_EMAIL_ENGLISH_NAME)
          .setLocalizedStatusText(
              LocalizedStrings.withDefaultValue(STATUS_WITH_NO_EMAIL_ENGLISH_NAME)
                  .updateTranslation(Locale.FRENCH, STATUS_WITH_NO_EMAIL_FRENCH_NAME))
          .build();

  @Test
  public void updateLocalizations_addsNewLocale() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.withDefaultValue("default image description"))
            .withStatusDefinitions(
                new StatusDefinitions(ImmutableList.of(STATUS_WITH_EMAIL, STATUS_WITH_NO_EMAIL)))
            .build();

    LocalizationUpdate updateData =
        LocalizationUpdate.builder()
            .setLocalizedDisplayName("German Name")
            .setLocalizedDisplayDescription("German Description")
            .setLocalizedConfirmationMessage("")
            .setLocalizedSummaryImageDescription("German Image Description")
            .setStatuses(
                ImmutableList.of(
                    LocalizationUpdate.StatusUpdate.builder()
                        .setStatusKeyToUpdate(STATUS_WITH_EMAIL_ENGLISH_NAME)
                        .setLocalizedStatusText(Optional.of("german-status-with-email"))
                        .setLocalizedEmailBody(Optional.of("german email body"))
                        .build(),
                    LocalizationUpdate.StatusUpdate.builder()
                        .setStatusKeyToUpdate(STATUS_WITH_NO_EMAIL_ENGLISH_NAME)
                        .setLocalizedStatusText(Optional.of("german-status-with-no-email"))
                        .build()))
            .setScreens(ImmutableList.of())
            .build();

    ErrorAnd<StatusDefinitions, CiviFormError> result =
        service.updateLocalization(
            program.getProgramDefinition().adminName(), Locale.GERMAN, updateData);

    assertThat(result.isError()).isFalse();
    ImmutableList<StatusDefinitions.Status> expectedStatuses =
        ImmutableList.of(
            StatusDefinitions.Status.builder()
                .setStatusText(STATUS_WITH_EMAIL.statusText())
                .setLocalizedStatusText(
                    STATUS_WITH_EMAIL
                        .localizedStatusText()
                        .updateTranslation(Locale.GERMAN, "german-status-with-email"))
                .setLocalizedEmailBodyText(
                    Optional.of(
                        STATUS_WITH_EMAIL
                            .localizedEmailBodyText()
                            .get()
                            .updateTranslation(Locale.GERMAN, "german email body")))
                .build(),
            StatusDefinitions.Status.builder()
                .setStatusText(STATUS_WITH_NO_EMAIL.statusText())
                .setLocalizedStatusText(
                    STATUS_WITH_NO_EMAIL
                        .localizedStatusText()
                        .updateTranslation(Locale.GERMAN, "german-status-with-no-email"))
                .build());
    assertThat(result.getResult().getStatuses()).isEqualTo(expectedStatuses);
    assertThat(
            applicationStatusesRepo
                .lookupActiveStatusDefinitions(program.getProgramDefinition().adminName())
                .getStatuses())
        .isEqualTo(expectedStatuses);
  }

  @Test
  public void updateLocalizations_updatesExistingLocale() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("English name", "English description")
            .withLocalizedName(Locale.FRENCH, "existing French name")
            .withLocalizedDescription(Locale.FRENCH, "existing French description")
            .withLocalizedConfirmationMessage(Locale.FRENCH, "")
            .setLocalizedSummaryImageDescription(
                LocalizedStrings.of(
                    Locale.US,
                    "English image description",
                    Locale.FRENCH,
                    "existing French image description"))
            .withStatusDefinitions(
                new StatusDefinitions(ImmutableList.of(STATUS_WITH_EMAIL, STATUS_WITH_NO_EMAIL)))
            .build();
    assertThat(
            applicationStatusesRepo
                .lookupActiveStatusDefinitions(program.getProgramDefinition().adminName())
                .getStatuses())
        .isNotEmpty();

    LocalizationUpdate updateData =
        LocalizationUpdate.builder()
            .setLocalizedDisplayName("new French name")
            .setLocalizedDisplayDescription("new French description")
            .setLocalizedSummaryImageDescription("new French image description")
            .setLocalizedConfirmationMessage("")
            .setStatuses(
                ImmutableList.of(
                    LocalizationUpdate.StatusUpdate.builder()
                        .setStatusKeyToUpdate(STATUS_WITH_EMAIL_ENGLISH_NAME)
                        .setLocalizedStatusText(
                            Optional.of(STATUS_WITH_EMAIL_FRENCH_NAME + "-updated"))
                        .setLocalizedEmailBody(
                            Optional.of(STATUS_WITH_EMAIL_FRENCH_EMAIL + "-updated"))
                        .build(),
                    LocalizationUpdate.StatusUpdate.builder()
                        .setStatusKeyToUpdate(STATUS_WITH_NO_EMAIL_ENGLISH_NAME)
                        .setLocalizedStatusText(
                            Optional.of(STATUS_WITH_NO_EMAIL_FRENCH_NAME + "-updated"))
                        .build()))
            .setScreens(ImmutableList.of())
            .build();
    ErrorAnd<StatusDefinitions, CiviFormError> result =
        service.updateLocalization(
            program.getProgramDefinition().adminName(), Locale.FRENCH, updateData);

    assertThat(result.isError()).isFalse();
    ImmutableList<StatusDefinitions.Status> expectedStatuses =
        ImmutableList.of(
            StatusDefinitions.Status.builder()
                .setStatusText(STATUS_WITH_EMAIL.statusText())
                .setLocalizedStatusText(
                    STATUS_WITH_EMAIL
                        .localizedStatusText()
                        .updateTranslation(
                            Locale.FRENCH, STATUS_WITH_EMAIL_FRENCH_NAME + "-updated"))
                .setLocalizedEmailBodyText(
                    Optional.of(
                        STATUS_WITH_EMAIL
                            .localizedEmailBodyText()
                            .get()
                            .updateTranslation(
                                Locale.FRENCH, STATUS_WITH_EMAIL_FRENCH_EMAIL + "-updated")))
                .build(),
            StatusDefinitions.Status.builder()
                .setStatusText(STATUS_WITH_NO_EMAIL.statusText())
                .setLocalizedStatusText(
                    STATUS_WITH_NO_EMAIL
                        .localizedStatusText()
                        .updateTranslation(
                            Locale.FRENCH, STATUS_WITH_NO_EMAIL_FRENCH_NAME + "-updated"))
                .build());
    assertThat(
            applicationStatusesRepo
                .lookupActiveStatusDefinitions(program.getProgramDefinition().adminName())
                .getStatuses())
        .isEqualTo(expectedStatuses);
  }

  @Test
  public void updateLocalizations_emailProvidedInUpdateWithNoEmailInConfigure_throws() {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("English name", "English description")
            .withLocalizedName(Locale.FRENCH, "existing French name")
            .withLocalizedDescription(Locale.FRENCH, "existing French description")
            .withLocalizedConfirmationMessage(Locale.FRENCH, "")
            .withStatusDefinitions(
                new StatusDefinitions(ImmutableList.of(STATUS_WITH_EMAIL, STATUS_WITH_NO_EMAIL)))
            .build();

    LocalizationUpdate updateData =
        LocalizationUpdate.builder()
            .setLocalizedDisplayName("new French name")
            .setLocalizedDisplayDescription("new French description")
            .setLocalizedConfirmationMessage("")
            .setStatuses(
                ImmutableList.of(
                    LocalizationUpdate.StatusUpdate.builder()
                        .setStatusKeyToUpdate(STATUS_WITH_EMAIL_ENGLISH_NAME)
                        .setLocalizedStatusText(
                            Optional.of(STATUS_WITH_EMAIL_FRENCH_NAME + "-updated"))
                        .setLocalizedEmailBody(
                            Optional.of(STATUS_WITH_EMAIL_FRENCH_EMAIL + "-updated"))
                        .build(),
                    LocalizationUpdate.StatusUpdate.builder()
                        .setStatusKeyToUpdate(STATUS_WITH_NO_EMAIL_ENGLISH_NAME)
                        .setLocalizedStatusText(
                            Optional.of(STATUS_WITH_NO_EMAIL_FRENCH_NAME + "-updated"))
                        .setLocalizedEmailBody(Optional.of("a localized email"))
                        .build()))
            .setScreens(ImmutableList.of())
            .build();

    assertThatThrownBy(
            () ->
                service.updateLocalization(
                    program.getProgramDefinition().adminName(), Locale.FRENCH, updateData))
        .isInstanceOf(OutOfDateStatusesException.class);
    // no updates to ApplicationStatus table
    StatusDefinitions currentStatus =
        applicationStatusesRepo.lookupActiveStatusDefinitions(
            program.getProgramDefinition().adminName());
    assertThat(currentStatus.getStatuses()).isNotEmpty();
    assertThat(currentStatus.getStatuses().size()).isEqualTo(2);
    assertThat(currentStatus.getStatuses().get(0).statusText())
        .isEqualTo(STATUS_WITH_EMAIL_ENGLISH_NAME);
    assertThat(currentStatus.getStatuses().get(1).statusText())
        .isEqualTo(STATUS_WITH_NO_EMAIL_ENGLISH_NAME);
  }

  @Test
  public void updateLocalizations_allowsClearingStatusFields() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("English name", "English description")
            .withLocalizedName(Locale.FRENCH, "existing French name")
            .withLocalizedDescription(Locale.FRENCH, "existing French description")
            .withLocalizedConfirmationMessage(Locale.FRENCH, "")
            .withStatusDefinitions(
                new StatusDefinitions(ImmutableList.of(STATUS_WITH_EMAIL, STATUS_WITH_NO_EMAIL)))
            .build();

    LocalizationUpdate updateData =
        LocalizationUpdate.builder()
            .setLocalizedDisplayName("new French name")
            .setLocalizedDisplayDescription("new French description")
            .setLocalizedConfirmationMessage("")
            .setStatuses(
                ImmutableList.of(
                    LocalizationUpdate.StatusUpdate.builder()
                        .setStatusKeyToUpdate(STATUS_WITH_EMAIL_ENGLISH_NAME)
                        .build(),
                    LocalizationUpdate.StatusUpdate.builder()
                        .setStatusKeyToUpdate(STATUS_WITH_NO_EMAIL_ENGLISH_NAME)
                        .build()))
            .setScreens(ImmutableList.of())
            .build();

    ErrorAnd<StatusDefinitions, CiviFormError> result =
        service.updateLocalization(
            program.getProgramDefinition().adminName(), Locale.FRENCH, updateData);

    assertThat(result.isError()).isFalse();

    assertThat(
            applicationStatusesRepo
                .lookupActiveStatusDefinitions(program.getProgramDefinition().adminName())
                .getStatuses())
        .isEqualTo(
            ImmutableList.of(
                StatusDefinitions.Status.builder()
                    .setStatusText(STATUS_WITH_EMAIL.statusText())
                    .setLocalizedStatusText(
                        STATUS_WITH_EMAIL
                            .localizedStatusText()
                            .updateTranslation(Locale.FRENCH, Optional.empty()))
                    .setLocalizedEmailBodyText(
                        Optional.of(
                            STATUS_WITH_EMAIL
                                .localizedEmailBodyText()
                                .get()
                                .updateTranslation(Locale.FRENCH, Optional.empty())))
                    .build(),
                StatusDefinitions.Status.builder()
                    .setStatusText(STATUS_WITH_NO_EMAIL.statusText())
                    .setLocalizedStatusText(
                        STATUS_WITH_NO_EMAIL
                            .localizedStatusText()
                            .updateTranslation(Locale.FRENCH, Optional.empty()))
                    .build()));
  }

  @Test
  public void updateLocalizations_providesUnrecognizedStatuses_throws() {
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withStatusDefinitions(
                new StatusDefinitions(ImmutableList.of(STATUS_WITH_EMAIL, STATUS_WITH_NO_EMAIL)))
            .build();

    LocalizationUpdate updateData =
        LocalizationUpdate.builder()
            .setLocalizedDisplayName("German Name")
            .setLocalizedDisplayDescription("German Description")
            .setLocalizedConfirmationMessage("")
            .setStatuses(
                ImmutableList.of(
                    LocalizationUpdate.StatusUpdate.builder()
                        .setStatusKeyToUpdate("unrecognized-status")
                        .setLocalizedStatusText(Optional.of("unrecognized-status"))
                        .setLocalizedEmailBody(Optional.of("unrecognized-status-email-body"))
                        .build(),
                    LocalizationUpdate.StatusUpdate.builder()
                        .setStatusKeyToUpdate(STATUS_WITH_EMAIL_ENGLISH_NAME)
                        .setLocalizedStatusText(Optional.of("german-status-with-email"))
                        .setLocalizedEmailBody(Optional.of("german email body"))
                        .build(),
                    LocalizationUpdate.StatusUpdate.builder()
                        .setStatusKeyToUpdate(STATUS_WITH_NO_EMAIL_ENGLISH_NAME)
                        .setLocalizedStatusText(Optional.of("german-status-with-no-email"))
                        .build()))
            .setScreens(ImmutableList.of())
            .build();

    assertThatThrownBy(
            () ->
                service.updateLocalization(
                    program.getProgramDefinition().adminName(), Locale.FRENCH, updateData))
        .isInstanceOf(OutOfDateStatusesException.class);
    assertThat(
            applicationStatusesRepo
                .lookupActiveStatusDefinitions(program.getProgramDefinition().adminName())
                .getStatuses())
        .isNotEmpty();
  }

  @Test
  public void updateLocalizations_doesNotProvideStatus_throws() {
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withStatusDefinitions(
                new StatusDefinitions(ImmutableList.of(STATUS_WITH_EMAIL, STATUS_WITH_NO_EMAIL)))
            .build();

    LocalizationUpdate updateData =
        LocalizationUpdate.builder()
            .setLocalizedDisplayName("German Name")
            .setLocalizedDisplayDescription("German Description")
            .setLocalizedConfirmationMessage("")
            .setStatuses(
                ImmutableList.of(
                    LocalizationUpdate.StatusUpdate.builder()
                        .setStatusKeyToUpdate(STATUS_WITH_EMAIL_ENGLISH_NAME)
                        .setLocalizedStatusText(Optional.of("german-status-with-email"))
                        .setLocalizedEmailBody(Optional.of("german email body"))
                        .build()))
            .setScreens(ImmutableList.of())
            .build();

    assertThatThrownBy(
            () ->
                service.updateLocalization(
                    program.getProgramDefinition().adminName(), Locale.FRENCH, updateData))
        .isInstanceOf(OutOfDateStatusesException.class);
    // no updates to ApplicationStatus table
    StatusDefinitions currentStatus =
        applicationStatusesRepo.lookupActiveStatusDefinitions(
            program.getProgramDefinition().adminName());
    assertThat(currentStatus.getStatuses()).isNotEmpty();
    assertThat(currentStatus.getStatuses().size()).isEqualTo(2);
    assertThat(currentStatus.getStatuses().get(0).statusText())
        .isEqualTo(STATUS_WITH_EMAIL_ENGLISH_NAME);
    assertThat(currentStatus.getStatuses().get(1).statusText())
        .isEqualTo(STATUS_WITH_NO_EMAIL_ENGLISH_NAME);
  }
}
