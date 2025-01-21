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
import models.ProgramModel;
import models.QuestionModel;
import models.VersionModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.cache.SyncCacheApi;
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
import services.settings.SettingsManifest;
import support.ProgramBuilder;

public class VersionRepositoryTest extends ResetPostgres {
  private VersionRepository versionRepository;
  private SyncCacheApi questionsByVersionCache;
  private SyncCacheApi programsByVersionCache;
  private SettingsManifest mockSettingsManifest;

  @Before
  public void setupVersionRepository() {
    mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    questionsByVersionCache = instanceOf(SyncCacheApi.class);
    programsByVersionCache = instanceOf(SyncCacheApi.class);
    versionRepository =
        new VersionRepository(
            instanceOf(ProgramRepository.class),
            instanceOf(QuestionRepository.class),
            instanceOf(DatabaseExecutionContext.class),
            mockSettingsManifest,
            questionsByVersionCache,
            programsByVersionCache);
  }

  @Test
  public void testPublish_tombstonesProgramsAndQuestionsOnlyCreatedInTheDraftVersion()
      throws Exception {
    QuestionModel draftOnlyQuestion = resourceCreator.insertQuestion("draft-only-question");
    draftOnlyQuestion.addVersion(versionRepository.getDraftVersionOrCreate()).save();

    ProgramModel draftOnlyProgram =
        ProgramBuilder.newDraftProgram("draft-only-program").withBlock("Screen 1").build();

    VersionModel draftForTombstoning = versionRepository.getDraftVersionOrCreate();
    assertThat(
            versionRepository.addTombstoneForQuestionInVersion(
                draftOnlyQuestion, draftForTombstoning))
        .isTrue();
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
    VersionModel updated = versionRepository.previewPublishNewSynchronizedVersion();
    assertThat(updated.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    assertThat(updated.getPrograms()).isEmpty();
    assertThat(updated.getTombstonedProgramNames()).isEmpty();
    assertThat(updated.getQuestions()).isEmpty();
    assertThat(updated.getTombstonedQuestionNames()).isEmpty();
  }

  @Test
  public void testPublish() {
    QuestionModel firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(versionRepository.getActiveVersion()).save();
    QuestionModel secondQuestion = resourceCreator.insertQuestion("second-question");
    secondQuestion.addVersion(versionRepository.getActiveVersion()).save();

    ProgramModel firstProgramActive =
        ProgramBuilder.newActiveProgram("foo")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .build();
    ProgramModel secondProgramActive =
        ProgramBuilder.newActiveProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(secondQuestion)
            .build();
    QuestionModel secondQuestionUpdated = resourceCreator.insertQuestion("second-question");
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersionOrCreate()).save();
    ProgramModel secondProgramDraft =
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

    VersionModel oldDraft = versionRepository.getDraftVersionOrCreate();
    VersionModel oldActive = versionRepository.getActiveVersion();

    // First, preview the changes and ensure no versions are updated.
    VersionModel toApplyNewActiveVersion = versionRepository.previewPublishNewSynchronizedVersion();
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
    QuestionModel firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(versionRepository.getActiveVersion()).save();
    QuestionModel secondQuestion = resourceCreator.insertQuestion("second-question");
    secondQuestion.addVersion(versionRepository.getActiveVersion()).save();

    ProgramModel firstProgramActive =
        ProgramBuilder.newActiveProgram("foo")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .build();
    ProgramModel secondProgramActive =
        ProgramBuilder.newActiveProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(secondQuestion)
            .build();

    VersionModel draftForTombstoning = versionRepository.getDraftVersionOrCreate();
    draftForTombstoning.addQuestion(firstQuestion).save();
    assertThat(
            versionRepository.addTombstoneForQuestionInVersion(firstQuestion, draftForTombstoning))
        .isTrue();
    QuestionModel secondQuestionUpdated = resourceCreator.insertQuestion("second-question");
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
    QuestionModel firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(versionRepository.getActiveVersion()).save();
    QuestionModel secondQuestion = resourceCreator.insertQuestion("second-question");
    secondQuestion.addVersion(versionRepository.getActiveVersion()).save();

    ProgramModel firstProgramActive =
        ProgramBuilder.newActiveProgram("foo")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .build();
    ProgramModel secondProgramActive =
        ProgramBuilder.newActiveProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(secondQuestion)
            .build();

    VersionModel draftForTombstoning = versionRepository.getDraftVersionOrCreate();
    draftForTombstoning.addQuestion(firstQuestion).save();
    assertThat(
            versionRepository.addTombstoneForQuestionInVersion(firstQuestion, draftForTombstoning))
        .isTrue();
    QuestionModel secondQuestionUpdated = resourceCreator.insertQuestion("second-question");
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersionOrCreate()).save();
    versionRepository.updateProgramsThatReferenceQuestion(secondQuestion.id);

