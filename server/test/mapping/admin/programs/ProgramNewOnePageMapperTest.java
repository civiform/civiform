package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import views.admin.programs.ProgramNewOnePageViewModel;

public final class ProgramNewOnePageMapperTest {

  private ProgramNewOnePageMapper mapper;

  @Before
  public void setup() {
    mapper = new ProgramNewOnePageMapper();
  }

  @Test
  public void map_setsExternalProgramCardsEnabled() {
    ProgramNewOnePageViewModel result =
        mapper.map(ImmutableList.of(), ImmutableList.of(), true, Optional.empty());

    assertThat(result.isExternalProgramCardsEnabled()).isTrue();
  }

  @Test
  public void map_setsFormActionUrl() {
    ProgramNewOnePageViewModel result =
        mapper.map(ImmutableList.of(), ImmutableList.of(), false, Optional.empty());

    assertThat(result.getFormActionUrl()).isNotEmpty();
  }

  @Test
  public void map_setsEmptyErrorMessage() {
    ProgramNewOnePageViewModel result =
        mapper.map(ImmutableList.of(), ImmutableList.of(), false, Optional.empty());

    assertThat(result.getErrorMessage()).isEmpty();
  }

  @Test
  public void map_setsErrorMessage() {
    ProgramNewOnePageViewModel result =
        mapper.map(ImmutableList.of(), ImmutableList.of(), false, Optional.of("some error"));

    assertThat(result.getErrorMessage()).contains("some error");
  }

  @Test
  public void map_setsDefaultNotificationPreference() {
    ProgramNewOnePageViewModel result =
        mapper.map(ImmutableList.of(), ImmutableList.of(), false, Optional.empty());

    assertThat(result.getDefaultNotificationPreferenceValue()).isNotEmpty();
  }
}
