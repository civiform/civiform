package mapping.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.PredicateUtils;
import controllers.admin.ReadablePredicate;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;
import models.CategoryModel;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramType;
import services.question.types.NullQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.StaticContentQuestionDefinition;
import views.admin.programs.ProgramBlockEditPageViewModel;

/** Maps program block data to the ProgramBlockEditPageViewModel for the block edit page. */
public final class ProgramBlockEditPageMapper {

  public ProgramBlockEditPageViewModel map(
      ProgramDefinition program,
      BlockDefinition block,
      ImmutableList<QuestionDefinition> allPreviousVersionQuestions,
      ImmutableList<QuestionDefinition> eligibleQuestions,
      boolean enumeratorImprovementsEnabled,
      boolean apiBridgeEnabled,
      boolean showQuestionBank,
      Optional<String> successMessage,
      Optional<String> errorMessage,
      ImmutableList<QuestionDefinition> allQuestions,
      boolean expandedFormLogicEnabled) {
    long programId = program.id();
    long blockId = block.id();
    boolean isExternal = program.programType() == ProgramType.EXTERNAL;

    String categoriesText =
        program.categories().isEmpty()
            ? "None"
            : program.categories().stream()
                .map(CategoryModel::getDefaultName)
                .collect(Collectors.joining(", "));

    boolean hasMalformedQuestions =
        program.getNonRepeatedBlockDefinitions().stream()
            .anyMatch(BlockDefinition::hasNullQuestion);

    // Block sidebar
    ImmutableList<ProgramBlockEditPageViewModel.BlockListItem> blockList =
        buildEditBlockList(program, program.getNonRepeatedBlockDefinitions(), blockId, 0);

    // Delete modal text
    String deleteModalItemsText = buildDeleteModalItemsText(block);

    // Questions for the current block
    ImmutableList<ProgramBlockEditPageViewModel.EditQuestionItem> questions =
        buildEditQuestionItems(program, block, allPreviousVersionQuestions);

    // Question bank
    ImmutableList<ProgramBlockEditPageViewModel.QuestionBankItem> universalBankQuestions =
        eligibleQuestions.stream()
            .filter(QuestionDefinition::isUniversal)
            .map(this::toQuestionBankItem)
            .collect(ImmutableList.toImmutableList());
    ImmutableList<ProgramBlockEditPageViewModel.QuestionBankItem> nonUniversalBankQuestions =
        eligibleQuestions.stream()
            .filter(q -> !q.isUniversal())
            .map(this::toQuestionBankItem)
            .collect(ImmutableList.toImmutableList());

    // Determine if this is an enumerator block (for special rendering)
    boolean isEnumeratorBlock = enumeratorImprovementsEnabled && block.getIsEnumerator();

    // Can delete block?
    boolean canDelete =
        !block.hasEnumeratorQuestion()
            || program.getBlockDefinitionsForEnumerator(block.id()).isEmpty();

    // Predicate display
    boolean isPrescreenerForm = program.programType() == ProgramType.PRE_SCREENER_FORM;

    Optional<String> visibilityPredicateHeading = Optional.empty();
    Optional<ImmutableList<String>> visibilityPredicateConditionList = Optional.empty();
    if (block.visibilityPredicate().isPresent()) {
      ReadablePredicate rp =
          PredicateUtils.getReadablePredicateDescription(
              block.getFullName(),
              block.visibilityPredicate().get(),
              allQuestions,
              expandedFormLogicEnabled);
      visibilityPredicateHeading = Optional.of(rp.heading());
      visibilityPredicateConditionList = rp.conditionList();
    }

    Optional<String> eligibilityPredicateHeading = Optional.empty();
    Optional<ImmutableList<String>> eligibilityPredicateConditionList = Optional.empty();
    if (block.eligibilityDefinition().isPresent()) {
      ReadablePredicate rp =
          PredicateUtils.getReadablePredicateDescription(
              block.getFullName(),
              block.eligibilityDefinition().get().predicate(),
              allQuestions,
              expandedFormLogicEnabled);
      eligibilityPredicateHeading = Optional.of(rp.heading());
      eligibilityPredicateConditionList = rp.conditionList();
    }

    return ProgramBlockEditPageViewModel.builder()
        .programName(program.localizedName().getDefault())
        .programDescription(program.localizedDescription().getDefault())
        .adminNote(program.adminDescription())
        .categoriesText(categoriesText)
        .isExternal(isExternal)
        .hasMalformedQuestions(hasMalformedQuestions)
        .programSlug(program.slug())
        .apiBridgeEnabled(apiBridgeEnabled)
        .hasEligibilityPredicate(block.eligibilityDefinition().isPresent())
        .hasVisibilityPredicate(block.visibilityPredicate().isPresent())
        .blockList(blockList)
        .programId(programId)
        .selectedBlockId(blockId)
        .enumeratorImprovementsEnabled(enumeratorImprovementsEnabled)
        .blockName(block.getFullName())
        .blockDescription(block.description())
        .isEnumeratorBlock(isEnumeratorBlock)
        .hasEnumeratorQuestion(block.hasEnumeratorQuestion())
        .canDeleteBlock(canDelete)
        .showDeleteButton(program.blockDefinitions().size() > 1)
        .deleteModalItemsText(deleteModalItemsText)
        .blockRawName(block.name())
        .blockRawDescription(block.description())
        .isRepeated(block.isRepeated())
        .namePrefix(block.namePrefix().orElse(""))
        .questions(questions)
        .showQuestionBank(showQuestionBank)
        .universalBankQuestions(universalBankQuestions)
        .nonUniversalBankQuestions(nonUniversalBankQuestions)
        .successMessage(successMessage)
        .errorMessage(errorMessage)
        .eligibilityIsGating(program.eligibilityIsGating())
        .isPrescreenerForm(isPrescreenerForm)
        .visibilityPredicateHeading(visibilityPredicateHeading)
        .visibilityPredicateConditionList(visibilityPredicateConditionList)
        .eligibilityPredicateHeading(eligibilityPredicateHeading)
        .eligibilityPredicateConditionList(eligibilityPredicateConditionList)
        .build();
  }

