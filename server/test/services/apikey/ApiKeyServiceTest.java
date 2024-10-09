package services.apikey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ApiKeyGrants.Permission;
import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import controllers.admin.NotChangeableException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import models.ApiKeyModel;
import org.junit.Before;
import org.junit.Test;
import play.data.DynamicForm;
import play.data.FormFactory;
import repository.ApiKeyRepository;
import repository.ResetPostgres;
import services.DateConverter;
import services.program.ProgramNotFoundException;

public class ApiKeyServiceTest extends ResetPostgres {

  private ApiKeyService apiKeyService;
  private ApiKeyRepository apiKeyRepository;
  private ProfileFactory profileFactory;
  private CiviFormProfile adminProfile;
  private FormFactory formFactory;
  private DateConverter dateConverter;

  @Before
  public void setUp() throws Exception {
    apiKeyService = instanceOf(ApiKeyService.class);
    apiKeyRepository = instanceOf(ApiKeyRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);
    CiviFormProfileData profileData = profileFactory.createNewAdmin();
    adminProfile = profileFactory.wrapProfileData(profileData);
    adminProfile.setAuthorityId("authority-id").join();
    formFactory = instanceOf(FormFactory.class);
    dateConverter = instanceOf(DateConverter.class);
  }

  @Test
  public void listActiveApiKeys() {
    resourceCreator.insertActiveProgram("test program");

    Instant now = Instant.now();

    Instant future = now.plusSeconds(60 * 60 * 24 * 7); // 1 week into the future.
    String futureExpirationDate = getApiKeyExpirationDate(future);

    Instant past = now.minusSeconds(60 * 60 * 24); // 1 day in the past.
    String pastExpirationDate = getApiKeyExpirationDate(past);

    for (int i = 0; i < 3; i++) {
      apiKeyService.createApiKey(
          buildForm(
              ImmutableMap.of(
                  "keyName",
                  String.format("test key %s", i),
                  "expiration",
                  futureExpirationDate,
                  "subnet",
                  "0.0.0.1/32",
                  "grant-program-read[test-program]",
                  "true")),
          adminProfile);
    }

    for (int i = 0; i < 3; i++) {
      ApiKeyModel retiredApiKey =
          apiKeyService
              .createApiKey(
                  buildForm(
                      ImmutableMap.of(
                          "keyName",
                          String.format("retired test key %s", i),
                          "expiration",
                          futureExpirationDate,
                          "subnet",
                          "0.0.0.1/32",
                          "grant-program-read[test-program]",
                          "true")),
                  adminProfile)
              .getApiKey();
      apiKeyService.retireApiKey(retiredApiKey.id, adminProfile);
    }
    for (int i = 0; i < 3; i++) {
      apiKeyService
          .createApiKey(
              buildForm(
                  ImmutableMap.of(
                      "keyName",
                      String.format("expired test key %s", i),
                      "expiration",
                      pastExpirationDate,
                      "subnet",
                      "0.0.0.1/32",
                      "grant-program-read[test-program]",
                      "true")),
              adminProfile)
          .getApiKey();
    }

    ImmutableList<ApiKeyModel> activeKeys = apiKeyService.listActiveApiKeys();
    // Keys should be shown in reverse creation order.
    assertThat(activeKeys.stream().map(ApiKeyModel::getName))
        .containsExactly("test key 2", "test key 1", "test key 0");
  }

  @Test
  public void listRetiredApiKeys() {
    resourceCreator.insertActiveProgram("test program");

    Instant now = Instant.now();

    Instant future = now.plusSeconds(60 * 60 * 24 * 7); // 1 week into the future.
    String futureExpirationDate = getApiKeyExpirationDate(future);

    Instant past = now.minusSeconds(60 * 60 * 24); // 1 day in the past.
    String pastExpirationDate = getApiKeyExpirationDate(past);

    for (int i = 0; i < 3; i++) {
      apiKeyService.createApiKey(
          buildForm(
              ImmutableMap.of(
                  "keyName",
                  String.format("test key %s", i),
                  "expiration",
                  futureExpirationDate,
                  "subnet",
                  "0.0.0.1/32",
                  "grant-program-read[test-program]",
                  "true")),
          adminProfile);
    }

    for (int i = 0; i < 3; i++) {
      ApiKeyModel retiredApiKey =
          apiKeyService
              .createApiKey(
                  buildForm(
                      ImmutableMap.of(
                          "keyName",
                          String.format("retired test key %s", i),
                          "expiration",
                          futureExpirationDate,
                          "subnet",
                          "0.0.0.1/32",
                          "grant-program-read[test-program]",
                          "true")),
                  adminProfile)
              .getApiKey();
      apiKeyService.retireApiKey(retiredApiKey.id, adminProfile);
    }

    for (int i = 0; i < 3; i++) {
      apiKeyService
          .createApiKey(
              buildForm(
                  ImmutableMap.of(
                      "keyName",
                      String.format("expired test key %s", i),
                      "expiration",
                      pastExpirationDate,
                      "subnet",
                      "0.0.0.1/32",
                      "grant-program-read[test-program]",
                      "true")),
              adminProfile)
          .getApiKey();
    }

    ImmutableList<ApiKeyModel> result = apiKeyService.listRetiredApiKeys();
    // Keys should be shown in reverse creation order.
    assertThat(result.stream().map(ApiKeyModel::getName))
        .containsExactly("retired test key 2", "retired test key 1", "retired test key 0");
  }

