package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import services.LocalizationUtils;
import services.Path;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.question.types.RepeaterQuestionDefinition;
import views.admin.GroupByKeyCollector;

public final class ReadOnlyQuestionServiceImpl implements ReadOnlyQuestionService {

  private final ImmutableMap<Long, QuestionDefinition> questionsById;
  private final ImmutableSet<QuestionDefinition> upToDateQuestions;

  private Locale preferredLocale = LocalizationUtils.DEFAULT_LOCALE;

  public ReadOnlyQuestionServiceImpl(ImmutableList<QuestionDefinition> questions) {
    checkNotNull(questions);

    ImmutableSet.Builder<QuestionDefinition> upToDateBuilder = ImmutableSet.builder();
    for (ImmutableList<QuestionDefinition> qds :
        questions.stream()
            .collect(new GroupByKeyCollector<>(QuestionDefinition::getName))
            .values()) {
      upToDateBuilder.add(
          qds.stream().max(Comparator.comparing(QuestionDefinition::getVersion)).get());
    }
    upToDateQuestions = upToDateBuilder.build();

    ImmutableMap.Builder<Long, QuestionDefinition> questionIdMap = ImmutableMap.builder();
    for (QuestionDefinition qd : questions) {
      questionIdMap.put(qd.getId(), qd);
    }
    questionsById = questionIdMap.build();
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
