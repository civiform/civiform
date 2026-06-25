package services.cloud.azure;

import com.azure.identity.ChainedTokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import javax.inject.Singleton;

/**
 * This class retrieves Azure credentials through default provider.
 *
 * <p>See
 * https://docs.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable
 * for more details.
 */
@Singleton
public final class Credentials {
  private ChainedTokenCredential defaultAzureCredential;

  public ChainedTokenCredential getCredentials() {
    if (defaultAzureCredential == null) {
      defaultAzureCredential = new DefaultAzureCredentialBuilder().build();
    }
    return defaultAzureCredential;
  }
}
