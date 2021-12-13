package services.cloud.azure;

import com.azure.identity.ChainedTokenCredential;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import javax.inject.Inject;
import org.mockito.Mockito;
import play.Environment;

/**
 * This class retrieves Azure credentials through default provider.
 *
 * <p>See
 * https://docs.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable
 * for more details.
 */
public class Credentials {

  private ChainedTokenCredential defaultAzureCredential;


  @Inject
  public Credentials(Environment environment) {
    if (environment.isProd()){
      defaultAzureCredential = new DefaultAzureCredentialBuilder().build();
    } else {
      defaultAzureCredential = Mockito.mock(ChainedTokenCredential.class);
    }
  }
  public ChainedTokenCredential getCredentials() {
    return defaultAzureCredential;
  }


}
