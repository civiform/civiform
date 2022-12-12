package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.input;

import com.google.common.collect.ImmutableList;
import featureflags.FeatureFlags;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.ScriptTag;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import services.cloud.StorageUploadRequest;
import services.cloud.azure.BlobStorageUploadRequest;

public final class AzureFileUploadViewStrategy extends FileUploadViewStrategy {

  private final ViewUtils viewUtils;
  private final FeatureFlags featureFlags;

  @Inject
  AzureFileUploadViewStrategy(ViewUtils viewUtils, FeatureFlags featureFlags) {
    this.viewUtils = checkNotNull(viewUtils);
    this.featureFlags = checkNotNull(featureFlags);
  }

  @Override
  protected ImmutableList<InputTag> fileUploadFields(
      Optional<StorageUploadRequest> request,
      String fileInputId,
      ImmutableList<String> ariaDescribedByIds,
      boolean hasErrors) {
    if (request.isEmpty()) {
      return ImmutableList.of();
    }
    BlobStorageUploadRequest signedRequest = castStorageRequest(request.get());
    ImmutableList.Builder<InputTag> builder = ImmutableList.builder();
    builder.add(
        input().withType("hidden").withName("fileName").withValue(signedRequest.fileName()),
        input().withType("hidden").withName("sasToken").withValue(signedRequest.sasToken()),
        input().withType("hidden").withName("blobUrl").withValue(signedRequest.blobUrl()),
        input()
            .withType("hidden")
            .withName("containerName")
            .withValue(signedRequest.containerName()),
        input().withType("hidden").withName("accountName").withValue(signedRequest.accountName()),
        input()
            .withType("hidden")
            .withName("successActionRedirect")
            .withValue(signedRequest.successActionRedirect()),
        input()
            .withId(fileInputId)
            .condAttr(hasErrors, "aria-invalid", "true")
            .condAttr(
                !ariaDescribedByIds.isEmpty(),
                "aria-describedby",
                StringUtils.join(ariaDescribedByIds, " "))
            .withType("file")
            .withName("file")
            .withClass("hidden")
            .withAccept(MIME_TYPES_IMAGES_AND_PDF));
    return builder.build();
  }

  private BlobStorageUploadRequest castStorageRequest(StorageUploadRequest request) {
    if (!(request instanceof BlobStorageUploadRequest)) {
      throw new RuntimeException(
          "Tried to upload a file to Azure Blob storage using incorrect request type");
    }
    return (BlobStorageUploadRequest) request;
  }

  @Override
  protected ImmutableList<ScriptTag> extraScriptTags() {
    ImmutableList.Builder<ScriptTag> builder = ImmutableList.builder();
    builder.add(viewUtils.makeAzureBlobStoreScriptTag());
    if (!featureFlags.isJsBundlingEnabled()) {
      builder.add(viewUtils.makeLocalJsTag("azure_upload"));
      builder.add(viewUtils.makeLocalJsTag("azure_delete"));
    }
    return builder.build();
  }

  @Override
  protected String getUploadFormClass() {
    return "azure-upload";
  }
}
