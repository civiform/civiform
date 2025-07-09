package controllers.api;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import models.ApplicationModel;
import models.LifecycleStage;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import repository.SubmittedApplicationFilter;
import repository.TimeFilter;
import repository.VersionRepository;
import services.DateConverter;
import services.export.JsonExporterService;
import services.export.enums.RevisionState;
import services.pagination.PaginationResult;
import services.pagination.RowIdSequentialAccessPaginationSpec;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.ProgramType;

/** API controller for admin access to a specific program's applications. */
public final class ProgramApplicationsApiController extends CiviFormApiController {

  public static final String PROGRAM_SLUG_PARAM_NAME = "programSlug";
  public static final String FROM_DATE_PARAM_NAME = "fromDate";
  public static final String UNTIL_DATE_PARAM_NAME = "toDate";
  public static final String REVISION_STATE_PARAM_NAME = "revisionState";
  private final DateConverter dateConverter;
  private final ProgramService programService;
  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final JsonExporterService jsonExporterService;
  private final int maxPageSize;

  @Inject
  public ProgramApplicationsApiController(
      ApiPaginationTokenSerializer apiPaginationTokenSerializer,
      ApiPayloadWrapper apiPayloadWrapper,
      DateConverter dateConverter,
      ProfileUtils profileUtils,
      JsonExporterService jsonExporterService,
      ClassLoaderExecutionContext classLoaderExecutionContext,
      ProgramService programService,
      VersionRepository versionRepository,
      Config config) {
    super(apiPaginationTokenSerializer, apiPayloadWrapper, profileUtils, versionRepository);
    this.dateConverter = checkNotNull(dateConverter);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.jsonExporterService = checkNotNull(jsonExporterService);
    this.programService = checkNotNull(programService);
    this.maxPageSize = checkNotNull(config).getInt("civiform_api_applications_list_max_page_size");
  }

  /**
   * Lists submitted applications for a given program with pagination and filtering support.
   *
   * @param request - HTTP request
   * @param programSlug - unique identifier for the program
   * @param fromDateParam - optional inclusive start date filter for application submissions
   * @param toDateParam - optional exclusive end date filter for application submissions
   * @param revisionStateParam - optional revision state filter
   * @param serializedNextPageToken - optional pagination token for retrieving next page
   * @param pageSizeParam - optional number of applications to return per page
   * @return CompletionStage containing the JSON response with applications and pagination info
   */
  public CompletionStage<Result> list(
      Http.Request request,
      String programSlug,
      Optional<String> fromDateParam,
      Optional<String> toDateParam,
      Optional<String> revisionStateParam,
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
        buildListFilters(paginationToken, fromDateParam, toDateParam, revisionStateParam);

    int pageSize = resolvePageSize(paginationToken, pageSizeParam);
    RowIdSequentialAccessPaginationSpec paginationSpec =
        buildPaginationSpec(paginationToken, pageSize);

    return programService
        .getActiveFullProgramDefinitionAsync(programSlug)
        .thenApplyAsync(
            programDefinition -> {
              if (programDefinition.programType().equals(ProgramType.EXTERNAL)) {
                return badRequest(new ProgramNotFoundException(programSlug).toString());
              }
              PaginationResult<ApplicationModel> paginationResult =
                  programService.getSubmittedProgramApplicationsAllVersions(
                      programDefinition.id(), paginationSpec, filters);

              String applicationsJson =
                  jsonExporterService.exportPage(programDefinition, paginationResult);

              String responseJson =
                  apiPayloadWrapper.wrapPayload(
                      applicationsJson,
                      getNextPageToken(
                          paginationResult,
                          programSlug,
                          pageSize,
                          filters.submitTimeFilter(),
                          filters.lifecycleStages()));

              return ok(responseJson).as("application/json");
            },
            classLoaderExecutionContext.current())
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

  /** Builds filter criteria for submitted applications based on request parameters. */
  private SubmittedApplicationFilter buildListFilters(
      Optional<ApiPaginationTokenPayload> paginationToken,
      Optional<String> fromDateParam,
      Optional<String> toDateParam,
      Optional<String> revisionStateParam) {
    TimeFilter timeFilter =
        TimeFilter.builder()
            .setFromTime(resolveDateParam(paginationToken, FROM_DATE_PARAM_NAME, fromDateParam))
            .setUntilTime(resolveDateParam(paginationToken, UNTIL_DATE_PARAM_NAME, toDateParam))
            .build();
    ImmutableList<LifecycleStage> lifecycleStage =
        resolveRevisionStateParam(paginationToken, revisionStateParam);

    return SubmittedApplicationFilter.builder()
        .setSubmitTimeFilter(timeFilter)
        .setLifecycleStages(lifecycleStage)
        .build();
  }

  /** Creates pagination specification for database queries. */
  private RowIdSequentialAccessPaginationSpec buildPaginationSpec(
      Optional<ApiPaginationTokenPayload> paginationToken, Integer pageSize) {
    return paginationToken
        .map(
            token ->
                new RowIdSequentialAccessPaginationSpec(
                    token.getPageSpec().getPageSize(),
                    Long.valueOf(token.getPageSpec().getOffsetIdentifier())))
        .orElse(new RowIdSequentialAccessPaginationSpec(pageSize, Long.MAX_VALUE));
  }

  /**
   * Generates a pagination token for retrieving the next page of results. The token encapsulates
   * both pagination state and filter parameters to ensure consistent filtering across paginated
   * requests.
   */
  private Optional<ApiPaginationTokenPayload> getNextPageToken(
      PaginationResult<ApplicationModel> paginationResult,
      String programSlug,
      int pageSize,
      TimeFilter timeFilter,
      ImmutableList<LifecycleStage> lifecycleStages) {
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
    String revisionStates =
        lifecycleStages.stream()
            .map(
                (LifecycleStage stage) ->
                    stage == LifecycleStage.ACTIVE ? RevisionState.CURRENT : RevisionState.OBSOLETE)
            .map((RevisionState state) -> state.name())
            .collect(Collectors.joining(","));
    requestSpec.put(REVISION_STATE_PARAM_NAME, revisionStates);

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
      return dateConverter.parseIso8601DateToLocalDateTimeInstant(paramDate);
    } catch (DateTimeParseException e) {
      throw new BadApiRequestException("Malformed query param: " + paramName);
    }
  }

