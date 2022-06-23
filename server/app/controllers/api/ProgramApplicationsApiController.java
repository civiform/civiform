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
import javax.annotation.Nullable;
import models.Application;
import play.api.libs.concurrent.AkkaSchedulerProvider;
import play.libs.F;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import repository.ApiKeyRepository;
import scala.concurrent.ExecutionContext;
import services.DateConverter;
import services.IdentifierBasedPaginationSpec;
import services.PaginationResult;
import services.export.JsonExporter;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

/** API controller for admin access to a specific program's applications. */
public class ProgramApplicationsApiController extends CiviFormApiController {

  public static final String PROGRAM_SLUG_PARAM_NAME = "programSlug";
  public static final String FROM_DATE_PARAM_NAME = "fromDate";
  public static final String TO_DATE_PARAM_NAME = "toDate";
  private final DateConverter dateConverter;
  private final ProgramService programService;
  private final HttpExecutionContext httpContext;
  private final JsonExporter jsonExporter;
  private final int maxPageSize;

  @Inject
  public ProgramApplicationsApiController(
      ApiPaginationTokenSerializer apiPaginationTokenSerializer,
      ApiKeyRepository apiKeyRepository,
      AkkaSchedulerProvider akkaSchedulerProvider,
      DateConverter dateConverter,
      ExecutionContext executionContext,
      HttpExecutionContext httpContext,
      JsonExporter jsonExporter,
      ProfileUtils profileUtils,
      ProgramService programService,
      Config config) {
    super(
        apiPaginationTokenSerializer,
        apiKeyRepository,
        akkaSchedulerProvider,
        executionContext,
        profileUtils);
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

    paginationToken.ifPresent(
        (token) -> {
          if (!token
              .getRequestSpec()
              .getOrDefault(PROGRAM_SLUG_PARAM_NAME, "")
              .equals(programSlug)) {
            throw new BadApiRequestException("Pagination token does not match requested resource.");
          }
        });

    Optional<Instant> fromTime =
        resolveDateParam(paginationToken, FROM_DATE_PARAM_NAME, fromDateParam);
    Optional<Instant> toTime = resolveDateParam(paginationToken, TO_DATE_PARAM_NAME, toDateParam);
    int pageSize = resolvePageSize(paginationToken, pageSizeParam);

    IdentifierBasedPaginationSpec<Long> paginationSpec =
        paginationToken
            .map(this::createPaginationSpec)
            .orElse(new IdentifierBasedPaginationSpec<>(pageSize, Long.MAX_VALUE));

    return programService
        .getProgramDefinitionAsync(programSlug)
        .thenCombineAsync(
            recordApiKeyUsage(request),
            (programDefinition, unusedApiKey) -> {
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
                      getNextPageToken(paginationResult, programSlug, pageSize, fromTime, toTime));

              return ok(responseJson).as("application/json");
            },
            httpContext.current())
        .whenCompleteAsync(
            (result, exception) -> {
              if (exception != null) {
                if (exception instanceof CompletionException) {
                  Throwable cause = exception.getCause();
                  if (cause instanceof ProgramNotFoundException) {
                    throw new BadApiRequestException(exception.getMessage());
                  }
                  throw new RuntimeException(cause);
                }
                throw new RuntimeException(exception);
              }
            });
  }

  private Optional<ApiPaginationTokenPayload> getNextPageToken(
      PaginationResult<Application> paginationResult,
      String programSlug,
      int pageSize,
      Optional<Instant> fromTime,
      Optional<Instant> toTime) {
    if (!paginationResult.hasMorePages()) {
      return Optional.empty();
    }

    var pageSpec =
        new ApiPaginationTokenPayload.PageSpec(
            Iterables.getLast(paginationResult.getPageContents()).id.toString(), pageSize);

    ImmutableMap.Builder<String, String> requestSpec = ImmutableMap.builder();
    requestSpec.put(PROGRAM_SLUG_PARAM_NAME, programSlug);
    fromTime.ifPresent(
        (fromInstant) ->
            requestSpec.put(FROM_DATE_PARAM_NAME, dateConverter.formatIso8601Date(fromInstant)));
    toTime.ifPresent(
        (toInstant) ->
            requestSpec.put(TO_DATE_PARAM_NAME, dateConverter.formatIso8601Date(toInstant)));

    return Optional.of(new ApiPaginationTokenPayload(pageSpec, requestSpec.build()));
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

    // Some minor differences in date format, such as a trailing "Z", can result in
    // different ISO-8601 date strings that parse to the same Instant. For this reason
    // we compare the values as instants, rather than as strings.
    if (tokenTime.equals(queryParamTime)) {
      return tokenTime;
    }

    if (tokenTime.isPresent() && queryParamTime.isPresent()) {
      throw new BadApiRequestException(
          "Request parameters must match pagination token: " + paramName);
    }

    return Optional.of(tokenTime.orElseGet(queryParamTime::get));
  }

  @Nullable
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

  private IdentifierBasedPaginationSpec<Long> createPaginationSpec(
      ApiPaginationTokenPayload apiPaginationTokenPayload) {
    return new IdentifierBasedPaginationSpec<>(
        apiPaginationTokenPayload.getPageSpec().getPageSize(),
        Long.valueOf(apiPaginationTokenPayload.getPageSpec().getOffsetIdentifier()));
  }
}
