package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.ebean.DB;
import io.ebean.Transaction;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import models.LifecycleStage;
import models.Program;
import models.Question;
import models.Version;
import org.junit.Before;
import org.junit.Test;
import services.applicant.question.Scalar;
import services.program.CantPublishProgramWithSharedQuestionsException;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
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
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;

public class VersionRepositoryTest extends ResetPostgres {
  private VersionRepository versionRepository;

  @Before
  public void setupVersionRepository() {
    versionRepository = instanceOf(VersionRepository.class);
  }

  @Test
  public void testPublish_tombstonesProgramsAndQuestionsOnlyCreatedInTheDraftVersion()
      throws Exception {
    Question draftOnlyQuestion = resourceCreator.insertQuestion("draft-only-question");
    draftOnlyQuestion.addVersion(versionRepository.getDraftVersionOrCreate()).save();

    Program draftOnlyProgram =
        ProgramBuilder.newDraftProgram("draft-only-program").withBlock("Screen 1").build();

    Version draftForTombstoning = versionRepository.getDraftVersionOrCreate();
    assertThat(draftForTombstoning.addTombstoneForQuestion(draftOnlyQuestion)).isTrue();
    assertThat(draftForTombstoning.addTombstoneForProgramForTest(draftOnlyProgram)).isTrue();
    draftForTombstoning.save();

    assertThat(versionRepository.getActiveVersion().getPrograms()).isEmpty();
    assertThat(versionRepository.getActiveVersion().getTombstonedProgramNames()).isEmpty();
    assertThat(versionRepository.getActiveVersion().getQuestions()).isEmpty();
    assertThat(versionRepository.getActiveVersion().getTombstonedQuestionNames()).isEmpty();
    assertThat(versionRepository.getDraftVersionOrCreate().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(draftOnlyProgram.id);
    assertThat(versionRepository.getDraftVersionOrCreate().getTombstonedProgramNames())
        .containsExactly(draftOnlyProgram.getProgramDefinition().adminName());
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(draftOnlyQuestion.id);
    assertThat(versionRepository.getDraftVersionOrCreate().getTombstonedQuestionNames())
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
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersionOrCreate()).save();
    Program secondProgramDraft =
        ProgramBuilder.newDraftProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(secondQuestionUpdated)
            .build();

    assertThat(versionRepository.getActiveVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(firstProgramActive.id, secondProgramActive.id);
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestion.id);
    assertThat(versionRepository.getDraftVersionOrCreate().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(secondProgramDraft.id);
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(secondQuestionUpdated.id);

    Version oldDraft = versionRepository.getDraftVersionOrCreate();
    Version oldActive = versionRepository.getActiveVersion();

    // First, preview the changes and ensure no versions are updated.
    Version toApplyNewActiveVersion = versionRepository.previewPublishNewSynchronizedVersion();
    assertThat(versionRepository.getDraftVersionOrCreate().id).isEqualTo(oldDraft.id);
    assertThat(versionRepository.getActiveVersion().id).isEqualTo(oldActive.id);
    assertThat(versionRepository.getDraftVersionOrCreate().getPrograms().stream().map(p -> p.id))
        .containsExactlyElementsOf(
            oldDraft.getPrograms().stream()
                .map(p -> p.id)
                .collect(ImmutableList.toImmutableList()));
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions().stream().map(q -> q.id))
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
    assertThat(versionRepository.getDraftVersionOrCreate().getPrograms()).isEmpty();
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions()).isEmpty();

