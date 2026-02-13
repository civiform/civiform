package mapping.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.PredicateUtils;
import controllers.admin.ReadablePredicate;
import java.util.Optional;
import java.util.stream.Collectors;
import models.CategoryModel;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramType;
import services.program.predicate.PredicateDefinition;
import services.question.types.NullQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.StaticContentQuestionDefinition;
import views.admin.programs.ProgramBlockShowPageViewModel;

/** Maps data to the ProgramBlockShowPageViewModel for the program block show page. */
public final class ProgramBlockShowPageMapper {

  public ProgramBlockShowPageViewModel map(
      ProgramDefinition program,
      BlockDefinition block,
      ImmutableList<QuestionDefinition> allPreviousVersionQuestions,
      ImmutableList<QuestionDefinition> allQuestions,
      boolean expandedFormLogicEnabled) {
    String categoriesText =
        program.categories().isEmpty()
            ? "None"
            : program.categories().stream()
                .map(CategoryModel::getDefaultName)
                .collect(Collectors.joining(", "));

    boolean isExternal = program.programType() == ProgramType.EXTERNAL;
    boolean isPreScreener = program.programType() == ProgramType.PRE_SCREENER_FORM;

    ImmutableList<ProgramBlockShowPageViewModel.BlockListItem> blockList =
        buildBlockList(program, program.getNonRepeatedBlockDefinitions(), block.id(), 0);

    ImmutableList<ProgramBlockShowPageViewModel.QuestionItem> questions =
        buildQuestionItems(block, allPreviousVersionQuestions);

    ProgramBlockShowPageViewModel.PredicateData visibilityPredicate =
        buildPredicateData(
            block.visibilityPredicate(),
            block.getFullName(),
            allQuestions,
            expandedFormLogicEnabled);

    boolean showEligibilitySection = !isPreScreener;
    ProgramBlockShowPageViewModel.PredicateData eligibilityPredicate =
        showEligibilitySection
            ? buildPredicateData(
                block.eligibilityDefinition().map(EligibilityDefinition::predicate),
                block.getFullName(),
                allQuestions,
                expandedFormLogicEnabled)
            : ProgramBlockShowPageViewModel.PredicateData.builder()
                .present(false)
                .heading("")
                .conditionList(ImmutableList.of())
                .build();

    return ProgramBlockShowPageViewModel.builder()
        .programName(program.localizedName().getDefault())
        .programDescription(program.localizedDescription().getDefault())
        .adminNote(program.adminDescription())
        .categoriesText(categoriesText)
        .isActive(true)
        .isExternal(isExternal)
        .programId(program.id())
        .programSlug(program.slug())
        .blockList(blockList)
        .selectedBlockId(block.id())
        .blockName(block.getFullName())
        .blockDescription(block.description())
        .questions(questions)
        .visibilityPredicate(visibilityPredicate)
        .showEligibilitySection(showEligibilitySection)
        .eligibilityIsGating(program.eligibilityIsGating())
        .eligibilityPredicate(eligibilityPredicate)
        .build();
  }

  private ProgramBlockShowPageViewModel.PredicateData buildPredicateData(
      Optional<PredicateDefinition> maybePredicate,
      String blockName,
      ImmutableList<QuestionDefinition> allQuestions,
      boolean expandedFormLogicEnabled) {
    if (maybePredicate.isEmpty()) {
      return ProgramBlockShowPageViewModel.PredicateData.builder()
          .present(false)
          .heading("")
          .conditionList(ImmutableList.of())
          .build();
    }
    ReadablePredicate readable =
        PredicateUtils.getReadablePredicateDescription(
            blockName, maybePredicate.get(), allQuestions, expandedFormLogicEnabled);
    ImmutableList<String> conditionList =
        readable.formattedHtmlConditionList().isPresent()
            ? readable.formattedHtmlConditionList().get().stream()
                .map(ut -> ut.render())
                .collect(ImmutableList.toImmutableList())
            : ImmutableList.of();
    return ProgramBlockShowPageViewModel.PredicateData.builder()
        .present(true)
        .heading(readable.formattedHtmlHeading().render())
        .conditionList(conditionList)
        .build();
  }

  private ImmutableList<ProgramBlockShowPageViewModel.BlockListItem> buildBlockList(
      ProgramDefinition program,
      ImmutableList<BlockDefinition> blocks,
      long selectedBlockId,
      int level) {
    ImmutableList.Builder<ProgramBlockShowPageViewModel.BlockListItem> builder =
        ImmutableList.builder();
    for (BlockDefinition block : blocks) {
      int numQuestions = block.getQuestionCount();
      String questionCountText = String.format("Question count: %d", numQuestions);

      ImmutableList<ProgramBlockShowPageViewModel.BlockListItem> children = ImmutableList.of();
      if (block.getIsEnumerator() || block.hasEnumeratorQuestion()) {
        children =
            buildBlockList(
                program,
                program.getBlockDefinitionsForEnumerator(block.id()),
                selectedBlockId,
                level + 1);
      }

      builder.add(
          ProgramBlockShowPageViewModel.BlockListItem.builder()
              .id(block.id())
              .name(block.getFullName())
              .questionCountText(questionCountText)
              .programId(program.id())
              .selected(block.id() == selectedBlockId)
              .hasVisibilityPredicate(block.visibilityPredicate().isPresent())
              .hasNullQuestion(block.hasNullQuestion())
              .indentLevel(level)
              .children(children)
              .build());
    }
    return builder.build();
  }

  private ImmutableList<ProgramBlockShowPageViewModel.QuestionItem> buildQuestionItems(
      BlockDefinition block, ImmutableList<QuestionDefinition> allPreviousVersionQuestions) {
    ImmutableList.Builder<ProgramBlockShowPageViewModel.QuestionItem> builder =
        ImmutableList.builder();
    for (ProgramQuestionDefinition pqd : block.programQuestionDefinitions()) {
      QuestionDefinition qd = pqd.getQuestionDefinition();

      // Try to find from previous versions if it's a NullQuestionDefinition
      if (qd instanceof NullQuestionDefinition) {
        qd =
            allPreviousVersionQuestions.stream()
                .filter(q -> q.getId() == pqd.id())
                .findFirst()
                .orElse(qd);
      }

      boolean isMalformed = qd instanceof NullQuestionDefinition;
      String questionText = isMalformed ? "" : qd.getQuestionText().getDefault();
      String helpText =
          (isMalformed || qd.getQuestionHelpText().isEmpty())
              ? ""
              : qd.getQuestionHelpText().getDefault();

      boolean isStaticContent = qd instanceof StaticContentQuestionDefinition;

      builder.add(
          ProgramBlockShowPageViewModel.QuestionItem.builder()
              .questionText(questionText)
              .helpText(helpText)
              .adminId(qd.getName())
              .questionTypeName(qd.getQuestionType().name())
              .isUniversal(qd.isUniversal())
              .isMalformed(isMalformed)
              .showOptionalLabel(!isStaticContent)
              .isOptional(pqd.optional())
              .isAddress(qd.isAddress())
              .addressCorrectionEnabled(pqd.addressCorrectionEnabled())
              .build());
    }
    return builder.build();
  }
}
