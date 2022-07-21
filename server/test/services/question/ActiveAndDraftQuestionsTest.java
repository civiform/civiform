package services.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import models.LifecycleStage;
import models.Question;
import models.Version;
import org.junit.Test;
import repository.ResetPostgres;
import services.question.types.QuestionDefinition;

public class ActiveAndDraftQuestionsTest extends ResetPostgres {
  @Test
  public void getQuestionNames() {
    Version activeVersion = new Version(LifecycleStage.ACTIVE);
    activeVersion.insert();
    Version draftVersion = new Version(LifecycleStage.DRAFT);
    draftVersion.insert();

    Question tombstonedQuestionFromActiveVersion =
        resourceCreator.insertQuestion("tombstoned-question");
    Question activeAndDraftQuestion = resourceCreator.insertQuestion("active-and-draft-question");
    Question activeOnlyQuestion = resourceCreator.insertQuestion("active-only-question");
    Question activeAndDraftQuestionUpdated =
        resourceCreator.insertQuestion("active-and-draft-question");
    Question draftOnlyQuestion = resourceCreator.insertQuestion("draft-only-question");

    activeVersion =
        activeVersion
            .addQuestion(tombstonedQuestionFromActiveVersion)
            .addQuestion(activeAndDraftQuestion)
            .addQuestion(activeOnlyQuestion);
    assertThat(activeVersion.addTombstoneForQuestion(tombstonedQuestionFromActiveVersion)).isTrue();
    activeVersion.save();

    draftVersion =
        draftVersion.addQuestion(activeAndDraftQuestionUpdated).addQuestion(draftOnlyQuestion);
    draftVersion.save();

    ActiveAndDraftQuestions questions = new ActiveAndDraftQuestions(activeVersion, draftVersion);
    assertThat(questions.getQuestionNames())
        .containsExactly(
            "tombstoned-question",
            "active-and-draft-question",
            "active-only-question",
            "draft-only-question");
  }

  @Test
  public void getActiveOrDraftQuestionDefinition() {
    Version activeVersion = new Version(LifecycleStage.ACTIVE);
    activeVersion.insert();
    Version draftVersion = new Version(LifecycleStage.DRAFT);
    draftVersion.insert();

    Question tombstonedQuestionFromActiveVersion =
        resourceCreator.insertQuestion("tombstoned-question");
    Question activeAndDraftQuestion = resourceCreator.insertQuestion("active-and-draft-question");
    Question activeOnlyQuestion = resourceCreator.insertQuestion("active-only-question");
    Question activeAndDraftQuestionUpdated =
        resourceCreator.insertQuestion("active-and-draft-question");
    Question draftOnlyQuestion = resourceCreator.insertQuestion("draft-only-question");

    activeVersion =
        activeVersion
            .addQuestion(tombstonedQuestionFromActiveVersion)
            .addQuestion(activeAndDraftQuestion)
            .addQuestion(activeOnlyQuestion);
    assertThat(activeVersion.addTombstoneForQuestion(tombstonedQuestionFromActiveVersion)).isTrue();
    activeVersion.save();

    draftVersion =
        draftVersion.addQuestion(activeAndDraftQuestionUpdated).addQuestion(draftOnlyQuestion);
    draftVersion.save();

    ActiveAndDraftQuestions questions = new ActiveAndDraftQuestions(activeVersion, draftVersion);
    assertThat(
            questions
                .getActiveQuestionDefinition("tombstoned-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.of(tombstonedQuestionFromActiveVersion.id));
    assertThat(
            questions
                .getDraftQuestionDefinition("tombstoned-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.empty());
    assertThat(
            questions
                .getActiveQuestionDefinition("active-and-draft-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.of(activeAndDraftQuestion.id));
    assertThat(
            questions
                .getDraftQuestionDefinition("active-and-draft-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.of(activeAndDraftQuestionUpdated.id));
    assertThat(
            questions
                .getActiveQuestionDefinition("active-only-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.of(activeOnlyQuestion.id));
    assertThat(
            questions
                .getDraftQuestionDefinition("active-only-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.empty());
    assertThat(
            questions
                .getActiveQuestionDefinition("draft-only-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.empty());
    assertThat(
            questions
                .getDraftQuestionDefinition("draft-only-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.of(draftOnlyQuestion.id));
    assertThat(
            questions
                .getActiveQuestionDefinition("non-existent-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.empty());
    assertThat(
            questions
                .getDraftQuestionDefinition("non-existent-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.empty());
  }

  @Test
  public void getDeletionStatus() {
    Version activeVersion = new Version(LifecycleStage.ACTIVE);
    activeVersion.insert();
    Version draftVersion = new Version(LifecycleStage.DRAFT);
    draftVersion.insert();

    // TODO(clouser): Still need tests for a draft edit of a question.

    // Not present in either. -> NOT_ACTIVE
    // Present in tombstoned list for draft -> PENDING_DELETION
    // Still referenced in active -> NOT_DELETABLE
    // Still referenced in draft -> NOT_DELETABLE
    // Not referenced in active, but referenced in draft -> NOT_DELETABLE
    // Not referenced in active or draft -> DELETABLE
    // Referenced in active program, but there's an edit that removes it in draft -> DELETABLE
  }
}
