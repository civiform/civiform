package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.types.AddressQuestionDefinition;
import services.question.types.QuestionType;

public class StaticContentQuestion implements PresentsErrors {

    public StaticContentQuestion(ApplicantQuestion applicantQuestion) {
      this.applicantQuestion = applicantQuestion;
      assertQuestionType();
    }
  
    public void assertQuestionType() {
      if (!applicantQuestion.getType().equals(QuestionType.STATIC)) {
        throw new RuntimeException(
            String.format(
                "Question is not a STATIC question: %s (type: %s)",
                applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
                applicantQuestion.getQuestionDefinition().getQuestionType()));
      }
    }

    @Override
    public boolean hasQuestionErrors() {
        return false;
    }

    @Override
    public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
        return ImmutableSet.of();
    }

    @Override
    public boolean hasTypeSpecificErrors() {
        return false;
    }

    @Override
    public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
        return ImmutableSet.of();
    }

    @Override
    public boolean isAnswered() {
        return true;
    }

    @Overrride
    public String getAnswerString() {
        return "";
    }

    @Override
    public ImmutableList<Path> getAllPaths() {
        return applicantQuestion.getContextualizedPath().join(Scalar.EMPTY);
    }
}