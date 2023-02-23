package services.applications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import junitparams.JUnitParamsRunner;
import models.Account;
import models.Applicant;
import models.Application;
import models.ApplicationEvent;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import repository.ApplicationEventRepository;
import repository.ApplicationRepository;
import repository.ResetPostgres;
import services.DeploymentType;
import services.LocalizedStrings;
import services.applicant.ApplicantService;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.NoteEvent;
import services.application.ApplicationEventDetails.StatusEvent;
import services.cloud.aws.SimpleEmail;
import services.program.ProgramDefinition;
import services.program.StatusDefinitions;
import services.program.StatusNotFoundException;
import support.ProgramBuilder;

@RunWith(JUnitParamsRunner.class)
public class ProgramAdminApplicationServiceTest extends ResetPostgres {
  private static final StatusDefinitions.Status STATUS_WITH_ONLY_ENGLISH_EMAIL =
      StatusDefinitions.Status.builder()
          .setStatusText("STATUS_WITH_ONLY_ENGLISH_EMAIL")
          .setLocalizedStatusText(
              LocalizedStrings.withDefaultValue("STATUS_WITH_ONLY_ENGLISH_EMAIL"))
          .setLocalizedEmailBodyText(
              Optional.of(
                  LocalizedStrings.withDefaultValue("STATUS_WITH_ONLY_ENGLISH_EMAIL email body")))
          .build();

