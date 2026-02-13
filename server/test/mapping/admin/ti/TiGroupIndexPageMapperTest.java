package mapping.admin.ti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import views.admin.ti.TiGroupIndexPageViewModel;

public final class TiGroupIndexPageMapperTest {

  private TiGroupIndexPageMapper mapper;
  private Http.Request mockRequest;
  private Http.Flash mockFlash;

  @Before
  public void setup() {
    mapper = new TiGroupIndexPageMapper();
    mockRequest = mock(Http.Request.class);
    mockFlash = mock(Http.Flash.class);
    when(mockRequest.flash()).thenReturn(mockFlash);
    when(mockFlash.get("providedName")).thenReturn(Optional.empty());
    when(mockFlash.get("providedDescription")).thenReturn(Optional.empty());
    when(mockFlash.get("error")).thenReturn(Optional.empty());
  }

  @Test
  public void map_emptyGroups_returnsEmptyList() {
    TiGroupIndexPageViewModel result = mapper.map(List.of(), mockRequest);

    assertThat(result.getGroups()).isEmpty();
    assertThat(result.getCreateActionUrl()).isNotEmpty();
  }

  @Test
  public void map_setsFlashValues() {
    when(mockFlash.get("providedName")).thenReturn(Optional.of("Test TI"));
    when(mockFlash.get("providedDescription")).thenReturn(Optional.of("A description"));

    TiGroupIndexPageViewModel result = mapper.map(List.of(), mockRequest);

    assertThat(result.getProvidedName()).isEqualTo("Test TI");
    assertThat(result.getProvidedDescription()).isEqualTo("A description");
  }

  @Test
  public void map_setsErrorMessage() {
    when(mockFlash.get("error")).thenReturn(Optional.of("Something went wrong"));

    TiGroupIndexPageViewModel result = mapper.map(List.of(), mockRequest);

    assertThat(result.getErrorMessage()).contains("Something went wrong");
  }

  @Test
  public void map_noError_emptyErrorMessage() {
    TiGroupIndexPageViewModel result = mapper.map(List.of(), mockRequest);

    assertThat(result.getErrorMessage()).isEmpty();
  }

  @Test
  public void map_withGroups_buildsGroupRows() {
    TrustedIntermediaryGroupModel tiGroup = mock(TrustedIntermediaryGroupModel.class);
    tiGroup.id = 42L;
    when(tiGroup.getName()).thenReturn("Group A");
    when(tiGroup.getDescription()).thenReturn("Group A desc");
    when(tiGroup.getTrustedIntermediaries()).thenReturn(ImmutableList.of());
    when(tiGroup.getManagedAccountsCount()).thenReturn(5);

    TiGroupIndexPageViewModel result = mapper.map(List.of(tiGroup), mockRequest);

    assertThat(result.getGroups()).hasSize(1);
    TiGroupIndexPageViewModel.TiGroupRow row = result.getGroups().get(0);
    assertThat(row.getId()).isEqualTo(42L);
    assertThat(row.getName()).isEqualTo("Group A");
    assertThat(row.getDescription()).isEqualTo("Group A desc");
    assertThat(row.getMemberCount()).isEqualTo(0);
    assertThat(row.getClientCount()).isEqualTo(5);
    assertThat(row.getEditUrl()).isNotEmpty();
    assertThat(row.getDeleteActionUrl()).isNotEmpty();
  }
}
