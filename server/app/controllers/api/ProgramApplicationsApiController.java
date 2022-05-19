package controllers.api;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
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
  private final int maxPageSize;

  @Inject
  public ProgramApplicationsApiController(
      ApiPaginationTokenSerializer apiPaginationTokenSerializer,
      DateConverter dateConverter,
      ProfileUtils profileUtils,
      JsonExporter jsonExporter,
      HttpExecutionContext httpContext,
      ProgramService programService,
      Config config) {
    super(profileUtils);
    this.apiPaginationTokenSerializer = checkNotNull(apiPaginationTokenSerializer);
    this.dateConverter = checkNotNull(dateConverter);
    this.httpContext = checkNotNull(httpContext);
    this.jsonExporter = checkNotNull(jsonExporter);
    this.programService = checkNotNull(programService);
    this.maxPageSize = checkNotNull(config).getInt("api_applications_list_maxPageSize");
  }

  public CompletionStage<Result> list(
      Http.Request request,
      String programSlug,
      Optional<String> fromDateParam,
      Optional<String> toDateParam,
      Optional<String> serializedNextPageToken,
      Optional<Integer> pageSizeParam) {
    assertHasProgramReadPermission(request, programSlug);

    Optional<ApiPaginationTokenPayload> paginationToken =
        serializedNextPageToken.map(apiPaginationTokenSerializer::deserialize);
    Optional<Instant> fromTime = resolveDateParam(paginationToken, "fromDate", fromDateParam);
    Optional<Instant> toTime = resolveDateParam(paginationToken, "toDate", toDateParam);
    int pageSize = resolvePageSize(paginationToken, pageSizeParam);

    OffsetBasedPaginationSpec<Long> paginationSpec =
        paginationToken
            .map(this::createPaginationSpec)
            .orElse(new OffsetBasedPaginationSpec<>(pageSize));

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

  private int resolvePageSize(
      Optional<ApiPaginationTokenPayload> apiPaginationTokenPayload,
      Optional<Integer> pageSizeParam) {
    Optional<Integer> tokenPageSize =
        apiPaginationTokenPayload.map((token) -> token.getPageSpec().getPageSize());

    if (tokenPageSize.isPresent()
        && pageSizeParam.isPresent()
        && !tokenPageSize.equals(pageSizeParam)) {
      throw new BadApiRequestException("Request parameters must match pagination token: pageSize");
    }

    if (tokenPageSize.isPresent()) {
      return tokenPageSize.get();
    }

    return pageSizeParam.orElse(maxPageSize);
  }

  private Optional<Instant> resolveDateParam(
      Optional<ApiPaginationTokenPayload> apiPaginationTokenPayload,
      String paramName,
      Optional<String> queryParamValue) {
    Optional<Instant> tokenTime =
        apiPaginationTokenPayload.map(
            (token) -> extractInstantFromPaginationToken(token, paramName));
    Optional<Instant> queryParamTime =
        queryParamValue.map((value) -> parseParamDateToInstant(paramName, value));

    if (tokenTime.equals(queryParamTime)) {
      return tokenTime;
    }

    if (tokenTime.isPresent() && queryParamTime.isPresent()) {
      throw new BadApiRequestException(
          "Request parameters must match pagination token: " + paramName);
    }

    return Optional.of(tokenTime.orElseGet(queryParamTime::get));
  }

  private Instant extractInstantFromPaginationToken(
      ApiPaginationTokenPayload apiPaginationTokenPayload, String paramName) {
    Map<String, String> requestSpec = apiPaginationTokenPayload.getRequestSpec();

    if (!requestSpec.containsKey(paramName)) {
      return null;
    }

    return parseParamDateToInstant(paramName, requestSpec.get(paramName));
  }

  private Instant parseParamDateToInstant(String paramName, String paramDate) {
    try {
      return dateConverter.parseIso8601DateToStartOfDateInstant(paramDate);
    } catch (DateTimeParseException e) {
      throw new BadApiRequestException("Malformed query param: " + paramName);
    }
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
