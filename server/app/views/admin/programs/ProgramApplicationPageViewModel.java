package views.admin.programs;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/**
 * ViewModel for the program admin's view of a single submitted application (Thymeleaf).
 *
 * <p>1:1 port of the legacy {@link ProgramApplicationView}; rendered with {@code
 * ProgramApplicationPage.html} inside the transitional {@code LegacyTailwindLayout}.
 */
@Data
@Builder
public final class ProgramApplicationPageViewModel implements BaseViewModel {

  private final long programId;
  private final long applicationId;
  private final String programName;
  // "<applicant name> (<application id>)"
  private final String applicantNameWithApplicationId;

  // Target of the "Back" link, carrying the application list's filter params
  private final String backUrl;

  // Human-readable submit time; null when the application has no submit time recorded
  private final String submitTime;

  private final boolean showDownloadButton;
  private final String downloadUrl;

  // True when the program has statuses configured; shows the status selector and note button
  private final boolean hasStatuses;
  // Random 8-letter id associating the "Status:" label with its select
  private final String statusSelectorId;
  // True when the application has no status yet, selecting the placeholder option
  private final boolean noStatusSelected;
  // Statuses in the order the program defines them
  private final ImmutableList<StatusOption> statusOptions;

  // True when the application already has a non-empty note ("Edit note" vs "Add note")
  private final boolean hasNote;
  private final NoteModal noteModal;
  // One confirmation modal per status, in the same order as statusOptions
  private final ImmutableList<StatusModal> statusModals;

  private final ImmutableList<BlockCard> blocks;

  // Flash success toast; null when there is none
  private final Toast successToast;

  /** One option of the status selector. */
  @Data
  @Builder
  public static final class StatusOption {
    private final String text;
    private final boolean selected;
  }

  /** The "Edit note" modal. */
  @Data
  @Builder
  public static final class NoteModal {
    // "uuid-<random>" modal id; the trigger button is "<id>-button"
    private final String id;
    // Random id of the note form, also referenced by the textarea's form attribute
    private final String formId;
    // Random 8-letter id of the note textarea
    private final String fieldId;
    // Current note text; null when there is none
    private final String note;
  }

  /** The confirmation modal shown when the admin picks a status in the selector. */
  @Data
  @Builder
  public static final class StatusModal {
    /** Which email notice the modal shows below the status change summary. */
    public enum EmailMode {
      // No email body is configured for the status
      NO_EMAIL_CONTENT,
      // The applicant has no account or applicant email address
      NO_APPLICANT_EMAIL,
      // A "Notify ..." checkbox, checked by default
      NOTIFY
    }

    // "uuid-<random>" modal id; the hidden trigger button is "<id>-button"
    private final String id;
    private final String statusText;
    // Current status as displayed, "Unset" when there is none
    private final String previousStatusDisplay;
    // Current status as posted back, "" when there is none
    private final String previousStatusData;
    private final EmailMode emailMode;
    // Account and applicant emails joined with " and "; null unless emailMode is NOTIFY
    private final String emailAddresses;
  }

  /** One screen of the application with its answers. */
  @Data
  @Builder
  public static final class BlockCard {
    private final String name;
    private final String description;
    private final ImmutableList<Answer> answers;
  }

  /** One answered question within a block. */
  @Data
  @Builder
  public static final class Answer {
    /** How the answer content is rendered. */
    public enum ContentType {
      // Multiple uploaded files, each a link, comma separated
      FILE_LIST,
      // A single uploaded file, the answer text linking to it
      FILE_LINK,
      // Plain answer text
      TEXT
    }

    private final String questionName;
    private final ContentType contentType;
    // Uploaded files; empty unless contentType is FILE_LIST
    private final ImmutableList<FileLink> files;
    // Link target of the single file; null unless contentType is FILE_LINK
    private final String fileHref;
    // Answer text with newlines collapsed to "; "; null when contentType is FILE_LIST
    private final String answerText;
    private final String answeredOn;
    private final boolean showEligibilityText;
    private final boolean eligible;
  }

  /** A link to an uploaded file. */
  @Data
  @Builder
  public static final class FileLink {
    private final String name;
    private final String href;
  }

  /** A flash toast message, rendered as a hidden cf-toast-data div. */
  @Data
  @Builder
  public static final class Toast {
    // Random UUID, mirroring ToastMessage's generated id
    private final String id;
    private final String message;
  }
}
