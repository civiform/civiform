package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;

public final class ReadOnlyQuestionServiceImpl implements ReadOnlyQuestionService {
  private final ImmutableMap<String, ScalarType> scalars;
  private final ImmutableMap<Long, QuestionDefinition> questionsById;
  private final ImmutableMap<String, QuestionDefinition> questionsByName;
  private final ImmutableMap<String, QuestionDefinition> scalarParents;

  private Locale preferredLocale = Locale.ENGLISH;

  public ReadOnlyQuestionServiceImpl(ImmutableList<QuestionDefinition> questions) {
    checkNotNull(questions);
    ImmutableMap.Builder<Long, QuestionDefinition> questionIdMap = ImmutableMap.builder();
    ImmutableMap.Builder<String, QuestionDefinition> questionNameMap = ImmutableMap.builder();
    ImmutableMap.Builder<String, ScalarType> scalarMap = ImmutableMap.builder();
    ImmutableMap.Builder<String, QuestionDefinition> scalarParentsMap = ImmutableMap.builder();
    for (QuestionDefinition qd : questions) {
      String questionPath = qd.getPath();
      questionIdMap.put(qd.getId(), qd);
      questionNameMap.put(questionPath, qd);
      ImmutableMap<String, ScalarType> questionScalars = qd.getScalars();
      questionScalars.entrySet().stream()
          .forEach(
              entry -> {
                String fullPath = questionPath + '.' + entry.getKey();
                scalarMap.put(fullPath, entry.getValue());
                scalarParentsMap.put(fullPath, qd);
              });
    }
    this.questionsById = questionIdMap.build();
    this.questionsByName = questionNameMap.build();
    this.scalars = scalarMap.build();
    this.scalarParents = scalarParentsMap.build();
  }

  public ImmutableList<QuestionDefinition> getAllQuestions() {
    return questionsByName.values().asList();
  }

  public ImmutableMap<String, ScalarType> getAllScalars() {
    return scalars;
  }

  public ImmutableMap<String, ScalarType> getPathScalars(String pathString)
      throws InvalidPathException {
    PathType pathType = this.getPathType(pathString);
    switch (pathType) {
      case QUESTION:
        return questionsByName.get(pathString).getFullyQualifiedScalars();
      case SCALAR:
        ScalarType scalarType = scalars.get(pathString);
        return ImmutableMap.of(pathString, scalarType);
      case NONE:
      default:
        throw new InvalidPathException(pathString);
    }
  }

  public PathType getPathType(String pathString) {
    if (questionsByName.containsKey(pathString)) {
      return PathType.QUESTION;
    } else if (scalars.containsKey(pathString)) {
      return PathType.SCALAR;
    }
    return PathType.NONE;
  }

  public QuestionDefinition getQuestionDefinition(String pathString) throws InvalidPathException {
    PathType pathType = this.getPathType(pathString);
    switch (pathType) {
      case QUESTION:
        return questionsByName.get(pathString);
      case SCALAR:
        return scalarParents.get(pathString);
      case NONE:
      default:
        throw new InvalidPathException(pathString);
    }
  }

  public QuestionDefinition getQuestionDefinition(long id) throws QuestionNotFoundException {
    if (questionsById.containsKey(id)) {
      return questionsById.get(id);
    }
    throw new QuestionNotFoundException(id);
  }

  public boolean isValid(String pathString) {
    return scalars.containsKey(pathString) || questionsByName.containsKey(pathString);
  }

  public void setPreferredLocale(Locale locale) {
    this.preferredLocale = locale;
  }
}
