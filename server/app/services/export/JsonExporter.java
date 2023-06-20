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
      AnswerDataConverter.exportToJsonApplication(jsonApplication, answerData);
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
