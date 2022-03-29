package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import io.ebean.DB;
import io.ebean.Transaction;
import models.LifecycleStage;
import models.Program;
import models.Question;
import models.Version;
import org.junit.Before;
import org.junit.Test;
import services.applicant.question.Scalar;
import services.program.ProgramDefinition;
import services.program.predicate.AndNode;
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
  public void setupProgramRepository() {
    versionRepository = instanceOf(VersionRepository.class);
  }

  @Test
  public void testPublish() {
    resourceCreator.insertActiveProgram("foo");
    resourceCreator.insertActiveProgram("bar");
    resourceCreator.insertDraftProgram("bar");
    assertThat(this.versionRepository.getActiveVersion().getPrograms()).hasSize(2);
    assertThat(this.versionRepository.getDraftVersion().getPrograms()).hasSize(1);
    Version oldDraft = this.versionRepository.getDraftVersion();
    this.versionRepository.publishNewSynchronizedVersion();
    assertThat(this.versionRepository.getActiveVersion().getPrograms()).hasSize(2);
    assertThat(this.versionRepository.getDraftVersion().getPrograms()).hasSize(0);
    oldDraft.refresh();
    assertThat(oldDraft.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
  }

  @Test
  public void testRollback() {
    resourceCreator.insertActiveProgram("foo");
    resourceCreator.insertDraftProgram("bar");
    Version oldDraft = this.versionRepository.getDraftVersion();
    Version oldActive = this.versionRepository.getActiveVersion();
    this.versionRepository.publishNewSynchronizedVersion();
    oldDraft.refresh();
    oldActive.refresh();

    assertThat(oldDraft.getPrograms()).hasSize(2);
    assertThat(oldDraft.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);

    this.versionRepository.setLiveVersion(oldActive.id);

    oldActive.refresh();
    oldDraft.refresh();
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    assertThat(oldDraft.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);

    this.versionRepository.setLiveVersion(oldDraft.id);

    oldActive.refresh();
    oldDraft.refresh();
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);
    assertThat(oldDraft.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
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
    //       LEAF2   LEAF3
    PredicateExpressionNode leafOne =
        PredicateExpressionNode.create(
            LeafOperationExpressionNode.create(
                oldOne.id, Scalar.TEXT, Operator.EQUAL_TO, PredicateValue.of("")));
    PredicateExpressionNode leafTwo =
        PredicateExpressionNode.create(
            LeafOperationExpressionNode.create(
                oldTwo.id, Scalar.TEXT, Operator.EQUAL_TO, PredicateValue.of("")));
    PredicateExpressionNode leafThree =
        PredicateExpressionNode.create(
            LeafOperationExpressionNode.create(
                oldOne.id, Scalar.NUMBER, Operator.LESS_THAN, PredicateValue.of(12)));
    PredicateExpressionNode or =
        PredicateExpressionNode.create(OrNode.create(ImmutableSet.of(leafTwo, leafThree)));
    PredicateExpressionNode and =
        PredicateExpressionNode.create(AndNode.create(ImmutableSet.of(leafOne, or)));

    PredicateExpressionNode updated = versionRepository.updatePredicateNode(and);

    // The tree should have the same structure, just with question IDs for the draft version.
    PredicateExpressionNode expectedLeafOne =
        PredicateExpressionNode.create(
            leafOne.getLeafNode().toBuilder().setQuestionId(newOne.id).build());
    PredicateExpressionNode expectedLeafTwo =
        PredicateExpressionNode.create(
            leafTwo.getLeafNode().toBuilder().setQuestionId(newTwo.id).build());
    PredicateExpressionNode expectedLeafThree =
        PredicateExpressionNode.create(
            leafThree.getLeafNode().toBuilder().setQuestionId(newOne.id).build());
    PredicateExpressionNode expectedOr =
        PredicateExpressionNode.create(
            OrNode.create(ImmutableSet.of(expectedLeafTwo, expectedLeafThree)));
    PredicateExpressionNode expectedAnd =
        PredicateExpressionNode.create(
            AndNode.create(ImmutableSet.of(expectedLeafOne, expectedOr)));

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
            .withPredicate(predicate)
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
                .getLeafNode()
                .questionId())
        .isEqualTo(newOne.id);
  }
}
