package views.admin.migration;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import services.RandomStringUtils;
import views.BaseViewModel;

/** ViewModel for the program-preview htmx partial (Thymeleaf). */
@Data
@Builder
public final class AdminImportProgramDataPartialViewModel implements BaseViewModel {

  // Program display name.
  private final String programTitle;

  // Program admin name.
  private final String adminName;

  // Whether to show the new/duplicate question summary alert.
  private final boolean showQuestionAlert;

  // Question summary alert type class: "usa-alert--info", or
  // "usa-alert--warning" when there are duplicate questions.
  private final String questionAlertTypeClass;

  // Question summary alert text.
  private final String questionAlertText;

  // JSON representation of the program, carried in a hidden textarea so it is
  // posted along with the duplicate-handling selections.
  private final String programJson;

  // htmx endpoint that saves the imported program.
  private final String hxSaveProgramUrl;

  // "Delete and start over" button redirect target (the import page).
  private final String startOverUrl;

  // Whether to show the radio group that handles all duplicate questions at
  // once (only when there is more than one duplicate).
  private final boolean showToplevelDuplicateOptions;

  // Program screens in definition order.
  private final ImmutableList<Block> blocks;

  /**
   * Generates a random field id for fields without an explicit id (labels need an id to stay
   * associated with their inputs).
   */
  public String randomFieldId() {
    return RandomStringUtils.randomAlphabetic(8);
  }

  /** One program screen and its question cards. */
  @Data
  @Builder
  public static final class Block {
    private final String name;
    private final String description;
    private final ImmutableList<QuestionCard> questionCards;
  }

  /** One imported question card. */
  @Data
  @Builder
  public static final class QuestionCard {
    private final String adminName;

    // Question text and help text pre-rendered as sanitized HTML (the legacy
    // view formatted them with TextFormatter).
    private final String questionTextHtml;
    private final String helpTextHtml;

    // Question type icon fragment name in LegacySvgFragments.html.
    private final String iconFragment;

    // Universal question badge text. Null hides the badge.
    private final String universalBadgeText;

    // Whether the question duplicates an existing question (switches the
    // new/duplicate badge).
    private final boolean duplicate;

    // Admin name of the question's enumerator. Null unless the question is
    // repeated; rendered as the card's data-enumerator attribute.
    private final String enumeratorName;

    // Option texts for multi-option question types. Null for other types.
    private final ImmutableList<String> optionTexts;

    // Duplicate-handling radio group data. Null unless the question is a
    // duplicate.
    private final DuplicateHandling duplicateHandling;
  }

  /** Data for one duplicate question's handling options. */
  @Data
  @Builder
  public static final class DuplicateHandling {
    // Admin name of the question, appended to the radio group's field name
    // prefix.
    private final String adminName;

    // Whether the question is an enumerator (shows the hidden repeated-
    // questions warning).
    private final boolean enumerator;

    // Whether the question is repeated (shows the hidden disabled-options
    // warning).
    private final boolean repeated;

    // Link target for the "existing question" links (the question list
    // filtered to this admin name).
    private final String existingQuestionUrl;
  }
}
