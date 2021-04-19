package services.question;

import akka.japi.Pair;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Optional;
import models.Question;
import models.Version;
import services.question.types.QuestionDefinition;

public class ActiveAndDraftQuestions {

  private final ImmutableMap<
          String, Pair<Optional<QuestionDefinition>, Optional<QuestionDefinition>>>
      versionedByName;
  private final int activeSize;
  private final int draftSize;

  public ActiveAndDraftQuestions(Version active, Version draft) {
    ImmutableMap.Builder<String, QuestionDefinition> activeToName = ImmutableMap.builder();
    ImmutableMap.Builder<String, QuestionDefinition> draftToName = ImmutableMap.builder();
    draft.getQuestions().stream()
        .map(Question::getQuestionDefinition)
        .forEach(qd -> draftToName.put(qd.getName(), qd));
    active.getQuestions().stream()
        .map(Question::getQuestionDefinition)
        .forEach(qd -> activeToName.put(qd.getName(), qd));
    ImmutableMap<String, QuestionDefinition> activeNames = activeToName.build();
    ImmutableMap<String, QuestionDefinition> draftNames = draftToName.build();
    activeSize = activeNames.size();
    draftSize = draftNames.size();
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
  }

  public ImmutableSet<String> getQuestionNames() {
    return versionedByName.keySet();
  }

  public Optional<QuestionDefinition> getActiveQuestionDefinition(String name) {
    return versionedByName.get(name).first();
  }

  public Optional<QuestionDefinition> getDraftQuestionDefinition(String name) {
    return versionedByName.get(name).second();
  }

  public int getActiveSize() {
    return activeSize;
  }

  public int getDraftSize() {
    return draftSize;
  }
}
