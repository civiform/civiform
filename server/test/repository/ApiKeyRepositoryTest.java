package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import auth.ApiKeyGrants;
import io.ebean.DataIntegrityException;
import java.time.Instant;
import java.util.concurrent.CompletionException;
import models.ApiKey;
import org.junit.Before;
import org.junit.Test;
import services.PageNumberBasedPaginationSpec;
import services.PaginationResult;

public class ApiKeyRepositoryTest extends ResetPostgres {

  private ApiKeyRepository repo;

  @Before
  public void setUp() {
    repo = instanceOf(ApiKeyRepository.class);
  }

  @Test
  public void listActiveApiKeys() {
    ApiKeyGrants grants = new ApiKeyGrants();
    grants.grantProgramPermission("program-a", ApiKeyGrants.Permission.READ);
    for (int i = 0; i < 3; i++) {
      ApiKey apiKey =
          new ApiKey(grants)
              .setName(String.format("key name %s", i))
              .setKeyId(String.format("key-id-%s", i))
              .setCreatedBy("test@example.com")
              .setSaltedKeySecret(String.format("secret%s", i))
              .setSubnet("0.0.0.0/32")
              .setExpiration(Instant.now().plusSeconds(60 * 60 * 24)); // 1 day in the future.
      repo.insert(apiKey).toCompletableFuture().join();
    }

    for (int i = 0; i < 3; i++) {
      ApiKey apiKey =
          new ApiKey(grants)
              .setName(String.format("retired key name %s", i))
              .setKeyId(String.format("retired-key-id-%s", i))
              .setCreatedBy("test@example.com")
              .setSaltedKeySecret(String.format("retiredsecret%s", i))
              .setSubnet("0.0.0.0/32")
              .setExpiration(Instant.now().plusSeconds(60 * 60 * 24)); // 1 day in the future.
      apiKey.retire("authorityid");
      repo.insert(apiKey).toCompletableFuture().join();
    }

    for (int i = 0; i < 3; i++) {
      ApiKey apiKey =
          new ApiKey(grants)
              .setName(String.format("expired key name %s", i))
              .setKeyId(String.format("expired-id-%s", i))
              .setCreatedBy("test@example.com")
              .setSaltedKeySecret(String.format("expiredsecret%s", i))
              .setSubnet("0.0.0.0/32")
              .setExpiration(Instant.now().minusSeconds(60 * 60)); // 1 hour in the past.
      repo.insert(apiKey).toCompletableFuture().join();
    }

    PageNumberBasedPaginationSpec paginationSpec =
        new PageNumberBasedPaginationSpec(/* pageSize= */ 2);
    PaginationResult<ApiKey> result = repo.listActiveApiKeys(paginationSpec);
    assertThat(result.getNumPages()).isEqualTo(2);
    assertThat(result.getPageContents().stream().map(ApiKey::getName))
        .containsExactly("key name 2", "key name 1");
    assertThat(result.hasMorePages()).isTrue();

    result =
        repo.listActiveApiKeys(
            new PageNumberBasedPaginationSpec(/* pageSize= */ 2, /* currentPage= */ 2));
    assertThat(result.getPageContents().stream().map(ApiKey::getName))
        .containsExactly("key name 0");
    assertThat(result.hasMorePages()).isFalse();
  }

