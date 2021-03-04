package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;

public final class ReadOnlyQuestionServiceImpl implements ReadOnlyQuestionService {
  private final ImmutableMap<String, ScalarType> scalars;
  private final ImmutableMap<Long, QuestionDefinition> questionsById;
  private final ImmutableMap<String, QuestionDefinition> questionsByPath;
  private final ImmutableMap<String, QuestionDefinition> scalarParents;

  private Locale preferredLocale = Locale.ENGLISH;

  public ReadOnlyQuestionServiceImpl(ImmutableList<QuestionDefinition> questions) {
    checkNotNull(questions);
    ImmutableMap.Builder<Long, QuestionDefinition> questionIdMap = ImmutableMap.builder();
    ImmutableMap.Builder<String, QuestionDefinition> questionPathMap = ImmutableMap.builder();
    ImmutableMap.Builder<String, ScalarType> scalarMap = ImmutableMap.builder();
    ImmutableMap.Builder<String, QuestionDefinition> scalarParentsMap = ImmutableMap.builder();
    for (QuestionDefinition qd : questions) {
      questionIdMap.put(qd.getId(), qd);
      questionPathMap.put(qd.getPath(), qd);
      ImmutableMap<String, ScalarType> questionScalars = qd.getScalars();
      questionScalars.entrySet().stream()
          .forEach(
              entry -> {
                scalarMap.put(entry.getKey(), entry.getValue());
                scalarParentsMap.put(entry.getKey(), qd);
              });
    }
    questionsById = questionIdMap.build();
    questionsByPath = questionPathMap.build();
    scalars = scalarMap.build();
    scalarParents = scalarParentsMap.build();
  }

  @Override
  public ImmutableList<QuestionDefinition> getAllQuestions() {
    return questionsByPath.values().asList();
  }

  @Override
  public ImmutableMap<String, ScalarType> getAllScalars() {
    return scalars;
  }

  @Override
  public ImmutableMap<String, ScalarType> getPathScalars(String pathString)
      throws InvalidPathException {
    PathType pathType = this.getPathType(pathString);
    switch (pathType) {
      case QUESTION:
        return questionsByPath.get(pathString).getScalars();
      case SCALAR:
        ScalarType scalarType = scalars.get(pathString);
        return ImmutableMap.of(pathString, scalarType);
      case NONE:
      default:
        throw new InvalidPathException(pathString);
    }
  }

  @Override
  public PathType getPathType(String pathString) {
    if (questionsByPath.containsKey(pathString)) {
      return PathType.QUESTION;
    } else if (scalars.containsKey(pathString)) {
      return PathType.SCALAR;
    }
    return PathType.NONE;
  }

  @Override
  public QuestionDefinition getQuestionDefinition(String pathString) throws InvalidPathException {
    PathType pathType = this.getPathType(pathString);
    switch (pathType) {
      case QUESTION:
        return questionsByPath.get(pathString);
      case SCALAR:
        return scalarParents.get(pathString);
      case NONE:
      default:
        throw new InvalidPathException(pathString);
    }
  }

  @Override
  public QuestionDefinition getQuestionDefinition(long id) throws QuestionNotFoundException {
    if (questionsById.containsKey(id)) {
      return questionsById.get(id);
    }
    throw new QuestionNotFoundException(id);
  }

  @Override
  public boolean isValid(String pathString) {
    return scalars.containsKey(pathString) || questionsByPath.containsKey(pathString);
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
