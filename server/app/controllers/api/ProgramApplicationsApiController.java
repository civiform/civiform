package controllers.api;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
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
    super(apiPaginationTokenSerializer, profileUtils);
    this.dateConverter = checkNotNull(dateConverter);
    this.httpContext = checkNotNull(httpContext);
    this.jsonExporter = checkNotNull(jsonExporter);
    this.programService = checkNotNull(programService);
    this.maxPageSize = checkNotNull(config).getInt("api_applications_list_max_page_size");
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

              // By now the program specified by the request has already been found and
              // retrieved, so if a ProgramNotFoundException occurs in the following code
              // it's due to an error in the server code, not a bad request.
              try {
                paginationResult =
                    programService.getSubmittedProgramApplicationsAllVersions(
                        programDefinition.id(), F.Either.Left(paginationSpec), fromTime, toTime);
              } catch (ProgramNotFoundException e) {
                throw new RuntimeException(e);
              }

              String applicationsJson =
                  jsonExporter.export(programDefinition, paginationResult).getLeft();

              String responseJson =
                  getResponseJson(
                      applicationsJson,
                      getNextPageToken(paginationResult, pageSize, fromTime, toTime));

              return ok(responseJson).as("application/json");
            },
            httpContext.current())
        .exceptionally(
            ex -> {
              if (ex instanceof CompletionException) {
                Throwable cause = ex.getCause();
                if (cause instanceof ProgramNotFoundException) {
                  return badRequest(cause.toString());
                }
                throw new RuntimeException(cause);
              }
              throw new RuntimeException(ex);
            });
  }

  private Optional<ApiPaginationTokenPayload> getNextPageToken(
      PaginationResult<Application> paginationResult,
      int pageSize,
      Optional<Instant> fromTime,
      Optional<Instant> toTime) {
    var pageSpec =
        new ApiPaginationTokenPayload.PageSpec(
            Iterables.getLast(paginationResult.getPageContents()).id.toString(), pageSize);

    ImmutableMap.Builder<String, String> requestSpec = ImmutableMap.builder();
    fromTime.ifPresent(
        (fromInstant) -> requestSpec.put("fromDate", dateConverter.formatIso8601Date(fromInstant)));
    toTime.ifPresent(
        (toInstant) -> requestSpec.put("toDate", dateConverter.formatIso8601Date(toInstant)));

    Optional<ApiPaginationTokenPayload> nextPageToken =
        paginationResult.hasMorePages()
            ? Optional.of(new ApiPaginationTokenPayload(pageSpec, requestSpec.build()))
            : Optional.empty();
    return nextPageToken;
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
        Optional.of(Long.valueOf(apiPaginationTokenPayload.getPageSpec().getOffsetIdentifier())));
  }
}
