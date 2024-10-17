package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Locale;
import play.i18n.Messages;
import services.MessageKey;
import services.applicant.AnswerData;

// Wrapper for AnswerData for ease of rendering in Thymeleaf.
// It's safer to process data in Java than at runtime in Thymeleaf.
public class NorthStarAnswerData implements Comparable<NorthStarAnswerData> {
  private final AnswerData answerData;
  private final Messages messages;

  public NorthStarAnswerData(AnswerData data, Messages messages) {
    this.answerData = checkNotNull(data);
    this.messages = checkNotNull(messages);
  }

  public String blockId() {
    return answerData.blockId();
  }

  public int questionIndex() {
    return answerData.questionIndex();
  }

  public String questionText() {
    return answerData.questionText();
  }

  public boolean isOptional() {
    return answerData.applicantQuestion().isOptional();
  }

  public String questionAriaLabel() {
    return messages.at(MessageKey.LINK_OPENS_NEW_TAB_SR.getKeyName()).toLowerCase(Locale.ROOT);
  }

  public String answerText() {
    String defaultAnswerString =
        answerData.applicantQuestion().getQuestion().getDefaultAnswerString();
    boolean hasAnswerText =
        !answerData.answerText().isBlank() && !answerData.answerText().equals(defaultAnswerString);

    // TODO(#8793) Support file question type

    if (answerData.isAnswered() || hasAnswerText) {
      // TODO(#8794) Make multi-line answers match the mocks
      return answerData.answerText();
    } else {
      return defaultAnswerString;
    }
  }

  // TODO(#8795): Handle enumerator questions

  @Override
  public int compareTo(NorthStarAnswerData other) {
    return Integer.compare(this.questionIndex(), other.questionIndex());
  }

  @Override
  public String toString() {
    return this.questionText() + " " + this.answerText();
  }
}
