package mapping.admin.apikeys;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import views.admin.apikeys.ApiKeyNewOnePageViewModel;

public final class ApiKeyNewOnePageMapperTest {

  private ApiKeyNewOnePageMapper mapper;

  @Before
  public void setup() {
    mapper = new ApiKeyNewOnePageMapper();
  }

  @Test
  public void map_withNoPrograms_setsHasProgramsFalse() {
    ApiKeyNewOnePageViewModel result = mapper.map(ImmutableSet.of(), Optional.empty());

    assertThat(result.isHasPrograms()).isFalse();
    assertThat(result.getPrograms()).isEmpty();
  }

  @Test
  public void map_withPrograms_setsHasProgramsTrue() {
    ImmutableSet<String> programNames = ImmutableSet.of("Program A", "Program B");

    ApiKeyNewOnePageViewModel result = mapper.map(programNames, Optional.empty());

    assertThat(result.isHasPrograms()).isTrue();
    assertThat(result.getPrograms()).hasSize(2);
  }

  @Test
  public void map_programsSortedAlphabetically() {
    ImmutableSet<String> programNames = ImmutableSet.of("Zebra Program", "Alpha Program");

    ApiKeyNewOnePageViewModel result = mapper.map(programNames, Optional.empty());

    assertThat(result.getPrograms().get(0).name()).isEqualTo("Alpha Program");
    assertThat(result.getPrograms().get(1).name()).isEqualTo("Zebra Program");
  }

  @Test
  public void map_withNoForm_setsDefaultFieldValues() {
    ApiKeyNewOnePageViewModel result = mapper.map(ImmutableSet.of(), Optional.empty());

    assertThat(result.getKeyNameValue()).isEmpty();
    assertThat(result.getExpirationValue()).isEmpty();
    assertThat(result.getSubnetValue()).isEmpty();
    assertThat(result.getKeyNameError()).isEmpty();
    assertThat(result.getExpirationError()).isEmpty();
    assertThat(result.getSubnetError()).isEmpty();
    assertThat(result.getProgramsError()).isEmpty();
  }
}
