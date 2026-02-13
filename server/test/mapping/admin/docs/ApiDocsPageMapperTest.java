package mapping.admin.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import repository.ExportServiceRepository;
import services.export.ProgramJsonSampler;
import views.admin.docs.ApiDocsPageViewModel;

public final class ApiDocsPageMapperTest {

  private ApiDocsPageMapper mapper;
  private Http.Request mockRequest;
  private ProgramJsonSampler mockJsonSampler;
  private ExportServiceRepository mockExportRepo;

  @Before
  public void setup() {
    mapper = new ApiDocsPageMapper();
    mockRequest = mock(Http.Request.class);
    mockJsonSampler = mock(ProgramJsonSampler.class);
    mockExportRepo = mock(ExportServiceRepository.class);
  }

  @Test
  public void map_noProgramDefinition_setsBasicFields() {
    ApiDocsPageViewModel result =
        mapper.map(
            mockRequest,
            "test-slug",
            Optional.empty(),
            ImmutableSet.of("test-slug", "other-slug"),
            true,
            mockJsonSampler,
            mockExportRepo);

    assertThat(result.getSelectedProgramSlug()).isEqualTo("test-slug");
    assertThat(result.isActiveVersion()).isTrue();
    assertThat(result.isProgramFound()).isFalse();
    assertThat(result.getQuestions()).isEmpty();
    assertThat(result.getJsonPreview()).isEmpty();
  }

  @Test
  public void map_buildsProgramSlugOptions() {
    ApiDocsPageViewModel result =
        mapper.map(
            mockRequest,
            "beta",
            Optional.empty(),
            ImmutableSet.of("alpha", "beta"),
            true,
            mockJsonSampler,
            mockExportRepo);

    assertThat(result.getProgramSlugs()).hasSize(2);
    assertThat(result.getProgramSlugs().get(0).getSlug()).isEqualTo("alpha");
    assertThat(result.getProgramSlugs().get(0).isSelected()).isFalse();
    assertThat(result.getProgramSlugs().get(1).getSlug()).isEqualTo("beta");
    assertThat(result.getProgramSlugs().get(1).isSelected()).isTrue();
  }

  @Test
  public void map_setsTabUrls() {
    ApiDocsPageViewModel result =
        mapper.map(
            mockRequest,
            "",
            Optional.empty(),
            ImmutableSet.of(),
            true,
            mockJsonSampler,
            mockExportRepo);

    assertThat(result.getApiDocsTabUrl()).isNotEmpty();
    assertThat(result.getSchemaTabUrl()).isNotEmpty();
  }

  @Test
  public void map_inactiveVersion_setsFlag() {
    ApiDocsPageViewModel result =
        mapper.map(
            mockRequest,
            "",
            Optional.empty(),
            ImmutableSet.of(),
            false,
            mockJsonSampler,
            mockExportRepo);

    assertThat(result.isActiveVersion()).isFalse();
  }
}
