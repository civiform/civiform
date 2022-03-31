package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.DocumentContext;
import java.io.IOException;
import java.util.Map;
import javax.inject.Inject;
import models.Application;
import models.Program;
import services.CfJsonDocumentContext;
import services.Path;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.JsonPathProvider;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.question.CurrencyQuestion;
import services.applicant.question.NumberQuestion;
import services.program.ProgramDefinition;

/** Exports all applications for a given program as JSON. */
public class JsonExporter {

  private final ApplicantService applicantService;

  @Inject
  JsonExporter(ApplicantService applicantService) {
    this.applicantService = checkNotNull(applicantService);
  }

  public String export(Program program) throws IOException {
    DocumentContext jsonApplications = makeEmptyJsonArray();
    ProgramDefinition programDefinition = program.getProgramDefinition();
    ImmutableList<Application> applications = program.getSubmittedApplications();

    for (Application application : applications) {
      CfJsonDocumentContext applicationJson = buildJsonApplication(application, programDefinition);
      jsonApplications.add("$", applicationJson.getDocumentContext().json());
    }

    return jsonApplications.jsonString();
  }

  private CfJsonDocumentContext buildJsonApplication(
      Application application, ProgramDefinition programDefinition) {
    ReadOnlyApplicantProgramService roApplicantProgramService =
        applicantService.getReadOnlyApplicantProgramService(application, programDefinition);
    CfJsonDocumentContext jsonApplication = new CfJsonDocumentContext(makeEmptyJsonObject());

    jsonApplication.putString(
        Path.create("program_name"), application.getProgram().getProgramDefinition().adminName());
    jsonApplication.putLong(Path.create("program_version_id"), application.getProgram().id);
    jsonApplication.putLong(Path.create("applicant_id"), application.getApplicant().id);
    jsonApplication.putLong(Path.create("application_id"), application.id);
    jsonApplication.putString(
        Path.create("language"),
        roApplicantProgramService.getApplicantData().preferredLocale().toLanguageTag());
    jsonApplication.putString(Path.create("create_time"), application.getCreateTime().toString());
    jsonApplication.putString(
        Path.create("submit_time"),
        application.getSubmitTime() == null ? "" : application.getSubmitTime().toString());
    jsonApplication.putString(
        Path.create("submitter_email"), application.getSubmitterEmail().orElse("Applicant"));

    for (AnswerData answerData : roApplicantProgramService.getSummaryData()) {
      // Answers to enumerator questions should not be included because the bath is incompatible
      // with the
      // JSON export schema. This is because enumerators store an identifier value for each repeated
      // entity, which
      // with the current export logic conflicts with the answers stored for repeated entities.
      if (answerData.questionDefinition().isEnumerator()) {
        continue;
      }

      switch (answerData.questionDefinition().getQuestionType()) {
        case NUMBER:
          {
            NumberQuestion numberQuestion = answerData.applicantQuestion().createNumberQuestion();
            Path path = numberQuestion.getNumberPath().asApplicationPath();

            if (numberQuestion.getNumberValue().isPresent()) {
              jsonApplication.putLong(path, numberQuestion.getNumberValue().get());
            } else {
              jsonApplication.putNull(path);
            }

            break;
          }
        case CURRENCY:
          {
            CurrencyQuestion currencyQuestion =
                answerData.applicantQuestion().createCurrencyQuestion();
            Path path = currencyQuestion.getCurrencyPath().asApplicationPath();

            if (currencyQuestion.getValue().isPresent()) {
              jsonApplication.putLong(path, currencyQuestion.getValue().get().getCents());
            } else {
              jsonApplication.putNull(path);
            }

            break;
          }
        default:
          {
            for (Map.Entry<Path, String> answer :
                answerData.scalarAnswersInDefaultLocale().entrySet()) {
              jsonApplication.putString(answer.getKey().asApplicationPath(), answer.getValue());
            }
          }
      }
    }

    return jsonApplication;
  }

  private DocumentContext makeEmptyJsonArray() {
    return JsonPathProvider.getJsonPath().parse("[]");
  }

  private DocumentContext makeEmptyJsonObject() {
    return JsonPathProvider.getJsonPath().parse("{}");
  }
}