    assertThat(versionRepository.getActiveVersion().getPrograms().stream().map(p -> p.id))
        .containsExactlyInAnyOrder(firstProgramActive.id, secondProgramActive.id);
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestion.id);
    // Second program is in draft, since a question within it was updated.
    ProgramModel newSecondProgram =
        versionRepository.getDraftVersionOrCreate().getPrograms().get(0);
    assertThat(
            versionRepository.getDraftVersionOrCreate().getPrograms().stream()
                .map(p -> p.getProgramDefinition().adminName()))
        .containsExactlyInAnyOrder(secondProgramActive.getProgramDefinition().adminName());
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestionUpdated.id);

    VersionModel oldDraft = versionRepository.getDraftVersionOrCreate();
    VersionModel oldActive = versionRepository.getActiveVersion();

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
    QuestionModel firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(versionRepository.getActiveVersion()).save();
    QuestionModel secondQuestion = resourceCreator.insertQuestion("second-question");
    secondQuestion.addVersion(versionRepository.getActiveVersion()).save();

    VersionModel draftForTombstoning = versionRepository.getDraftVersionOrCreate();
    draftForTombstoning.addQuestion(firstQuestion).save();
    assertThat(
            versionRepository.addTombstoneForQuestionInVersion(firstQuestion, draftForTombstoning))
        .isTrue();
    QuestionModel secondQuestionUpdated = resourceCreator.insertQuestion("second-question");
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersionOrCreate()).save();

    assertThat(versionRepository.getActiveVersion().getPrograms()).isEmpty();
    assertThat(versionRepository.getActiveVersion().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestion.id);
    assertThat(versionRepository.getDraftVersionOrCreate().getPrograms()).isEmpty();
    assertThat(versionRepository.getDraftVersionOrCreate().getQuestions().stream().map(q -> q.id))
        .containsExactlyInAnyOrder(firstQuestion.id, secondQuestionUpdated.id);

    VersionModel oldDraft = versionRepository.getDraftVersionOrCreate();
    VersionModel oldActive = versionRepository.getActiveVersion();

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
    QuestionModel firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(versionRepository.getActiveVersion()).save();
    QuestionModel secondQuestion = resourceCreator.insertQuestion("second-question");
    secondQuestion.addVersion(versionRepository.getActiveVersion()).save();
    // Third question is only a draft.
    QuestionModel thirdQuestion = resourceCreator.insertQuestion("third-question");
    thirdQuestion.addVersion(versionRepository.getDraftVersionOrCreate()).save();

    ProgramModel firstProgramActive =
        ProgramBuilder.newActiveProgram("foo")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .build();
    ProgramModel secondProgramActive =
        ProgramBuilder.newActiveProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(secondQuestion)
            .build();
    ProgramModel thirdProgramActive =
        ProgramBuilder.newActiveProgram("baz")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .build();

    // secondProgramDraft and its question, secondQuestionUpdated, should be published.
    QuestionModel secondQuestionUpdated = resourceCreator.insertQuestion("second-question");
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersionOrCreate()).save();
    ProgramModel secondProgramDraft =
        ProgramBuilder.newDraftProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(secondQuestionUpdated)
            .build();

    // thirdProgramDraft should not be published.
    ProgramModel thirdProgramDraft =
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

    VersionModel oldDraft = versionRepository.getDraftVersionOrCreate();
    VersionModel oldActive = versionRepository.getActiveVersion();

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
    QuestionModel firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(versionRepository.getActiveVersion()).save();
    QuestionModel secondQuestion = resourceCreator.insertQuestion("second-question");
    secondQuestion.addVersion(versionRepository.getActiveVersion()).save();

    ProgramModel firstProgramActive =
        ProgramBuilder.newActiveProgram("foo")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .build();
    QuestionModel secondQuestionUpdated = resourceCreator.insertQuestion("second-question");
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersionOrCreate()).save();

    // Program being published has no existing active version.
    ProgramModel secondProgramDraft =
        ProgramBuilder.newDraftProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .build();

    VersionModel oldDraft = versionRepository.getDraftVersionOrCreate();
    VersionModel oldActive = versionRepository.getActiveVersion();

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
    QuestionModel firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(versionRepository.getActiveVersion()).save();
    QuestionModel secondQuestion = resourceCreator.insertQuestion("second-question");
    secondQuestion.addVersion(versionRepository.getActiveVersion()).save();

    ProgramModel firstProgramActive =
        ProgramBuilder.newActiveProgram("foo")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .withRequiredQuestion(secondQuestion)
            .build();
    ProgramModel secondProgramActive =
        ProgramBuilder.newActiveProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(secondQuestion)
            .build();

    // firstProgram and secondProgram both reference secondQuestionUpdated.
    QuestionModel secondQuestionUpdated = resourceCreator.insertQuestion("second-question");
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersionOrCreate()).save();
    ProgramModel firstProgramDraft =
        ProgramBuilder.newDraftProgram("foo")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .withRequiredQuestion(secondQuestionUpdated)
            .build();
    ProgramModel secondProgramDraft =
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
    QuestionModel question = resourceCreator.insertQuestion("first-question");
    question.addVersion(versionRepository.getActiveVersion()).save();

    ProgramModel activeProgram =
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

  private QuestionModel insertActiveQuestion(String name) {
    QuestionModel q = resourceCreator.insertQuestion(name);
    q.addVersion(versionRepository.getActiveVersion()).save();
    return q;
  }

  private QuestionModel insertDraftQuestion(String name) {
    QuestionModel q = resourceCreator.insertQuestion(name);
    q.addVersion(versionRepository.getDraftVersionOrCreate()).save();
    return q;
  }

  @Test
  public void testPublishDoesNotUpdateProgramTimestamps() throws InterruptedException {
    ImmutableList<ProgramModel> programs =
        ImmutableList.of(
            resourceCreator.insertActiveProgram("active"),
            resourceCreator.insertActiveProgram("other_active"),
            resourceCreator.insertDraftProgram("draft"),
            resourceCreator.insertActiveProgram("active_with_draft"),
            resourceCreator.insertDraftProgram("active_with_draft"));
    ImmutableMap<String, Instant> beforeProgramTimestamps =
        programs.stream()
            .map(ProgramModel::getProgramDefinition)
            .collect(
                ImmutableMap.toImmutableMap(
                    program -> String.format("%d %s", program.id(), program.adminName()),
                    program -> program.lastModifiedTime().orElseThrow()));

    ImmutableList<QuestionModel> questions =
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
                        .find(ProgramModel.class)
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
                        .find(QuestionModel.class)
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
    VersionModel draft = versionRepository.getDraftVersionOrCreate();
    assertThat(outer.isActive()).isTrue();
    VersionModel draft2 = versionRepository.getDraftVersionOrCreate();
    assertThat(outer.isActive()).isTrue();
    outer.rollback();
    assertThat(outer.isActive()).isFalse();
    assertThat(draft).isEqualTo(draft2);
  }

  @Test
  public void updatePredicateNode() {
    VersionModel draft = versionRepository.getDraftVersionOrCreate();
    VersionModel active = versionRepository.getActiveVersion();

    // Old versions of questions
    QuestionModel oldOne = resourceCreator.insertQuestion("one");
    oldOne.addVersion(active);
    oldOne.save();
    QuestionModel oldTwo = resourceCreator.insertQuestion("two");
    oldTwo.addVersion(active);
    oldTwo.save();

    // New versions of questions
    QuestionModel newOne = resourceCreator.insertQuestion("one");
    newOne.addVersion(draft);
    newOne.save();
    QuestionModel newTwo = resourceCreator.insertQuestion("two");
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
        PredicateExpressionNode.create(
            LeafAddressServiceAreaExpressionNode.create(oldOne.id, "", Operator.IN_SERVICE_AREA));
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
    VersionModel draft = versionRepository.getDraftVersionOrCreate();
    VersionModel active = versionRepository.getActiveVersion();

    // Create some old questions
    QuestionModel oldOne = resourceCreator.insertQuestion("one");
    oldOne.addVersion(active);
    oldOne.save();
    QuestionModel oldTwo = resourceCreator.insertQuestion("two");
    oldTwo.addVersion(active);
    oldTwo.save();

    // Create new versions of the old questions
    QuestionModel newOne = resourceCreator.insertQuestion("one");
    newOne.addVersion(draft);
    newOne.save();
    QuestionModel newTwo = resourceCreator.insertQuestion("two");
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
    ProgramModel program =
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
            .getProgramByNameForVersion(
                program.getProgramDefinition().adminName(),
                versionRepository.getDraftVersionOrCreate())
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
    ProgramModel draftProgram = ProgramBuilder.newDraftProgram("draft program").build();
    assertThat(versionRepository.isDraftProgram(draftProgram.id)).isEqualTo(true);
    ProgramModel activeProgram = ProgramBuilder.newActiveProgram("active program").build();
    assertThat(versionRepository.isDraftProgram(activeProgram.id)).isEqualTo(false);
  }

  @Test
  public void testIsActiveProgram() {
    ProgramModel draftProgram = ProgramBuilder.newDraftProgram("draft program").build();
    assertThat(versionRepository.isActiveProgram(draftProgram.id)).isEqualTo(false);
    ProgramModel activeProgram = ProgramBuilder.newActiveProgram("active program").build();
    assertThat(versionRepository.isActiveProgram(activeProgram.id)).isEqualTo(true);
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
    QuestionModel firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(versionRepository.getActiveVersion()).save();
    QuestionModel secondQuestion = resourceCreator.insertQuestion("second-question");
    secondQuestion.addVersion(versionRepository.getActiveVersion()).save();
    // Third question is only a draft.
    QuestionModel thirdQuestion = resourceCreator.insertQuestion("third-question");
    thirdQuestion.addVersion(versionRepository.getDraftVersionOrCreate()).save();

    ProgramModel firstProgramActive =
        ProgramBuilder.newActiveProgram("foo")
            .withBlock("Screen 1")
            .withRequiredQuestion(firstQuestion)
            .build();
    ProgramModel secondProgramActive =
        ProgramBuilder.newActiveProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(secondQuestion)
            .build();

    QuestionModel secondQuestionUpdated = resourceCreator.insertQuestion("second-question");
    secondQuestionUpdated.addVersion(versionRepository.getDraftVersionOrCreate()).save();
    ProgramModel secondProgramDraft =
        ProgramBuilder.newDraftProgram("bar")
            .withBlock("Screen 1")
            .withRequiredQuestion(secondQuestionUpdated)
            .withRequiredQuestion(thirdQuestion)
            .build();

    VersionModel draft = versionRepository.getDraftVersionOrCreate();
    VersionModel active = versionRepository.getActiveVersion();

    assertThat(
            versionRepository.getProgramQuestionNamesInVersion(
                firstProgramActive.getProgramDefinition(), active))
        .containsExactlyInAnyOrder(firstQuestion.getQuestionDefinition().getName());
    assertThat(
            versionRepository.getProgramQuestionNamesInVersion(
                firstProgramActive.getProgramDefinition(), draft))
        .isEmpty();

    assertThat(
            versionRepository.getProgramQuestionNamesInVersion(
                secondProgramActive.getProgramDefinition(), active))
        .containsExactlyInAnyOrder(secondQuestion.getQuestionDefinition().getName());
    assertThat(
            versionRepository.getProgramQuestionNamesInVersion(
                secondProgramDraft.getProgramDefinition(), draft))
        .containsExactlyInAnyOrder(
            secondQuestionUpdated.getQuestionDefinition().getName(),
            thirdQuestion.getQuestionDefinition().getName());
  }

  @Test
  public void previousVersion_isFound() {
    // Create first version
    VersionModel version1 = versionRepository.getDraftVersionOrCreate();
    QuestionModel firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(version1).save();
    version1.save();
    versionRepository.publishNewSynchronizedVersion();
    version1.refresh();

    // Test finding previous version
    VersionModel activeVersion = versionRepository.getActiveVersion();
    Optional<VersionModel> previousVersion = versionRepository.getPreviousVersion(activeVersion);

    assertThat(previousVersion.isPresent()).isTrue();
  }

  @Test
  public void getQuestionByNameForVersion_found() {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String questionName = "question";
    QuestionModel question = resourceCreator.insertQuestion(questionName);
    question.addVersion(version).save();
    version.refresh();

    Optional<QuestionModel> result =
        versionRepository.getQuestionByNameForVersion(questionName, version);
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get().getQuestionDefinition().getName()).isEqualTo(questionName);
  }

  @Test
  public void getQuestionByNameForVersion_notFound() {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String questionName = "question";
    QuestionModel question = resourceCreator.insertQuestion(questionName);
    question.addVersion(version).save();
    version.refresh();

    Optional<QuestionModel> result =
        versionRepository.getQuestionByNameForVersion(questionName + "other", version);
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  public void getQuestions_usesCacheIfEnabledForObsoleteVersion() {
    Mockito.when(mockSettingsManifest.getVersionCacheEnabled()).thenReturn(true);

    VersionModel version1 = versionRepository.getDraftVersionOrCreate();
    ProgramBuilder.newDraftProgram("draft program").build();
    versionRepository.publishNewSynchronizedVersion();
    version1.refresh();

    // Create another version and publish it, so the original version becomes obsolete
    VersionModel version2 = versionRepository.getDraftVersionOrCreate();
    ProgramBuilder.newDraftProgram("draft program2").build();
    version2.save();
    versionRepository.publishNewSynchronizedVersion();
    version2.refresh();

    String version1Key = String.valueOf(version1.id);

    // Associate question with obsolete version
    QuestionModel firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(version1).save();

    ImmutableList<QuestionModel> questionsForVersion =
        versionRepository.getQuestionsForVersion(version1);

    assertThat(questionsByVersionCache.get(version1Key).get()).isEqualTo(questionsForVersion);
  }

  @Test
  public void getQuestions_usesCacheIfEnabledForActiveVersion() {
    Mockito.when(mockSettingsManifest.getVersionCacheEnabled()).thenReturn(true);

    VersionModel version1 = versionRepository.getDraftVersionOrCreate();
    ProgramBuilder.newDraftProgram("draft program").build();
    versionRepository.publishNewSynchronizedVersion();
    version1.refresh();

    String version1Key = String.valueOf(version1.id);

    // Associate question with active version
    QuestionModel firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(version1).save();

    ImmutableList<QuestionModel> questionsForVersion =
        versionRepository.getQuestionsForVersion(version1);

    assertThat(questionsByVersionCache.get(version1Key).get()).isEqualTo(questionsForVersion);
  }

  @Test
  public void getQuestions_doesNotUseCacheForDraftVersion() {
    Mockito.when(mockSettingsManifest.getVersionCacheEnabled()).thenReturn(true);

    VersionModel version1 = versionRepository.getDraftVersionOrCreate();
    version1.save();
    QuestionModel firstQuestion = resourceCreator.insertQuestion("first-question");
    firstQuestion.addVersion(version1).save();

    String version1Key = String.valueOf(version1.id);

    assertThat(questionsByVersionCache.get(version1Key).isPresent()).isFalse();

    versionRepository.getQuestionsForVersion(version1);

    assertThat(questionsByVersionCache.get(version1Key).isPresent()).isFalse();
  }

  @Test
  public void getPrograms_usesCacheIfEnabledForObsoleteVersion() {
    Mockito.when(mockSettingsManifest.getVersionCacheEnabled()).thenReturn(true);

    VersionModel version1 = versionRepository.getDraftVersionOrCreate();
    resourceCreator.insertDraftProgram("first-program");
    versionRepository.publishNewSynchronizedVersion();
    version1.refresh();

    // Create another version and publish it, so the original version becomes obsolete
    VersionModel version2 = versionRepository.getDraftVersionOrCreate();
    resourceCreator.insertDraftProgram("second-program");
    versionRepository.publishNewSynchronizedVersion();
    version2.refresh();

    String version1Key = String.valueOf(version1.id);

    ImmutableList<ProgramModel> programsForVersion =
        versionRepository.getProgramsForVersion(version1);

    assertThat(programsByVersionCache.get(version1Key).get()).isEqualTo(programsForVersion);
  }

  @Test
  public void getPrograms_usesCacheIfEnabledForActiveVersion() {
    Mockito.when(mockSettingsManifest.getVersionCacheEnabled()).thenReturn(true);
    VersionModel version1 = versionRepository.getDraftVersionOrCreate();
    resourceCreator.insertDraftProgram("first-program");
    versionRepository.publishNewSynchronizedVersion();
    version1.refresh();

    String version1Key = String.valueOf(version1.id);

    ImmutableList<ProgramModel> programsForVersion =
        versionRepository.getProgramsForVersion(version1);

    assertThat(programsByVersionCache.get(version1Key).get()).isEqualTo(programsForVersion);
  }

  @Test
  public void getPrograms_doesNotUseCacheForDraftVersion() {
    Mockito.when(mockSettingsManifest.getVersionCacheEnabled()).thenReturn(true);
    VersionModel version1 = versionRepository.getDraftVersionOrCreate();
    resourceCreator.insertDraftProgram("first-program");
    version1.save();

    String version1Key = String.valueOf(version1.id);

    assertThat(programsByVersionCache.get(version1Key).isPresent()).isFalse();

    versionRepository.getProgramsForVersion(version1);

    assertThat(programsByVersionCache.get(version1Key).isPresent()).isFalse();
  }

  @Test
  public void testAnyDisabledPrograms_activeProgramDisabled() {
    // When no programs, there are no disabled programs
    assertThat(versionRepository.anyDisabledPrograms()).isFalse();

    // Adding a non-disabled active program and verify that there are still no disabled programs
    ProgramBuilder.newActiveProgram("active-program").build();
    assertThat(versionRepository.anyDisabledPrograms()).isFalse();

    // Adding a disabled program and verify that now we have disabled programs
    ProgramBuilder.newDisabledActiveProgram("disabled-active-program").build();
    assertThat(versionRepository.anyDisabledPrograms()).isTrue();
  }

  @Test
  public void testAnyDisabledPrograms_draftProgramDisabled() {
    // When no programs, there are no disabled programs
    assertThat(versionRepository.anyDisabledPrograms()).isFalse();

    // Adding non-disabled draft programs and verify that there are still no disabled programs
    ProgramBuilder.newDraftProgram("draft-program").build();
    assertThat(versionRepository.anyDisabledPrograms()).isFalse();

    // Adding a disabled program and verify that now we have disabled programs
    ProgramBuilder.newDisabledDraftProgram("disabled-draft-program").build();
    assertThat(versionRepository.anyDisabledPrograms()).isTrue();
  }
}