  private ImmutableList<ProgramBlockEditPageViewModel.BlockListItem> buildEditBlockList(
      ProgramDefinition program,
      ImmutableList<BlockDefinition> blocks,
      long selectedBlockId,
      int level) {
    ImmutableList.Builder<ProgramBlockEditPageViewModel.BlockListItem> builder =
        ImmutableList.builder();
    for (int i = 0; i < blocks.size(); i++) {
      BlockDefinition block = blocks.get(i);
      int numQuestions = block.getQuestionCount();
      String questionCountText = String.format("Question count: %d", numQuestions);

      ImmutableList<ProgramBlockEditPageViewModel.BlockListItem> children = ImmutableList.of();
      if (block.getIsEnumerator() || block.hasEnumeratorQuestion()) {
        children =
            buildEditBlockList(
                program,
                program.getBlockDefinitionsForEnumerator(block.id()),
                selectedBlockId,
                level + 1);
      }

      builder.add(
          ProgramBlockEditPageViewModel.BlockListItem.builder()
              .id(block.id())
              .name(block.getFullName())
              .questionCountText(questionCountText)
              .programId(program.id())
              .selected(block.id() == selectedBlockId)
              .hasVisibilityPredicate(block.visibilityPredicate().isPresent())
              .hasNullQuestion(block.hasNullQuestion())
              .indentLevel(level)
              .showMoveUp(i > 0)
              .showMoveDown(i < blocks.size() - 1)
              .children(children)
              .build());
    }
    return builder.build();
  }

