package mapping.admin.migration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.admin.routes;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import mapping.admin.questions.QuestionTypeIconFragments;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.admin.migration.AdminImportProgramDataPartialViewModel;
import views.admin.migration.AdminImportProgramDataPartialViewModel.Block;
import views.admin.migration.AdminImportProgramDataPartialViewModel.DuplicateHandling;
import views.admin.migration.AdminImportProgramDataPartialViewModel.QuestionCard;
import views.components.TextFormatter;

/** Maps data to the AdminImportProgramDataPartialViewModel. */
public final class AdminImportProgramDataPartialMapper {

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
        .programJson(json)
        .hxSaveProgramUrl(routes.AdminImportController.hxSaveProgram().url())
        .startOverUrl(routes.AdminImportController.index().url())
        .showToplevelDuplicateOptions(numDuplicateQuestions > 1)
        .blocks(blocks.build())
        .build();
  }

  private QuestionCard buildQuestionCard(
      QuestionDefinition questionDefinition,
      ImmutableList<String> duplicateQuestionNames,
      ImmutableMap<Long, QuestionDefinition> questionsById) {
    // We use the old admin name (the one inputted by the admin) rather than the de-duped suffixed
    // name, since the admin has not yet decided how to handle this duplicate question. In the event
    // they choose to create a new duplicate, then the de-duped suffixed name will be used by the
    // backend to create a new question.
    String adminName = questionDefinition.getName();
    boolean questionIsDuplicate = duplicateQuestionNames.contains(adminName);
    boolean questionIsRepeated = questionDefinition.getEnumeratorId().isPresent();

    DuplicateHandling duplicateHandling = null;
    if (questionIsDuplicate) {
      duplicateHandling =
          DuplicateHandling.builder()
              .adminName(adminName)
              .enumerator(questionDefinition.isEnumerator())
              .repeated(questionDefinition.isRepeated())
              .existingQuestionUrl(
                  routes.AdminQuestionController.index(Optional.of("Admin ID: " + adminName)).url())
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
    ImmutableList<String> optionTexts = null;
    if (questionDefinition.getQuestionType().isMultiOptionType()) {
      optionTexts =
          ((MultiOptionQuestionDefinition) questionDefinition)
              .getOptions().stream()
                  .map(option -> option.optionText().getDefault())
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
        .optionTexts(optionTexts)
        .duplicateHandling(duplicateHandling)
        .build();
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
