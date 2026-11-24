package mapping.admin.migration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Objects;
import mapping.admin.questions.QuestionTypeIconFragments;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.admin.migration.AdminImportProgramDataPartialViewModel;
import views.admin.migration.AdminImportProgramDataPartialViewModel.Block;
import views.admin.migration.AdminImportProgramDataPartialViewModel.DuplicateHandling;
import views.admin.migration.AdminImportProgramDataPartialViewModel.Option;
import views.admin.migration.AdminImportProgramDataPartialViewModel.QuestionCard;
import views.components.TextFormatter;

/** Maps data to the AdminImportProgramDataPartialViewModel. */
public final class AdminImportProgramDataPartialMapper {

  public static final String TOP_LEVEL_EXISTING_IMAGE_WARNING =
      "Question images are not included in program exports. Overwriting an existing question keeps"
          + " its current image; new or duplicated questions keep their image alt text and will"
          + " need their images uploaded manually.";

  public static final String TOP_LEVEL_IMPORTED_ALT_TEXT_WARNING =
      "Question images are not included in program exports. Questions with image alt text keep"
          + " their alt text and will need their images uploaded manually after importing.";

  public static final String NEW_QUESTION_IMAGE_WARNING =
      "This question includes image alt text, but question images are not included in program"
          + " exports. Upload the image manually after importing.";

  public static final String DUPLICATE_EXISTING_IMAGE_WARNING =
      "Question images are not included in program exports. Overwriting this question will keep its"
          + " existing image; creating a new duplicate will not include the image.";

  public static final String DUPLICATE_IMPORTED_ALT_TEXT_WARNING =
      "This question includes image alt text, but question images are not included in program"
          + " exports. Upload the image manually after importing if you create a duplicate or"
          + " overwrite the existing question.";

