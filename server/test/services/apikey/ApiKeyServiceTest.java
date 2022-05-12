package services.apikey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static play.test.Helpers.fakeRequest;

import auth.ApiKeyGrants.Permission;
import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import controllers.admin.NotChangeableException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import models.ApiKey;
import org.junit.Before;
import org.junit.Test;
import play.data.DynamicForm;
import play.data.FormFactory;
import repository.ApiKeyRepository;
import repository.ResetPostgres;
import services.DateConverter;
import services.PaginationResult;
import services.PaginationSpec;
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
  public void listApiKeys() {
    String keyName1 = "test key 1";
    String keyName2 = "test key 2";

    for (String keyName : List.of(keyName1, keyName2)) {
      apiKeyService.createApiKey(
          buildForm(
              ImmutableMap.of(
                  "keyName", keyName,
                  "expiration", "2020-01-30",
                  "subnet", "0.0.0.1/32")),
          adminProfile);
    }

    PaginationResult<ApiKey> paginationResult =
        apiKeyService.listApiKeys(PaginationSpec.MAX_PAGE_SIZE_SPEC);

    assertThat(paginationResult.getPageContents().get(0).getName()).isEqualTo(keyName2);
    assertThat(paginationResult.getPageContents().get(1).getName()).isEqualTo(keyName1);
  }

  @Test
  public void retireApiKey_retiresAnApiKey() {
    ApiKey apiKey =
        apiKeyService
            .createApiKey(
                buildForm(
                    ImmutableMap.of(
                        "keyName", "test key 1",
                        "expiration", "2020-01-30",
                        "subnet", "0.0.0.1/32")),
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
    ApiKey apiKey =
        apiKeyService
            .createApiKey(
                buildForm(
                    ImmutableMap.of(
                        "keyName", "test key 1",
                        "expiration", "2020-01-30",
                        "subnet", "0.0.0.1/32")),
                adminProfile)
            .getApiKey();
    apiKeyService.retireApiKey(apiKey.id, adminProfile);

    NotChangeableException exception =
        assertThrows(
            NotChangeableException.class,
            () -> apiKeyService.retireApiKey(apiKey.id, adminProfile));
    assertThat(exception).hasMessage(String.format("ApiKey %s is already retired", apiKey));
  }

  @Test
  public void retireApiKey_keyDoesNotExist_throws() {
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> apiKeyService.retireApiKey(111l, adminProfile));
    assertThat(exception.getCause()).isInstanceOf(ApiKeyNotFoundException.class);
    assertThat(exception.getCause()).hasMessage("ApiKey not found for database ID: 111");
  }

  @Test
  public void createApiKey_createsAnApiKey() {
    resourceCreator.insertActiveProgram("test program");

    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "keyName", "test key",
                "expiration", "2020-01-30",
                "subnet", "0.0.0.1/32",
                "grant-program-read[test-program]", "true"));

    ApiKeyCreationResult apiKeyCreationResult = apiKeyService.createApiKey(form, adminProfile);

    assertThat(apiKeyCreationResult.isSuccessful()).isTrue();

    String credentialString = apiKeyCreationResult.getCredentials();
    byte[] keyIdBytes = Base64.getDecoder().decode(credentialString);
    String keyId =
        Iterables.get(Splitter.on(':').split(new String(keyIdBytes, StandardCharsets.UTF_8)), 0);
    ApiKey apiKey = apiKeyRepository.lookupApiKey(keyId).toCompletableFuture().join().get();

    assertThat(apiKey.getName()).isEqualTo("test key");
    assertThat(apiKey.getSubnet()).isEqualTo("0.0.0.1/32");
    assertThat(apiKey.getExpiration())
        .isEqualTo(dateConverter.parseIso8601DateToStartOfDateInstant("2020-01-30"));
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
  public void createApiKey_grantedProgramSlugNotFound_raisesProgramNotFound() {
    DynamicForm form =
        buildForm(
            ImmutableMap.of(
                "keyName", "test key",
                "expiration", "2020-01-30",
                "subnet", "0.1.1.1",
                "grant-program-read[test-program]", "true"));

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> apiKeyService.createApiKey(form, adminProfile));

    assertThat(exception).hasCause(new ProgramNotFoundException("test-program"));
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

    assertThrows(
        RuntimeException.class, () -> apiKeyService.createApiKey(form, missingAuthorityIdProfile));
  }

  private DynamicForm buildForm(ImmutableMap<String, String> formContents) {
    return formFactory.form().bindFromRequest(fakeRequest().bodyForm(formContents).build());
  }
}
