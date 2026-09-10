package mapping.admin.apikeys;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.Arrays;
import java.util.function.Function;
import models.ApiKeyModel;
import modules.MainModule;
import services.DateConverter;
import views.admin.apikeys.ApiKeyIndexPageViewModel;
import views.admin.apikeys.ApiKeyIndexPageViewModel.FilterLink;
import views.admin.apikeys.ApiKeyIndexPageViewModel.ProgramGrant;

/** Maps data to the ApiKeyIndexPageViewModel for the API keys index page. */
public final class ApiKeyIndexPageMapper {

  /**
   * Maps the API keys with the selected status to the view model.
   *
   * @param selectedStatus the status listing being shown
   * @param apiKeys the keys with the selected status
   * @param allProgramNames every program name, used to label the program grants by slug
   * @param dateConverter renders the create/expire/retire timestamps
   */
  public ApiKeyIndexPageViewModel map(
      ApiKeyStatus selectedStatus,
      ImmutableList<ApiKeyModel> apiKeys,
      ImmutableSet<String> allProgramNames,
      DateConverter dateConverter) {
    ImmutableMap<String, String> programSlugToName = buildProgramSlugToName(allProgramNames);

    return ApiKeyIndexPageViewModel.builder()
        .title("API Keys")
        .newKeyUrl(controllers.admin.routes.AdminApiKeysController.newOne().url())
        .filterLinks(buildFilterLinks(selectedStatus))
        .hasApiKeys(!apiKeys.isEmpty())
        .selectedStatusLowercase(selectedStatus.lowercaseName())
        .apiKeys(
            apiKeys.stream()
                .map(apiKey -> buildApiKey(apiKey, programSlugToName, dateConverter))
                .collect(ImmutableList.toImmutableList()))
        .build();
  }

  private ImmutableList<FilterLink> buildFilterLinks(ApiKeyStatus selectedStatus) {
    return Arrays.stream(ApiKeyStatus.values())
        .map(
            status ->
                FilterLink.builder()
                    .text(status.displayName())
                    .href(urlForStatus(status))
                    .selected(status == selectedStatus)
                    .build())
        .collect(ImmutableList.toImmutableList());
  }

  private String urlForStatus(ApiKeyStatus status) {
    return switch (status) {
      case ACTIVE -> controllers.admin.routes.AdminApiKeysController.index().url();
      case RETIRED -> controllers.admin.routes.AdminApiKeysController.indexRetired().url();
      case EXPIRED -> controllers.admin.routes.AdminApiKeysController.indexExpired().url();
    };
  }

  private ApiKeyIndexPageViewModel.ApiKey buildApiKey(
      ApiKeyModel apiKey,
      ImmutableMap<String, String> programSlugToName,
      DateConverter dateConverter) {
    ApiKeyIndexPageViewModel.ApiKey.ApiKeyBuilder builder =
        ApiKeyIndexPageViewModel.ApiKey.builder()
            .name(apiKey.getName())
            .status(statusOf(apiKey).lowercaseName())
            .nameSlug(MainModule.SLUGIFIER.slugify(apiKey.getName()))
            .keyId(apiKey.getKeyId())
            .subnets(String.join(", ", apiKey.getSubnetSet()))
            .expirationDate(dateConverter.renderDate(apiKey.getExpiration()))
            .createdDate(dateConverter.renderDateTimeHumanReadable(apiKey.getCreateTime()))
            .createdBy(apiKey.getCreatedBy())
            .lastCallIp(apiKey.getLastCallIpAddress().orElse("N/A"))
            .callCount(apiKey.getCallCount())
            .retired(apiKey.isRetired())
            .grants(buildGrants(apiKey, programSlugToName));

    // Retired keys show when they were retired; every other key can still be retired.
    if (apiKey.isRetired()) {
      builder.retiredDate(dateConverter.formatRfc1123(apiKey.getRetiredTime().get()));
    } else {
      builder.retireUrl(controllers.admin.routes.AdminApiKeysController.retire(apiKey.id).url());
    }

    return builder.build();
  }

  private ApiKeyStatus statusOf(ApiKeyModel apiKey) {
    if (apiKey.isRetired()) {
      return ApiKeyStatus.RETIRED;
    }

    if (apiKey.expiredAfter(Instant.now())) {
      return ApiKeyStatus.EXPIRED;
    }

    return ApiKeyStatus.ACTIVE;
  }

  private ImmutableList<ProgramGrant> buildGrants(
      ApiKeyModel apiKey, ImmutableMap<String, String> programSlugToName) {
    return apiKey.getGrants().getProgramGrants().entries().stream()
        .map(
            grant ->
                ProgramGrant.builder()
                    .programName(programSlugToName.get(grant.getKey()))
                    .programSlug(grant.getKey())
                    .permission(grant.getValue().name())
                    .build())
        .collect(ImmutableList.toImmutableList());
  }

  private ImmutableMap<String, String> buildProgramSlugToName(ImmutableSet<String> programNames) {
    return programNames.stream()
        .collect(ImmutableMap.toImmutableMap(MainModule.SLUGIFIER::slugify, Function.identity()));
  }
}
