package services.question;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableList;
import java.util.Locale;

public class ReadOnlyQuestionServiceImpl implements ReadOnlyQuestionService {
  private final ImmutableMap<String, ScalarType> scalars;
  private final ImmutableMap<String, QuestionDefinition> questions;
  private final ImmutableMap<String, QuestionDefinition> scalarParents;

  private Locale preferredLocale = Locale.ENGLISH;

  public ReadOnlyQuestionServiceImpl(ImmutableList<QuestionDefinition> questions) {
    ImmutableMap.Builder<String, QuestionDefinition> questionMap = ImmutableMap.builder();
    ImmutableMap.Builder<String, ScalarType> scalarMap = ImmutableMap.builder();
    ImmutableMap.Builder<String, QuestionDefinition> scalarParentsMap = ImmutableMap.builder();
    for (QuestionDefinition qd : questions) {
      String questionPath = qd.getPath();
      questionMap.put(questionPath, qd);
      ImmutableMap<String, ScalarType> questionScalars = qd.getScalars();
      questionScalars.entrySet().stream()
          .forEach(
              entry -> {
                String fullPath = questionPath + '.' + entry.getKey();
                scalarMap.put(fullPath, questionScalars.get(entry.getValue()));
                scalarParentsMap.put(fullPath, qd);
              });
    }
    this.questions = questionMap.build();
    this.scalars = scalarMap.build();
    this.scalarParents = scalarParentsMap.build();
  }

  public ImmutableList<QuestionDefinition> getAllQuestions() {
    return questions.values().asList();
  }

  public ImmutableMap<String, ScalarType> getAllScalars() {
    return scalars;
  }

  public ImmutableMap<String, ScalarType> getPathScalars(String pathString)
      throws InvalidPathException {
    PathType pathType = this.getPathType(pathString);
    if (pathType == PathType.QUESTION) {
      return questions.get(pathString).getScalars(/* includeFullPath = */ true);
    } else if (pathType == PathType.SCALAR) {
      ScalarType scalarType = scalars.get(pathString);
      return ImmutableMap.of(pathString, scalarType);
    }
    throw new InvalidPathException(pathString);
  }

  public PathType getPathType(String pathString) {
    return questions.containsKey(pathString)
        ? PathType.QUESTION
        : scalars.containsKey(pathString) ? PathType.SCALAR : PathType.NONE;
  }

  public QuestionDefinition getQuestionDefinition(String pathString) throws InvalidPathException {
    PathType pathType = this.getPathType(pathString);
    if (pathType == PathType.QUESTION) {
      return questions.get(pathString);
    } else if (pathType == PathType.SCALAR) {
      return scalarParents.get(pathString);
    }
    throw new InvalidPathException(pathString);
  }

  public boolean isValid(String pathString) {
    return scalars.containsKey(pathString) || questions.containsKey(pathString);
  }

  public void setPreferredLocale(Locale locale) {
    this.preferredLocale = locale;
  }
}
