package services.question;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import services.LocalizationUtils;
import java.util.Set;
import java.util.stream.Collectors;
import models.LifecycleStage;
import models.Question;
import models.Version;
import services.Path;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.question.types.RepeaterQuestionDefinition;
import services.question.types.ScalarType;

public final class ReadOnlyQuestionServiceImpl implements ReadOnlyQuestionService {

  private final ImmutableMap<Long, QuestionDefinition> questionsById;
  private final ImmutableSet<QuestionDefinition> upToDateQuestions;
  private final ActiveAndDraftQuestions activeAndDraftQuestions;
  private final ImmutableMap<Path, ScalarType> scalars;

  private Locale preferredLocale = LocalizationUtils.DEFAULT_LOCALE;

  public ReadOnlyQuestionServiceImpl(Version active, Version draft) {
    checkNotNull(active);
    checkState(active.getLifecycleStage().equals(LifecycleStage.ACTIVE));
    checkNotNull(draft);
    checkState(draft.getLifecycleStage().equals(LifecycleStage.DRAFT));
    ImmutableMap.Builder<Long, QuestionDefinition> questionIdMap = ImmutableMap.builder();
    ImmutableMap.Builder<Path, QuestionDefinition> questionPathMap = ImmutableMap.builder();
    ImmutableMap.Builder<Path, ScalarType> scalarMap = ImmutableMap.builder();
    ImmutableSet.Builder<QuestionDefinition> upToDateBuilder = ImmutableSet.builder();
    Set<String> namesFoundInDraft = new HashSet<>();
    for (QuestionDefinition qd :
        draft.getQuestions().stream()
            .map(Question::getQuestionDefinition)
            .collect(Collectors.toList())) {
      upToDateBuilder.add(qd);
      questionIdMap.put(qd.getId(), qd);
      namesFoundInDraft.add(qd.getName());
    }
    for (QuestionDefinition qd :
        active.getQuestions().stream()
            .map(Question::getQuestionDefinition)
            .collect(Collectors.toList())) {
      questionPathMap.put(qd.getPath(), qd);

      ImmutableMap<Path, ScalarType> questionScalars = qd.getScalars();
      questionScalars.entrySet().stream()
          .forEach(
              entry -> {
                scalarMap.put(entry.getKey(), entry.getValue());
              });
      questionIdMap.put(qd.getId(), qd);
      if (!namesFoundInDraft.contains(qd.getName())) {
        upToDateBuilder.add(qd);
      }
    }
    questionsById = questionIdMap.build();
    scalars = scalarMap.build();
    upToDateQuestions = upToDateBuilder.build();
    activeAndDraftQuestions = new ActiveAndDraftQuestions(active, draft);
  }

  @Override
  public ActiveAndDraftQuestions getActiveAndDraftQuestions() {
    return activeAndDraftQuestions;
  }

  @Override
  public ImmutableList<QuestionDefinition> getAllQuestions() {
    return questionsById.values().asList();
  }

  @Override
  public ImmutableList<QuestionDefinition> getUpToDateQuestions() {
    return upToDateQuestions.asList();
  }

  @Override
  public ImmutableList<RepeaterQuestionDefinition> getUpToDateRepeaterQuestions() {
    return getUpToDateQuestions().stream()
        .filter(QuestionDefinition::isRepeater)
        .map(questionDefinition -> (RepeaterQuestionDefinition) questionDefinition)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public ImmutableMap<Path, ScalarType> getAllScalars() {
    return scalars;
  }

  @Override
  public ImmutableList<RepeaterQuestionDefinition> getAllRepeaterQuestions() {
    return getAllQuestions().stream()
        .filter(QuestionDefinition::isRepeater)
        .map(questionDefinition -> (RepeaterQuestionDefinition) questionDefinition)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public Path makePath(Optional<Long> maybeRepeaterId, String questionName, boolean isRepeater)
      throws QuestionNotFoundException, InvalidQuestionTypeException {
    String questionNameFormattedForPath =
        questionName.replaceAll("[^a-zA-Z ]", "").replaceAll("\\s", "_");
    if (isRepeater) {
      questionNameFormattedForPath += Path.ARRAY_SUFFIX;
    }

    // No repeater, then use "applicant" as root.
    if (maybeRepeaterId.isEmpty()) {
      return Path.create("applicant").join(questionNameFormattedForPath);
    }

    QuestionDefinition repeaterQuestionDefinition = getQuestionDefinition(maybeRepeaterId.get());
    if (!repeaterQuestionDefinition.getQuestionType().equals(QuestionType.REPEATER)) {
      throw new InvalidQuestionTypeException(repeaterQuestionDefinition.getQuestionType().name());
    }

    return repeaterQuestionDefinition.getPath().join(questionNameFormattedForPath);
  }

  @Override
  public QuestionDefinition getQuestionDefinition(long id) throws QuestionNotFoundException {
    if (questionsById.containsKey(id)) {
      return questionsById.get(id);
    }
    throw new QuestionNotFoundException(id);
  }

  @Override
  public Locale getPreferredLocale() {
    return this.preferredLocale;
  }

  @Override
  public void setPreferredLocale(Locale locale) {
    this.preferredLocale = locale;
  }
}
