package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.Authorizers;
import auth.ProfileUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import controllers.CiviFormController;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.libs.Files;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;
import views.admin.importexport.AdminExportView;
import views.admin.importexport.AdminImportView;
import views.admin.importexport.AdminProgramExportForm;

/** TODO */
public final class AdminImportExportController extends CiviFormController {
  private final AdminExportView adminExportView;
  private final AdminImportView adminImportView;
  private final FormFactory formFactory;
  private final ObjectMapper objectMapper;
  private final ProgramService programService;
  private final QuestionService questionService;

  @Inject
  public AdminImportExportController(
      AdminExportView adminExportView,
      AdminImportView adminImportView,
      FormFactory formFactory,
      ObjectMapper objectMapper,
      ProfileUtils profileUtils,
      ProgramService programService,
      QuestionService questionService,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.adminExportView = adminExportView;
    this.adminImportView = checkNotNull(adminImportView);
    this.formFactory = checkNotNull(formFactory);
    this.objectMapper =
        checkNotNull(objectMapper)
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module());
    // https://stackoverflow.com/questions/54249568/cannot-find-a-deserializer-for-non-concrete-map-type-map-type-class-com-google
    // Lets it serialize & deserialize guava types like  ImmutableMap
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
    objectMapper.configure(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION, true);
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> export(Http.Request request) {
    // TODO: active or draft?
    return questionService
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyQuestionService ->
                ok(
                    adminExportView.render(
                        request,
                        programService.getActiveAndDraftPrograms().getActivePrograms(),
                        readOnlyQuestionService
                            .getActiveAndDraftQuestions()
                            .getActiveQuestions())));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result exportPrograms(Http.Request request) throws JsonProcessingException {
    Form<AdminProgramExportForm> form =
        formFactory.form(AdminProgramExportForm.class).bindFromRequest(request);

    List<Long> exportingIds = form.get().getProgramIds();
    System.out.println("exported IDs=" + exportingIds);

    // TODO: Need to fetch all the question definitions for the programs

    ImmutableList<ProgramDefinition> programs =
        exportingIds.stream()
            .map(
                programId -> {
                  try {
                    return programService.getFullProgramDefinition(programId);
                  } catch (ProgramNotFoundException e) {
                    throw new RuntimeException(e);
                  }
                })
            .collect(ImmutableList.toImmutableList());

    ImmutableSet.Builder<Long> questionsUsedByPrograms = ImmutableSet.builder();
    programs.forEach(
        programDefinition ->
            questionsUsedByPrograms.addAll(programDefinition.getQuestionIdsInProgram()));

    System.out.println("questions used= " + questionsUsedByPrograms.build());

    ImmutableList<QuestionDefinition> questions =
        questionsUsedByPrograms.build().stream()
            .map(
                questionId -> {
                  try {
                    return questionService
                        .getReadOnlyQuestionServiceSync()
                        .getQuestionDefinition(questionId);
                  } catch (QuestionNotFoundException e) {
                    throw new RuntimeException(e);
                  }
                })
            .collect(ImmutableList.toImmutableList());

    // String programJson = getPrettyJson(programs);
    // String questionJson = getPrettyJson(questions);

    String json =
        objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(new JsonExportingClass(programs, questions));

    //  CfJsonDocumentContext jsonBuilder = new
    // CfJsonDocumentContext(JsonPathProvider.getJsonPath().parse("{}"));

    /*
    jsonBuilder.putArray(Path.create("programs"), programs.stream().map(this::getPrettyJson).collect(Collectors.toList()));
    jsonBuilder.putArray(Path.create("questions"), questions.stream().map(this::getPrettyJson).collect(Collectors.toList()));


     */
    // jsonApplication.putString(Path.create("programs"), programJson);
    // jsonApplication.putString(Path.create("questions"), questionJson);

    //   objectMapper.writerWithDefaultPrettyPrinter().wri

    String filename = "exported.json";
    // String json = jsonBuilder.asJsonString();
    return ok(json)
        .as(Http.MimeTypes.JSON)
        .withHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));

    /*
    return supplyAsync(
        () -> redirect(controllers.admin.routes.AdminImportExportController.index()));

     */
    /*
    return questionService.getReadOnlyQuestionService()
            .thenApplyAsync(
                    readOnlyQuestionService ->
                            ok(adminImportExportView.render(request,
                                    programService.getActiveAndDraftPrograms().getActivePrograms(),
                                    readOnlyQuestionService.getActiveAndDraftQuestions().getActiveQuestions()))

            );

     */
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> importProgramsPage(Http.Request request) {
    return supplyAsync(() -> ok(adminImportView.render(request, Optional.empty())));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> importPrograms(Http.Request request) throws FileNotFoundException {
    System.out.println("import: request=" + request);

    Http.MultipartFormData<Files.TemporaryFile> body = request.body().asMultipartFormData();
    Http.MultipartFormData.FilePart<Files.TemporaryFile> jsonMaybe = body.getFile("file");
    if (jsonMaybe != null) {
      System.out.println(
          "filename="
              + jsonMaybe.getFilename()
              + "  contenttype="
              + jsonMaybe.getContentType()
              + "  size="
              + jsonMaybe.getFileSize());
      // TODO: Guard based on size?
      Files.TemporaryFile temporaryFile = jsonMaybe.getRef();
      // Needs to be a new filename everytime, otherwise the file content won't change and still
      // have the old content
      // Also we need to delete the file afterwards
      File file = new File("./imported_content_" + Math.random() + ".json");
      System.out.println("fileName=" + file.getName());
      temporaryFile.moveTo(file);

      InputStream inputStream = new FileInputStream(file);
      JsonNode result = Json.parse(inputStream);
      System.out.println("RESULT = " + result);

      try {
        JsonExportingClass receivedStuff = objectMapper.readValue(file, JsonExportingClass.class);
        System.out.println("receivedPrograms= " + receivedStuff.getPrograms());

        // TODO: Can this redirect back to /import instead of /import/programs?
        return supplyAsync(() -> ok(adminImportView.render(request, Optional.of(receivedStuff))));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return supplyAsync(
        () -> redirect(controllers.admin.routes.AdminImportExportController.importProgramsPage()));
  }

  public static final class JsonExportingClass {
    private List<ProgramDefinition> programs;

      private List<QuestionDefinition> questions;

    @JsonCreator
    public JsonExportingClass(
        @JsonProperty("programs")
            List<ProgramDefinition> programs , List<QuestionDefinition> questions) {
      this.programs = programs;
        this.questions = questions;
    }

    public List<ProgramDefinition> getPrograms() {
      return programs;
    }

    public void setPrograms(List<ProgramDefinition> programs) {
      this.programs = programs;
    }

    public List<QuestionDefinition> getQuestions() {
      return questions;
    }

    public void setQuestions(List<QuestionDefinition> questions) {
      this.questions = questions;
    }
  }

  /*
  private <T> String getPrettyJson(T item) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(item);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

   */

  // From DatabaseSeedView
  /*
  private <T> String getPrettyJson(ImmutableList<T> list) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

   */
}