  @Test
  public void listExpiredApiKeys() {
    resourceCreator.insertActiveProgram("test program");

    Instant now = Instant.now();

    Instant future = now.plusSeconds(60 * 60 * 24 * 7); // 1 week into the future.
    String futureExpirationDate = getApiKeyExpirationDate(future);

    Instant past = now.minusSeconds(60 * 60 * 24); // 1 day in the past.
    String pastExpirationDate = getApiKeyExpirationDate(past);

    for (int i = 0; i < 3; i++) {
      // tODO: programmaticaly set expiration date in the future
      apiKeyService.createApiKey(
          buildForm(
              ImmutableMap.of(
                  "keyName",
                  String.format("test key %s", i),
                  "expiration",
                  futureExpirationDate,
                  "subnet",
                  "0.0.0.1/32",
                  "grant-program-read[test-program]",
                  "true")),
          adminProfile);
    }
    for (int i = 0; i < 3; i++) {
      ApiKeyModel retiredApiKey =
          apiKeyService
              .createApiKey(
                  buildForm(
                      ImmutableMap.of(
                          "keyName",
                          String.format("retired test key %s", i),
                          "expiration",
                          futureExpirationDate,
                          "subnet",
                          "0.0.0.1/32",
                          "grant-program-read[test-program]",
                          "true")),
                  adminProfile)
              .getApiKey();
      apiKeyService.retireApiKey(retiredApiKey.id, adminProfile);
    }

    for (int i = 0; i < 3; i++) {
      apiKeyService
          .createApiKey(
              buildForm(
                  ImmutableMap.of(
                      "keyName",
                      String.format("expired test key %s", i),
                      "expiration",
                      pastExpirationDate,
                      "subnet",
                      "0.0.0.1/32",
                      "grant-program-read[test-program]",
                      "true")),
              adminProfile)
          .getApiKey();
    }

    ImmutableList<ApiKeyModel> result = apiKeyService.listExpiredApiKeys();
    // Keys should be shown in reverse creation order.
    assertThat(result.stream().map(ApiKeyModel::getName))
        .containsExactly("expired test key 2", "expired test key 1", "expired test key 0");
  }

  @Test
  public void retireApiKey_retiresAnApiKey() {
    resourceCreator.insertActiveProgram("test program");

    ApiKeyModel apiKey =
        apiKeyService
            .createApiKey(
                buildForm(
                    ImmutableMap.of(
                        "keyName", "test key 1",
                        "expiration", "2020-01-30",
                        "subnet", "0.0.0.1/32",
                        "grant-program-read[test-program]", "true")),
                adminProfile)
            .getApiKey();

    apiKeyService.retireApiKey(apiKey.id, adminProfile);

    apiKey.refresh();

    assertThat(apiKey.isRetired()).isTrue();
    assertThat(apiKey.getRetiredBy().get()).isEqualTo(adminProfile.getAuthorityId().join());
    assertThat(apiKey.getRetiredTime()).isPresent();
  }

