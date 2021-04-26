package support;

import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;

/** A class that has static methods used to answer {@link ApplicantQuestion}s for each type. */
public class QuestionAnswerer {

    public static void answerAddressQuestion(
            ApplicantData applicantData,
            Path contextualizedPath,
            String street,
            String city,
            String state,
            String zip) {
        applicantData.putString(contextualizedPath.join(Scalar.STREET), street);
        applicantData.putString(contextualizedPath.join(Scalar.CITY), city);
        applicantData.putString(contextualizedPath.join(Scalar.STATE), state);
        applicantData.putString(contextualizedPath.join(Scalar.ZIP), zip);
    }

    public static void answerFileQuestion(
            ApplicantData applicantData, Path contextualizedPath, String fileKey) {
        applicantData.putString(contextualizedPath.join(Scalar.FILE_KEY), fileKey);
    }

    public static void answerMultiSelectQuestion(
            ApplicantData applicantData, Path contextualizedPath, int index, long value) {
        applicantData.putLong(
                contextualizedPath.join(Scalar.SELECTION + Path.ARRAY_SUFFIX).atIndex(index), value);
    }

    public static void answerNameQuestion(
            ApplicantData applicantData,
            Path contextualizedPath,
            String first,
            String middle,
            String last) {

        applicantData.putString(contextualizedPath.join(Scalar.FIRST_NAME), first);
        applicantData.putString(contextualizedPath.join(Scalar.MIDDLE_NAME), middle);
        applicantData.putString(contextualizedPath.join(Scalar.LAST_NAME), last);
    }

    public static void answerNumberQuestion(
            ApplicantData applicantData, Path contextualizedPath, String value) {
        applicantData.putLong(contextualizedPath.join(Scalar.NUMBER), value);
    }

    public static void answerNumberQuestion(
            ApplicantData applicantData, Path contextualizedPath, long value) {
        applicantData.putLong(contextualizedPath.join(Scalar.NUMBER), value);
    }

    public static void answerSingleSelectQuestion(
            ApplicantData applicantData, Path contextualizedPath, long value) {
        applicantData.putLong(contextualizedPath.join(Scalar.SELECTION), value);
    }

    public static void answerTextQuestion(
            ApplicantData applicantData, Path contextualizedPath, String value) {
        applicantData.putString(contextualizedPath.join(Scalar.TEXT), value);
    }

    public static void addMetadata(
            ApplicantData applicantData, Path contextualizedPath, long programId, long timestamp) {
        applicantData.putLong(contextualizedPath.join(Scalar.PROGRAM_UPDATED_IN), programId);
        applicantData.putLong(contextualizedPath.join(Scalar.UPDATED_AT), timestamp);
    }

}
