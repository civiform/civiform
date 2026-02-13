package views.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public final class ProgramApplicationShowPageViewModel implements BaseViewModel {
  private final long programId;
  private final String programName;
  private final long applicationId;
  private final String applicantNameWithApplicationId;
  private final String submitTime;
  private final boolean showDownloadButton;
  private final Optional<String> successMessage;

  // Search/filter state for back navigation
  private final Optional<String> search;
  private final Optional<Integer> page;
  private final Optional<String> fromDate;
  private final Optional<String> toDate;
  private final Optional<String> selectedApplicationStatus;

  // Status tracking
  private final boolean hasStatuses;
  private final ImmutableList<StatusOption> statusOptions;
  private final String currentStatusDisplay;
  private final boolean noCurrentStatus;
  private final String dropdownPlaceholder;

  // Note
  private final Optional<String> note;

  // Status update confirmation modals
  private final ImmutableList<StatusUpdateModal> statusUpdateModals;

  // Application blocks and answers
  private final ImmutableList<BlockData> blocks;

  public String getBackUrl() {
    return routes.AdminApplicationController.index(
            programId,
            search,
            page,
            fromDate,
            toDate,
            selectedApplicationStatus,
            Optional.empty(),
            Optional.empty(),
            Optional.empty())
        .url();
  }

  public Optional<String> getDownloadUrl() {
    return showDownloadButton
        ? Optional.of(routes.AdminApplicationController.download(programId, applicationId).url())
        : Optional.empty();
  }

  @Data
  @Builder
  public static final class StatusOption {
    private final String statusText;
    private final boolean isCurrentStatus;
  }

  @Data
  @Builder
  public static final class StatusUpdateModal {
    private final String modalId;
    private final String statusText;
    private final String previousStatusDisplay;
    private final String previousStatusData;
    private final String applicantName;
    private final String programName;
    private final long programId;
    private final long applicationId;
    // Email section
    private final boolean hasEmailContent;
    private final boolean hasApplicantEmail;
    private final String emailString;
    private final boolean isSendEmailCheckedByDefault;
    private final String noEmailReason;
  }

  @Data
  @Builder
  public static final class BlockData {
    private final String blockName;
    private final String blockDescription;
    private final ImmutableList<AnswerDataView> answers;
  }

  @Data
  @Builder
  public static final class AnswerDataView {
    private final String questionName;
    private final String answerText;
    private final String answeredDate;
    // File links
    private final ImmutableList<FileLink> fileLinks;
    private final boolean isFileAnswer;
    // Eligibility
    private final boolean showEligibilityText;
    private final String eligibilityText;
  }

  @Data
  @Builder
  public static final class FileLink {
    private final String fileName;
    private final String encodedFileKey;

    public String getUrl() {
      return controllers.routes.FileController.acledAdminShow(encodedFileKey).url();
    }
  }
}
