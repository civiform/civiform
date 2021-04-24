package support;

import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalars;

/** A class that has static methods used to answer {@link ApplicantQuestion}s for each type. */
public class QuestionAnswerer {

  public static void answerAddressQuestion(
      ApplicantData applicantData,
      Path contextualizedPath,
      String street,
      String city,
      String state,
      String zip) {
    applicantData.putString(contextualizedPath.join(Scalars.STREET), street);
    applicantData.putString(contextualizedPath.join(Scalars.CITY), city);
    applicantData.putString(contextualizedPath.join(Scalars.STATE), state);
    applicantData.putString(contextualizedPath.join(Scalars.ZIP), zip);
  }

  public static void answerFileQuestion(
      ApplicantData applicantData, Path contextualizedPath, String fileKey) {
    applicantData.putString(contextualizedPath.join(Scalars.FILE), fileKey);
  }

  public static void answerMultiSelectQuestion(
      ApplicantData applicantData, Path contextualizedPath, int index, long value) {
    applicantData.putLong(
        contextualizedPath.join(Scalars.SELECTION + Path.ARRAY_SUFFIX).atIndex(index), value);
  }

  public static void answerNameQuestion(
      ApplicantData applicantData,
      Path contextualizedPath,
      String first,
      String middle,
      String last) {

    applicantData.putString(contextualizedPath.join(Scalars.FIRST), first);
    applicantData.putString(contextualizedPath.join(Scalars.MIDDLE), middle);
    applicantData.putString(contextualizedPath.join(Scalars.LAST), last);
  }

  public static void answerNumberQuestion(
      ApplicantData applicantData, Path contextualizedPath, String value) {
    applicantData.putLong(contextualizedPath.join(Scalars.NUMBER), value);
  }

  public static void answerNumberQuestion(
      ApplicantData applicantData, Path contextualizedPath, long value) {
    applicantData.putLong(contextualizedPath.join(Scalars.NUMBER), value);
  }

  public static void answerSingleSelectQuestion(
      ApplicantData applicantData, Path contextualizedPath, long value) {
    applicantData.putLong(contextualizedPath.join(Scalars.SELECTION), value);
  }

  public static void answerTextQuestion(
      ApplicantData applicantData, Path contextualizedPath, String value) {
    applicantData.putString(contextualizedPath.join(Scalars.TEXT), value);
  }

  public static void addMetadata(
      ApplicantData applicantData, Path contextualizedPath, long programId, long timestamp) {
    applicantData.putLong(
        contextualizedPath.join(Scalars.METADATA_UPDATED_PROGRAM_ID_KEY), programId);
    applicantData.putLong(contextualizedPath.join(Scalars.METADATA_UPDATED_AT_KEY), timestamp);
  }
}