    assertThat(versionRepository.getActiveVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(secondProgramDraft.id, firstProgramActive.id);
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestionUpdated.id);
    oldActive.refresh();
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);
  }

  @Test
  public void testPublishWithQuestionsNotIncludedInPrograms() throws Exception {
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

    Version draftForTombstoning = versionRepository.getDraftVersionOrCreate();
    draftForTombstoning.addQuestion(firstQuestion).save();
    assertThat(draftForTombstoning.addTombstoneForQuestion(firstQuestion)).isTrue();
    Question secondQuestionUpdated = resourceCreator.insertQuestion("second-question");
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersionOrCreate()).save();

    assertThat(versionRepository.getActiveVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(firstProgramActive.id, secondProgramActive.id);
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestion.id);
    assertThat(versionRepository.getDraftVersionOrCreate().getPrograms()).isEmpty();
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestionUpdated.id);

    // Trying to publish program without calling updateProgramsThatReferenceQuestion
    assertThatThrownBy(() -> versionRepository.publishNewSynchronizedVersion())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void testPublishWithDraftQuestionsAndActivePrograms() throws Exception {
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

    Version draftForTombstoning = versionRepository.getDraftVersionOrCreate();
    draftForTombstoning.addQuestion(firstQuestion).save();
    assertThat(draftForTombstoning.addTombstoneForQuestion(firstQuestion)).isTrue();
    Question secondQuestionUpdated = resourceCreator.insertQuestion("second-question");
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersionOrCreate()).save();
    versionRepository.updateProgramsThatReferenceQuestion(secondQuestion.id);

    assertThat(versionRepository.getActiveVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(firstProgramActive.id, secondProgramActive.id);
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestion.id);
    // Second program is in draft, since a question within it was updated.
    Program newSecondProgram = versionRepository.getDraftVersionOrCreate().getPrograms().get(0);
    assertThat(
            versionRepository.getDraftVersionOrCreate().getPrograms().stream()
                .map(p -> p.getProgramDefinition().adminName()))
        .containsExactlyInAnyOrder(secondProgramActive.getProgramDefinition().adminName());
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestionUpdated.id);

    Version oldDraft = versionRepository.getDraftVersionOrCreate();
    Version oldActive = versionRepository.getActiveVersion();

    versionRepository.publishNewSynchronizedVersion();

    oldDraft.refresh();
    assertThat(oldDraft.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    oldActive.refresh();
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);

    // The newly created draft should not contain any questions or programs.
    assertThat(versionRepository.getDraftVersionOrCreate().getPrograms()).isEmpty();
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions()).isEmpty();

    assertThat(versionRepository.getActiveVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(newSecondProgram.id, firstProgramActive.id);
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestionUpdated.id);
    oldActive.refresh();
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);
  }

  @Test
  public void testPublishWithDraftQuestionsAndNoActiveOrDraftPrograms() throws Exception {
    Question firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(versionRepository.getActiveVersion()).save();
    Question secondQuestion = resourceCreator.insertQuestion("second-question");
    secondQuestion.addVersion(versionRepository.getActiveVersion()).save();

    Version draftForTombstoning = versionRepository.getDraftVersionOrCreate();
    draftForTombstoning.addQuestion(firstQuestion).save();
    assertThat(draftForTombstoning.addTombstoneForQuestion(firstQuestion)).isTrue();
    Question secondQuestionUpdated = resourceCreator.insertQuestion("second-question");
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersionOrCreate()).save();

    assertThat(versionRepository.getActiveVersion().getPrograms()).isEmpty();
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestion.id);
    assertThat(versionRepository.getDraftVersionOrCreate().getPrograms()).isEmpty();
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestionUpdated.id);

    Version oldDraft = versionRepository.getDraftVersionOrCreate();
    Version oldActive = versionRepository.getActiveVersion();

    versionRepository.publishNewSynchronizedVersion();

    oldDraft.refresh();
    assertThat(oldDraft.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    oldActive.refresh();
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);

    // The newly created draft should not contain any questions or programs.
    assertThat(versionRepository.getDraftVersionOrCreate().getPrograms()).isEmpty();
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions()).isEmpty();

    assertThat(versionRepository.getActiveVersion().getPrograms()).isEmpty();
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestionUpdated.id);
    oldActive.refresh();
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);
  }

  @Test
  public void testPublishProgram() throws Exception {
    Question firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(versionRepository.getActiveVersion()).save();
    Question secondQuestion = resourceCreator.insertQuestion("second-question");
    secondQuestion.addVersion(versionRepository.getActiveVersion()).save();
    // Third question is only a draft.
    Question thirdQuestion = resourceCreator.insertQuestion("third-question");
    thirdQuestion.addVersion(versionRepository.getDraftVersionOrCreate()).save();

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
    Program thirdProgramActive =
        ProgramBuilder.newActiveProgram("baz")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .build();

    // secondProgramDraft and its question, secondQuestionUpdated, should be published.
    Question secondQuestionUpdated = resourceCreator.insertQuestion("second-question");
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersionOrCreate()).save();
    Program secondProgramDraft =
        ProgramBuilder.newDraftProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(secondQuestionUpdated)
            .build();

    // thirdProgramDraft should not be published.
    Program thirdProgramDraft =
        ProgramBuilder.newDraftProgram("baz")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .build();

    // Validate versions are as expected before publishing.
    assertThat(versionRepository.getActiveVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(
            firstProgramActive.id, secondProgramActive.id, thirdProgramActive.id);
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestion.id);
    assertThat(versionRepository.getDraftVersionOrCreate().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(secondProgramDraft.id, thirdProgramDraft.id);
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(secondQuestionUpdated.id, thirdQuestion.id);

    Version oldDraft = versionRepository.getDraftVersionOrCreate();
    Version oldActive = versionRepository.getActiveVersion();

    // Publish the second program.
    versionRepository.publishNewSynchronizedVersion("bar");

    // Verify LifecyleStages are updated.
    oldDraft.refresh();
    assertThat(oldDraft.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    oldActive.refresh();
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);

    // The newly created draft should contain the remaining draft programs and questions.
    assertThat(versionRepository.getDraftVersionOrCreate().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(thirdProgramDraft.id);
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(thirdQuestion.id);

    // The active version should contain the newly published program and question and the existing
    // active programs and questions.
    assertThat(versionRepository.getActiveVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(
            secondProgramDraft.id, firstProgramActive.id, thirdProgramActive.id);
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestionUpdated.id);
  }

  @Test
  public void testPublishProgramWithNewProgram() throws Exception {
    Question firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(versionRepository.getActiveVersion()).save();
    Question secondQuestion = resourceCreator.insertQuestion("second-question");
    secondQuestion.addVersion(versionRepository.getActiveVersion()).save();

    Program firstProgramActive =
        ProgramBuilder.newActiveProgram("foo")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .build();
    Question secondQuestionUpdated = resourceCreator.insertQuestion("second-question");
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersionOrCreate()).save();

    // Program being published has no existing active version.
    Program secondProgramDraft =
        ProgramBuilder.newDraftProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .build();

    Version oldDraft = versionRepository.getDraftVersionOrCreate();
    Version oldActive = versionRepository.getActiveVersion();

    versionRepository.publishNewSynchronizedVersion("bar");

    oldDraft.refresh();
    assertThat(oldDraft.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    oldActive.refresh();
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);

    // The newly created draft should contain the remaining drafts.
    assertThat(versionRepository.getDraftVersionOrCreate().getPrograms()).hasSize(0);
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(secondQuestionUpdated.id);

    // The active version should contain the newly published program and the existing active
    // programs and questions.
    assertThat(versionRepository.getActiveVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(secondProgramDraft.id, firstProgramActive.id);
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestion.id);
  }

  @Test
  public void testPublishProgramDoesNotAllowPublishingWhenQuestionsAreShared() throws Exception {
    Question firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(versionRepository.getActiveVersion()).save();
    Question secondQuestion = resourceCreator.insertQuestion("second-question");
    secondQuestion.addVersion(versionRepository.getActiveVersion()).save();

    Program firstProgramActive =
        ProgramBuilder.newActiveProgram("foo")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .withRequiredQuestion(secondQuestion)
            .build();
    Program secondProgramActive =
        ProgramBuilder.newActiveProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(secondQuestion)
            .build();

    // firstProgram and secondProgram both reference secondQuestionUpdated.
    Question secondQuestionUpdated = resourceCreator.insertQuestion("second-question");
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersionOrCreate()).save();
    Program firstProgramDraft =
        ProgramBuilder.newDraftProgram("foo")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .withRequiredQuestion(secondQuestionUpdated)
            .build();
    Program secondProgramDraft =
        ProgramBuilder.newDraftProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(secondQuestionUpdated)
            .build();

    // Trying to publish secondProgram throws an error.
    assertThatThrownBy(() -> versionRepository.publishNewSynchronizedVersion("bar"))
        .isInstanceOf(CantPublishProgramWithSharedQuestionsException.class);

    // Verify that the versions have not been modified.
    assertThat(versionRepository.getDraftVersionOrCreate().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(firstProgramDraft.id, secondProgramDraft.id);
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(secondQuestionUpdated.id);

    assertThat(versionRepository.getActiveVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(firstProgramActive.id, secondProgramActive.id);
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestion.id);
  }

  @Test
  public void testPublishProgramDoesNotAllowPublishingNonDraftProgram() throws Exception {
    Question question = resourceCreator.insertQuestion("first-question");
    question.addVersion(versionRepository.getActiveVersion()).save();

    Program activeProgram =
        ProgramBuilder.newActiveProgram("foo")
            .withBlock("Screen 1")
            .withRequiredQuestion(question)
            .build();

    assertThatThrownBy(() -> versionRepository.publishNewSynchronizedVersion("foo"))
        .isInstanceOf(ProgramNotFoundException.class);

    // Verify that the versions have not been modified.
    assertThat(versionRepository.getDraftVersionOrCreate().getPrograms()).hasSize(0);
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions()).hasSize(0);

    assertThat(versionRepository.getActiveVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(activeProgram.id);
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(question.id);
  }

  private Question insertActiveQuestion(String name) {
    Question q = resourceCreator.insertQuestion(name);
    q.addVersion(versionRepository.getActiveVersion()).save();
    return q;
  }

  private Question insertDraftQuestion(String name) {
    Question q = resourceCreator.insertQuestion(name);
    q.addVersion(versionRepository.getDraftVersionOrCreate()).save();
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
    Version draft = versionRepository.getDraftVersionOrCreate();
    assertThat(outer.isActive()).isTrue();
    Version draft2 = versionRepository.getDraftVersionOrCreate();
    assertThat(outer.isActive()).isTrue();
    outer.rollback();
    assertThat(outer.isActive()).isFalse();
    assertThat(draft).isEqualTo(draft2);
  }

  @Test
  public void updatePredicateNode() {
    Version draft = versionRepository.getDraftVersionOrCreate();
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
    Version draft = versionRepository.getDraftVersionOrCreate();
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
                OrNode.create(
                    ImmutableList.of(
                        PredicateExpressionNode.create(
                            AndNode.create(
                                ImmutableList.of(
                                    PredicateExpressionNode.create(
                                        LeafOperationExpressionNode.create(
                                            oldOne.id,
                                            Scalar.NUMBER,
                                            Operator.EQUAL_TO,
                                            PredicateValue.of(100))),
                                    PredicateExpressionNode.create(
                                        LeafOperationExpressionNode.create(
                                            oldTwo.id,
                                            Scalar.NUMBER,
                                            Operator.GREATER_THAN,
                                            PredicateValue.of(10))))))))),
            PredicateAction.SHOW_BLOCK);

    // Create a program that uses the old questions in blocks and block predicates.
    Program program =
        ProgramBuilder.newDraftProgram("questions-need-updating")
            .withBlock()
            .withRequiredQuestion(oldOne)
            .withBlock()
            .withRequiredQuestion(oldTwo)
            .withVisibilityPredicate(predicate)
            .withEligibilityDefinition(
                EligibilityDefinition.builder().setPredicate(predicate).build())
            .build();
    program.save();

    versionRepository.updateQuestionVersions(program);
    ProgramDefinition updated =
        versionRepository
            .getDraftVersionOrCreate()
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
                .getOrNode()
                .children()
                .stream()
                .findFirst()
                .get()
                .getAndNode()
                .children()
                .stream()
                .findFirst()
                .get()
                .getLeafOperationNode()
                .questionId())
        .isEqualTo(newOne.id);
    assertThat(
            updated
                .blockDefinitions()
                .get(1)
                .eligibilityDefinition()
                .get()
                .predicate()
                .rootNode()
                .getOrNode()
                .children()
                .stream()
                .findFirst()
                .get()
                .getAndNode()
                .children()
                .stream()
                .findFirst()
                .get()
                .getLeafOperationNode()
                .questionId())
        .isEqualTo(newOne.id);
    assertThat(
            updated
                .blockDefinitions()
                .get(1)
                .eligibilityDefinition()
                .get()
                .predicate()
                .predicateFormat())
        .isEqualTo(PredicateDefinition.PredicateFormat.OR_OF_SINGLE_LAYER_ANDS);
  }

  @Test
  public void testIsDraftProgram() {
    Program draftProgram = ProgramBuilder.newDraftProgram("draft program").build();
    assertThat(versionRepository.isDraftProgram(draftProgram.getProgramDefinition().id()))
        .isEqualTo(true);
    Program activeProgram = ProgramBuilder.newActiveProgram("active program").build();
    assertThat(versionRepository.isDraftProgram(activeProgram.getProgramDefinition().id()))
        .isEqualTo(false);
  }

  @Test
  public void testIsActiveProgram() {
    Program draftProgram = ProgramBuilder.newDraftProgram("draft program").build();
    assertThat(versionRepository.isActiveProgram(draftProgram.getProgramDefinition().id()))
        .isEqualTo(false);
    Program activeProgram = ProgramBuilder.newActiveProgram("active program").build();
    assertThat(versionRepository.isActiveProgram(activeProgram.getProgramDefinition().id()))
        .isEqualTo(true);
  }

  @Test
  public void getDraftVersion_returnsEmptyIfDraftNotAvailable() {
    assertThat(versionRepository.getDraftVersion()).isEmpty();
  }

  @Test
  public void getDraftVersionOrCreate_createsDraftIfNotAvailable() {
    assertThat(versionRepository.getDraftVersion()).isEmpty();

    versionRepository.getDraftVersionOrCreate();

    assertThat(versionRepository.getDraftVersion()).isPresent();
  }

  @Test
  public void validateNoDuplicateQuestions_duplicatesThrowException() {
    QuestionDefinition firstQuestion =
        resourceCreator.insertQuestion("first-question").getQuestionDefinition();
    QuestionDefinition secondQuestion =
        resourceCreator.insertQuestion("second-question").getQuestionDefinition();
    QuestionDefinition secondQuestion2 =
        resourceCreator.insertQuestion("second-question").getQuestionDefinition();

    assertThatThrownBy(
            () ->
                versionRepository.validateNoDuplicateQuestions(
                    ImmutableList.of(firstQuestion, secondQuestion2, secondQuestion)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void getProgramQuestionNamesInVersion() {
    Question firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(versionRepository.getActiveVersion()).save();
    Question secondQuestion = resourceCreator.insertQuestion("second-question");
    secondQuestion.addVersion(versionRepository.getActiveVersion()).save();
    // Third question is only a draft.
    Question thirdQuestion = resourceCreator.insertQuestion("third-question");
    thirdQuestion.addVersion(versionRepository.getDraftVersionOrCreate()).save();

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
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersionOrCreate()).save();
    Program secondProgramDraft =
        ProgramBuilder.newDraftProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(secondQuestionUpdated)
            .withRequiredQuestion(thirdQuestion)
            .build();

    Version draft = versionRepository.getDraftVersionOrCreate();
    Version active = versionRepository.getActiveVersion();

    assertThat(
            VersionRepository.getProgramQuestionNamesInVersion(
                firstProgramActive.getProgramDefinition(), active))
        .containsExactlyInAnyOrder(firstQuestion.getQuestionDefinition().getName());
    assertThat(
            VersionRepository.getProgramQuestionNamesInVersion(
                firstProgramActive.getProgramDefinition(), draft))
        .isEmpty();

    assertThat(
            VersionRepository.getProgramQuestionNamesInVersion(
                secondProgramActive.getProgramDefinition(), active))
        .containsExactlyInAnyOrder(secondQuestion.getQuestionDefinition().getName());
    assertThat(
            VersionRepository.getProgramQuestionNamesInVersion(
                secondProgramDraft.getProgramDefinition(), draft))
        .containsExactlyInAnyOrder(
            secondQuestionUpdated.getQuestionDefinition().getName(),
            thirdQuestion.getQuestionDefinition().getName());
  }

  @Test
  public void previousVersion_isFound() {
    // Create first version
    Version version1 = versionRepository.getDraftVersionOrCreate();
    Question firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(version1).save();
    version1.save();
    versionRepository.publishNewSynchronizedVersion();
    version1.refresh();

    // Test finding previous version
    Version activeVersion = versionRepository.getActiveVersion();
    Optional<Version> previousVersion = versionRepository.getPreviousVersion(activeVersion);

    assertThat(previousVersion.isPresent()).isTrue();
  }

  @Test
  public void getQuestionByNameForVersion_found() {
    Version version = versionRepository.getDraftVersionOrCreate();
    String questionName = "question";
    Question question = resourceCreator.insertQuestion(questionName);
    question.addVersion(version).save();
    version.refresh();

    Optional<Question> result = versionRepository.getQuestionByNameForVersion(questionName, version);
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get().getQuestionDefinition().getName()).isEqualTo(questionName);
  }

  @Test
  public void getQuestionByNameForVersion_notFound() {
    Version version = versionRepository.getDraftVersionOrCreate();
    String questionName = "question";
    Question question = resourceCreator.insertQuestion(questionName);
    question.addVersion(version).save();
    version.refresh();

    Optional<Question> result = versionRepository.getQuestionByNameForVersion(questionName + "other", version);
    assertThat(result.isPresent()).isFalse();
  }
}
