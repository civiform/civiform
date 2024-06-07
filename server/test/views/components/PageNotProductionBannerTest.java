package views.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import services.DeploymentType;
import services.settings.SettingsManifest;

public class PageNotProductionBannerTest {
  SettingsManifest settingsManifest;
  Http.Request request;

  @Before
  public void setUp() {
    settingsManifest = mock(SettingsManifest.class);
    request = mock(Http.Request.class);
  }

  @Test
  public void devEnvironment_returnsEmpty() {
    DeploymentType deploymentType = new DeploymentType(/* isDev */ true, /* isStaging */ false);
    PageNotProductionBanner component =
        new PageNotProductionBanner(settingsManifest, deploymentType);

    var actual = component.render(request);
    assertThat(actual).isEqualTo(Optional.empty());
  }

  @Test
  public void productionEnvironment_returnsEmpty() {
    DeploymentType deploymentType = new DeploymentType(/* isDev */ false, /* isStaging */ false);
    PageNotProductionBanner component =
        new PageNotProductionBanner(settingsManifest, deploymentType);

    var actual = component.render(request);
    assertThat(actual).isEqualTo(Optional.empty());
  }

  @Test
  public void stagingEnvironment_withEmptyProductionUrlSetting_returnsEmpty() {
    when(settingsManifest.getCivicEntityProductionUrl(request)).thenReturn(Optional.empty());

    DeploymentType deploymentType = new DeploymentType(/* isDev */ false, /* isStaging */ true);
    PageNotProductionBanner component =
        new PageNotProductionBanner(settingsManifest, deploymentType);

    var actual = component.render(request);
    assertThat(actual).isEqualTo(Optional.empty());
  }

  @Test
  public void stagingEnvironment_withProductionUrlSetting_returnsDivTag() {
    String productionUrl = "https://civiform.example.com";
    when(settingsManifest.getCivicEntityProductionUrl(request))
        .thenReturn(Optional.of(productionUrl));

    DeploymentType deploymentType = new DeploymentType(/* isDev */ false, /* isStaging */ true);
    PageNotProductionBanner component =
        new PageNotProductionBanner(settingsManifest, deploymentType);

    var actual = component.render(request);
    assertThat(actual).isNotEqualTo(Optional.empty());
    assertThat(actual.toString()).contains("This site is for testing purposes only");
    assertThat(actual.toString()).contains(productionUrl);
  }
}
