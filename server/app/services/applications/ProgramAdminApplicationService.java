package services.applications;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import java.util.Locale;
import java.util.Optional;
import models.Account;
import models.Applicant;
import models.Application;
import models.ApplicationEvent;
import models.Program;
import repository.ApplicationEventRepository;
import repository.ApplicationRepository;
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

  private final ApplicantService applicantService;
  private final ApplicationRepository applicationRepository;
  private final ApplicationEventRepository eventRepository;
  private final SimpleEmail emailClient;

  @Inject
  ProgramAdminApplicationService(
      ApplicantService applicantService,
      ApplicationRepository applicationRepository,
      ApplicationEventRepository eventRepository,
      SimpleEmail emailClient) {
    this.applicantService = applicantService;
    this.applicationRepository = checkNotNull(applicationRepository);
    this.eventRepository = checkNotNull(eventRepository);
    this.emailClient = emailClient;
  }

  /**
   * Retrieves the application with the given ID and validates that it is associated with the given
   * program.
   */
  public Optional<Application> getApplication(long applicationId, ProgramDefinition program) {
    Optional<Application> maybeApplication =
        applicationRepository.getApplication(applicationId).toCompletableFuture().join();
    if (!maybeApplication.isPresent()) {
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
      throws StatusNotFoundException {
    Program program = application.getProgram();
    Applicant applicant = application.getApplicant();
    String newStatusText = newStatusEvent.statusText();
    // Phrasing is a little as the service layer is converting between intent and reality.
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
    ApplicationEvent event = new ApplicationEvent(application, admin, details);

    // Send email if requested and present.
    if (sendEmail && statusDef.localizedEmailBodyText().isPresent()) {
      Optional<String> applicantEmail =
          applicantService.getEmail(application.getApplicant().id).toCompletableFuture().join();
      if (applicantEmail.isPresent()) {
        Locale locale = applicant.getApplicantData().preferredLocale();
        String emailBody = statusDef.localizedEmailBodyText().get().getOrDefault(locale);
        emailClient.send(applicantEmail.get(), "An update on your Application", emailBody);
      }
    }

    eventRepository.insertSync(event);
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
    ApplicationEvent event = new ApplicationEvent(application, admin, details);
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
