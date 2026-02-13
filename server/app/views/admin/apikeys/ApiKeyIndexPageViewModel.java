package views.admin.apikeys;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import controllers.admin.routes;
import java.time.Instant;
import java.util.StringJoiner;
import java.util.function.Function;
import lombok.Builder;
import lombok.Data;
import models.ApiKeyModel;
import modules.MainModule;
import services.DateConverter;
import views.admin.BaseViewModel;

@Data
@Builder
public final class ApiKeyIndexPageViewModel implements BaseViewModel {

  private final String selectedStatus;
  private final ImmutableList<ApiKeyData> apiKeys;

  public String getNewKeyUrl() {
    return routes.AdminApiKeysController.newOne().url();
  }

  public String getActiveUrl() {
    return routes.AdminApiKeysController.index().url();
  }

  public String getRetiredUrl() {
    return routes.AdminApiKeysController.indexRetired().url();
  }

  public String getExpiredUrl() {
    return routes.AdminApiKeysController.indexExpired().url();
  }

  public boolean isActiveSelected() {
    return "Active".equals(selectedStatus);
  }

  public boolean isRetiredSelected() {
    return "Retired".equals(selectedStatus);
  }

  public boolean isExpiredSelected() {
    return "Expired".equals(selectedStatus);
  }

  public String emptyMessage() {
    return "No " + selectedStatus.toLowerCase() + " API keys found.";
  }

  /** Pre-processed API key data for template rendering. */
  @Data
  @Builder
  public static final class ApiKeyData {
    private final long id;
    private final String name;
    private final String nameSlug;
    private final String keyId;
    private final String subnets;
    private final String expirationDate;
    private final String createTime;
    private final String createdBy;
    private final String lastCallIp;
    private final long callCount;
    private final boolean retired;
    private final String retiredTime;
    private final String status;
    private final ImmutableList<GrantData> grants;

    public String getRetireUrl() {
      return routes.AdminApiKeysController.retire(id).url();
    }
  }

  /** A single program grant entry. */
  public record GrantData(String programName, String programSlug, String permission) {}

  /** Factory method to build ApiKeyData from a model. */
  public static ApiKeyData buildApiKeyData(
      ApiKeyModel apiKey,
      ImmutableMap<String, String> programSlugToName,
      DateConverter dateConverter) {
    StringJoiner subnetJoiner = new StringJoiner(", ");
    apiKey.getSubnetSet().forEach(subnetJoiner::add);

    String status = "active";
    if (apiKey.isRetired()) {
      status = "retired";
    } else if (apiKey.expiredAfter(Instant.now())) {
      status = "expired";
    }

    ImmutableList<GrantData> grants =
        apiKey.getGrants().getProgramGrants().entries().stream()
            .map(
                entry ->
                    new GrantData(
                        programSlugToName.getOrDefault(entry.getKey(), entry.getKey()),
                        entry.getKey(),
                        entry.getValue().name()))
            .collect(ImmutableList.toImmutableList());

    return ApiKeyData.builder()
        .id(apiKey.id)
        .name(apiKey.getName())
        .nameSlug(MainModule.SLUGIFIER.slugify(apiKey.getName()))
        .keyId(apiKey.getKeyId())
        .subnets(subnetJoiner.toString())
        .expirationDate(dateConverter.renderDate(apiKey.getExpiration()))
        .createTime(dateConverter.renderDateTimeHumanReadable(apiKey.getCreateTime()))
        .createdBy(apiKey.getCreatedBy())
        .lastCallIp(apiKey.getLastCallIpAddress().orElse("N/A"))
        .callCount(apiKey.getCallCount())
        .retired(apiKey.isRetired())
        .retiredTime(apiKey.getRetiredTime().map(dateConverter::formatRfc1123).orElse(""))
        .status(status)
        .grants(grants)
        .build();
  }

  /** Build a map from program slug to display name. */
  public static ImmutableMap<String, String> buildProgramSlugToName(
      ImmutableSet<String> programNames) {
    return programNames.stream()
        .collect(ImmutableMap.toImmutableMap(MainModule.SLUGIFIER::slugify, Function.identity()));
  }
}
