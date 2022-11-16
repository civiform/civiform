package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.DocumentContext;
import featureflags.FeatureFlags;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import models.Application;
import org.apache.commons.lang3.tuple.Pair;
import play.libs.F;
import repository.SubmittedApplicationFilter;
import services.CfJsonDocumentContext;
import services.DateConverter;
import services.IdentifierBasedPaginationSpec;
import services.PaginationResult;
import services.Path;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.JsonPathProvider;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.question.CurrencyQuestion;
import services.applicant.question.DateQuestion;
import services.applicant.question.MultiSelectQuestion;
import services.applicant.question.NumberQuestion;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.LocalizedQuestionOption;

/** Exports all applications for a given program as JSON. */
public final class JsonExporter {

  private final ApplicantService applicantService;
  private final ProgramService programService;
  private final FeatureFlags featureFlags;
  private final DateConverter dateConverter;

  @Inject
  JsonExporter(
      ApplicantService applicantService, ProgramService programService, FeatureFlags featureFlags,
    DateConverter dateConverter) {
    this.applicantService = checkNotNull(applicantService);
    this.programService = checkNotNull(programService);
    this.featureFlags = checkNotNull(featureFlags);
    this.dateConverter = dateConverter;
  }

  public Pair<String, PaginationResult<Application>> export(
      ProgramDefinition programDefinition,
      IdentifierBasedPaginationSpec<Long> paginationSpec,
      SubmittedApplicationFilter filters) {
    PaginationResult<Application> paginationResult;
    try {
      paginationResult =
          programService.getSubmittedProgramApplicationsAllVersions(
              programDefinition.id(), F.Either.Left(paginationSpec), filters);
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException(e);
    }

    return export(programDefinition, paginationResult);
  }

  public Pair<String, PaginationResult<Application>> export(
      ProgramDefinition programDefinition, PaginationResult<Application> paginationResult) {
    var applications = paginationResult.getPageContents();

    DocumentContext jsonApplications = makeEmptyJsonArray();

    for (Application application : applications) {
      CfJsonDocumentContext applicationJson = buildJsonApplication(application, programDefinition);
      jsonApplications.add("$", applicationJson.getDocumentContext().json());
    }

    return Pair.of(jsonApplications.jsonString(), paginationResult);
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
    jsonApplication.putString(Path.create("create_time"),
      dateConverter.renderDateTimeDataOnly(application.getCreateTime()));
    jsonApplication.putString(
        Path.create("submitter_email"), application.getSubmitterEmail().orElse("Applicant"));

    Path submitTimePath = Path.create("submit_time");
    Optional.ofNullable(application.getSubmitTime())
      .ifPresentOrElse(
        submitTime -> jsonApplication.putString(
          submitTimePath, dateConverter.renderDateTimeDataOnly(submitTime)),
        () -> jsonApplication.putNull(submitTimePath));

    if (featureFlags.isStatusTrackingEnabled()) {
      Path statusPath = Path.create("status");
      application
          .getLatestStatus()
          .ifPresentOrElse(
              status -> jsonApplication.putString(statusPath, status),
              () -> jsonApplication.putNull(statusPath));
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
              ImmutableList<String> selectedOptions =
                  multiSelectQuestion.getSelectedOptionsValue().get().stream()
                      .map(LocalizedQuestionOption::optionText)
                      .collect(ImmutableList.toImmutableList());

              jsonApplication.putArray(path, selectedOptions);
            }
            break;
          }
        case CURRENCY:
          {
            CurrencyQuestion currencyQuestion =
                answerData.applicantQuestion().createCurrencyQuestion();
            Path path =
                currencyQuestion
                    .getCurrencyPath()
                    .asApplicationPath()
                    .replacingLastSegment("currency_dollars");

            if (currencyQuestion.getCurrencyValue().isPresent()) {
              Long centsTotal = Long.valueOf(currencyQuestion.getCurrencyValue().get().getCents());

              jsonApplication.putDouble(path, centsTotal.doubleValue() / 100.0);
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
        case DATE:
          {
            DateQuestion dateQuestion = answerData.applicantQuestion().createDateQuestion();
            Path path = dateQuestion.getDatePath().asApplicationPath();

            if (dateQuestion.getDateValue().isPresent()) {
              LocalDate date = dateQuestion.getDateValue().get();
              jsonApplication.putString(path, DateTimeFormatter.ISO_DATE.format(date));
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
