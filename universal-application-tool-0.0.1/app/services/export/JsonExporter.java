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

    jsonApplication.putLong(Path.create("program_name"), application.getProgram().getProgramDefinition().adminName());
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
      for (Map.Entry<Path, String> answer : answerData.scalarAnswersInDefaultLocale().entrySet()) {
        jsonApplication.putString(answer.getKey().asApplicationPath(), answer.getValue());
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
