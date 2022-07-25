package services.question;

import akka.japi.Pair;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import java.util.Optional;
import models.Question;
import models.Version;
import services.DeletionStatus;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;

/**
 * A data class storing the current active and draft questions. For efficient querying of
 * information about current active / draft questions which does not hit the database. Lifespan
 * should be measured in milliseconds - seconds at the maximum - within one request serving path -
 * because it does not have any mechanism for a refresh.
 */
public final class ActiveAndDraftQuestions {

  private final ImmutableMap<
          String, Pair<Optional<QuestionDefinition>, Optional<QuestionDefinition>>>
      versionedByName;
  private final ImmutableMap<String, DeletionStatus> deletionStatusByName;

  public ActiveAndDraftQuestions(Version active, Version draft) {
    ImmutableMap.Builder<String, QuestionDefinition> activeToName = ImmutableMap.builder();
    ImmutableMap.Builder<String, QuestionDefinition> draftToName = ImmutableMap.builder();
    ImmutableMap.Builder<String, DeletionStatus> deletionStatusBuilder = ImmutableMap.builder();
    draft.getQuestions().stream()
        .map(Question::getQuestionDefinition)
        .forEach(qd -> draftToName.put(qd.getName(), qd));
    active.getQuestions().stream()
        .map(Question::getQuestionDefinition)
        .forEach(qd -> activeToName.put(qd.getName(), qd));
    ImmutableMap<String, QuestionDefinition> activeNames = activeToName.build();
    ImmutableMap<String, QuestionDefinition> draftNames = draftToName.build();
    ImmutableMap.Builder<String, Pair<Optional<QuestionDefinition>, Optional<QuestionDefinition>>>
        versionedByNameBuilder = ImmutableMap.builder();
    for (String name : Sets.union(activeNames.keySet(), draftNames.keySet())) {
      versionedByNameBuilder.put(
          name,
          Pair.create(
              Optional.ofNullable(activeNames.get(name)),
              Optional.ofNullable(draftNames.get(name))));
    }
    versionedByName = versionedByNameBuilder.build();
    for (String questionName : activeNames.keySet()) {
      if (draft.getTombstonedQuestionNames().contains(questionName)) {
        deletionStatusBuilder.put(questionName, DeletionStatus.PENDING_DELETION);
      } else if (isNotDeletable(active, draft, activeNames, draftNames, questionName)) {
        deletionStatusBuilder.put(questionName, DeletionStatus.NOT_DELETABLE);
      } else {
        deletionStatusBuilder.put(questionName, DeletionStatus.DELETABLE);
      }
    }
    deletionStatusByName = deletionStatusBuilder.build();
  }

  private static boolean isNotDeletable(
      Version active,
      Version draft,
      ImmutableMap<String, QuestionDefinition> activeNames,
      ImmutableMap<String, QuestionDefinition> draftNames,
      String questionName) {
    return Streams.concat(active.getPrograms().stream(), draft.getPrograms().stream())
        .anyMatch(
            program -> {
              QuestionDefinition activeQuestion = activeNames.get(questionName);
              QuestionDefinition draftQuestion = draftNames.get(questionName);
              ProgramDefinition programDefinition = program.getProgramDefinition();
              if (activeQuestion != null && programDefinition.hasQuestion(activeQuestion)) {
                return true;
              } else return draftQuestion != null && programDefinition.hasQuestion(draftQuestion);
            });
  }

  public DeletionStatus getDeletionStatus(String questionName) {
    return this.deletionStatusByName.getOrDefault(questionName, DeletionStatus.NOT_ACTIVE);
  }

  public ImmutableSet<String> getQuestionNames() {
    return versionedByName.keySet();
  }

  public Optional<QuestionDefinition> getActiveQuestionDefinition(String name) {
    return versionedByName.containsKey(name) ? versionedByName.get(name).first() : Optional.empty();
  }

  public Optional<QuestionDefinition> getDraftQuestionDefinition(String name) {
    return versionedByName.containsKey(name)
        ? versionedByName.get(name).second()
        : Optional.empty();
  }
}