  /**
   * Resolves the lifecycle stage from either the revision state parameter or pagination token.
   * Ensures consistency between pagination token and query parameter when both are present.
   */
  private ImmutableList<LifecycleStage> resolveRevisionStateParam(
      Optional<ApiPaginationTokenPayload> apiPaginationTokenPayload,
      Optional<String> revisionStateParam) {
    // Defaults to all submitted applications if no revision state is provided
    ImmutableList<LifecycleStage> queryLifecycleStages =
        revisionStateParam.isEmpty()
            ? ImmutableList.of(LifecycleStage.ACTIVE, LifecycleStage.OBSOLETE)
            : ImmutableList.of(getLifecycleStage(revisionStateParam.get()));
    ImmutableList<LifecycleStage> tokenLifecycleStages =
        apiPaginationTokenPayload
            .map((token) -> extractLifecycleStagesFromPaginationToken(token))
            .orElse(ImmutableList.of());

    if (tokenLifecycleStages.equals(queryLifecycleStages)) {
      return tokenLifecycleStages;
    }

    if (!tokenLifecycleStages.isEmpty() && !queryLifecycleStages.isEmpty()) {
      throw new BadApiRequestException(
          "Request parameters must match pagination token: " + revisionStateParam);
    }

    return !tokenLifecycleStages.isEmpty() ? tokenLifecycleStages : queryLifecycleStages;
  }

  /**
   * Extracts the lifecycle stage from a pagination token's request specification.
   *
   * @param apiPaginationTokenPayload The pagination token containing previous request context
   * @return Optional LifecycleStage if found in token, empty otherwise
   */
  private ImmutableList<LifecycleStage> extractLifecycleStagesFromPaginationToken(
      ApiPaginationTokenPayload apiPaginationTokenPayload) {
    Map<String, String> requestSpec = apiPaginationTokenPayload.getRequestSpec();

    if (!requestSpec.containsKey(REVISION_STATE_PARAM_NAME)) {
      return ImmutableList.of();
    }

    return Arrays.stream(requestSpec.get(REVISION_STATE_PARAM_NAME).split(","))
        .map(String::trim)
        .map(this::getLifecycleStage)
        .collect(ImmutableList.toImmutableList());
  }

  /** Converts a revision state parameter string to the corresponding LifecycleStage enum */
  private LifecycleStage getLifecycleStage(String revisionStateParam) {
    try {
      RevisionState state = RevisionState.valueOf(revisionStateParam);
      return state == RevisionState.CURRENT ? LifecycleStage.ACTIVE : LifecycleStage.OBSOLETE;
    } catch (IllegalArgumentException e) {
      throw new BadApiRequestException("Invalid revision state parameter: " + revisionStateParam);
    }
  }
}
