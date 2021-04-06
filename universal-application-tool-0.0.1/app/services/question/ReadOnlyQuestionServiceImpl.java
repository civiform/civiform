package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.LifecycleStage;
import services.Path;
import views.admin.questions.GroupByKeyCollector;

public final class ReadOnlyQuestionServiceImpl implements ReadOnlyQuestionService {
  private final ImmutableMap<Path, ScalarType> scalars;
  private final ImmutableMap<Long, QuestionDefinition> questionsById;
  private final ImmutableMap<Path, QuestionDefinition> questionsByPath;

  private Locale preferredLocale = Locale.US;

  public ReadOnlyQuestionServiceImpl(ImmutableList<QuestionDefinition> questions) {
    checkNotNull(questions);
    ImmutableMap.Builder<Long, QuestionDefinition> questionIdMap = ImmutableMap.builder();
    ImmutableMap.Builder<Path, QuestionDefinition> questionPathMap = ImmutableMap.builder();
    ImmutableMap.Builder<Path, ScalarType> scalarMap = ImmutableMap.builder();
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
    }
    for (QuestionDefinition qd : questions) {
      questionIdMap.put(qd.getId(), qd);
    }
    questionsById = questionIdMap.build();
    questionsByPath = questionPathMap.build();
    scalars = scalarMap.build();
  }

  @Override
  public ImmutableList<QuestionDefinition> getAllQuestions() {
    return questionsById.values().asList();
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
  public void setPreferredLocale(Locale locale) {
    this.preferredLocale = locale;
  }

  @Override
  public Locale getPreferredLocale() {
    return this.preferredLocale;
  }
}
