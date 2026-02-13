package mapping.admin.programs;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import java.time.Instant;
import java.util.Optional;
import models.ApplicationModel;
import services.DateConverter;
import services.applicant.AnswerData;
import services.applicant.Block;
import services.statuses.StatusDefinitions;
import views.admin.programs.ProgramApplicationShowPageViewModel;

/** Maps application data to the ProgramApplicationShowPageViewModel for the show page. */
public final class ProgramApplicationShowPageMapper {

  public ProgramApplicationShowPageViewModel map(
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
      DateConverter dateConverter,
      Optional<String> search,
      Optional<String> fromDate,
      Optional<String> toDate,
      Optional<Integer> page,
      Optional<String> selectedApplicationStatus,
      Optional<String> successMessage) {

    String submitTime;
    if (application.getSubmitTime() == null) {
      submitTime = "Application submitted without submission time marked.";
    } else {
      submitTime = dateConverter.renderDateTimeHumanReadable(application.getSubmitTime());
    }

    // Build status options
    String latestStatusText = application.getLatestStatus().orElse("");
    ImmutableList<ProgramApplicationShowPageViewModel.StatusOption> statusOptions =
        statusDefinitions.getStatuses().stream()
            .map(
                status ->
                    ProgramApplicationShowPageViewModel.StatusOption.builder()
                        .statusText(status.statusText())
                        .isCurrentStatus(status.statusText().equals(latestStatusText))
                        .build())
            .collect(ImmutableList.toImmutableList());

    // Build status update modals
    String previousStatusDisplay = application.getLatestStatus().orElse("Unset");
    String previousStatusData = application.getLatestStatus().orElse("");
    ImmutableList<ProgramApplicationShowPageViewModel.StatusUpdateModal> statusUpdateModals =
        statusDefinitions.getStatuses().stream()
            .map(
                status -> {
                  Optional<String> optionalAccountEmail =
                      Optional.ofNullable(
                          application.getApplicant().getAccount().getEmailAddress());
                  Optional<String> optionalApplicantEmail =
                      application.getApplicant().getEmailAddress();
                  boolean hasEmailContent = status.localizedEmailBodyText().isPresent();
                  boolean hasApplicantEmail =
                      optionalAccountEmail.isPresent() || optionalApplicantEmail.isPresent();
                  String emailString =
                      generateEmailString(optionalAccountEmail, optionalApplicantEmail);

                  return ProgramApplicationShowPageViewModel.StatusUpdateModal.builder()
                      .modalId("status-modal-" + status.statusText().hashCode())
                      .statusText(status.statusText())
                      .previousStatusDisplay(previousStatusDisplay)
                      .previousStatusData(previousStatusData)
                      .applicantName(applicantNameWithApplicationId)
                      .programName(programName)
                      .programId(programId)
                      .applicationId(application.id)
                      .hasEmailContent(hasEmailContent)
                      .hasApplicantEmail(hasApplicantEmail)
                      .emailString(emailString)
                      .isSendEmailCheckedByDefault(hasEmailContent && hasApplicantEmail)
                      .noEmailReason("")
                      .build();
                })
            .collect(ImmutableList.toImmutableList());

    // Build blocks with answers
    ListMultimap<Block, AnswerData> blockToAnswers = ArrayListMultimap.create();
    for (AnswerData answer : answers) {
      Block answerBlock =
          blocks.stream()
              .filter(block -> block.getId().equals(answer.blockId()))
              .findFirst()
              .orElseThrow();
      blockToAnswers.put(answerBlock, answer);
    }

    ImmutableList<ProgramApplicationShowPageViewModel.BlockData> blockDataList =
        blocks.stream()
            .map(
                block -> {
                  boolean isEligibilityEnabledInBlock =
                      hasEligibilityEnabled && block.getEligibilityDefinition().isPresent();
                  ImmutableList<ProgramApplicationShowPageViewModel.AnswerDataView> answerViews =
                      blockToAnswers.get(block).stream()
                          .map(
                              answerData -> {
                                String date =
                                    dateConverter.renderDate(
                                        Instant.ofEpochMilli(answerData.timestamp()));
                                boolean showEligibilityText =
                                    isEligibilityEnabledInBlock
                                        && block
                                            .getEligibilityDefinition()
                                            .map(
                                                definition ->
                                                    definition
                                                        .predicate()
                                                        .getQuestions()
                                                        .contains(
                                                            answerData
                                                                .questionDefinition()
                                                                .getId()))
                                            .orElse(false);
                                String eligibilityText =
                                    answerData.isEligible()
                                        ? "Meets eligibility"
                                        : "Doesn't meet eligibility";

                                // Build file links
                                ImmutableList.Builder<ProgramApplicationShowPageViewModel.FileLink>
                                    fileLinkBuilder = ImmutableList.builder();
                                boolean isFileAnswer = false;
                                String answerText = answerData.answerText();

                                if (!answerData.encodedFileKeys().isEmpty()) {
                                  isFileAnswer = true;
                                  for (int i = 0; i < answerData.encodedFileKeys().size(); i++) {
                                    fileLinkBuilder.add(
                                        ProgramApplicationShowPageViewModel.FileLink.builder()
                                            .fileName(answerData.fileNames().get(i))
                                            .encodedFileKey(answerData.encodedFileKeys().get(i))
                                            .build());
                                  }
                                } else if (answerData.encodedFileKey().isPresent()) {
                                  isFileAnswer = true;
                                  fileLinkBuilder.add(
                                      ProgramApplicationShowPageViewModel.FileLink.builder()
                                          .fileName(answerData.answerText())
                                          .encodedFileKey(answerData.encodedFileKey().get())
                                          .build());
                                } else {
                                  answerText = answerData.answerText().replace("\n", "; ");
                                }

                                return ProgramApplicationShowPageViewModel.AnswerDataView.builder()
                                    .questionName(answerData.questionDefinition().getName())
                                    .answerText(answerText)
                                    .answeredDate(date)
                                    .fileLinks(fileLinkBuilder.build())
                                    .isFileAnswer(isFileAnswer)
                                    .showEligibilityText(showEligibilityText)
                                    .eligibilityText(eligibilityText)
                                    .build();
                              })
                          .collect(ImmutableList.toImmutableList());
                  return ProgramApplicationShowPageViewModel.BlockData.builder()
                      .blockName(block.getName())
                      .blockDescription(block.getDescription())
                      .answers(answerViews)
                      .build();
                })
            .collect(ImmutableList.toImmutableList());

    return ProgramApplicationShowPageViewModel.builder()
        .programId(programId)
        .programName(programName)
        .applicationId(application.id)
        .applicantNameWithApplicationId(applicantNameWithApplicationId)
        .submitTime(submitTime)
        .showDownloadButton(showDownloadButton)
        .successMessage(successMessage)
        .search(search)
        .page(page)
        .fromDate(fromDate)
        .toDate(toDate)
        .selectedApplicationStatus(selectedApplicationStatus)
        .hasStatuses(!statusDefinitions.getStatuses().isEmpty())
        .statusOptions(statusOptions)
        .currentStatusDisplay(application.getLatestStatus().orElse(""))
        .noCurrentStatus(application.getLatestStatus().isEmpty())
        .dropdownPlaceholder("Choose an option:")
        .note(noteMaybe)
        .statusUpdateModals(statusUpdateModals)
        .blocks(blockDataList)
        .build();
  }

  private String generateEmailString(
      Optional<String> optionalAccountEmail, Optional<String> optionalApplicantEmail) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    optionalAccountEmail.ifPresent(builder::add);
    optionalApplicantEmail.ifPresent(builder::add);
    return String.join(" and ", builder.build());
  }
}
