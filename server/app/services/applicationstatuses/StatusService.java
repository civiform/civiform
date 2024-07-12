package services.applicationstatuses;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import repository.ApplicationStatusesRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.program.LocalizationUpdate;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

public final class StatusService {

  private final ApplicationStatusesRepository appStatusesRepo;
  private final ProgramService programService;

  @Inject
  public StatusService(
      ApplicationStatusesRepository applicationStatusesRepository, ProgramService programService) {
    appStatusesRepo = checkNotNull(applicationStatusesRepository);
    this.programService = checkNotNull(programService);
  }

  /**
   * Appends a new status available for application reviews.
   *
   * @param programId The program to update.
   * @param status The status that should be appended.
   * @throws ProgramNotFoundException If the specified Program could not be found.
   * @throws DuplicateStatusException If the provided status to already exists in the list of
   *     available statuses for application review.
   */
  public ErrorAnd<StatusDefinitions, CiviFormError> appendStatus(
      long programId, StatusDefinitions.Status status)
      throws ProgramNotFoundException, DuplicateStatusException {
    ProgramDefinition program = programService.getFullProgramDefinition(programId);
    StatusDefinitions currentStatus =
        appStatusesRepo.lookupActiveStatusDefinitions(program.adminName());
    if (currentStatus.getStatuses().stream()
        .filter(s -> s.statusText().equals(status.statusText()))
        .findAny()
        .isPresent()) {
      throw new DuplicateStatusException(status.statusText());
    }

    ImmutableList<StatusDefinitions.Status> updatedStatuses =
        status.defaultStatus().orElse(false)
            ? unsetDefaultStatus(currentStatus.getStatuses(), Optional.empty())
            : currentStatus.getStatuses();

    appStatusesRepo.createOrUpdateStatusDefinitions(
        program.adminName(),
        new StatusDefinitions()
            .setStatuses(
                ImmutableList.<StatusDefinitions.Status>builder()
                    .addAll(updatedStatuses)
                    .add(status)
                    .build()));
    return ErrorAnd.of(appStatusesRepo.lookupActiveStatusDefinitions(program.adminName()));
  }