  private String buildDeleteModalItemsText(BlockDefinition block) {
    java.util.ArrayList<String> items = new java.util.ArrayList<>();
    if (block.getQuestionCount() > 0) {
      items.add("questions");
    }
    if (block.eligibilityDefinition().isPresent()) {
      items.add("eligibility conditions");
    }
    if (block.visibilityPredicate().isPresent()) {
      items.add("visibility conditions");
    }
    if (items.isEmpty()) {
      return "";
    }
    if (items.size() == 1) {
      return items.get(0);
    }
    if (items.size() == 2) {
      return items.get(0) + " and " + items.get(1);
    }
    return items.get(0) + ", " + items.get(1) + " and " + items.get(2);
  }

  private ImmutableList<ProgramBlockEditPageViewModel.EditQuestionItem> buildEditQuestionItems(
      ProgramDefinition program,
      BlockDefinition block,
      ImmutableList<QuestionDefinition> allPreviousVersionQuestions) {
    ImmutableList.Builder<ProgramBlockEditPageViewModel.EditQuestionItem> builder =
        ImmutableList.builder();
    ImmutableList<ProgramQuestionDefinition> blockQuestions = block.programQuestionDefinitions();
    long programId = program.id();
    long blockId = block.id();

    boolean canRemoveEnumerator =
        !block.hasEnumeratorQuestion()
            || program.getBlockDefinitionsForEnumerator(block.id()).isEmpty();

    for (int i = 0; i < blockQuestions.size(); i++) {
      ProgramQuestionDefinition pqd = blockQuestions.get(i);
      QuestionDefinition qd = pqd.getQuestionDefinition();

      if (qd instanceof NullQuestionDefinition) {
        qd =
            allPreviousVersionQuestions.stream()
                .filter(q -> q.getId() == pqd.id())
                .findFirst()
                .orElse(qd);
      }

      boolean isMalformed = qd instanceof NullQuestionDefinition;
      boolean isStaticContent = qd instanceof StaticContentQuestionDefinition;
      String questionText = isMalformed ? "" : qd.getQuestionText().getDefault();
      String helpText =
          (isMalformed || qd.getQuestionHelpText().isEmpty())
              ? ""
              : qd.getQuestionHelpText().getDefault();

      builder.add(
          ProgramBlockEditPageViewModel.EditQuestionItem.builder()
              .questionId(qd.getId())
              .questionText(questionText)
              .helpText(helpText)
              .adminId(qd.getName())
              .questionTypeName(qd.getQuestionType().name())
              .isUniversal(qd.isUniversal())
              .isMalformed(isMalformed)
              .showOptionalToggle(!isStaticContent)
              .isOptional(pqd.optional())
              .isAddress(qd.isAddress())
              .addressCorrectionEnabled(pqd.addressCorrectionEnabled())
              .showAddressCorrectionToggle(qd.isAddress())
              .showMoveUp(i > 0)
              .showMoveDown(i < blockQuestions.size() - 1)
              .moveUpPosition(Math.max(0, i - 1))
              .moveDownPosition(Math.min(blockQuestions.size() - 1, i + 1))
              .canRemove(canRemoveEnumerator)
              .programId(programId)
              .blockId(blockId)
              .build());
    }
    return builder.build();
  }

  private ProgramBlockEditPageViewModel.QuestionBankItem toQuestionBankItem(QuestionDefinition qd) {
    String helpText =
        qd.getQuestionHelpText().isEmpty() ? "" : qd.getQuestionHelpText().getDefault();
    String relevantFilterText =
        String.join(
            " ", qd.getQuestionText().getDefault(), helpText, qd.getName(), qd.getDescription());
    return ProgramBlockEditPageViewModel.QuestionBankItem.builder()
        .id(qd.getId())
        .questionText(qd.getQuestionText().getDefault())
        .helpText(helpText)
        .adminId(qd.getName())
        .adminNote(qd.getDescription())
        .isUniversal(qd.isUniversal())
        .questionTypeName(qd.getQuestionType().name())
        .relevantFilterText(relevantFilterText)
        .lastModifiedTime(qd.getLastModifiedTime().orElse(Instant.EPOCH).toString())
        .build();
  }
}
