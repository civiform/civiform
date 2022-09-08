package services.applications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import models.Account;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import repository.ApplicationEventRepository;
import repository.ApplicationRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.applicant.ApplicantService;
import services.application.ApplicationEventDetails.NoteEvent;
import services.application.ApplicationEventDetails.StatusEvent;
import services.cloud.aws.SimpleEmail;
import services.program.ProgramDefinition;
import services.program.StatusDefinitions;
import support.ProgramBuilder;

public class ProgramAdminApplicationServiceTest extends ResetPostgres {
  private static final StatusDefinitions.Status STATUS_WITH_ONLY_ENGLISH_EMAIL =
      StatusDefinitions.Status.builder()
          .setStatusText("Approved")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Approved"))
          .setLocalizedEmailBodyText(
              Optional.of(LocalizedStrings.withDefaultValue("Approved email body")))
          .build();

  private static final StatusDefinitions.Status STATUS_WITH_NO_EMAIL =
      StatusDefinitions.Status.builder()
          .setStatusText("Rejected")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Rejected"))
          .build();

  private static final StatusDefinitions.Status STATUS_WITH_MULTI_LANGUAGE_EMAIL =
      StatusDefinitions.Status.builder()
          .setStatusText("With translations")
          .setLocalizedStatusText(
              LocalizedStrings.create(
                  ImmutableMap.of(
                      Locale.US, "With translations",
                      Locale.FRENCH, "With translations (French)")))
          .setLocalizedEmailBodyText(
              Optional.of(
                  LocalizedStrings.create(
                      ImmutableMap.of(
                          Locale.US, "A translatable email body",
                          Locale.FRENCH, "A translatable email body (French)"))))
          .build();

  private static final ImmutableList<StatusDefinitions.Status> ORIGINAL_STATUSES =
      ImmutableList.of(
          STATUS_WITH_ONLY_ENGLISH_EMAIL, STATUS_WITH_NO_EMAIL, STATUS_WITH_MULTI_LANGUAGE_EMAIL);
  private ProgramAdminApplicationService service;

  @Before
  public void setProgramServiceImpl() {
    service = instanceOf(ProgramAdminApplicationService.class);
  }

  @Test
  public void getApplication() {
    ProgramDefinition program = ProgramBuilder.newActiveProgram("some-program").buildDefinition();

    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow();

    Optional<Application> result = service.getApplication(application.id, program);
    assertThat(result).isPresent();
    assertThat(result.get().id).isEqualTo(application.id);
  }

  @Test
  public void getApplication_notFound() {
    ProgramDefinition program = ProgramBuilder.newActiveProgram().buildDefinition();
    assertThat(service.getApplication(Long.MAX_VALUE, program)).isEmpty();
  }

  @Test
  public void getApplication_programMismatch() {
    ProgramDefinition firstProgram =
        ProgramBuilder.newActiveProgram("first-program").buildDefinition();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application firstProgramApplication =
        Application.create(applicant, firstProgram.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow();

    ProgramDefinition secondProgram =
        ProgramBuilder.newActiveProgram("second-program").buildDefinition();

    assertThat(service.getApplication(firstProgramApplication.id, secondProgram)).isEmpty();
  }

  @Test
  public void getApplication_emptyAdminName() {
    ProgramDefinition program = ProgramBuilder.newActiveProgram("").buildDefinition();

    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow();

    assertThat(service.getApplication(application.id, program)).isEmpty();
  }

  @Test
  public void getNote_noNotes_empty() {
    ProgramDefinition program = ProgramBuilder.newActiveProgram("some-program").buildDefinition();

    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow();

    // Execute, verify.
    assertThat(service.getNote(application)).isEmpty();
  }

  @Test
  public void getNote_multipleNotes_findsLatest() throws Exception {
    String note = "Application note";
    ProgramDefinition program = ProgramBuilder.newActiveProgram("some-program").buildDefinition();

    Account account = resourceCreator.insertAccount();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow();

    service.setNote(application, NoteEvent.create("first note"), account);
    // Sleep for a few milliseconds to ensure that a subsequent update would
    // have a distinct timestamp.
    // https://github.com/seattle-uat/civiform/pull/2499#issuecomment-1133325484.
    TimeUnit.MILLISECONDS.sleep(2);
    service.setNote(application, NoteEvent.create(note), account);
    application.refresh();

    // Execute, verify.
    assertThat(service.getNote(application)).contains(note);
  }

  @Test
  public void setStatus_sentEmailFalse_doesNotSendEmail() throws Exception {
    SimpleEmail simpleEmail = Mockito.mock(SimpleEmail.class);
    service =
        new ProgramAdminApplicationService(
            instanceOf(ApplicantService.class),
            instanceOf(ApplicationRepository.class),
            instanceOf(ApplicationEventRepository.class),
            simpleEmail);

    ProgramDefinition program =
        ProgramBuilder.newActiveProgram("some-program")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .buildDefinition();
    Account account = resourceCreator.insertAccount();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow();

    StatusEvent event =
        StatusEvent.builder()
            .setEmailSent(false)
            .setStatusText(STATUS_WITH_ONLY_ENGLISH_EMAIL.statusText())
            .build();

    service.setStatus(application, event, account);

    verify(simpleEmail, never()).send(anyString(), anyString(), anyString());
  }
}
