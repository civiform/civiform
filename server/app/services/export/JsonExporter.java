package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.jayway.jsonpath.DocumentContext;
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
import services.applicant.question.*;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

/** Exports all applications for a given program as JSON. */
public final class JsonExporter {

  private final ApplicantService applicantService;
  private final ProgramService programService;
  private final DateConverter dateConverter;

  @Inject
  JsonExporter(
      ApplicantService applicantService,
      ProgramService programService,
      DateConverter dateConverter) {
    this.applicantService = checkNotNull(applicantService);
    this.programService = checkNotNull(programService);
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
    jsonApplication.putString(
        Path.create("create_time"),
        dateConverter.renderDateTimeDataOnly(application.getCreateTime()));
    jsonApplication.putString(
        Path.create("submitter_email"), application.getSubmitterEmail().orElse("Applicant"));

    Path submitTimePath = Path.create("submit_time");
    Optional.ofNullable(application.getSubmitTime())
        .ifPresentOrElse(
            submitTime ->
                jsonApplication.putString(
                    submitTimePath, dateConverter.renderDateTimeDataOnly(submitTime)),
            () -> jsonApplication.putNull(submitTimePath));

    Path statusPath = Path.create("status");
    application
        .getLatestStatus()
        .ifPresentOrElse(
            status -> jsonApplication.putString(statusPath, status),
            () -> jsonApplication.putNull(statusPath));

    for (AnswerData answerData : roApplicantProgramService.getSummaryData()) {

      switch (answerData.questionDefinition().getQuestionType()) {
        case ENUMERATOR:
        case STATIC:
          {
            // Enumerator and static content questions are not included in API response. See
            // EnumeratorQuestion.getJsonEntries and StaticContentQuestion.getJsonEntries.
            break;
          }
        case CHECKBOX:
          {
            MultiSelectQuestion multiSelectQuestion =
                answerData.applicantQuestion().createMultiSelectQuestion();
            multiSelectQuestion.getJsonEntries().forEach(jsonApplication::putArray);
            break;
          }
        case CURRENCY:
          {
            CurrencyQuestion currencyQuestion =
                answerData.applicantQuestion().createCurrencyQuestion();
            currencyQuestion.getJsonEntries().forEach(jsonApplication::putDouble);

            break;
          }
        case NUMBER:
          {
            NumberQuestion numberQuestion = answerData.applicantQuestion().createNumberQuestion();
            numberQuestion.getJsonEntries().forEach(jsonApplication::putLong);

            break;
          }
        case DATE:
          {
            DateQuestion dateQuestion = answerData.applicantQuestion().createDateQuestion();
            dateQuestion.getJsonEntries().forEach(jsonApplication::putString);

            break;
          }
        case PHONE:
          {
            PhoneQuestion phoneQuestion = answerData.applicantQuestion().createPhoneQuestion();
            phoneQuestion.getJsonEntries().forEach(jsonApplication::putString);
            break;
          }
        case NAME:
          {
            NameQuestion nameQuestion = answerData.applicantQuestion().createNameQuestion();
            nameQuestion
                .getJsonEntries()
                .forEach((key, value) -> jsonApplication.putString(key.asApplicationPath(), value));
            break;
          }
        case ID:
          {
            IdQuestion idQuestion = answerData.applicantQuestion().createIdQuestion();
            idQuestion
                .getJsonEntries()
                .forEach((key, value) -> jsonApplication.putString(key.asApplicationPath(), value));
            break;
          }
        case TEXT:
          {
            TextQuestion textQuestion = answerData.applicantQuestion().createTextQuestion();
            textQuestion
                .getJsonEntries()
                .forEach((key, value) -> jsonApplication.putString(key.asApplicationPath(), value));
            break;
          }
        case EMAIL:
          {
            EmailQuestion emailQuestion = answerData.applicantQuestion().createEmailQuestion();
            emailQuestion
                .getJsonEntries()
                .forEach((key, value) -> jsonApplication.putString(key.asApplicationPath(), value));
            break;
          }
        case ADDRESS:
          {
            AddressQuestion addressQuestion =
                answerData.applicantQuestion().createAddressQuestion();
            addressQuestion
                .getJsonEntries()
                .forEach((key, value) -> jsonApplication.putString(key.asApplicationPath(), value));
            break;
          }
        case DROPDOWN:
        case RADIO_BUTTON:
          {
            SingleSelectQuestion singleSelectQuestion =
                answerData.applicantQuestion().createSingleSelectQuestion();
            singleSelectQuestion
                .getJsonEntries()
                .forEach((key, value) -> jsonApplication.putString(key.asApplicationPath(), value));
            break;
          }
        case FILEUPLOAD:
          {
            FileUploadQuestion fileUploadQuestion =
                answerData.applicantQuestion().createFileUploadQuestion();
            fileUploadQuestion
                .getJsonEntries()
                .forEach((key, value) -> jsonApplication.putString(key.asApplicationPath(), value));
            break;
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
