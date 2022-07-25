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

public class ApiKeyRepositoryTest extends ResetPostgres {

  private ApiKeyRepository repo;

  @Before
  public void setUp() {
    repo = instanceOf(ApiKeyRepository.class);
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

    ApiKeyRepository repo = instanceOf(ApiKeyRepository.class);
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
