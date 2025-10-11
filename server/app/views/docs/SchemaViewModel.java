package views.docs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import lombok.Builder;
import services.openapi.OpenApiVersion;
import views.admin.BaseViewModel;

@Builder
public record SchemaViewModel(
    String url,
    String formChangeUrl,
    String programSlug,
    Optional<String> stage,
    Optional<String> openApiVersion,
    ImmutableSet<String> allNonExternalProgramSlugs)
    implements BaseViewModel {
  public ImmutableList<OpenApiVersion> getOpenApiVersions() {
    return Arrays.stream(OpenApiVersion.values())
        .sorted(Comparator.comparing(OpenApiVersion::toString))
        .collect(ImmutableList.toImmutableList());
  }
}
