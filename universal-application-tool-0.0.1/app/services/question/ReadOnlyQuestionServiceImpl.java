package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import models.LifecycleStage;
import services.ErrorAnd;
import services.Path;
import services.question.exceptions.InvalidPathException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;
import services.question.types.RepeaterQuestionDefinition;
import services.question.types.ScalarType;
import views.admin.questions.GroupByKeyCollector;

public final class ReadOnlyQuestionServiceImpl implements ReadOnlyQuestionService {

  private final ImmutableMap<Path, ScalarType> scalars;
  private final ImmutableMap<Long, QuestionDefinition> questionsById;
  private final ImmutableMap<Path, QuestionDefinition> questionsByPath;
  private final ImmutableSet<QuestionDefinition> upToDateQuestions;

  private Locale preferredLocale = Locale.US;

  public ReadOnlyQuestionServiceImpl(ImmutableList<QuestionDefinition> questions) {
    checkNotNull(questions);
    ImmutableMap.Builder<Long, QuestionDefinition> questionIdMap = ImmutableMap.builder();
    ImmutableMap.Builder<Path, QuestionDefinition> questionPathMap = ImmutableMap.builder();
    ImmutableMap.Builder<Path, ScalarType> scalarMap = ImmutableMap.builder();
    ImmutableSet.Builder<QuestionDefinition> upToDateBuilder = ImmutableSet.builder();
    for (ImmutableList<QuestionDefinition> qds :
        questions.stream()
            .collect(new GroupByKeyCollector<>(QuestionDefinition::getName))
            .values()) {
      Optional<QuestionDefinition> qdMaybe =
          qds.stream().filter(qd -> qd.getLifecycleStage().equals(LifecycleStage.ACTIVE)).findAny();
      if (qdMaybe.isPresent()) {
        QuestionDefinition qd = qdMaybe.get();
        questionPathMap.put(qd.getPath(), qd);
        ImmutableMap<Path, ScalarType> questionScalars = qd.getScalars();
        questionScalars.entrySet().stream()
            .forEach(
                entry -> {
                  scalarMap.put(entry.getKey(), entry.getValue());
                });
      }
      upToDateBuilder.add(
          qds.stream().max(Comparator.comparing(QuestionDefinition::getVersion)).get());
    }
    for (QuestionDefinition qd : questions) {
      questionIdMap.put(qd.getId(), qd);
    }
    questionsById = questionIdMap.build();
    questionsByPath = questionPathMap.build();
    scalars = scalarMap.build();
    upToDateQuestions = upToDateBuilder.build();
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
      throws QuestionNotFoundException {
    String questionNameFormattedForPath =
        questionName.replaceAll("\\s", "_").replaceAll("[^a-zA-Z_]", "");
    if (isRepeater) {
      questionNameFormattedForPath += Path.ARRAY_SUFFIX;
    }

    ErrorAnd<Path, QuestionNotFoundException> maybeParentPath =
        maybeRepeaterId
            .map(
                repeaterId -> {
                  try {
                    return ErrorAnd.<Path, QuestionNotFoundException>of(
                        getQuestionDefinition(repeaterId).getPath());
                  } catch (QuestionNotFoundException e) {
                    return ErrorAnd.<Path, QuestionNotFoundException>error(ImmutableSet.of(e));
                  }
                })
            .orElse(ErrorAnd.of(Path.create("applicant")));

    if (maybeParentPath.isError()) {
      throw maybeParentPath.getErrors().asList().get(0);
    }
    return maybeParentPath.getResult().join(questionNameFormattedForPath);
  }

  @Override
  public ImmutableMap<Path, ScalarType> getAllScalars() {
    return scalars;
  }

  @Override
  public ImmutableMap<Path, ScalarType> getPathScalars(Path path) throws InvalidPathException {
    PathType pathType = this.getPathType(path);
    switch (pathType) {
      case QUESTION:
        return questionsByPath.get(path).getScalars();
      case SCALAR:
        ScalarType scalarType = scalars.get(path);
        return ImmutableMap.of(path, scalarType);
      case NONE:
      default:
        throw new InvalidPathException(path);
    }
  }

  @Override
  public PathType getPathType(Path path) {
    if (questionsByPath.containsKey(path)) {
      return PathType.QUESTION;
    } else if (scalars.containsKey(path)) {
      return PathType.SCALAR;
    }
    return PathType.NONE;
  }

  @Override
  public QuestionDefinition getQuestionDefinition(long id) throws QuestionNotFoundException {
    if (questionsById.containsKey(id)) {
      return questionsById.get(id);
    }
    throw new QuestionNotFoundException(id);
  }

  @Override
  public boolean isValid(Path path) {
    return scalars.containsKey(path) || questionsByPath.containsKey(path);
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