  /**
   * Builds the view model for the parsed program preview.
   *
   * <p>Like the legacy view, this evaluates the question summary alert before checking {@code
   * questions} for null, so a null questions list throws and the controller's catch block renders
   * the error partial instead.
   */
  public AdminImportProgramDataPartialViewModel map(
      ProgramDefinition program,
      ImmutableList<QuestionDefinition> questions,
      ImmutableList<String> duplicateQuestionNames,
      ImmutableSet<String> existingQuestionNamesWithImages,
      String json) {
    int numDuplicateQuestions = duplicateQuestionNames.size();
    int numNewQuestions = questions.size() - numDuplicateQuestions;

    boolean hasDuplicates = numDuplicateQuestions > 0;
    StringBuilder alertText = new StringBuilder("This program ");
    if (numNewQuestions > 0) {
      alertText.append(
          String.format(
              "will add %d new question%s to the question bank",
              numNewQuestions, numNewQuestions > 1 ? "s" : ""));
      if (hasDuplicates) {
        alertText.append(" and ");
      }
    }
    if (hasDuplicates) {
      alertText.append(
          String.format(
              "contains %d duplicate question%s that must be resolved",
              numDuplicateQuestions, numDuplicateQuestions > 1 ? "s" : ""));
    }
    alertText.append(".");

    boolean anyExistingDuplicateHasImage =
        duplicateQuestionNames.stream().anyMatch(existingQuestionNamesWithImages::contains);
    boolean anyImportedQuestionHasAltText =
        questions.stream().anyMatch(AdminImportProgramDataPartialMapper::hasNonBlankImageAltText);
    boolean showQuestionImageAlert = anyExistingDuplicateHasImage || anyImportedQuestionHasAltText;
    String questionImageAlertText =
        !showQuestionImageAlert
            ? null
            : (anyExistingDuplicateHasImage
                ? TOP_LEVEL_EXISTING_IMAGE_WARNING
                : TOP_LEVEL_IMPORTED_ALT_TEXT_WARNING);

    // If there are no questions in the program, the "questions" field will not
    // be included in the JSON and questions will be null here.
    ImmutableMap<Long, QuestionDefinition> questionsById =
        questions == null
            ? ImmutableMap.of()
            : questions.stream()
                .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getId, qd -> qd));

    ImmutableList.Builder<Block> blocks = ImmutableList.builder();
    for (BlockDefinition block : program.blockDefinitions()) {
      ImmutableList.Builder<QuestionCard> cards = ImmutableList.builder();
      if (!questionsById.isEmpty()) {
        for (ProgramQuestionDefinition question : block.programQuestionDefinitions()) {
          cards.add(
              buildQuestionCard(
                  Objects.requireNonNull(questionsById.get(question.id())),
                  duplicateQuestionNames,
                  existingQuestionNamesWithImages,
                  questionsById));
        }
      }

      blocks.add(
          Block.builder()
              .name(block.name())
              .description(block.description())
              .questionCards(cards.build())
              .build());
    }

    return AdminImportProgramDataPartialViewModel.builder()
        .programTitle(program.localizedName().getDefault())
        .adminName(program.adminName())
        .showQuestionAlert(!questions.isEmpty())
        .questionAlertTypeClass(hasDuplicates ? "usa-alert--warning" : "usa-alert--info")
        .questionAlertText(alertText.toString())
        .showQuestionImageAlert(showQuestionImageAlert)
        .questionImageAlertText(questionImageAlertText)
        .programJson(json)
        .showToplevelDuplicateOptions(numDuplicateQuestions > 1)
        .blocks(blocks.build())
        .build();
  }

  private QuestionCard buildQuestionCard(
      QuestionDefinition questionDefinition,
      ImmutableList<String> duplicateQuestionNames,
      ImmutableSet<String> existingQuestionNamesWithImages,
      ImmutableMap<Long, QuestionDefinition> questionsById) {
    // We use the old admin name (the one inputted by the admin) rather than the de-duped suffixed
    // name, since the admin has not yet decided how to handle this duplicate question. In the event
    // they choose to create a new duplicate, then the de-duped suffixed name will be used by the
    // backend to create a new question.
    String adminName = questionDefinition.getName();
    boolean questionIsDuplicate = duplicateQuestionNames.contains(adminName);
    boolean questionIsRepeated = questionDefinition.getEnumeratorId().isPresent();
    String imageWarningText =
        buildImageWarningText(
            questionDefinition,
            questionIsDuplicate,
            questionIsDuplicate && existingQuestionNamesWithImages.contains(adminName));

    DuplicateHandling duplicateHandling = null;
    if (questionIsDuplicate) {
      duplicateHandling =
          DuplicateHandling.builder()
              .adminName(adminName)
              .enumerator(questionDefinition.isEnumerator())
              .repeated(questionDefinition.isRepeated())
              .build();
    }

    String universalBadgeText = null;
    if (questionDefinition.isUniversal()) {
      universalBadgeText =
          String.format(
              "Universal %s question",
              questionDefinition.getQuestionType().getLabel().toLowerCase(Locale.getDefault()));
    }

    // Only shown during program import, so the option list is rendered for
    // every multi-option question type.
    ImmutableList<Option> options = null;
    if (questionDefinition.getQuestionType().isMultiOptionType()) {
      options =
          ((MultiOptionQuestionDefinition) questionDefinition)
              .getOptions().stream()
                  .map(
                      option ->
                          Option.builder()
                              .text(option.optionText().getDefault())
                              .score(option.score().map(QuestionOption::formatScore).orElse(null))
                              .build())
                  .collect(ImmutableList.toImmutableList());
    }

    String helpText =
        questionDefinition.getQuestionHelpText().isEmpty()
            ? ""
            : questionDefinition.getQuestionHelpText().getDefault();

    return QuestionCard.builder()
        .adminName(adminName)
        .questionTextHtml(formatForAdmins(questionDefinition.getQuestionText().getDefault()))
        .helpTextHtml(formatForAdmins(helpText))
        .iconFragment(
            QuestionTypeIconFragments.questionTypeIconFragment(
                questionDefinition.getQuestionType()))
        .universalBadgeText(universalBadgeText)
        .duplicate(questionIsDuplicate)
        .enumeratorName(
            questionIsRepeated
                ? questionsById.get(questionDefinition.getEnumeratorId().get()).getName()
                : null)
        .options(options)
        .duplicateHandling(duplicateHandling)
        .imageWarningText(imageWarningText)
        .build();
  }

  private static String buildImageWarningText(
      QuestionDefinition questionDefinition, boolean questionIsDuplicate, boolean existingHasImage) {
    boolean hasImportedAltText = hasNonBlankImageAltText(questionDefinition);
    if (!questionIsDuplicate && hasImportedAltText) {
      return NEW_QUESTION_IMAGE_WARNING;
    }
    if (existingHasImage) {
      return DUPLICATE_EXISTING_IMAGE_WARNING;
    }
    if (questionIsDuplicate && hasImportedAltText) {
      return DUPLICATE_IMPORTED_ALT_TEXT_WARNING;
    }
    return null;
  }

  private static boolean hasNonBlankImageAltText(QuestionDefinition questionDefinition) {
    return questionDefinition
        .getLocalizedImageDescription()
        .filter(
            description ->
                description.translations().values().stream().anyMatch(value -> !value.isBlank()))
        .isPresent();
  }

  private static String formatForAdmins(String text) {
    // Same parameters as TextFormatter.formatTextForAdmins, which the legacy
    // view used.
    return TextFormatter.formatTextToSanitizedHTML(
        text,
        /* preserveEmptyLines= */ false,
        /* addRequiredIndicator= */ false,
        /* ariaLabelForNewTabs= */ "opens in a new tab");
  }
}
