package support;

import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalars;

/** A class that has static methods used to answer {@link ApplicantQuestion}s for each type. */
public class QuestionAnswerer {

  public static void answerAddressQuestion(
      ApplicantData applicantData,
      ApplicantQuestion applicantQuestion,
      String street,
      String city,
      String state,
      String zip) {
    applicantData.putString(applicantQuestion.getContextualizedPath().join(Scalars.STREET), street);
    applicantData.putString(applicantQuestion.getContextualizedPath().join(Scalars.CITY), city);
    applicantData.putString(applicantQuestion.getContextualizedPath().join(Scalars.STATE), state);
    applicantData.putString(applicantQuestion.getContextualizedPath().join(Scalars.ZIP), zip);
  }

  public static void answerFileQuestion(
      ApplicantData applicantData, ApplicantQuestion applicantQuestion, String fileKey) {
    applicantData.putString(applicantQuestion.getContextualizedPath().join(Scalars.FILE), fileKey);
  }

  public static void answerMultiSelectQuestion(
      ApplicantData applicantData, ApplicantQuestion applicantQuestion, int index, long value) {
    applicantData.putLong(
        applicantQuestion
            .getContextualizedPath()
            .join(Scalars.SELECTION + Path.ARRAY_SUFFIX)
            .atIndex(index),
        value);
  }

  public static void answerNameQuestion(
      ApplicantData applicantData,
      ApplicantQuestion applicantQuestion,
      String first,
      String middle,
      String last) {

    applicantData.putString(applicantQuestion.getContextualizedPath().join(Scalars.FIRST), first);
    applicantData.putString(applicantQuestion.getContextualizedPath().join(Scalars.MIDDLE), middle);
    applicantData.putString(applicantQuestion.getContextualizedPath().join(Scalars.LAST), last);
  }

  public static void answerNumberQuestion(
      ApplicantData applicantData, ApplicantQuestion applicantQuestion, String value) {
    applicantData.putLong(applicantQuestion.getContextualizedPath().join(Scalars.NUMBER), value);
  }

  public static void answerNumberQuestion(
      ApplicantData applicantData, ApplicantQuestion applicantQuestion, long value) {
    applicantData.putLong(applicantQuestion.getContextualizedPath().join(Scalars.NUMBER), value);
  }

  public static void answerSingleSelectQuestion(
      ApplicantData applicantData, ApplicantQuestion applicantQuestion, long value) {
    applicantData.putLong(applicantQuestion.getContextualizedPath().join(Scalars.SELECTION), value);
  }

  public static void answerTextQuestion(
      ApplicantData applicantData, ApplicantQuestion applicantQuestion, String value) {
    applicantData.putString(applicantQuestion.getContextualizedPath().join(Scalars.TEXT), value);
  }
}
