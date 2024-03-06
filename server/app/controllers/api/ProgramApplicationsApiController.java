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
import models.ApplicationModel;
import play.libs.F;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import repository.SubmittedApplicationFilter;
import repository.TimeFilter;
import repository.VersionRepository;
import services.DateConverter;
import services.IdentifierBasedPaginationSpec;
import services.PaginationResult;
import services.export.JsonExporter;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

/** API controller for admin access to a specific program's applications. */
public final class ProgramApplicationsApiController extends CiviFormApiController {

  public static final String PROGRAM_SLUG_PARAM_NAME = "programSlug";
  public static final String FROM_DATE_PARAM_NAME = "fromDate";
  public static final String UNTIL_DATE_PARAM_NAME = "toDate";
  private final DateConverter dateConverter;
  private final ProgramService programService;
  private final HttpExecutionContext httpContext;
  private final JsonExporter jsonExporter;
  private final int maxPageSize;

  @Inject
  public ProgramApplicationsApiController(
      ApiPaginationTokenSerializer apiPaginationTokenSerializer,
      ApiPayloadWrapper apiPayloadWrapper,
      DateConverter dateConverter,
      ProfileUtils profileUtils,
      JsonExporter jsonExporter,
      HttpExecutionContext httpContext,
      ProgramService programService,
      VersionRepository versionRepository,
      Config config) {
    super(apiPaginationTokenSerializer, apiPayloadWrapper, profileUtils, versionRepository);
    this.dateConverter = checkNotNull(dateConverter);
    this.httpContext = checkNotNull(httpContext);
    this.jsonExporter = checkNotNull(jsonExporter);
    this.programService = checkNotNull(programService);
    this.maxPageSize = checkNotNull(config).getInt("civiform_api_applications_list_max_page_size");
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

    SubmittedApplicationFilter filters =
        SubmittedApplicationFilter.builder()
            .setSubmitTimeFilter(
                TimeFilter.builder()
                    .setFromTime(
                        resolveDateParam(paginationToken, FROM_DATE_PARAM_NAME, fromDateParam))
                    .setUntilTime(
                        resolveDateParam(paginationToken, UNTIL_DATE_PARAM_NAME, toDateParam))
                    .build())
            .build();
    int pageSize = resolvePageSize(paginationToken, pageSizeParam);

    IdentifierBasedPaginationSpec<Long> paginationSpec =
        paginationToken
            .map(this::createPaginationSpec)
            .orElse(new IdentifierBasedPaginationSpec<>(pageSize, Long.MAX_VALUE));

    return programService
        .getActiveFullProgramDefinitionAsync(programSlug)
        .thenApplyAsync(
            programDefinition -> {
              PaginationResult<ApplicationModel> paginationResult =
                  programService.getSubmittedProgramApplicationsAllVersions(
                      programDefinition.id(), F.Either.Left(paginationSpec), filters, request);

              String applicationsJson =
                  jsonExporter.exportPage(programDefinition, paginationResult);

              String responseJson =
                  apiPayloadWrapper.wrapPayload(
                      applicationsJson,
                      getNextPageToken(
                          paginationResult, programSlug, pageSize, filters.submitTimeFilter()));

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
      PaginationResult<ApplicationModel> paginationResult,
      String programSlug,
      int pageSize,
      TimeFilter timeFilter) {
    if (!paginationResult.hasMorePages()) {
      return Optional.empty();
    }

    var pageSpec =
        new ApiPaginationTokenPayload.PageSpec(
            Iterables.getLast(paginationResult.getPageContents()).id.toString(), pageSize);

    ImmutableMap.Builder<String, String> requestSpec = ImmutableMap.builder();
    requestSpec.put(PROGRAM_SLUG_PARAM_NAME, programSlug);
    timeFilter
        .fromTime()
        .ifPresent(
            (fromInstant) ->
                requestSpec.put(
                    FROM_DATE_PARAM_NAME, dateConverter.formatIso8601Date(fromInstant)));
    timeFilter
        .untilTime()
        .ifPresent(
            (untilInstant) ->
                requestSpec.put(
                    UNTIL_DATE_PARAM_NAME, dateConverter.formatIso8601Date(untilInstant)));

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
      return dateConverter.parseIso8601DateToStartOfLocalDateInstant(paramDate);
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
