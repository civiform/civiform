package controllers.api;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.CompletionStage;
import models.Application;
import org.apache.commons.lang3.tuple.Pair;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import services.OffsetBasedPaginationSpec;
import services.PaginationResult;
import services.export.JsonExporter;
import services.program.ProgramService;

/** API controller for admin access to a specific program's applications. */
public final class ProgramApplicationsApiController extends CiviFormApiController {

  private final ProgramService programService;
  private final HttpExecutionContext httpContext;
  private final JsonExporter jsonExporter;

  @Inject
  public ProgramApplicationsApiController(
      ProfileUtils profileUtils,
      JsonExporter jsonExporter,
      HttpExecutionContext httpContext,
      ProgramService programService) {
    super(profileUtils);
    this.httpContext = checkNotNull(httpContext);
    this.jsonExporter = checkNotNull(jsonExporter);
    this.programService = checkNotNull(programService);
  }

  public CompletionStage<Result> list(Http.Request request, String programSlug) {
    assertHasProgramReadPermission(request, programSlug);

    return programService
        .getProgramDefinitionAsync(programSlug)
        .thenApplyAsync(
            programDefinition -> {
              // TODO parse and validate incoming pagination token
              var paginationSpec = new OffsetBasedPaginationSpec<Long>(1000);

              Pair<String, PaginationResult<Application>> exportResult =
                  jsonExporter.export(programDefinition, paginationSpec);
              var applicationsJson = exportResult.getLeft();
              var paginationResult = exportResult.getRight();
              var offsetIdentifier = Iterables.getLast(paginationResult.getPageContents()).id;

              String responseJson =
                  getResponseJson(
                      applicationsJson,
                      paginationResult.hasMorePages(),
                      paginationSpec.setCurrentPageOffsetIdentifier(offsetIdentifier));

              return ok(responseJson).as("application/json");
            },
            httpContext.current());
  }

  protected String getResponseJson(
      String payload, boolean hasMorePages, OffsetBasedPaginationSpec<Long> paginationSpec) {
    var writer = new StringWriter();

    try {
      var jsonGenerator = new JsonFactory().createGenerator(writer);
      jsonGenerator.writeStartObject();
      jsonGenerator.writeFieldName("payload");
      jsonGenerator.writeRawValue(payload);

      jsonGenerator.writeFieldName("next_page_token");
      if (hasMorePages) {
        // TODO compute the next page token
      } else {
        jsonGenerator.writeNull();
      }
      jsonGenerator.writeEndObject();

      jsonGenerator.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return writer.toString();
  }
}
