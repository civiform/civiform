package controllers.api;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import models.Application;
import play.libs.F;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import services.DateConverter;
import services.OffsetBasedPaginationSpec;
import services.PaginationResult;
import services.export.JsonExporter;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

/** API controller for admin access to a specific program's applications. */
public class ProgramApplicationsApiController extends CiviFormApiController {

  private final ApiPaginationTokenSerializer apiPaginationTokenSerializer;
  private final DateConverter dateConverter;
  private final ProgramService programService;
  private final HttpExecutionContext httpContext;
  private final JsonExporter jsonExporter;

  @Inject
  public ProgramApplicationsApiController(
      ApiPaginationTokenSerializer apiPaginationTokenSerializer,
      DateConverter dateConverter,
      ProfileUtils profileUtils,
      JsonExporter jsonExporter,
      HttpExecutionContext httpContext,
      ProgramService programService) {
    super(profileUtils);
    this.apiPaginationTokenSerializer = checkNotNull(apiPaginationTokenSerializer);
    this.dateConverter = checkNotNull(dateConverter);
    this.httpContext = checkNotNull(httpContext);
    this.jsonExporter = checkNotNull(jsonExporter);
    this.programService = checkNotNull(programService);
  }

  public CompletionStage<Result> list(
      Http.Request request,
      String programSlug,
      Optional<String> fromDateSerialized,
      Optional<String> toDateSerialized,
      Optional<String> serializedNextPageToken) {
    assertHasProgramReadPermission(request, programSlug);

    var paginationToken = serializedNextPageToken.map(apiPaginationTokenSerializer::deserialize);
    var requestSpec = paginationToken.map(ApiPaginationTokenPayload::getRequestSpec);
    var fromTime =
        requestSpec.map(
            spec -> {
              if (spec.containsKey("fromDate")) {
                return null;
              }
              return dateConverter.parseIso8601DateToStartOfDateInstant(spec.get("fromDate"));
            });
    var toTime =
        requestSpec.map(
            spec -> {
              if (spec.containsKey("toDate")) {
                return null;
              }
              return dateConverter.parseIso8601DateToStartOfDateInstant(spec.get("toDate"));
            });

    OffsetBasedPaginationSpec<Long> paginationSpec =
        paginationToken
            .map(this::createPaginationSpec)
            .orElse(new OffsetBasedPaginationSpec<>(1000));

    return programService
        .getProgramDefinitionAsync(programSlug)
        .thenApplyAsync(
            programDefinition -> {
              PaginationResult<Application> paginationResult;
              try {
                paginationResult =
                    programService.getSubmittedProgramApplicationsAllVersions(
                        programDefinition.id(), F.Either.Left(paginationSpec), fromTime, toTime);
              } catch (ProgramNotFoundException e) {
                throw new RuntimeException(e);
              }

              String applicationsJson =
                  jsonExporter.export(programDefinition, paginationResult).getLeft();
              Long offsetIdentifier = Iterables.getLast(paginationResult.getPageContents()).id;

              String responseJson =
                  getResponseJson(
                      applicationsJson,
                      paginationResult.hasMorePages(),
                      paginationSpec.setCurrentPageOffsetIdentifier(offsetIdentifier));

              return ok(responseJson).as("application/json");
            },
            httpContext.current());
  }

  private OffsetBasedPaginationSpec<Long> createPaginationSpec(
      ApiPaginationTokenPayload apiPaginationTokenPayload) {
    return new OffsetBasedPaginationSpec<>(
        apiPaginationTokenPayload.getPageSpec().getPageSize(),
        1,
        Optional.of(Long.valueOf(apiPaginationTokenPayload.getPageSpec().getOffsetIdentifier())));
  }

  protected String getResponseJson(
      String payload, boolean hasMorePages, OffsetBasedPaginationSpec<Long> paginationSpec) {
    var writer = new StringWriter();

    try {
      var jsonGenerator = new JsonFactory().createGenerator(writer);
      jsonGenerator.writeStartObject();
      jsonGenerator.writeFieldName("payload");
      jsonGenerator.writeRawValue(payload);

      jsonGenerator.writeFieldName("nextPageToken");
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