  @Test
  public void retireApiKey_keyAlreadyRetired_throws() {
    resourceCreator.insertActiveProgram("test program");

    ApiKeyModel apiKey =
        apiKeyService
            .createApiKey(
                buildForm(
                    ImmutableMap.of(
                        "keyName", "test key 1",
                        "expiration", "2020-01-30",
                        "subnet", "0.0.0.1/32",
                        "grant-program-read[test-program]", "true")),
                adminProfile)
            .getApiKey();

    assertThat(apiKey.getRetiredTime().isPresent())
        .withFailMessage("Key is retired, but should not be.")
        .isFalse();
    ApiKeyModel retiredApiKey = apiKeyService.retireApiKey(apiKey.id, adminProfile);
    assertThat(retiredApiKey.getRetiredTime().isPresent())
        .withFailMessage("Key is not retired, but should be.")
        .isTrue();

    assertThatThrownBy(() -> apiKeyService.retireApiKey(retiredApiKey.id, adminProfile))
        .isInstanceOf(NotChangeableException.class)
        .hasMessage(String.format("ApiKey %s is already retired", retiredApiKey.id));
  }

  @Test
  public void retireApiKey_keyDoesNotExist_throws() {
    assertThatThrownBy(() -> apiKeyService.retireApiKey(111l, adminProfile))
        .isInstanceOf(RuntimeException.class)
        .cause()
        .isInstanceOf(ApiKeyNotFoundException.class)
        .hasMessage("ApiKey not found for database ID: 111");
  }

