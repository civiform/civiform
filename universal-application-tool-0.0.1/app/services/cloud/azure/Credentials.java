package services.cloud.azure;

import com.azure.identity.ChainedTokenCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.mockito.Mockito;
import play.Environment;

/**
 * This class retrieves Azure credentials through default provider.
 *
 * <p>See
 * https://docs.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable
 * for more details.
 */
@Singleton
public class Credentials {

  private ChainedTokenCredential azureCredential;

  @Inject
  public Credentials(Environment environment) {
    if (environment.isProd()) {
      azureCredential = new ManagedIdentityCredentialBuilder().build();
    } else {
      azureCredential = Mockito.mock(ChainedTokenCredential.class);
    }
  }

  public ChainedTokenCredential getCredentials() {
    return azureCredential;
  }
}
