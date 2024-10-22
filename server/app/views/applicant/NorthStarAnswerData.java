package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import services.applicant.AnswerData;
import services.question.types.QuestionType;

// Wrapper for AnswerData for ease of rendering in Thymeleaf.
// It's safer to process data in Java than at runtime in Thymeleaf.
public class NorthStarAnswerData implements Comparable<NorthStarAnswerData> {
  private final AnswerData answerData;

  public NorthStarAnswerData(AnswerData data) {
    this.answerData = checkNotNull(data);
  }

  public String blockId() {
    return answerData.blockId();
  }

  public int questionIndex() {
    return answerData.questionIndex();
  }

  public String questionHtml() {
    return answerData.applicantQuestion().getFormattedQuestionText();
  }

  public ImmutableList<String> multilineAnswerText() {
    String defaultAnswerString =
        answerData.applicantQuestion().getQuestion().getDefaultAnswerString();
    boolean hasAnswerText =
        !answerData.answerText().isBlank() && !answerData.answerText().equals(defaultAnswerString);
    boolean isAnswered = answerData.isAnswered() || hasAnswerText;
    boolean isFileUploadQuestion =
        answerData.questionDefinition().getQuestionType() == QuestionType.FILEUPLOAD;
    boolean hasFiles = !answerData.encodedFileKeys().isEmpty();

    if (isFileUploadQuestion && hasFiles) {
      // TODO(#8985): Allow user to download files on this page
      return fileNames();
    } else if (isAnswered) {
      return ImmutableList.of(answerData.answerText());
    } else {
      return ImmutableList.of(defaultAnswerString);
    }
  }

  /**
   * Assumes this question is a file upload question.
   *
   * @return A list of file names (may be empty)
   */
  private ImmutableList<String> fileNames() {
    ArrayList<String> fileNames = new ArrayList<String>();
    for (int i = 0; i < answerData.encodedFileKeys().size(); i++) {
      String fileName = answerData.fileNames().get(i);
      fileNames.add(fileName);
    }
    return ImmutableList.copyOf(fileNames);
  }

  // TODO(#8795): Handle enumerator questions

  @Override
  public int compareTo(NorthStarAnswerData other) {
    return Integer.compare(this.questionIndex(), other.questionIndex());
  }
}