  private ImmutableList<StatusDefinitions.Status> unsetDefaultStatus(
      List<StatusDefinitions.Status> statuses, Optional<String> exceptStatusName) {
    return statuses.stream()
        .<StatusDefinitions.Status>map(
            status ->
                exceptStatusName.map(name -> status.matches(name)).orElse(false)
                    ? status
                    : status.toBuilder().setDefaultStatus(Optional.of(false)).build())
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Updates an existing status this is available for application reviews.
   *
   * @param programId The program to update.
   * @param toReplaceStatusName The name of the status that should be updated.
   * @param statusReplacer A single argument function that maps the existing status to the value it
   *     should be updated to. The existing status is provided in case the caller might want to
   *     preserve values from the previous status (e.g. localized text).
   * @throws ProgramNotFoundException If the specified Program could not be found.
   * @throws DuplicateStatusException If the updated status already exists in the list of available
   *     statuses for application review.
   */
  public ErrorAnd<StatusDefinitions, CiviFormError> editStatus(
      long programId,
      String toReplaceStatusName,
      Function<StatusDefinitions.Status, StatusDefinitions.Status> statusReplacer)
      throws ProgramNotFoundException, DuplicateStatusException {
    ProgramDefinition program = programService.getFullProgramDefinition(programId);
    StatusDefinitions currentStatus =
        appStatusesRepo.lookupActiveStatusDefinitions(program.adminName());
    ImmutableMap<String, Integer> statusNameToIndex =
        statusNameToIndexMap(currentStatus.getStatuses());
    if (!statusNameToIndex.containsKey(toReplaceStatusName)) {
      return ErrorAnd.error(
          ImmutableSet.of(
              CiviFormError.of(
                  "The status being edited no longer exists and may have been modified in a"
                      + " separate window.")));
    }
    List<StatusDefinitions.Status> statusesCopy = Lists.newArrayList(currentStatus.getStatuses());
    StatusDefinitions.Status editedStatus =
        statusReplacer.apply(statusesCopy.get(statusNameToIndex.get(toReplaceStatusName)));
    // If the status name was changed and it matches another status, issue an error.
    if (!toReplaceStatusName.equals(editedStatus.statusText())
        && statusNameToIndex.containsKey(editedStatus.statusText())) {
      throw new DuplicateStatusException(editedStatus.statusText());
    }

    statusesCopy.set(statusNameToIndex.get(toReplaceStatusName), editedStatus);
    ImmutableList<StatusDefinitions.Status> updatedStatuses =
        editedStatus.defaultStatus().orElse(false)
            ? unsetDefaultStatus(statusesCopy, Optional.of(editedStatus.statusText()))
            : ImmutableList.copyOf(statusesCopy);
    appStatusesRepo.createOrUpdateStatusDefinitions(
        program.adminName(), new StatusDefinitions().setStatuses(updatedStatuses));

    return ErrorAnd.of(appStatusesRepo.lookupActiveStatusDefinitions(program.adminName()));
  }

  /**
   * Removes an existing status from the list of available statuses for application reviews.
   *
   * @param programId The program to update.
   * @param toRemoveStatusName The name of the status that should be removed.
   * @throws ProgramNotFoundException If the specified Program could not be found.
   */
  public ErrorAnd<StatusDefinitions, CiviFormError> deleteStatus(
      long programId, String toRemoveStatusName) throws ProgramNotFoundException {
    ProgramDefinition program = programService.getFullProgramDefinition(programId);
    StatusDefinitions currentStatus =
        appStatusesRepo.lookupActiveStatusDefinitions(program.adminName());
    ImmutableMap<String, Integer> statusNameToIndex =
        statusNameToIndexMap(currentStatus.getStatuses());
    if (!statusNameToIndex.containsKey(toRemoveStatusName)) {
      return ErrorAnd.error(
          ImmutableSet.of(
              CiviFormError.of(
                  "The status being deleted no longer exists and may have been deleted in a"
                      + " separate window.")));
    }
    List<StatusDefinitions.Status> statusesCopy = Lists.newArrayList(currentStatus.getStatuses());
    statusesCopy.remove(statusNameToIndex.get(toRemoveStatusName).intValue());
    appStatusesRepo.createOrUpdateStatusDefinitions(
        program.adminName(),
        new StatusDefinitions().setStatuses(ImmutableList.copyOf(statusesCopy)));

    return ErrorAnd.of(appStatusesRepo.lookupActiveStatusDefinitions(program.adminName()));
  }

  private static ImmutableMap<String, Integer> statusNameToIndexMap(
      ImmutableList<StatusDefinitions.Status> statuses) {
    return IntStream.range(0, statuses.size())
        .boxed()
        .collect(ImmutableMap.toImmutableMap(i -> statuses.get(i).statusText(), i -> i));
  }

  /**
   * Add or update a localization of the program's publicly-visible display name and description.
   *
   * @param programId the ID of the program to update
   * @param locale the {@link Locale} to update
   * @param localizationUpdate the localization update to apply
   * @return the {@link StatusDefinitions} that was successfully updated, or a set of errors if the
   *     update failed
   * @throws ProgramNotFoundException if the programId does not correspond to a valid program
   * @throws OutOfDateStatusesException if the program's status definitions are out of sync with
   *     those in the provided update
   */
  public ErrorAnd<StatusDefinitions, CiviFormError> updateLocalization(
      long programId, Locale locale, LocalizationUpdate localizationUpdate)
      throws ProgramNotFoundException, OutOfDateStatusesException {
    ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
    StatusDefinitions currentStatusDefinitions =
        appStatusesRepo.lookupActiveStatusDefinitions(programDefinition.adminName());
    validateLocalizationStatuses(localizationUpdate, currentStatusDefinitions);

    // We iterate the existing statuses along with the provided statuses since they were verified
    // to be consistently ordered above.
    ImmutableList.Builder<StatusDefinitions.Status> toUpdateStatusesBuilder =
        ImmutableList.builder();
    for (int statusIdx = 0;
        statusIdx < currentStatusDefinitions.getStatuses().size();
        statusIdx++) {
      LocalizationUpdate.StatusUpdate statusUpdateData =
          localizationUpdate.statuses().get(statusIdx);
      StatusDefinitions.Status existingStatus =
          programDefinition.statusDefinitions().getStatuses().get(statusIdx);
      StatusDefinitions.Status.Builder updateBuilder =
          existingStatus.toBuilder()
              .setLocalizedStatusText(
                  existingStatus
                      .localizedStatusText()
                      .updateTranslation(locale, statusUpdateData.localizedStatusText()));
      // If the status has email content, update the localization to whatever was provided;
      // otherwise if there's a localization update when there is no email content to
      // localize, that indicates a mismatch between the frontend and the database.
      if (existingStatus.localizedEmailBodyText().isPresent()) {
        updateBuilder.setLocalizedEmailBodyText(
            Optional.of(
                existingStatus
                    .localizedEmailBodyText()
                    .get()
                    .updateTranslation(locale, statusUpdateData.localizedEmailBody())));
      } else if (statusUpdateData.localizedEmailBody().isPresent()) {
        throw new OutOfDateStatusesException();
      }
      toUpdateStatusesBuilder.add(updateBuilder.build());
    }
    appStatusesRepo.createOrUpdateStatusDefinitions(
        programDefinition.adminName(),
        new StatusDefinitions().setStatuses(toUpdateStatusesBuilder.build()));

    return ErrorAnd.of(
        appStatusesRepo.lookupActiveStatusDefinitions(programDefinition.adminName()));
  }

  /**
   * Determines whether the list of provided localized application status updates exactly correspond
   * to the list of configured application statuses within the program. This means that:
   * <li>The lists are of the same length
   * <li>Have the exact same ordering of statuses
   */
  private void validateLocalizationStatuses(
      LocalizationUpdate localizationUpdate, StatusDefinitions statusDefinitions)
      throws OutOfDateStatusesException {
    ImmutableList<String> localizationStatusNames =
        localizationUpdate.statuses().stream()
            .map(LocalizationUpdate.StatusUpdate::statusKeyToUpdate)
            .collect(ImmutableList.toImmutableList());
    ImmutableList<String> configuredStatusNames =
        statusDefinitions.getStatuses().stream()
            .map(StatusDefinitions.Status::statusText)
            .collect(ImmutableList.toImmutableList());
    if (!localizationStatusNames.equals(configuredStatusNames)) {
      throw new OutOfDateStatusesException();
    }
  }

  /** Get all the statusDefinitions for a given program */
  public ImmutableList<StatusDefinitions> getAllStatusDefinitions(Long programId)
      throws ProgramNotFoundException {
    ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
    return appStatusesRepo.getAllApplicationStatusModels(programDefinition.adminName()).stream()
        .map(p -> p.getStatusDefinitions())
        .collect(ImmutableList.toImmutableList());
  }
}
