package services.applicationstatuses;

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
import services.program.ProgramNotFoundException;
import support.ProgramBuilder;

public class StatusServiceTest extends ResetPostgres {
  private ApplicationStatusesRepository applicationStatusesRepo;
  private StatusService service;

  @Before
  public void setup() {
    applicationStatusesRepo = instanceOf(ApplicationStatusesRepository.class);
    service = instanceOf(StatusService.class);
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
        service.appendStatus(program.id, APPROVED_DEFAULT_STATUS);

    assertThat(firstResult.isError()).isFalse();
    assertThat(firstResult.getResult().getStatuses()).containsExactly(APPROVED_DEFAULT_STATUS);
    assertThat(applicationStatusesRepo.lookupActiveStatusDefinitions(programName).getStatuses())
        .containsExactly(APPROVED_DEFAULT_STATUS);

    // Ensure that appending to a non-empty list actually appends.
    ErrorAnd<StatusDefinitions, CiviFormError> secondResult =
        service.appendStatus(program.id, REJECTED_DEFAULT_STATUS);

    assertThat(secondResult.isError()).isFalse();
    assertThat(secondResult.getResult().getStatuses())
        .containsExactly(APPROVED_STATUS, REJECTED_DEFAULT_STATUS);
    assertThat(applicationStatusesRepo.lookupActiveStatusDefinitions(programName).getStatuses())
        .containsExactly(APPROVED_STATUS, REJECTED_DEFAULT_STATUS);
  }

  @Test
  public void appendStatus_programNotFound_throws() throws Exception {
    assertThatThrownBy(() -> service.appendStatus(Long.MAX_VALUE, APPROVED_STATUS))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("Program not found for ID:");
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
              service.appendStatus(program.id, newApprovedStatus);
            });

    assertThat(exc.userFacingMessage()).contains("A status with name Approved already exists");
  }

  @Test
  public void editStatus() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .build();
    assertThat(
            applicationStatusesRepo
                .lookupActiveStatusDefinitions(program.getProgramDefinition().adminName())
                .getStatuses())
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
            program.id,
            APPROVED_STATUS.statusText(),
            (existing) -> {
              assertThat(existing).isEqualTo(APPROVED_STATUS);
              return editedStatus;
            });
    assertThat(result.isError()).isFalse();
    assertThat(result.getResult().getStatuses()).containsExactly(editedStatus);
    assertThat(
            applicationStatusesRepo
                .lookupActiveStatusDefinitions(program.getProgramDefinition().adminName())
                .getStatuses())
        .containsExactly(editedStatus);
  }

  @Test
  public void editStatus_programNotFound_throws() throws Exception {
    assertThatThrownBy(
            () ->
                service.editStatus(
                    Long.MAX_VALUE,
                    APPROVED_STATUS.statusText(),
                    (existing) -> {
                      fail("unexpected edit entry found");
                      throw new RuntimeException("unexpected edit entry found");
                    }))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("Program not found for ID:");
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
                    program.id,
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
            program.id,
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
        service.deleteStatus(program.id, APPROVED_STATUS.statusText());
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
    assertThatThrownBy(() -> service.deleteStatus(Long.MAX_VALUE, APPROVED_STATUS.statusText()))
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("Program not found for ID:");
  }

  @Test
  public void deleteStatus_missingStatus_returnsError() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram()
            .withStatusDefinitions(new StatusDefinitions(ImmutableList.of(APPROVED_STATUS)))
            .build();
    ErrorAnd<StatusDefinitions, CiviFormError> result =
        service.deleteStatus(program.id, REJECTED_STATUS.statusText());
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
}
