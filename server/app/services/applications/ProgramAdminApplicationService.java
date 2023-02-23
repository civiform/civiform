package services.applications;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.util.Locale;
import java.util.Optional;
import models.Account;
import models.Applicant;
import models.Application;
import models.ApplicationEvent;
import models.Program;
import repository.ApplicationEventRepository;
import repository.ApplicationRepository;
import services.DeploymentType;
import services.applicant.ApplicantService;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.NoteEvent;
import services.application.ApplicationEventDetails.StatusEvent;
import services.cloud.aws.SimpleEmail;
import services.program.ProgramDefinition;
import services.program.StatusDefinitions.Status;
import services.program.StatusNotFoundException;

/** The service responsible for mediating a program admin's access to the Application resource. */
public final class ProgramAdminApplicationService {
  @VisibleForTesting
  static final String STATUS_UPDATE_EMAIL_SUBJECT_FORMAT = "An update on your application %s";

  private final ApplicantService applicantService;
  private final ApplicationRepository applicationRepository;
  private final ApplicationEventRepository eventRepository;
  private final SimpleEmail emailClient;
  private final String baseUrl;
  private final boolean isStaging;
  private final String stagingApplicantNotificationMailingList;
  private final String stagingTiNotificationMailingList;

  @Inject
  ProgramAdminApplicationService(
      ApplicantService applicantService,
      ApplicationRepository applicationRepository,
      ApplicationEventRepository eventRepository,
      Config configuration,
      SimpleEmail emailClient,
      DeploymentType deploymentType) {
    this.applicantService = checkNotNull(applicantService);
    this.applicationRepository = checkNotNull(applicationRepository);
    this.eventRepository = checkNotNull(eventRepository);
    this.emailClient = checkNotNull(emailClient);

    checkNotNull(configuration);
    checkNotNull(deploymentType);

    this.isStaging = deploymentType.isStaging();
    this.baseUrl = configuration.getString("base_url");
    this.stagingApplicantNotificationMailingList =
        configuration.getString("staging_applicant_notification_mailing_list");
    this.stagingTiNotificationMailingList =
        configuration.getString("staging_ti_notification_mailing_list");
  }

  /**
   * Retrieves the application with the given ID and validates that it is associated with the given
   * program.
   */
  public Optional<Application> getApplication(long applicationId, ProgramDefinition program) {
    Optional<Application> maybeApplication =
        applicationRepository.getApplication(applicationId).toCompletableFuture().join();
    if (maybeApplication.isEmpty()) {
      return Optional.empty();
    }
    Application application = maybeApplication.get();
    if (program.adminName().isEmpty()
        || !application
            .getProgram()
            .getProgramDefinition()
            .adminName()
            .equals(program.adminName())) {
      return Optional.empty();
    }
    return Optional.of(application);
  }

  /**
   * Sets the status on the {@code Application}.
   *
   * @param admin The Account that instigated the change.
   */
  public void setStatus(Application application, StatusEvent newStatusEvent, Account admin)
      throws StatusEmailNotFoundException, StatusNotFoundException, AccountHasNoEmailException {
    Program program = application.getProgram();
    Applicant applicant = application.getApplicant();
    String newStatusText = newStatusEvent.statusText();
    // The send/sent phrasing is a little weird as the service layer is converting between intent
    // and reality.
    boolean sendEmail = newStatusEvent.emailSent();

    Optional<Status> statusDefMaybe =
        program.getStatusDefinitions().getStatuses().stream()
            .filter(s -> s.statusText().equals(newStatusText))
            .findFirst();
    if (statusDefMaybe.isEmpty()) {
      throw new StatusNotFoundException(newStatusText, program.id);
    }
    Status statusDef = statusDefMaybe.get();

    ApplicationEventDetails details =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
            .setStatusEvent(newStatusEvent)
            .build();
    ApplicationEvent event = new ApplicationEvent(application, Optional.of(admin), details);

    // Send email if requested and present.
    if (sendEmail) {
      if (statusDef.localizedEmailBodyText().isEmpty()) {
        throw new StatusEmailNotFoundException(newStatusText, program.id);
      }
      // Notify an Admin/TI if they applied.
      Optional<String> adminSubmitterEmail = application.getSubmitterEmail();
      if (adminSubmitterEmail.isPresent()) {
        sendAdminSubmitterEmail(
            program.getProgramDefinition(), applicant, newStatusText, adminSubmitterEmail);
      }
      // Notify the applicant.
      Optional<String> applicantEmail =
          applicantService.getEmail(application.getApplicant().id).toCompletableFuture().join();
      if (applicantEmail.isPresent()) {
        sendApplicantEmail(program.getProgramDefinition(), applicant, statusDef, applicantEmail);
      } else {
        // An email was requested to be sent but the applicant doesn't have one.
        throw new AccountHasNoEmailException(applicant.getAccount().id);
      }
    }

    eventRepository.insertSync(event);
  }

  private void sendApplicantEmail(
      ProgramDefinition programDef,
      Applicant applicant,
      Status statusDef,
      Optional<String> applicantEmail) {
    String civiformLink = baseUrl;
    Locale locale = applicant.getApplicantData().preferredLocale();
    // TODO(#3377): Review email content in particular localizing the subject and program name.
    String programName = programDef.localizedName().getDefault();
    // TODO(#3377): Review email content in particular mentioning CiviForm here, and translations.
    // Note: We knowingly mix the english boilerplate with potentially translated body content
    // believing something is better than nothing.
    String emailBody =
        String.format(
            "%s\n\nLog in to CiviForm at %s.",
            statusDef.localizedEmailBodyText().get().getOrDefault(locale), civiformLink);
    emailClient.send(
        isStaging ? stagingApplicantNotificationMailingList : applicantEmail.get(),
        String.format(STATUS_UPDATE_EMAIL_SUBJECT_FORMAT, programName),
        emailBody);
  }

  private void sendAdminSubmitterEmail(
      ProgramDefinition programDef,
      Applicant applicant,
      String newStatusText,
      Optional<String> adminSubmitterEmail) {
    String programName = programDef.localizedName().getDefault();
    String tiDashLink =
        baseUrl
            + controllers.ti.routes.TrustedIntermediaryController.dashboard(
                    /* nameQuery= */ Optional.empty(),
                    /* dateQuery= */ Optional.empty(),
                    /* page= */ Optional.of(1))
                .url();
    String subject =
        String.format(
            "An update on the application for program %s on behalf of applicant %d",
            programName, applicant.id);
    String body =
        String.format(
            "The status for applicant %d on program %s has changed to %s.\n\n"
                + "Manage your clients at %s.",
            applicant.id, programName, newStatusText, tiDashLink);

    emailClient.send(
        isStaging ? stagingTiNotificationMailingList : adminSubmitterEmail.get(), subject, body);
  }

  /**
   * Sets the note on the {@code Application}.
   *
   * @param admin The Account that instigated the change.
   */
  public void setNote(Application application, NoteEvent note, Account admin) {
    ApplicationEventDetails details =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.NOTE_CHANGE)
            .setNoteEvent(note)
            .build();
    ApplicationEvent event = new ApplicationEvent(application, Optional.of(admin), details);
    eventRepository.insertSync(event);
  }

  /** Returns the note content for {@code application}. */
  public Optional<String> getNote(Application application) {
    // The most recent note event is the current value for the note.
    return application.getApplicationEvents().stream()
        .filter(app -> app.getEventType().equals(ApplicationEventDetails.Type.NOTE_CHANGE))
        .findFirst()
        .map(app -> app.getDetails().noteEvent().get().note());
  }
}
