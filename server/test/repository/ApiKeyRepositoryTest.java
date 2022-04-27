package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    ApiKey apiKey = new ApiKey();
    apiKey.setName("key name");
    apiKey.setKeyId("key-id");
    apiKey.setCreatedBy("test@example.com");
    apiKey.setSaltedKeySecret("secret");
    apiKey.setSubnet("0.0.0.0/32");
    apiKey.setExpiration(Instant.ofEpochSecond(100));
    ApiKeyGrants grants = new ApiKeyGrants();
    grants.grantProgramPermission("program-a", ApiKeyGrants.Permission.READ);
    apiKey.setGrants(grants);

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
    ApiKey apiKey = new ApiKey();

    CompletionException exception =
        assertThrows(
            CompletionException.class, () -> repo.insert(apiKey).toCompletableFuture().join());

    assertThat(exception.getCause()).isInstanceOf(DataIntegrityException.class);
    assertThat(exception.getCause().getMessage()).contains("violates not-null constraint");
  }
}
