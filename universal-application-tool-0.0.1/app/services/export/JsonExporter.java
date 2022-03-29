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

    applications.stream()
        .forEach(
            application -> {
              ReadOnlyApplicantProgramService applicantProgramService =
                  applicantService.getReadOnlyApplicantProgramService(
                      application, programDefinition);
              DocumentContext applicationJson = buildJsonApplication(applicantProgramService);
              jsonApplications.add("$", applicationJson.json());
            });

    return jsonApplications.jsonString();
  }

  private DocumentContext buildJsonApplication(
      ReadOnlyApplicantProgramService roApplicantProgramService) {
    DocumentContext jsonApplication = makeEmptyJsonObject();
    CfJsonObject cfJsonObject = new CfJsonObject(jsonApplication);

    for (AnswerData answerData : roApplicantProgramService.getSummaryData()) {
      for (Map.Entry<Path, String> answer : answerData.scalarAnswersInDefaultLocale().entrySet()) {
        cfJsonObject.putString(answer.getKey(), answer.getValue());
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
