package mapping.admin.apikeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.ApiKeyGrants;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import models.ApiKeyModel;
import org.junit.Before;
import org.junit.Test;
import services.DateConverter;
import views.admin.apikeys.ApiKeyIndexPageViewModel;
import views.admin.apikeys.ApiKeyIndexPageViewModel.ApiKey;
import views.admin.apikeys.ApiKeyIndexPageViewModel.FilterLink;

public final class ApiKeyIndexPageMapperTest {

  private static final Instant EXPIRES_IN_2100 = Instant.parse("2100-01-01T00:00:00Z");
  private static final Instant EXPIRED_IN_2000 = Instant.parse("2000-01-01T00:00:00Z");
  private static final ImmutableSet<String> PROGRAM_NAMES = ImmutableSet.of("Utility Discount");

  private ApiKeyIndexPageMapper mapper;
  private DateConverter dateConverter;

  @Before
  public void setup() {
    mapper = new ApiKeyIndexPageMapper();
    dateConverter = mock(DateConverter.class);
    when(dateConverter.renderDate(EXPIRES_IN_2100)).thenReturn("2100-01-01");
    when(dateConverter.renderDateTimeHumanReadable(null)).thenReturn("2024/01/01 at 9:00 AM PST");
  }

  private static ApiKeyModel activeApiKey() {
    ApiKeyGrants grants = new ApiKeyGrants();
    grants.grantProgramPermission("utility-discount", ApiKeyGrants.Permission.READ);

    ApiKeyModel apiKey = new ApiKeyModel(grants);
    apiKey.id = 7L;
    return apiKey
        .setName("Test API key")
        .setKeyId("key-id")
        .setSubnet("1.1.1.1/32,2.2.2.2/32")
        .setExpiration(EXPIRES_IN_2100)
        .setCreatedBy("admin@example.com");
  }

  private ApiKeyIndexPageViewModel map(ApiKeyStatus selectedStatus, ApiKeyModel... apiKeys) {
    return mapper.map(selectedStatus, ImmutableList.copyOf(apiKeys), PROGRAM_NAMES, dateConverter);
  }

  @Test
  public void map_setsTitle() {
    ApiKeyIndexPageViewModel result = map(ApiKeyStatus.ACTIVE, activeApiKey());

    assertThat(result.getTitle()).isEqualTo("API Keys");
  }

  @Test
  public void map_setsNewKeyUrl() {
    ApiKeyIndexPageViewModel result = map(ApiKeyStatus.ACTIVE, activeApiKey());

    assertThat(result.getNewKeyUrl()).isEqualTo("/admin/apiKeys/new");
  }

  @Test
  public void map_setsFilterLinks() {
    ApiKeyIndexPageViewModel result = map(ApiKeyStatus.ACTIVE, activeApiKey());

    assertThat(result.getFilterLinks())
        .extracting(FilterLink::getText, FilterLink::getHref)
        .containsExactly(
            tuple("Active", "/admin/apiKeys"),
            tuple("Retired", "/admin/apiKeys/retired"),
            tuple("Expired", "/admin/apiKeys/expired"));
  }

  @Test
  public void map_marksSelectedFilterLink() {
    ApiKeyIndexPageViewModel result = map(ApiKeyStatus.RETIRED, activeApiKey());

    assertThat(result.getFilterLinks())
        .extracting(FilterLink::isSelected)
        .containsExactly(false, true, false);
  }

  @Test
  public void map_noApiKeys_setsEmptyState() {
    ApiKeyIndexPageViewModel result = map(ApiKeyStatus.EXPIRED);

    assertThat(result.isHasApiKeys()).isFalse();
    assertThat(result.getSelectedStatusLowercase()).isEqualTo("expired");
  }

  @Test
  public void map_setsKeyDetails() {
    ApiKey result = map(ApiKeyStatus.ACTIVE, activeApiKey()).getApiKeys().get(0);

    assertThat(result.getName()).isEqualTo("Test API key");
    assertThat(result.getKeyId()).isEqualTo("key-id");
    assertThat(result.getSubnets()).isEqualTo("1.1.1.1/32, 2.2.2.2/32");
    assertThat(result.getExpirationDate()).isEqualTo("2100-01-01");
    assertThat(result.getCreatedDate()).isEqualTo("2024/01/01 at 9:00 AM PST");
    assertThat(result.getCreatedBy()).isEqualTo("admin@example.com");
  }

  @Test
  public void map_setsNameSlugFromKeyName() {
    ApiKey result = map(ApiKeyStatus.ACTIVE, activeApiKey()).getApiKeys().get(0);

    assertThat(result.getNameSlug()).isEqualTo("test-api-key");
  }

  @Test
  public void map_neverUsedKey_setsLastCallIpNotAvailable() {
    ApiKey result = map(ApiKeyStatus.ACTIVE, activeApiKey()).getApiKeys().get(0);

    assertThat(result.getLastCallIp()).isEqualTo("N/A");
    assertThat(result.getCallCount()).isEqualTo(0L);
  }

  @Test
  public void map_usedKey_setsLastCallIpAddress() {
    ApiKeyModel apiKey = activeApiKey().setLastCallIpAddress("1.1.1.1");

    ApiKey result = map(ApiKeyStatus.ACTIVE, apiKey).getApiKeys().get(0);

    assertThat(result.getLastCallIp()).isEqualTo("1.1.1.1");
  }

  @Test
  public void map_activeKey_setsActiveStatusAndRetireUrl() {
    ApiKey result = map(ApiKeyStatus.ACTIVE, activeApiKey()).getApiKeys().get(0);

    assertThat(result.isRetired()).isFalse();
    assertThat(result.getStatus()).isEqualTo("active");
    assertThat(result.getRetireUrl()).isEqualTo("/admin/apiKeys/7/retire");
    assertThat(result.getRetiredDate()).isNull();
  }

  @Test
  public void map_expiredKey_setsExpiredStatus() {
    ApiKeyModel apiKey = activeApiKey().setExpiration(EXPIRED_IN_2000);
    when(dateConverter.renderDate(EXPIRED_IN_2000)).thenReturn("2000-01-01");

    ApiKey result = map(ApiKeyStatus.EXPIRED, apiKey).getApiKeys().get(0);

    assertThat(result.getStatus()).isEqualTo("expired");
    assertThat(result.isRetired()).isFalse();
  }

  @Test
  public void map_retiredKey_setsRetiredDateAndNoRetireUrl() {
    ApiKeyModel apiKey = activeApiKey();
    apiKey.retire("admin@example.com");
    when(dateConverter.formatRfc1123(apiKey.getRetiredTime().get()))
        .thenReturn("Mon, 1 Jan 2024 09:00:00 GMT");

    ApiKey result = map(ApiKeyStatus.RETIRED, apiKey).getApiKeys().get(0);

    assertThat(result.isRetired()).isTrue();
    assertThat(result.getStatus()).isEqualTo("retired");
    assertThat(result.getRetiredDate()).isEqualTo("Mon, 1 Jan 2024 09:00:00 GMT");
    assertThat(result.getRetireUrl()).isNull();
  }

  @Test
  public void map_setsProgramGrantsWithProgramNameFromSlug() {
    ApiKey result = map(ApiKeyStatus.ACTIVE, activeApiKey()).getApiKeys().get(0);

    assertThat(result.getGrants()).hasSize(1);
    assertThat(result.getGrants().get(0).getProgramName()).isEqualTo("Utility Discount");
    assertThat(result.getGrants().get(0).getProgramSlug()).isEqualTo("utility-discount");
    assertThat(result.getGrants().get(0).getPermission()).isEqualTo("READ");
  }
}