  @Test
  public void listRetiredApiKeys() {
    ApiKeyGrants grants = new ApiKeyGrants();
    grants.grantProgramPermission("program-a", ApiKeyGrants.Permission.READ);
    for (int i = 0; i < 3; i++) {
      ApiKey apiKey =
          new ApiKey(grants)
              .setName(String.format("key name %s", i))
              .setKeyId(String.format("key-id-%s", i))
              .setCreatedBy("test@example.com")
              .setSaltedKeySecret(String.format("secret%s", i))
              .setSubnet("0.0.0.0/32")
              .setExpiration(Instant.now().plusSeconds(60 * 60 * 24)); // 1 day in the future.
      repo.insert(apiKey).toCompletableFuture().join();
    }

    for (int i = 0; i < 3; i++) {
      ApiKey apiKey =
          new ApiKey(grants)
              .setName(String.format("retired key name %s", i))
              .setKeyId(String.format("retired-key-id-%s", i))
              .setCreatedBy("test@example.com")
              .setSaltedKeySecret(String.format("retiredsecret%s", i))
              .setSubnet("0.0.0.0/32")
              .setExpiration(Instant.now().plusSeconds(60 * 60 * 24)); // 1 day in the future.
      apiKey.retire("authorityid");
      repo.insert(apiKey).toCompletableFuture().join();
    }

    for (int i = 0; i < 3; i++) {
      ApiKey apiKey =
          new ApiKey(grants)
              .setName(String.format("expired key name %s", i))
              .setKeyId(String.format("expired-id-%s", i))
              .setCreatedBy("test@example.com")
              .setSaltedKeySecret(String.format("expiredsecret%s", i))
              .setSubnet("0.0.0.0/32")
              .setExpiration(Instant.now().minusSeconds(60 * 60)); // 1 hour in the past.
      repo.insert(apiKey).toCompletableFuture().join();
    }

    PageNumberBasedPaginationSpec paginationSpec =
        new PageNumberBasedPaginationSpec(/* pageSize= */ 2);
    PaginationResult<ApiKey> result = repo.listRetiredApiKeys(paginationSpec);
    assertThat(result.getNumPages()).isEqualTo(2);
    assertThat(result.getPageContents().stream().map(ApiKey::getName))
        .containsExactly("retired key name 2", "retired key name 1");
    assertThat(result.hasMorePages()).isTrue();

    result =
        repo.listRetiredApiKeys(
            new PageNumberBasedPaginationSpec(/* pageSize= */ 2, /* currentPage= */ 2));
    assertThat(result.getPageContents().stream().map(ApiKey::getName))
        .containsExactly("retired key name 0");
    assertThat(result.hasMorePages()).isFalse();
  }

  @Test
  public void listExpiredApiKeys() {
    ApiKeyGrants grants = new ApiKeyGrants();
    grants.grantProgramPermission("program-a", ApiKeyGrants.Permission.READ);
    for (int i = 0; i < 3; i++) {
      ApiKey apiKey =
          new ApiKey(grants)
              .setName(String.format("key name %s", i))
              .setKeyId(String.format("key-id-%s", i))
              .setCreatedBy("test@example.com")
              .setSaltedKeySecret(String.format("secret%s", i))
              .setSubnet("0.0.0.0/32")
              .setExpiration(Instant.now().plusSeconds(60 * 60 * 24)); // 1 day in the future.
      repo.insert(apiKey).toCompletableFuture().join();
    }

    for (int i = 0; i < 3; i++) {
      ApiKey apiKey =
          new ApiKey(grants)
              .setName(String.format("retired key name %s", i))
              .setKeyId(String.format("retired-key-id-%s", i))
              .setCreatedBy("test@example.com")
              .setSaltedKeySecret(String.format("retiredsecret%s", i))
              .setSubnet("0.0.0.0/32")
              .setExpiration(Instant.now().plusSeconds(60 * 60 * 24)); // 1 day in the future.
      apiKey.retire("authorityid");
      repo.insert(apiKey).toCompletableFuture().join();
    }

    for (int i = 0; i < 3; i++) {
      ApiKey apiKey =
          new ApiKey(grants)
              .setName(String.format("expired key name %s", i))
              .setKeyId(String.format("expired-id-%s", i))
              .setCreatedBy("test@example.com")
              .setSaltedKeySecret(String.format("expiredsecret%s", i))
              .setSubnet("0.0.0.0/32")
              .setExpiration(Instant.now().minusSeconds(60 * 60)); // 1 hour in the past.
      repo.insert(apiKey).toCompletableFuture().join();
    }

    PageNumberBasedPaginationSpec paginationSpec =
        new PageNumberBasedPaginationSpec(/* pageSize= */ 2);
    PaginationResult<ApiKey> result = repo.listExpiredApiKeys(paginationSpec);
    assertThat(result.getNumPages()).isEqualTo(2);
    assertThat(result.getPageContents().stream().map(ApiKey::getName))
        .containsExactly("expired key name 2", "expired key name 1");
    assertThat(result.hasMorePages()).isTrue();

    result =
        repo.listExpiredApiKeys(
            new PageNumberBasedPaginationSpec(/* pageSize= */ 2, /* currentPage= */ 2));
    assertThat(result.getPageContents().stream().map(ApiKey::getName))
        .containsExactly("expired key name 0");
    assertThat(result.hasMorePages()).isFalse();
  }