  private static final StatusDefinitions.Status STATUS_WITH_NO_EMAIL =
      StatusDefinitions.Status.builder()
          .setStatusText("STATUS_WITH_NO_EMAIL")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("STATUS_WITH_NO_EMAIL"))
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
  public void setStatus_sendsEmail() throws Exception {
    Instant start = Instant.now();
    String userEmail = "user@email.com";
    SimpleEmail simpleEmail = Mockito.mock(SimpleEmail.class);
    String programDisplayName = "Some Program";
    service =
        new ProgramAdminApplicationService(
            instanceOf(ApplicantService.class),
            instanceOf(ApplicationRepository.class),
            instanceOf(ApplicationEventRepository.class),
            instanceOf(Config.class),
            simpleEmail,
            instanceOf(DeploymentType.class));

    ProgramDefinition program =
        ProgramBuilder.newActiveProgramWithDisplayName("some-program", programDisplayName)
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .buildDefinition();
    Account account = resourceCreator.insertAccount();
    Applicant applicant = resourceCreator.insertApplicantWithAccount(Optional.of(userEmail));
    Application application =
        Application.create(applicant, program.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow();

    StatusEvent event =
        StatusEvent.builder()
            .setEmailSent(true)
            .setStatusText(STATUS_WITH_ONLY_ENGLISH_EMAIL.statusText())
            .build();

    service.setStatus(application, event, account);

    verify(simpleEmail, times(1))
        .send(
            eq(userEmail),
            eq(
                String.format(
                    ProgramAdminApplicationService.STATUS_UPDATE_EMAIL_SUBJECT_FORMAT,
                    programDisplayName)),
            Mockito.contains(
                STATUS_WITH_ONLY_ENGLISH_EMAIL.localizedEmailBodyText().get().getDefault()));

    application.refresh();
    assertThat(application.getApplicationEvents()).hasSize(1);
    ApplicationEvent gotEvent = application.getApplicationEvents().get(0);
    assertThat(gotEvent.getEventType()).isEqualTo(ApplicationEventDetails.Type.STATUS_CHANGE);
    assertThat(gotEvent.getDetails().statusEvent()).isPresent();
    assertThat(gotEvent.getDetails().statusEvent().get().statusText())
        .isEqualTo(STATUS_WITH_ONLY_ENGLISH_EMAIL.statusText());
    assertThat(gotEvent.getDetails().statusEvent().get().emailSent()).isTrue();
    assertThat(gotEvent.getCreator()).isEqualTo(Optional.of(account));
    assertThat(gotEvent.getCreateTime()).isAfter(start);
  }

  @Test
  public void setStatus_sendsEmail_nonDefaultLocale() throws Exception {
    Locale userLocale = Locale.FRENCH;
    String userEmail = "user@email.com";
    String programDisplayName = "Some Program";
    SimpleEmail simpleEmail = Mockito.mock(SimpleEmail.class);
    service =
        new ProgramAdminApplicationService(
            instanceOf(ApplicantService.class),
            instanceOf(ApplicationRepository.class),
            instanceOf(ApplicationEventRepository.class),
            instanceOf(Config.class),
            simpleEmail,
            instanceOf(DeploymentType.class));

    ProgramDefinition program =
        ProgramBuilder.newActiveProgramWithDisplayName("some-program", programDisplayName)
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .buildDefinition();
    Account account = resourceCreator.insertAccount();
    Applicant applicant = resourceCreator.insertApplicantWithAccount(Optional.of(userEmail));
    // Set the user to French.
    applicant.getApplicantData().setPreferredLocale(userLocale);
    applicant.save();
    Application application =
        Application.create(applicant, program.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow();

    StatusEvent event =
        StatusEvent.builder()
            .setEmailSent(true)
            .setStatusText(STATUS_WITH_MULTI_LANGUAGE_EMAIL.statusText())
            .build();

    service.setStatus(application, event, account);

    verify(simpleEmail, times(1))
        .send(
            eq(userEmail),
            eq(
                String.format(
                    ProgramAdminApplicationService.STATUS_UPDATE_EMAIL_SUBJECT_FORMAT,
                    programDisplayName)),
            Mockito.contains(
                STATUS_WITH_MULTI_LANGUAGE_EMAIL.localizedEmailBodyText().get().getDefault()));
  }

  @Test
  public void setStatus_tiApplicant_sendsEmail() throws Exception {
    String userEmail = "user@email.com";
    String tiEmail = "ti@email.com";
    SimpleEmail simpleEmail = Mockito.mock(SimpleEmail.class);
    String programDisplayName = "Some Program";
    service =
        new ProgramAdminApplicationService(
            instanceOf(ApplicantService.class),
            instanceOf(ApplicationRepository.class),
            instanceOf(ApplicationEventRepository.class),
            instanceOf(Config.class),
            simpleEmail,
            instanceOf(DeploymentType.class));

    ProgramDefinition program =
        ProgramBuilder.newActiveProgramWithDisplayName("some-program", programDisplayName)
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .buildDefinition();
    Account account = resourceCreator.insertAccount();
    Applicant applicant = resourceCreator.insertApplicantWithAccount(Optional.of(userEmail));
    Application application =
        Application.create(applicant, program.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow()
            .setSubmitterEmail(tiEmail);

    StatusEvent event =
        StatusEvent.builder()
            .setEmailSent(true)
            .setStatusText(STATUS_WITH_ONLY_ENGLISH_EMAIL.statusText())
            .build();

    service.setStatus(application, event, account);

    verify(simpleEmail, times(1))
        .send(
            eq(tiEmail),
            Mockito.contains("An update on the application for program"),
            Mockito.contains("has changed to " + STATUS_WITH_ONLY_ENGLISH_EMAIL.statusText()));
    verify(simpleEmail, times(1))
        .send(
            eq(userEmail),
            eq(
                String.format(
                    ProgramAdminApplicationService.STATUS_UPDATE_EMAIL_SUBJECT_FORMAT,
                    programDisplayName)),
            Mockito.contains(
                STATUS_WITH_ONLY_ENGLISH_EMAIL.localizedEmailBodyText().get().getDefault()));
  }

  @Test
  public void setStatus_invalidStatus_throws() throws Exception {
    String userEmail = "user@email.com";

    ProgramDefinition program =
        ProgramBuilder.newActiveProgram("some-program")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .buildDefinition();
    Account account = resourceCreator.insertAccount();
    Applicant applicant = resourceCreator.insertApplicantWithAccount(Optional.of(userEmail));
    Application application =
        Application.create(applicant, program.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow();

    StatusEvent event =
        StatusEvent.builder().setEmailSent(true).setStatusText("Not an actual status").build();

    assertThrows(
        StatusNotFoundException.class, () -> service.setStatus(application, event, account));
    application.refresh();
    assertThat(application.getApplicationEvents()).isEmpty();
  }

  @Test
  public void setStatus_sendEmailWithNoStatusEmail_throws() throws Exception {
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram("some-program")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .buildDefinition();
    Account account = resourceCreator.insertAccount();
    Applicant applicant =
        resourceCreator.insertApplicantWithAccount(Optional.of("user@example.com"));
    Application application =
        Application.create(applicant, program.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow();

    // Request email to be sent when there is not one.
    StatusEvent event =
        StatusEvent.builder()
            .setEmailSent(true)
            .setStatusText(STATUS_WITH_NO_EMAIL.statusText())
            .build();

    assertThrows(
        StatusEmailNotFoundException.class, () -> service.setStatus(application, event, account));
    application.refresh();
    assertThat(application.getApplicationEvents()).isEmpty();
  }

  @Test
  public void setStatus_sendEmailWithNoUserEmail_throws() throws Exception {
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram("some-program")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .buildDefinition();
    Account account = resourceCreator.insertAccount();
    Applicant applicant = resourceCreator.insertApplicantWithAccount(Optional.empty());
    Application application =
        Application.create(applicant, program.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow();

    // Request email to be sent when the user doesn't have one.
    StatusEvent event =
        StatusEvent.builder()
            .setEmailSent(true)
            .setStatusText(STATUS_WITH_ONLY_ENGLISH_EMAIL.statusText())
            .build();

    assertThrows(
        AccountHasNoEmailException.class, () -> service.setStatus(application, event, account));
    application.refresh();
    assertThat(application.getApplicationEvents()).isEmpty();
  }

  @Test
  public void setStatus_sentEmailFalse_doesNotSendEmail() throws Exception {
    Instant start = Instant.now();
    String status = STATUS_WITH_ONLY_ENGLISH_EMAIL.statusText();
    SimpleEmail simpleEmail = Mockito.mock(SimpleEmail.class);
    service =
        new ProgramAdminApplicationService(
            instanceOf(ApplicantService.class),
            instanceOf(ApplicationRepository.class),
            instanceOf(ApplicationEventRepository.class),
            instanceOf(Config.class),
            simpleEmail,
            instanceOf(DeploymentType.class));

    ProgramDefinition program =
        ProgramBuilder.newActiveProgram("some-program")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .buildDefinition();
    Account account = resourceCreator.insertAccount();
    Applicant applicant =
        resourceCreator.insertApplicantWithAccount(Optional.of("user@example.com"));
    Application application =
        Application.create(applicant, program.toProgram(), LifecycleStage.ACTIVE)
            .setSubmitTimeToNow();

    // Do not request an email to be sent.
    StatusEvent event = StatusEvent.builder().setEmailSent(false).setStatusText(status).build();

    service.setStatus(application, event, account);

    verify(simpleEmail, never()).send(anyString(), anyString(), anyString());

    application.refresh();
    assertThat(application.getApplicationEvents()).hasSize(1);
    ApplicationEvent gotEvent = application.getApplicationEvents().get(0);
    assertThat(gotEvent.getEventType()).isEqualTo(ApplicationEventDetails.Type.STATUS_CHANGE);
    assertThat(gotEvent.getDetails().statusEvent()).isPresent();
    assertThat(gotEvent.getDetails().statusEvent().get().statusText()).isEqualTo(status);
    assertThat(gotEvent.getDetails().statusEvent().get().emailSent()).isFalse();
    assertThat(gotEvent.getCreator()).isEqualTo(Optional.of(account));
    assertThat(gotEvent.getCreateTime()).isAfter(start);
  }
}
