package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.jayway.jsonpath.DocumentContext;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import models.Application;
import repository.ProgramRepository;
import services.CfJsonDocumentContext;
import services.PaginationResult;
import services.PaginationSpec;
import services.Path;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.JsonPathProvider;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.question.CurrencyQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.applicant.question.NumberQuestion;
import services.program.ProgramDefinition;
import services.question.LocalizedQuestionOption;

/** Exports all applications for a given program as JSON. */
public class JsonExporter {

  private final ApplicantService applicantService;
  private final ProgramRepository programRepository;

  @Inject
  JsonExporter(ApplicantService applicantService, ProgramRepository programRepository) {
    this.applicantService = checkNotNull(applicantService);
    this.programRepository = checkNotNull(programRepository);
  }

  public String export(ProgramDefinition programDefinition) {
    DocumentContext jsonApplications = makeEmptyJsonArray();
    PaginationResult<Application> applicationPaginationResult =
        programRepository.getApplicationsForAllProgramVersions(
            programDefinition.id(),
            PaginationSpec.MAX_PAGE_SIZE_SPEC,
            /* searchNameFragment= */ Optional.empty());

    for (Application application : applicationPaginationResult.getPageContents()) {
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
        Path.create("submitter_email"), application.getSubmitterEmail().orElse("Applicant"));

    if (application.getSubmitTime() == null) {
      jsonApplication.putNull(Path.create("submit_time"));
    } else {
      jsonApplication.putString(Path.create("submit_time"), application.getSubmitTime().toString());
    }

    for (AnswerData answerData : roApplicantProgramService.getSummaryData()) {
      // Answers to enumerator questions should not be included because the path is incompatible
      // with the JSON export schema. This is because enumerators store an identifier value for
      // each repeated entity, which with the current export logic conflicts with the answers
      // stored for repeated entities.
      if (answerData.questionDefinition().isEnumerator()) {
        continue;
      }

      switch (answerData.questionDefinition().getQuestionType()) {
        case CHECKBOX:
          {
            MultiSelectQuestion multiSelectQuestion =
                answerData.applicantQuestion().createMultiSelectQuestion();
            Path path = multiSelectQuestion.getSelectionPath().asApplicationPath();

            if (multiSelectQuestion.getSelectedOptionsValue().isPresent()) {
              int i = 0;
              for (LocalizedQuestionOption localizedQuestionOption :
                  multiSelectQuestion.getSelectedOptionsValue().get()) {
                jsonApplication.putString(
                    path.asArrayElement().atIndex(i++), localizedQuestionOption.optionText());
              }
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
