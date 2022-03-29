package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import models.Application;
import models.Program;
import services.Path;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.JsonPathProvider;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.ProgramDefinition;

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
      DocumentContext applicationJson = buildJsonApplication(application, programDefinition);
      jsonApplications.add("$", applicationJson.json());
    }

    return jsonApplications.jsonString();
  }

  private DocumentContext buildJsonApplication(
      Application application, ProgramDefinition programDefinition) {
    ReadOnlyApplicantProgramService roApplicantProgramService =
        applicantService.getReadOnlyApplicantProgramService(application, programDefinition);
    DocumentContext jsonApplication = makeEmptyJsonObject();
    CfJsonObject cfJsonObject = new CfJsonObject(jsonApplication);

    for (AnswerData answerData : roApplicantProgramService.getSummaryData()) {
      cfJsonObject.putLong(Path.create("applicant_id"), application.getApplicant().id);
      cfJsonObject.putLong(Path.create("application_id"), application.id);
      cfJsonObject.putString(
          Path.create("language"),
          roApplicantProgramService.getApplicantData().preferredLocale().toLanguageTag());
      cfJsonObject.putString(Path.create("create_time"), application.getCreateTime().toString());
      cfJsonObject.putString(
          Path.create("submit_time"),
          application.getSubmitTime() == null ? null : application.getSubmitTime().toString());
      cfJsonObject.putString(
          Path.create("submitter_email"), application.getSubmitterEmail().orElse("Applicant"));

      for (Map.Entry<Path, String> answer : answerData.scalarAnswersInDefaultLocale().entrySet()) {
        cfJsonObject.putString(answer.getKey().asApplicationPath(), answer.getValue());
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

  static class CfJsonObject {
    private final DocumentContext jsonData;

    CfJsonObject(DocumentContext jsonData) {
      this.jsonData = checkNotNull(jsonData);
    }

    public void putString(Path path, String value) {
      if (value.isEmpty()) {
        putNull(path);
      } else {
        put(path, value);
      }
    }

    public void putLong(Path path, long value) {
      put(path, value);
    }

    private void putNull(Path path) {
      if (!path.isArrayElement()) {
        put(path, null);
      }
    }

    private void put(Path path, Object value) {
      putParentIfMissing(path);
      if (path.isArrayElement()) {
        putArrayIfMissing(path.withoutArrayReference());
        addAt(path, value);
      } else {
        putAt(path, value);
      }
    }

    private void putParentIfMissing(Path path) {
      Path parentPath = path.parentPath();

      if (hasPath(parentPath)) {
        return;
      }

      putParentIfMissing(parentPath);

      if (parentPath.isArrayElement()) {
        putParentArray(path);
      } else {
        putAt(parentPath, new HashMap<>());
      }
    }

    private void putParentArray(Path path) {
      Path parentPath = path.parentPath();
      int index = parentPath.arrayIndex();

      // For n=0, put a new array in, and add the 0th element.
      if (index == 0) {
        putAt(parentPath.withoutArrayReference(), new ArrayList<>());
        addAt(parentPath, new HashMap<>());

        // For n>0, only add the nth element if the n-1 element exists.
      } else if (hasPath(parentPath.atIndex(index - 1))) {
        addAt(parentPath, new HashMap<>());
      } else {
        Path fakePathForRecursion = path.parentPath().atIndex(index - 1).join("fake");
        putParentIfMissing(fakePathForRecursion);
        addAt(parentPath, new HashMap<>());
      }
    }

    private void putArrayIfMissing(Path path) {
      if (!hasPath(path)) {
        putAt(path, new ArrayList<>());
      }
    }

    public boolean hasPath(Path path) {
      try {
        jsonData.read(path.toString());
      } catch (PathNotFoundException e) {
        return false;
      }
      return true;
    }

    private void putAt(Path path, Object value) {
      jsonData.put(path.parentPath().toString(), path.keyName(), value);
    }

    private void addAt(Path path, Object value) {
      jsonData.add(path.withoutArrayReference().toString(), value);
    }
  }
}
