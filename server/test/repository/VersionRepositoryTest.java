package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.ebean.DB;
import io.ebean.Transaction;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import models.LifecycleStage;
import models.Program;
import models.Question;
import models.Version;
import org.junit.Before;
import org.junit.Test;
import services.applicant.question.Scalar;
import services.program.ProgramDefinition;
import services.program.predicate.AndNode;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.OrNode;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateExpressionNodeType;
import services.program.predicate.PredicateValue;
import support.ProgramBuilder;

public class VersionRepositoryTest extends ResetPostgres {
  private VersionRepository versionRepository;

  @Before
  public void setupVersionRepository() {
    versionRepository = instanceOf(VersionRepository.class);
  }

  @Test
  public void testPublish_tombstonesProgramsAndQuestionsOnlyCreatedInTheDraftVersion() {
    Question draftOnlyQuestion = resourceCreator.insertQuestion("draft-only-question");
    draftOnlyQuestion.addVersion(versionRepository.getDraftVersion()).save();

    Program draftOnlyProgram =
        ProgramBuilder.newDraftProgram("draft-only-program").withBlock("Screen 1").build();

    Version draftForTombstoning = versionRepository.getDraftVersion();
    assertThat(draftForTombstoning.addTombstoneForQuestion(draftOnlyQuestion)).isTrue();
    assertThat(draftForTombstoning.addTombstoneForProgramForTest(draftOnlyProgram)).isTrue();
    draftForTombstoning.save();

    assertThat(versionRepository.getActiveVersion().getPrograms()).isEmpty();
    assertThat(versionRepository.getActiveVersion().getTombstonedProgramNames()).isEmpty();
    assertThat(versionRepository.getActiveVersion().getQuestions()).isEmpty();
    assertThat(versionRepository.getActiveVersion().getTombstonedQuestionNames()).isEmpty();
    assertThat(versionRepository.getDraftVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(draftOnlyProgram.id);
    assertThat(versionRepository.getDraftVersion().getTombstonedProgramNames())
        .containsExactly(draftOnlyProgram.getProgramDefinition().adminName());
    assertThat(versionRepository.getDraftVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(draftOnlyQuestion.id);
    assertThat(versionRepository.getDraftVersion().getTombstonedQuestionNames())
        .containsExactly(draftOnlyQuestion.getQuestionDefinition().getName());

    // Publish and ensure that both the program and question aren't carried forward.
    Version updated = versionRepository.previewPublishNewSynchronizedVersion();
    assertThat(updated.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    assertThat(updated.getPrograms()).isEmpty();
    assertThat(updated.getTombstonedProgramNames()).isEmpty();
    assertThat(updated.getQuestions()).isEmpty();
    assertThat(updated.getTombstonedQuestionNames()).isEmpty();
  }

  @Test
  public void testPublish() {
    Question firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(versionRepository.getActiveVersion()).save();
    Question secondQuestion = resourceCreator.insertQuestion("second-question");
    secondQuestion.addVersion(versionRepository.getActiveVersion()).save();

    Program firstProgramActive =
        ProgramBuilder.newActiveProgram("foo")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .build();
    Program secondProgramActive =
        ProgramBuilder.newActiveProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(secondQuestion)
            .build();
    Question secondQuestionUpdated = resourceCreator.insertQuestion("second-question");
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersion()).save();
    Program secondProgramDraft =
        ProgramBuilder.newDraftProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(secondQuestionUpdated)
            .build();

    assertThat(versionRepository.getActiveVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(firstProgramActive.id, secondProgramActive.id);
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestion.id);
    assertThat(versionRepository.getDraftVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(secondProgramDraft.id);
    assertThat(versionRepository.getDraftVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(secondQuestionUpdated.id);

    assertThat(versionRepository.getActiveVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(firstProgramActive.id, secondProgramActive.id);
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestion.id);
    assertThat(versionRepository.getDraftVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(secondProgramDraft.id);

    Version oldDraft = versionRepository.getDraftVersion();
    Version oldActive = versionRepository.getActiveVersion();

    // First, preview the changes and ensure no versions are updated.
    Version toApplyNewActiveVersion = versionRepository.previewPublishNewSynchronizedVersion();
    assertThat(versionRepository.getDraftVersion().id).isEqualTo(oldDraft.id);
    assertThat(versionRepository.getActiveVersion().id).isEqualTo(oldActive.id);
    assertThat(versionRepository.getDraftVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyElementsOf(
            oldDraft.getPrograms().stream()
                .map(p -> p.id)
                .collect(ImmutableList.toImmutableList()));
    assertThat(versionRepository.getDraftVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyElementsOf(
            oldDraft.getQuestions().stream()
                .map(q -> q.id)
                .collect(ImmutableList.toImmutableList()));
    assertThat(versionRepository.getActiveVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyElementsOf(
            oldActive.getPrograms().stream()
                .map(p -> p.id)
                .collect(ImmutableList.toImmutableList()));
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyElementsOf(
            oldActive.getQuestions().stream()
                .map(q -> q.id)
                .collect(ImmutableList.toImmutableList()));
    assertThat(oldDraft.getLifecycleStage()).isEqualTo(LifecycleStage.DRAFT);
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);

    assertThat(toApplyNewActiveVersion.id).isEqualTo(oldDraft.id);
    assertThat(toApplyNewActiveVersion.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    assertThat(toApplyNewActiveVersion.getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(secondProgramDraft.id, firstProgramActive.id);
    assertThat(toApplyNewActiveVersion.getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestionUpdated.id);

    // Now actually publish the version and assert the results.
    versionRepository.publishNewSynchronizedVersion();

    oldDraft.refresh();
    assertThat(oldDraft.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    oldActive.refresh();
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);

    // The newly created draft should not contain any questions or programs.
    assertThat(versionRepository.getDraftVersion().getPrograms()).isEmpty();
    assertThat(versionRepository.getDraftVersion().getQuestions()).isEmpty();

    assertThat(versionRepository.getActiveVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(secondProgramDraft.id, firstProgramActive.id);
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestionUpdated.id);
    oldActive.refresh();
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);
  }

  private Question insertActiveQuestion(String name) {
    Question q = resourceCreator.insertQuestion(name);
    q.addVersion(versionRepository.getActiveVersion()).save();
    return q;
  }

  private Question insertDraftQuestion(String name) {
    Question q = resourceCreator.insertQuestion(name);
    q.addVersion(versionRepository.getDraftVersion()).save();
    return q;
  }

  @Test
  public void testPublishDoesNotUpdateProgramTimestamps() throws InterruptedException {
    ImmutableList<Program> programs =
        ImmutableList.of(
            resourceCreator.insertActiveProgram("active"),
            resourceCreator.insertActiveProgram("other_active"),
            resourceCreator.insertDraftProgram("draft"),
            resourceCreator.insertActiveProgram("active_with_draft"),
            resourceCreator.insertDraftProgram("active_with_draft"));
    ImmutableMap<String, Instant> beforeProgramTimestamps =
        programs.stream()
            .map(Program::getProgramDefinition)
            .collect(
                ImmutableMap.toImmutableMap(
                    program -> String.format("%d %s", program.id(), program.adminName()),
                    program -> program.lastModifiedTime().orElseThrow()));

    ImmutableList<Question> questions =
        ImmutableList.of(
            insertActiveQuestion("active"),
            insertActiveQuestion("other_active"),
            insertDraftQuestion("draft"),
            insertActiveQuestion("active_with_draft"),
            insertDraftQuestion("active_with-draft"));
    ImmutableMap<String, Instant> beforeQuestionTimestamps =
        questions.stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    question ->
                        String.format(
                            "%d %s", question.id, question.getQuestionDefinition().getName()),
                    question ->
                        question.getQuestionDefinition().getLastModifiedTime().orElseThrow()));

    // When persisting models with @WhenModified fields, EBean
    // truncates the persisted timestamp to milliseconds:
    // https://github.com/seattle-uat/civiform/pull/2499#issuecomment-1133325484.
    // Sleep for a few milliseconds to ensure that a subsequent
    // update would have a distinct timestamp.
    TimeUnit.MILLISECONDS.sleep(5);
    versionRepository.publishNewSynchronizedVersion();

    // Refresh each program / question to ensure they get the newest DB state after
    // publishing.
    ImmutableMap<String, Instant> afterProgramTimestamps =
        programs.stream()
            .map(
                p ->
                    DB.getDefault()
                        .find(Program.class)
                        .where()
                        .eq("id", p.id)
                        .findOneOrEmpty()
                        .orElseThrow()
                        .getProgramDefinition())
            .collect(
                ImmutableMap.toImmutableMap(
                    program -> String.format("%d %s", program.id(), program.adminName()),
                    program -> program.lastModifiedTime().orElseThrow()));
    ImmutableMap<String, Instant> afterQuestionTimestamps =
        questions.stream()
            .map(
                q ->
                    DB.getDefault()
                        .find(Question.class)
                        .where()
                        .eq("id", q.id)
                        .findOneOrEmpty()
                        .orElseThrow())
            .collect(
                ImmutableMap.toImmutableMap(
                    question ->
                        String.format(
                            "%d %s", question.id, question.getQuestionDefinition().getName()),
                    question ->
                        question.getQuestionDefinition().getLastModifiedTime().orElseThrow()));

    assertThat(beforeProgramTimestamps).isEqualTo(afterProgramTimestamps);
    assertThat(beforeQuestionTimestamps).isEqualTo(afterQuestionTimestamps);
  }

  @Test
  public void testTransactionality() {
    Transaction outer = DB.getDefault().beginTransaction();
    assertThat(outer.isActive()).isTrue();
    Version draft = versionRepository.getDraftVersion();
    assertThat(outer.isActive()).isTrue();
    Version draft2 = versionRepository.getDraftVersion();
    assertThat(outer.isActive()).isTrue();
    outer.rollback();
    assertThat(outer.isActive()).isFalse();
    assertThat(draft).isEqualTo(draft2);
  }

  @Test
  public void updatePredicateNode() {
    Version draft = versionRepository.getDraftVersion();
    Version active = versionRepository.getActiveVersion();

    // Old versions of questions
    Question oldOne = resourceCreator.insertQuestion("one");
    oldOne.addVersion(active);
    oldOne.save();
    Question oldTwo = resourceCreator.insertQuestion("two");
    oldTwo.addVersion(active);
    oldTwo.save();

    // New versions of questions
    Question newOne = resourceCreator.insertQuestion("one");
    newOne.addVersion(draft);
    newOne.save();
    Question newTwo = resourceCreator.insertQuestion("two");
    newTwo.addVersion(draft);
    newTwo.save();

    // Build a predicate tree that covers all node types:
    //        AND
    //      /     \
    //   LEAF1    OR
    //          /    \
    //       LEAF2   LEAF_ADDRESS
    PredicateExpressionNode leafOne =
        PredicateExpressionNode.create(
            LeafOperationExpressionNode.create(
                oldOne.id, Scalar.TEXT, Operator.EQUAL_TO, PredicateValue.of("")));
    PredicateExpressionNode leafTwo =
        PredicateExpressionNode.create(
            LeafOperationExpressionNode.create(
                oldTwo.id, Scalar.TEXT, Operator.EQUAL_TO, PredicateValue.of("")));
    PredicateExpressionNode leafAddress =
        PredicateExpressionNode.create(LeafAddressServiceAreaExpressionNode.create(oldOne.id, ""));
    PredicateExpressionNode or =
        PredicateExpressionNode.create(OrNode.create(ImmutableList.of(leafTwo, leafAddress)));
    PredicateExpressionNode and =
        PredicateExpressionNode.create(AndNode.create(ImmutableList.of(leafOne, or)));

    PredicateExpressionNode updated = versionRepository.updatePredicateNodeVersions(and);

    // The tree should have the same structure, just with question IDs for the draft version.
    PredicateExpressionNode expectedLeafOne =
        PredicateExpressionNode.create(
            leafOne.getLeafOperationNode().toBuilder().setQuestionId(newOne.id).build());
    PredicateExpressionNode expectedLeafTwo =
        PredicateExpressionNode.create(
            leafTwo.getLeafOperationNode().toBuilder().setQuestionId(newTwo.id).build());
    PredicateExpressionNode expectedLeafThree =
        PredicateExpressionNode.create(
            leafAddress.getLeafAddressNode().toBuilder().setQuestionId(newOne.id).build());
    PredicateExpressionNode expectedOr =
        PredicateExpressionNode.create(
            OrNode.create(ImmutableList.of(expectedLeafTwo, expectedLeafThree)));
    PredicateExpressionNode expectedAnd =
        PredicateExpressionNode.create(
            AndNode.create(ImmutableList.of(expectedLeafOne, expectedOr)));

    assertThat(updated.getType()).isEqualTo(PredicateExpressionNodeType.AND);
    assertThat(updated).isEqualTo(expectedAnd);
  }

  @Test
  public void updateQuestionVersions_updatesAllQuestionsInBlocks() {
    Version draft = versionRepository.getDraftVersion();
    Version active = versionRepository.getActiveVersion();

    // Create some old questions
    Question oldOne = resourceCreator.insertQuestion("one");
    oldOne.addVersion(active);
    oldOne.save();
    Question oldTwo = resourceCreator.insertQuestion("two");
    oldTwo.addVersion(active);
    oldTwo.save();

    // Create new versions of the old questions
    Question newOne = resourceCreator.insertQuestion("one");
    newOne.addVersion(draft);
    newOne.save();
    Question newTwo = resourceCreator.insertQuestion("two");
    newTwo.addVersion(draft);
    newTwo.save();

    // Create a predicate based on the old questions
    PredicateDefinition predicate =
        PredicateDefinition.create(
            PredicateExpressionNode.create(
                LeafOperationExpressionNode.create(
                    oldOne.id, Scalar.NUMBER, Operator.EQUAL_TO, PredicateValue.of(100))),
            PredicateAction.SHOW_BLOCK);

    // Create a program that uses the old questions in blocks and block predicates.
    Program program =
        ProgramBuilder.newDraftProgram("questions-need-updating")
            .withBlock()
            .withRequiredQuestion(oldOne)
            .withBlock()
            .withRequiredQuestion(oldTwo)
            .withVisibilityPredicate(predicate)
            .build();
    program.save();

    versionRepository.updateQuestionVersions(program);
    ProgramDefinition updated =
        versionRepository
            .getDraftVersion()
            .getProgramByName(program.getProgramDefinition().adminName())
            .get()
            .getProgramDefinition();

    assertThat(updated.blockDefinitions()).hasSize(2);
    // Note: compare IDs here directly since ProgramQuestionDefinitions don't have
    // QuestionDefinitions on load.
    assertThat(updated.blockDefinitions().get(0).programQuestionDefinitions().get(0).id())
        .isEqualTo(newOne.getQuestionDefinition().getId());
    assertThat(updated.blockDefinitions().get(1).programQuestionDefinitions().get(0).id())
        .isEqualTo(newTwo.getQuestionDefinition().getId());
    assertThat(
            updated
                .blockDefinitions()
                .get(1)
                .visibilityPredicate()
                .get()
                .rootNode()
                .getLeafOperationNode()
                .questionId())
        .isEqualTo(newOne.id);
  }
}
