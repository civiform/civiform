package services.azure;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;

/**
 * This class retrieves Azure credentials through default provider.
 *
 * <p>See
 * https://docs.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable
 * for more details.
 */
public class Credentials {
    
    private static final DefaultAzureCredential defaultAzureCredential = new DefaultAzureCredentialBuilder()
        .build();

    public DefaultAzureCredential getCredentials() {
        return defaultAzureCredential.getCredentials();
    }    
}
