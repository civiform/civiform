package mapping.admin.programs;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import models.ApplicationModel;
import services.DateConverter;
import services.RandomStringUtils;
import services.applicant.AnswerData;
import services.applicant.Block;
import services.statuses.StatusDefinitions;
import views.admin.programs.ProgramApplicationPageViewModel;
import views.admin.programs.ProgramApplicationPageViewModel.Answer;
import views.admin.programs.ProgramApplicationPageViewModel.BlockCard;
import views.admin.programs.ProgramApplicationPageViewModel.FileLink;
import views.admin.programs.ProgramApplicationPageViewModel.NoteModal;
import views.admin.programs.ProgramApplicationPageViewModel.StatusModal;
import views.admin.programs.ProgramApplicationPageViewModel.StatusOption;
import views.admin.programs.ProgramApplicationPageViewModel.Toast;

/** Maps a submitted application to the ProgramApplicationPageViewModel. */
public final class ProgramApplicationPageMapper {

  /** Filter params of the application list the admin came from, echoed into the back link. */
  public record ListFilters(
      Optional<String> search,
      Optional<String> fromDate,
      Optional<String> toDate,
      Optional<Integer> page,
      Optional<String> selectedApplicationStatus) {}

  /**
   * Maps the application to the view model.
   *
   * @param programId the program the application was submitted to
   * @param programName the program's admin name
   * @param application the submitted application
   * @param applicantNameWithApplicationId "<applicant name> (<application id>)"
   * @param blocks the active blocks of the program version the application was submitted to
   * @param answers the applicant's answers, each belonging to one of {@code blocks}
   * @param statusDefinitions the program's active statuses
   * @param noteMaybe the latest note on the application
   * @param hasEligibilityEnabled whether the program has eligibility enabled
   * @param showDownloadButton whether the current admin may export the application to PDF
   * @param listFilters filter params of the application list, echoed into the back link
   * @param successMessage flash success message to show as a toast
   * @param dateConverter renders the submit and answer timestamps
   */
  public ProgramApplicationPageViewModel map(
      long programId,
      String programName,
      ApplicationModel application,
      String applicantNameWithApplicationId,
      ImmutableList<Block> blocks,
      ImmutableList<AnswerData> answers,
      StatusDefinitions statusDefinitions,
      Optional<String> noteMaybe,
      boolean hasEligibilityEnabled,
      boolean showDownloadButton,
      ListFilters listFilters,
      Optional<String> successMessage,
      DateConverter dateConverter) {
    ListMultimap<Block, AnswerData> blockToAnswers = ArrayListMultimap.create();
    for (AnswerData answer : answers) {
      Block answerBlock =
          blocks.stream()
              .filter(block -> block.getId().equals(answer.blockId()))
              .findFirst()
              .orElseThrow();
      blockToAnswers.put(answerBlock, answer);
    }

    Optional<String> latestStatus = application.getLatestStatus();

    return ProgramApplicationPageViewModel.builder()
        .programId(programId)
        .applicationId(application.id)
        .programName(programName)
        .applicantNameWithApplicationId(applicantNameWithApplicationId)
        .backUrl(buildBackUrl(programId, listFilters))
        .submitTime(
            application.getSubmitTime() == null
                ? null
                : dateConverter.renderDateTimeHumanReadable(application.getSubmitTime()))
        .showDownloadButton(showDownloadButton)
        .downloadUrl(
            controllers.admin.routes.AdminApplicationController.download(programId, application.id)
                .url())
        .hasStatuses(!statusDefinitions.getStatuses().isEmpty())
        .statusSelectorId(RandomStringUtils.randomAlphabetic(8))
        .noStatusSelected(latestStatus.isEmpty())
        .statusOptions(buildStatusOptions(statusDefinitions, latestStatus))
        .hasNote(noteMaybe.isPresent() && !noteMaybe.get().isEmpty())
        .noteModal(
            NoteModal.builder()
                .id(randomModalId())
                .formId(randomModalId())
                .fieldId(RandomStringUtils.randomAlphabetic(8))
                .note(noteMaybe.orElse(null))
                .build())
        .statusModals(buildStatusModals(application, statusDefinitions, latestStatus))
        .blocks(
            blocks.stream()
                .map(
                    block ->
                        buildBlockCard(
                            block, blockToAnswers.get(block), hasEligibilityEnabled, dateConverter))
                .collect(ImmutableList.toImmutableList()))
        .successToast(
            successMessage
                .map(
                    message ->
                        Toast.builder().id(UUID.randomUUID().toString()).message(message).build())
                .orElse(null))
        .build();
  }

  private String buildBackUrl(long programId, ListFilters listFilters) {
    return controllers.admin.routes.AdminApplicationController.index(
            programId,
            listFilters.search(),
            listFilters.page(),
            listFilters.fromDate(),
            listFilters.toDate(),
            listFilters.selectedApplicationStatus(),
            /* selectedApplicationUri= */ Optional.empty(),
            /* showDownloadModal= */ Optional.empty(),
            /* message= */ Optional.empty())
        .url();
  }

