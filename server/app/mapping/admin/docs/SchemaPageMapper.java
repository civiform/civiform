package mapping.admin.docs;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import models.LifecycleStage;
import services.openapi.OpenApiVersion;
import views.admin.docs.SchemaPageViewModel;
import views.admin.docs.SchemaPageViewModel.ProgramSlugOption;
import views.admin.docs.SchemaPageViewModel.SelectOption;

/** Maps schema page data to the SchemaPageViewModel. */
public final class SchemaPageMapper {

  public SchemaPageViewModel map(
      String programSlug,
      Optional<String> stage,
      Optional<String> openApiVersion,
      String apiUrl,
      ImmutableSet<String> allProgramSlugs) {

    String resolvedStage = stage.orElse(LifecycleStage.ACTIVE.getValue());
    String resolvedOpenApiVersion = openApiVersion.orElse(OpenApiVersion.OPENAPI_V3_0.toString());

    List<ProgramSlugOption> slugOptions =
        allProgramSlugs.stream()
            .sorted()
            .map(
                slug ->
                    ProgramSlugOption.builder()
                        .slug(slug)
                        .selected(slug.equalsIgnoreCase(programSlug))
                        .build())
            .collect(Collectors.toList());

    List<SelectOption> stageOptions =
        List.of(
            SelectOption.builder()
                .label("Active")
                .value("active")
                .selected("active".equalsIgnoreCase(resolvedStage))
                .build(),
            SelectOption.builder()
                .label("Draft")
                .value("draft")
                .selected("draft".equalsIgnoreCase(resolvedStage))
                .build());

    List<SelectOption> openApiVersionOptions =
        Arrays.stream(OpenApiVersion.values())
            .sorted(Comparator.comparing(OpenApiVersion::toString))
            .map(
                v ->
                    SelectOption.builder()
                        .label(v.getVersionNumber())
                        .value(v.toString())
                        .selected(v.toString().equalsIgnoreCase(resolvedOpenApiVersion))
                        .build())
            .collect(Collectors.toList());

    return SchemaPageViewModel.builder()
        .programSlug(programSlug)
        .stage(resolvedStage)
        .openApiVersion(resolvedOpenApiVersion)
        .programSlugs(slugOptions)
        .stageOptions(stageOptions)
        .openApiVersionOptions(openApiVersionOptions)
        .apiUrl(apiUrl)
        .build();
  }
}