  @Test
  public void createApiKey_createsAnApiKey() {
    resourceCreator.insertActiveProgram("test program");

    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "keyName", "test key",
                "expiration", "2020-01-30",
                "subnet", "0.0.0.1/32,1.1.1.0/32,1.1.1.0/20",
                "grant-program-read[test-program]", "true"));

    ApiKeyCreationResult apiKeyCreationResult = apiKeyService.createApiKey(form, adminProfile);

    assertThat(apiKeyCreationResult.isSuccessful()).isTrue();

    String credentialString = apiKeyCreationResult.getEncodedCredentials();
    byte[] keyIdBytes = Base64.getDecoder().decode(credentialString);
    String keyId =
        Iterables.get(Splitter.on(':').split(new String(keyIdBytes, StandardCharsets.UTF_8)), 0);
    ApiKeyModel apiKey = apiKeyRepository.lookupApiKey(keyId).toCompletableFuture().join().get();

    assertThat(apiKey.getName()).isEqualTo("test key");
    assertThat(apiKey.getSubnet()).isEqualTo("0.0.0.1/32,1.1.1.0/32,1.1.1.0/20");
    assertThat(apiKey.getSubnetSet())
        .isEqualTo(ImmutableSet.of("0.0.0.1/32", "1.1.1.0/32", "1.1.1.0/20"));
    assertThat(apiKey.getExpiration())
        .isEqualTo(dateConverter.parseIso8601DateToStartOfLocalDateInstant("2020-01-30"));
    assertThat(apiKey.getGrants().hasProgramPermission("test-program", Permission.READ)).isTrue();
  }

  @Test
  public void createApiKey_stripsWhitespace() {
    resourceCreator.insertActiveProgram("test program");

    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "keyName", "test key",
                "expiration", "2020-01-30",
                "subnet", "\t0.0.0.1/32,1.1.1.0/32 ",
                "grant-program-read[test-program]", "true"));

    ApiKeyCreationResult apiKeyCreationResult = apiKeyService.createApiKey(form, adminProfile);

    assertThat(apiKeyCreationResult.isSuccessful()).isTrue();

    String credentialString = apiKeyCreationResult.getEncodedCredentials();
    byte[] keyIdBytes = Base64.getDecoder().decode(credentialString);
    String keyId =
        Iterables.get(Splitter.on(':').split(new String(keyIdBytes, StandardCharsets.UTF_8)), 0);
    ApiKeyModel apiKey = apiKeyRepository.lookupApiKey(keyId).toCompletableFuture().join().get();

    assertThat(apiKey.getName()).isEqualTo("test key");
    assertThat(apiKey.getSubnet()).isEqualTo("0.0.0.1/32,1.1.1.0/32");
    assertThat(apiKey.getSubnetSet()).isEqualTo(ImmutableSet.of("0.0.0.1/32", "1.1.1.0/32"));
    assertThat(apiKey.getExpiration())
        .isEqualTo(dateConverter.parseIso8601DateToStartOfLocalDateInstant("2020-01-30"));
    assertThat(apiKey.getGrants().hasProgramPermission("test-program", Permission.READ)).isTrue();
  }

  @Test
  public void createApiKey_missingKeyName_reportsError() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "keyName", "",
                "expiration", "2020-01-30",
                "subnet", "0.0.0.1/32"));

    ApiKeyCreationResult apiKeyCreationResult = apiKeyService.createApiKey(form, adminProfile);

    assertThat(apiKeyCreationResult.getForm().error("keyName").get().message())
        .isEqualTo("Key name cannot be blank.");

    form =
        buildForm(
            ImmutableMap.of(
                "expiration", "2020-01-30",
                "subnet", "0.0.0.1/32"));

    apiKeyCreationResult = apiKeyService.createApiKey(form, adminProfile);

    assertThat(apiKeyCreationResult.getForm().error("keyName").get().message())
        .isEqualTo("Key name cannot be blank.");
  }

  @Test
  public void createApiKey_missingExpiration_reportsError() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "keyName", "test key",
                "subnet", "0.0.0.1/32"));

    ApiKeyCreationResult apiKeyCreationResult = apiKeyService.createApiKey(form, adminProfile);

    assertThat(apiKeyCreationResult.getForm().error("expiration").get().message())
        .isEqualTo("Expiration cannot be blank.");
  }

  @Test
  public void createApiKey_malformedExpiration_reportsError() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "keyName", "test key",
                "expiration", "01-2020-30",
                "subnet", "0.0.0.1/32"));

    ApiKeyCreationResult apiKeyCreationResult = apiKeyService.createApiKey(form, adminProfile);

    assertThat(apiKeyCreationResult.getForm().error("expiration").get().message())
        .isEqualTo("Expiration must be in the form YYYY-MM-DD.");
  }

  @Test
  public void createApiKey_missingSubnet_reportsError() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "keyName", "test key",
                "expiration", "2020-01-30"));

    ApiKeyCreationResult apiKeyCreationResult = apiKeyService.createApiKey(form, adminProfile);

    assertThat(apiKeyCreationResult.getForm().error("subnet").get().message())
        .isEqualTo("Subnet cannot be blank.");
  }

  @Test
  public void createApiKey_malformedSubnet_reportsError() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "keyName", "test key",
                "expiration", "2020-01-30",
                "subnet", "0.1.1.1"));

    ApiKeyCreationResult apiKeyCreationResult = apiKeyService.createApiKey(form, adminProfile);

    assertThat(apiKeyCreationResult.getForm().error("subnet").get().message())
        .isEqualTo("Subnet must be in CIDR notation.");
  }

  @Test
  public void createApiKey_globalSubnet_reportsError() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "keyName", "test key",
                "expiration", "2020-01-30",
                "subnet", "0.1.1.1/0"));

    ApiKeyCreationResult apiKeyCreationResult = apiKeyService.createApiKey(form, adminProfile);

    assertThat(apiKeyCreationResult.getForm().error("subnet").get().message())
        .isEqualTo("Subnet cannot allow all IP addresses.");
  }

  @Test
  public void createApiKey_noProgramSpecified_reportsError() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "keyName", "test key",
                "expiration", "2020-01-30",
                "subnet", "0.0.0.1/32,1.1.1.0/32"));

    ApiKeyCreationResult apiKeyCreationResult = apiKeyService.createApiKey(form, adminProfile);

    assertThat(apiKeyCreationResult.getForm().error("programs")).isPresent();
  }

  @Test
  public void createApiKey_grantedProgramSlugNotFound_raisesProgramNotFound() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "keyName", "test key",
                "expiration", "2020-01-30",
                "subnet", "0.1.1.1",
                "grant-program-read[test-program]", "true"));

    assertThatThrownBy(() -> apiKeyService.createApiKey(form, adminProfile))
        .isInstanceOf(RuntimeException.class)
        .cause()
        .isInstanceOf(ProgramNotFoundException.class)
        .hasMessageContaining("test-program");
  }

  @Test
  public void createApiKey_profileMissingAuthorityId_raisesRuntimeException() {
    CiviFormProfileData profileData = profileFactory.createNewAdmin();
    CiviFormProfile missingAuthorityIdProfile = profileFactory.wrapProfileData(profileData);
    resourceCreator.insertActiveProgram("test program");

    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "keyName", "test key",
                "expiration", "2020-01-30",
                "subnet", "0.0.0.1/32",
                "grant-program-read[test-program]", "true"));

    assertThatThrownBy(() -> apiKeyService.createApiKey(form, missingAuthorityIdProfile))
        .isInstanceOf(RuntimeException.class);
  }

  private DynamicForm buildForm(ImmutableMap<String, String> formContents) {
    return formFactory.form().bindFromRequest(fakeRequestBuilder().bodyForm(formContents).build());
  }

  private static String getApiKeyExpirationDate(Instant expiration) {
    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withLocale(Locale.US)
            .withZone(ZoneId.systemDefault());
    return formatter.format(expiration);
  }
}
