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
  private final long applicantId;

  public NorthStarAnswerData(AnswerData data, long applicantId) {
    this.answerData = checkNotNull(data);
    this.applicantId = applicantId;
  }

  public String blockId() {
    return answerData.blockId();
  }

  public int questionIndex() {
    return answerData.questionIndex();
  }

  public String questionHtml(String ariaLabel) {
    return answerData.applicantQuestion().getFormattedQuestionText(ariaLabel);
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

  public ImmutableList<String> urls() {
    ArrayList<String> urls = new ArrayList<String>();

    AnswerData data = this.answerData;
    if (!data.encodedFileKeys().isEmpty()) {
      for (int i = 0; i < data.encodedFileKeys().size(); i++) {
        String encodedFileKey = data.encodedFileKeys().get(i);
        String fileUrl = controllers.routes.FileController.show(applicantId, encodedFileKey).url();
        urls.add(fileUrl);
      }
    } else if (data.encodedFileKey().isPresent()) {
      // TODO(#7493): When single encoded file key is deprecated, delete this branch
      String encodedFileKey = data.encodedFileKey().get();
      String fileUrl = controllers.routes.FileController.show(applicantId, encodedFileKey).url();
      urls.add(fileUrl);
    }
    return ImmutableList.copyOf(urls);
  }

  @Override
  public int compareTo(NorthStarAnswerData other) {
    return Integer.compare(this.questionIndex(), other.questionIndex());
  }
}
