package services.applications;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.util.Locale;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationEventModel;
import models.ApplicationModel;
import models.ProgramModel;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import repository.AccountRepository;
import repository.ApplicationEventRepository;
import repository.ApplicationRepository;
import repository.ProgramRepository;
import services.DeploymentType;
import services.LocalizedStrings;
import services.MessageKey;
import services.applicant.ApplicantData;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantPersonalInfo.ApplicantType;
import services.applicant.ApplicantService;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.NoteEvent;
import services.application.ApplicationEventDetails.StatusEvent;
import services.cloud.aws.SimpleEmail;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.StatusDefinitions.Status;
import services.program.StatusNotFoundException;

/** The service responsible for mediating a program admin's access to the Application resource. */
public final class ProgramAdminApplicationService {

  private final ApplicantService applicantService;
  private final ApplicationEventRepository eventRepository;
  private final AccountRepository accountRepository;

  private final ProgramRepository programRepository;
  private final SimpleEmail emailClient;
  private final String baseUrl;
  private final boolean isStaging;
  private final String stagingApplicantNotificationMailingList;
  private final String stagingTiNotificationMailingList;
  private final MessagesApi messagesApi;
  private final ApplicationRepository applicationRepository;

  @Inject
  ProgramAdminApplicationService(
      ApplicantService applicantService,
      ApplicationEventRepository eventRepository,
      AccountRepository accountRepository,
      ProgramRepository programRepository,
      Config configuration,
      SimpleEmail emailClient,
      DeploymentType deploymentType,
      MessagesApi messagesApi,
      ApplicationRepository applicationRepository) {
    this.applicantService = checkNotNull(applicantService);
    this.applicationRepository = checkNotNull(applicationRepository);
    this.accountRepository = checkNotNull(accountRepository);
    this.programRepository = checkNotNull(programRepository);
    this.eventRepository = checkNotNull(eventRepository);
    this.emailClient = checkNotNull(emailClient);
    this.messagesApi = checkNotNull(messagesApi);

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
   * Sets the status on the {@code Application}.
   *
   * @param admin The Account that instigated the change.
   */
  public void setStatus(
      ApplicationModel application, StatusEvent newStatusEvent, AccountModel admin)
      throws StatusEmailNotFoundException, StatusNotFoundException, AccountHasNoEmailException {
    ProgramModel program = application.getProgram();
    ApplicantModel applicant = application.getApplicant();
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
    ApplicationEventModel event =
        new ApplicationEventModel(application, Optional.of(admin), details);

    // Send email if requested and present.
    if (sendEmail) {
      if (statusDef.localizedEmailBodyText().isEmpty()) {
        throw new StatusEmailNotFoundException(newStatusText, program.id);
      }
      // Notify an Admin/TI if they applied.
      Optional<String> adminSubmitterEmail = application.getSubmitterEmail();
      if (adminSubmitterEmail.isPresent()) {
        sendAdminSubmitterEmail(
            programRepository.getProgramDefinition(program),
            applicant,
            statusDef,
            adminSubmitterEmail);
      }
      // Notify the applicant.
      ApplicantPersonalInfo personalInfo =
          applicantService.getPersonalInfo(applicant.id).toCompletableFuture().join();
      Optional<String> applicantEmail =
          personalInfo.getType() == ApplicantType.LOGGED_IN
              ? personalInfo.loggedIn().email()
              : Optional.empty();
      if (applicantEmail.isPresent()) {
        sendApplicantEmail(
            programRepository.getProgramDefinition(program), applicant, statusDef, applicantEmail);
      } else {
        // An email was requested to be sent but the applicant doesn't have one.
        throw new AccountHasNoEmailException(applicant.getAccount().id);
      }
    }

    eventRepository.insertSync(event);
  }

  private void sendApplicantEmail(
      ProgramDefinition programDef,
      ApplicantModel applicant,
      Status statusDef,
      Optional<String> applicantEmail) {
    String civiformLink = baseUrl;
    Locale locale = applicant.getApplicantData().preferredLocale();
    Messages messages =
        messagesApi.preferred(ImmutableSet.of(Lang.forCode(locale.toLanguageTag())));
    String programName = programDef.localizedName().getDefault();
    String emailBody =
        String.format(
            "%s\n%s",
            statusDef.localizedEmailBodyText().get().getOrDefault(locale),
            messages.at(MessageKey.EMAIL_LOGIN_TO_CIVIFORM.getKeyName(), civiformLink));
    emailClient.send(
        isStaging ? stagingApplicantNotificationMailingList : applicantEmail.get(),
        messages.at(MessageKey.EMAIL_APPLICATION_UPDATE_SUBJECT.getKeyName(), programName),
        emailBody);
  }

  private void sendAdminSubmitterEmail(
      ProgramDefinition programDef,
      ApplicantModel applicant,
      Status statusDef,
      Optional<String> adminSubmitterEmail) {
    String programName = programDef.localizedName().getDefault();
    String tiDashLink =
        baseUrl
            + controllers.ti.routes.TrustedIntermediaryController.dashboard(
                    /* nameQuery= */ Optional.empty(),
                    /* dayQuery= */ Optional.empty(),
                    /* monthQuery= */ Optional.empty(),
                    /* yearQuery= */ Optional.empty(),
                    /* page= */ Optional.of(1))
                .url();
    if (!adminSubmitterEmail.isPresent()) {
      return;
    }

    Locale locale =
        accountRepository
            .lookupAccountByEmail(adminSubmitterEmail.get())
            .flatMap(AccountModel::newestApplicant)
            .map(ApplicantModel::getApplicantData)
            .map(ApplicantData::preferredLocale)
            .orElse(LocalizedStrings.DEFAULT_LOCALE);
    Messages messages =
        messagesApi.preferred(ImmutableSet.of(Lang.forCode(locale.toLanguageTag())));
    String subject =
        messages.at(
            MessageKey.EMAIL_TI_APPLICATION_UPDATE_SUBJECT.getKeyName(), programName, applicant.id);
    String body =
        String.format(
            "%s\n%s",
            statusDef.localizedEmailBodyText().get().getOrDefault(locale),
            messages.at(MessageKey.EMAIL_TI_MANAGE_YOUR_CLIENTS.getKeyName(), tiDashLink));

    emailClient.send(
        isStaging ? stagingTiNotificationMailingList : adminSubmitterEmail.get(), subject, body);
  }

  /**
   * Sets the note on the {@code Application}.
   *
   * @param admin The Account that instigated the change.
   */
  public void setNote(ApplicationModel application, NoteEvent note, AccountModel admin) {
    ApplicationEventDetails details =
        ApplicationEventDetails.builder()
            .setEventType(ApplicationEventDetails.Type.NOTE_CHANGE)
            .setNoteEvent(note)
            .build();
    ApplicationEventModel event =
        new ApplicationEventModel(application, Optional.of(admin), details);
    eventRepository.insertSync(event);
  }

  /** Returns the note content for {@code application}. */
  public Optional<String> getNote(ApplicationModel application) {
    // The most recent note event is the current value for the note.
    return application.getApplicationEvents().stream()
        .filter(app -> app.getEventType().equals(ApplicationEventDetails.Type.NOTE_CHANGE))
        .findFirst()
        .map(app -> app.getDetails().noteEvent().get().note());
  }

  /**
   * Retrieves the application with the given ID and validates that it is associated with the given
   * program.
   */
  public Optional<ApplicationModel> getApplication(long applicationId, ProgramDefinition program) {
    try {
      return validateProgram(
          applicationRepository.getApplication(applicationId).toCompletableFuture().join(),
          program);
    } catch (ProgramNotFoundException e) {
      return Optional.empty();
    }
  }

  /** Validates that the given application is part of the given program. */
  private Optional<ApplicationModel> validateProgram(
      Optional<ApplicationModel> application, ProgramDefinition program)
      throws ProgramNotFoundException {
    if (application.isEmpty()
        || programRepository
            .getProgramDefinition(application.get().getProgram())
            .adminName()
            .isEmpty()
        || !programRepository
            .getProgramDefinition(application.get().getProgram())
            .adminName()
            .equals(program.adminName())) {
      throw new ProgramNotFoundException("Application or program is empty or mismatched");
    }
    return application;
  }
}
