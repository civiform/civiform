package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import services.Path;
import services.applicant.ValidationErrorMessage;

public final class MapQuestion extends AbstractQuestion {

  MapQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    return ImmutableMap.of();
  }

  @Override
  public String getAnswerString() {
    return "";
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of();
  }
}