  @Test
  public void retiredAndExpiredKeyIsConsideredRetired() {
    ApiKeyGrants grants = new ApiKeyGrants();
    grants.grantProgramPermission("program-a", ApiKeyGrants.Permission.READ);
    // Key is both retired and expired.
    ApiKey apiKey =
        new ApiKey(grants)
            .setName("key name")
            .setKeyId("key-id")
            .setCreatedBy("test@example.com")
            .setSaltedKeySecret("secret")
            .setSubnet("0.0.0.0/32")
            .setExpiration(Instant.now().minusSeconds(60 * 60 * 24)); // 1 day in the past.
    apiKey.retire("authorityid");
    repo.insert(apiKey).toCompletableFuture().join();

    // Key only shows up in retired list.
    assertThat(
            repo.listExpiredApiKeys(new PageNumberBasedPaginationSpec(/* pageSize= */ 1))
                .getPageContents())
        .isEmpty();
    assertThat(
            repo
                .listRetiredApiKeys(new PageNumberBasedPaginationSpec(/* pageSize= */ 1))
                .getPageContents()
                .stream()
                .map(ApiKey::getName))
        .containsExactly("key name");
  }

  @Test
  public void insert_persistsANewKey() {
    ApiKey foundKey;
    ApiKeyGrants grants = new ApiKeyGrants();
    grants.grantProgramPermission("program-a", ApiKeyGrants.Permission.READ);
    ApiKey apiKey = new ApiKey(grants);

    apiKey
        .setName("key name")
        .setKeyId("key-id")
        .setCreatedBy("test@example.com")
        .setSaltedKeySecret("secret")
        .setSubnet("0.0.0.0/32")
        .setExpiration(Instant.ofEpochSecond(100));

    repo.insert(apiKey).toCompletableFuture().join();

    long id = apiKey.id;
    foundKey = repo.lookupApiKey(id).toCompletableFuture().join().get();
    assertThat(foundKey.id).isEqualTo(id);

    foundKey = repo.lookupApiKey("key-id").toCompletableFuture().join().get();
    assertThat(foundKey.id).isEqualTo(id);
    assertThat(foundKey.getName()).isEqualTo("key name");
    assertThat(foundKey.getKeyId()).isEqualTo("key-id");
    assertThat(foundKey.getCreatedBy()).isEqualTo("test@example.com");
    assertThat(foundKey.getSaltedKeySecret()).isEqualTo("secret");
    assertThat(foundKey.getSubnet()).isEqualTo("0.0.0.0/32");
    assertThat(foundKey.getExpiration()).isEqualTo(Instant.ofEpochSecond(100));
    assertThat(foundKey.getGrants().hasProgramPermission("program-a", ApiKeyGrants.Permission.READ))
        .isTrue();
  }

  @Test
  public void insert_missingRequiredAttributes_raisesAnException() {
    ApiKeyGrants grants = new ApiKeyGrants();
    ApiKey apiKey = new ApiKey(grants);

    assertThatThrownBy(() -> repo.insert(apiKey).toCompletableFuture().join())
        .isInstanceOf(CompletionException.class)
        .cause()
        .isInstanceOf(DataIntegrityException.class)
        .hasMessageContaining("violates not-null constraint");
  }
}
