package mapping.admin.docs;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import views.admin.docs.SchemaPageViewModel;

public final class SchemaPageMapperTest {

  private SchemaPageMapper mapper;

  @Before
  public void setup() {
    mapper = new SchemaPageMapper();
  }

  @Test
  public void map_setsProgramSlug() {
    SchemaPageViewModel result =
        mapper.map(
            "test-slug",
            Optional.empty(),
            Optional.empty(),
            "http://api",
            ImmutableSet.of("test-slug"));

    assertThat(result.getProgramSlug()).isEqualTo("test-slug");
  }

  @Test
  public void map_defaultsStageToActive() {
    SchemaPageViewModel result =
        mapper.map("slug", Optional.empty(), Optional.empty(), "http://api", ImmutableSet.of());

    assertThat(result.getStage()).isEqualTo("active");
  }

  @Test
  public void map_explicitStage_usesProvided() {
    SchemaPageViewModel result =
        mapper.map("slug", Optional.of("draft"), Optional.empty(), "http://api", ImmutableSet.of());

    assertThat(result.getStage()).isEqualTo("draft");
  }

  @Test
  public void map_defaultsOpenApiVersion() {
    SchemaPageViewModel result =
        mapper.map("slug", Optional.empty(), Optional.empty(), "http://api", ImmutableSet.of());

    assertThat(result.getOpenApiVersion()).isNotEmpty();
  }

  @Test
  public void map_setsApiUrl() {
    SchemaPageViewModel result =
        mapper.map("slug", Optional.empty(), Optional.empty(), "http://api/v1", ImmutableSet.of());

    assertThat(result.getApiUrl()).isEqualTo("http://api/v1");
  }

  @Test
  public void map_buildsProgramSlugOptions() {
    SchemaPageViewModel result =
        mapper.map(
            "alpha",
            Optional.empty(),
            Optional.empty(),
            "http://api",
            ImmutableSet.of("alpha", "beta", "gamma"));

    assertThat(result.getProgramSlugs()).hasSize(3);
    assertThat(result.getProgramSlugs().get(0).getSlug()).isEqualTo("alpha");
    assertThat(result.getProgramSlugs().get(0).isSelected()).isTrue();
    assertThat(result.getProgramSlugs().get(1).isSelected()).isFalse();
  }

  @Test
  public void map_buildsStageOptions() {
    SchemaPageViewModel result =
        mapper.map("slug", Optional.of("draft"), Optional.empty(), "http://api", ImmutableSet.of());

    assertThat(result.getStageOptions()).hasSize(2);
    assertThat(result.getStageOptions().stream().filter(o -> o.isSelected()).count()).isEqualTo(1);
    assertThat(
            result.getStageOptions().stream()
                .filter(o -> o.isSelected())
                .findFirst()
                .get()
                .getValue())
        .isEqualTo("draft");
  }

  @Test
  public void map_buildsOpenApiVersionOptions() {
    SchemaPageViewModel result =
        mapper.map("slug", Optional.empty(), Optional.empty(), "http://api", ImmutableSet.of());

    assertThat(result.getOpenApiVersionOptions()).isNotEmpty();
  }

  @Test
  public void map_setsUrls() {
    SchemaPageViewModel result =
        mapper.map("slug", Optional.empty(), Optional.empty(), "http://api", ImmutableSet.of());

    assertThat(result.getFormActionUrl()).isNotEmpty();
    assertThat(result.getApiDocsTabUrl()).isNotEmpty();
    assertThat(result.getSchemaTabUrl()).isNotEmpty();
  }
}
