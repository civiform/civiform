package services;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import models.StatusesModel;
import repository.ProgramRepository;
import repository.StatusRepository;
import services.program.DuplicateStatusException;
import services.program.LocalizationUpdate;
import services.program.OutOfDateStatusesException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.StatusDefinitions;
import services.program.StatusNotFoundForProgram;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.checkerframework.errorprone.com.google.common.base.Preconditions.checkNotNull;

final public class StatusService {
  private final StatusRepository statusRepository;
  private final ProgramRepository programRepository;

  public StatusService(StatusRepository statusRepository, ProgramRepository programRepository){
    this.statusRepository = checkNotNull(statusRepository);
    this.programRepository = checkNotNull(programRepository);
  }
  /**
   * Appends a new status available for application reviews.
   *
   * @param programId The program to update.
   * @param status    The status that should be appended.
   * @throws ProgramNotFoundException If the specified Program could not be found.
   * @throws DuplicateStatusException If the provided status to already exists in the list of
   *                                  available statuses for application review.
   */
  public StatusesModel appendStatus(
    long programId, StatusDefinitions.Status status)
    throws ProgramNotFoundException, DuplicateStatusException, StatusNotFoundForProgram {
    Optional<ProgramDefinition> mayBeProgram = programRepository.getFullProgramDefinitionFromCache(programId);
    if(mayBeProgram.isEmpty()){
      throw new ProgramNotFoundException(programId);
    }
    ProgramDefinition program = mayBeProgram.get();
    Optional<StatusesModel> mayBeStatusModel= statusRepository.getLatestStatus(program.adminName());
    if(mayBeStatusModel.isEmpty()){
      throw new StatusNotFoundForProgram(program.adminName());
    }
    StatusDefinitions statusDefinitions = mayBeStatusModel.get().getStatusDefinitions();
    if (statusDefinitions.getStatuses().stream()
      .filter(s -> s.statusText().equals(status.statusText()))
      .findAny()
      .isPresent()) {
      throw new DuplicateStatusException(status.statusText());
    }
    ImmutableList<StatusDefinitions.Status> currentStatuses =
      statusDefinitions.getStatuses();
    ImmutableList<StatusDefinitions.Status> updatedStatuses =
      status.defaultStatus().orElse(false)
        ? unsetDefaultStatus(currentStatuses, Optional.empty())
        : currentStatuses;

  statusDefinitions
      .setStatuses(
        ImmutableList.<StatusDefinitions.Status>builder()
          .addAll(updatedStatuses)
          .add(status)
          .build());
    return statusRepository.updateStatus(program.adminName(),statusDefinitions);
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
  public ErrorAnd<ProgramDefinition, CiviFormError> editStatus(
    long programId,
    String toReplaceStatusName,
    Function<StatusDefinitions.Status, StatusDefinitions.Status> statusReplacer)
    throws ProgramNotFoundException, DuplicateStatusException {
    ProgramDefinition program = getFullProgramDefinition(programId);
    ImmutableMap<String, Integer> statusNameToIndex =
      statusNameToIndexMap(program.statusDefinitions().getStatuses());
    if (!statusNameToIndex.containsKey(toReplaceStatusName)) {
      return ErrorAnd.error(
        ImmutableSet.of(
          CiviFormError.of(
            "The status being edited no longer exists and may have been modified in a"
              + " separate window.")));
    }
    List<StatusDefinitions.Status> statusesCopy =
      Lists.newArrayList(program.statusDefinitions().getStatuses());
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

    program.statusDefinitions().setStatuses(updatedStatuses);

    return ErrorAnd.of(
      syncProgramDefinitionQuestions(
        programRepository.getShallowProgramDefinition(
          programRepository.updateProgramSync(program.toProgram())))
        .toCompletableFuture()
        .join());
  }

  /**
   * Removes an existing status from the list of available statuses for application reviews.
   *
   * @param programId The program to update.
   * @param toRemoveStatusName The name of the status that should be removed.
   * @throws ProgramNotFoundException If the specified Program could not be found.
   */
  public ErrorAnd<ProgramDefinition, CiviFormError> deleteStatus(
    long programId, String toRemoveStatusName) throws ProgramNotFoundException {
    ProgramDefinition program = getFullProgramDefinition(programId);
    ImmutableMap<String, Integer> statusNameToIndex =
      statusNameToIndexMap(program.statusDefinitions().getStatuses());
    if (!statusNameToIndex.containsKey(toRemoveStatusName)) {
      return ErrorAnd.error(
        ImmutableSet.of(
          CiviFormError.of(
            "The status being deleted no longer exists and may have been deleted in a"
              + " separate window.")));
    }
    List<StatusDefinitions.Status> statusesCopy =
      Lists.newArrayList(.getStatuses());
    statusesCopy.remove(statusNameToIndex.get(toRemoveStatusName).intValue());
    program.statusDefinitions().setStatuses(ImmutableList.copyOf(statusesCopy));

  }

  private static ImmutableMap<String, Integer> statusNameToIndexMap(
    ImmutableList<StatusDefinitions.Status> statuses) {
    return IntStream.range(0, statuses.size())
      .boxed()
      .collect(ImmutableMap.toImmutableMap(i -> statuses.get(i).statusText(), i -> i));
  }

  public ErrorAnd<ProgramDefinition, CiviFormError> updateLocalization(
    long programId, Locale locale, LocalizationUpdate localizationUpdate){
    programId
    // We iterate the existing statuses along with the provided statuses since they were verified
    // to be consistently ordered above.
    ImmutableList.Builder<StatusDefinitions.Status> toUpdateStatusesBuilder =
      ImmutableList.builder();
    for (int statusIdx = 0;
         statusIdx < programDefinition.statusDefinitions().getStatuses().size();
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

    /**
     * Determines whether the list of provided localized application status updates exactly correspond
     * to the list of configured application statuses within the program. This means that:
     * <li>The lists are of the same length
     * <li>Have the exact same ordering of statuses
     */
    private void validateLocalizationStatuses(
      LocalizationUpdate localizationUpdate, ProgramDefinition program)
      throws OutOfDateStatusesException {
      ImmutableList<String> localizationStatusNames =
        localizationUpdate.statuses().stream()
          .map(LocalizationUpdate.StatusUpdate::statusKeyToUpdate)
          .collect(ImmutableList.toImmutableList());
      ImmutableList<String> configuredStatusNames =
        program.statusDefinitions().getStatuses().stream()
          .map(StatusDefinitions.Status::statusText)
          .collect(ImmutableList.toImmutableList());
      if (!localizationStatusNames.equals(configuredStatusNames)) {
        throw new OutOfDateStatusesException();
      }
    }
}
