package actions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.Test;

public final class RouteExtractorTest {
  @Test
  public void successfully_extracts_route_parameters() {
    String routePattern = "/programs/$programId<[^/]+>/blocks/$blockId<[^/]+>/edit";
    String path = "/programs/1/blocks/2/edit";
    RouteExtractor routeExtractor = new RouteExtractor(routePattern, path);

    assertThat(routeExtractor.size()).isEqualTo(2);
    assertThat(routeExtractor.getParamLongValue("programId")).isEqualTo(1);
    assertThat(routeExtractor.getParamLongValue("blockId")).isEqualTo(2);
  }

  @Test
  public void has_no_route_parameters_configured() {
    String routePattern = "/programs/path/edit";
    String path = "/programs/path/edit";
    RouteExtractor routeExtractor = new RouteExtractor(routePattern, path);

    assertThat(routeExtractor.size()).isEqualTo(0);
  }

  @Test
  public void asking_for_route_parameter_that_does_not_exist_throws() {
    String routePattern = "/programs/$programId<[^/]+>/blocks/$blockId<[^/]+>/edit";
    String path = "/programs/1/blocks/2/edit";
    RouteExtractor routeExtractor = new RouteExtractor(routePattern, path);

    assertThatThrownBy(() -> routeExtractor.getParamLongValue("unexpectedId"));
  }

  @Test
  public void asking_for_route_parameter_that_does_not_exist_returns_empty() {
    String routePattern = "/programs/$programId<[^/]+>/blocks/$blockId<[^/]+>/edit";
    String path = "/programs/1/blocks/2/edit";
    RouteExtractor routeExtractor = new RouteExtractor(routePattern, path);

    assertThat(routeExtractor.getParamOptionalLongValue("unexpectedId"))
        .isEqualTo(Optional.empty());
  }
}