  private ImmutableList<StatusOption> buildStatusOptions(
      StatusDefinitions statusDefinitions, Optional<String> latestStatus) {
    String latestStatusText = latestStatus.orElse("");

    return statusDefinitions.getStatuses().stream()
        .map(StatusDefinitions.Status::statusText)
        .map(
            statusText ->
                StatusOption.builder()
                    .text(statusText)
                    .selected(statusText.equals(latestStatusText))
                    .build())
        .collect(ImmutableList.toImmutableList());
  }

  private ImmutableList<StatusModal> buildStatusModals(
      ApplicationModel application,
      StatusDefinitions statusDefinitions,
      Optional<String> latestStatus) {
    Optional<String> accountEmail =
        Optional.ofNullable(application.getApplicant().getAccount().getEmailAddress());
    Optional<String> applicantEmail = application.getApplicant().getEmailAddress();
    boolean hasNoEmail = accountEmail.isEmpty() && applicantEmail.isEmpty();

    // A set so that identical account and applicant emails are listed once.
    ImmutableSet.Builder<String> emails = ImmutableSet.builder();
    accountEmail.ifPresent(emails::add);
    applicantEmail.ifPresent(emails::add);
    String emailAddresses = String.join(" and ", emails.build());

    return statusDefinitions.getStatuses().stream()
        .map(
            status -> {
              StatusModal.EmailMode emailMode;
              if (status.localizedEmailBodyText().isEmpty()) {
                emailMode = StatusModal.EmailMode.NO_EMAIL_CONTENT;
              } else if (hasNoEmail) {
                emailMode = StatusModal.EmailMode.NO_APPLICANT_EMAIL;
              } else {
                emailMode = StatusModal.EmailMode.NOTIFY;
              }

              return StatusModal.builder()
                  .id(randomModalId())
                  .statusText(status.statusText())
                  .previousStatusDisplay(latestStatus.orElse("Unset"))
                  .previousStatusData(latestStatus.orElse(""))
                  .emailMode(emailMode)
                  .emailAddresses(emailMode == StatusModal.EmailMode.NOTIFY ? emailAddresses : null)
                  .build();
            })
        .collect(ImmutableList.toImmutableList());
  }

  private BlockCard buildBlockCard(
      Block block,
      Collection<AnswerData> answers,
      boolean hasEligibilityEnabled,
      DateConverter dateConverter) {
    boolean isEligibilityEnabledInBlock =
        hasEligibilityEnabled && block.getEligibilityDefinition().isPresent();

    return BlockCard.builder()
        .name(block.getName())
        .description(block.getDescription())
        .answers(
            answers.stream()
                .map(
                    answer ->
                        buildAnswer(
                            answer,
                            // If an eligibility predicate is defined for the block, check if
                            // the question is part of the predicate to determine whether to
                            // show the eligibility status.
                            isEligibilityEnabledInBlock
                                && block
                                    .getEligibilityDefinition()
                                    .map(
                                        definition ->
                                            definition
                                                .predicate()
                                                .getQuestions()
                                                .contains(answer.questionDefinition().getId()))
                                    .orElse(false),
                            dateConverter))
                .collect(ImmutableList.toImmutableList()))
        .build();
  }

  private Answer buildAnswer(
      AnswerData answerData, boolean showEligibilityText, DateConverter dateConverter) {
    Answer.AnswerBuilder builder =
        Answer.builder()
            .questionName(answerData.questionDefinition().getName())
            .files(ImmutableList.of())
            .answeredOn(dateConverter.renderDate(Instant.ofEpochMilli(answerData.timestamp())))
            .showEligibilityText(showEligibilityText)
            .eligible(answerData.isEligible());

    if (!answerData.encodedFileKeys().isEmpty()) {
      ImmutableList.Builder<FileLink> files = ImmutableList.builder();
      for (int i = 0; i < answerData.encodedFileKeys().size(); i++) {
        files.add(
            FileLink.builder()
                .name(answerData.fileNames().get(i))
                .href(fileUrl(answerData.encodedFileKeys().get(i)))
                .build());
      }

      return builder.contentType(Answer.ContentType.FILE_LIST).files(files.build()).build();
    }

    if (answerData.encodedFileKey().isPresent()) {
      return builder
          .contentType(Answer.ContentType.FILE_LINK)
          .fileHref(fileUrl(answerData.encodedFileKey().get()))
          .answerText(answerData.answerText())
          .build();
    }

    return builder
        .contentType(Answer.ContentType.TEXT)
        .answerText(answerData.answerText().replace("\n", "; "))
        .build();
  }

  private String fileUrl(String encodedFileKey) {
    return controllers.routes.FileController.acledAdminShow(encodedFileKey).url();
  }

  /** Mirrors the legacy Modal.randomModalId(): ids must start with a letter, UUIDs may not. */
  private String randomModalId() {
    return "uuid-" + UUID.randomUUID();
  }
}
