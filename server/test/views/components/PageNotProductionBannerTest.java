package views.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import services.DeploymentType;
import services.MessageKey;
import services.settings.SettingsManifest;

public class PageNotProductionBannerTest {
  SettingsManifest settingsManifest;
  Http.Request request;
  Messages messages;

  @Before
  public void setUp() {
    settingsManifest = mock(SettingsManifest.class);
    request = mock(Http.Request.class);

    // Setup messages
    Langs langs = new Langs(new play.api.i18n.DefaultLangs());
    Map<String, String> messagesMap = new java.util.HashMap<>();
    messagesMap.put(MessageKey.NOT_FOR_PRODUCTION_BANNER_LINE_1.getKeyName(), "line1");
    messagesMap.put(MessageKey.NOT_FOR_PRODUCTION_BANNER_LINE_2.getKeyName(), "line2 {0}");

    Map<String, Map<String, String>> langMap =
        Collections.singletonMap(Lang.defaultLang().code(), messagesMap);
    MessagesApi messagesApi = play.test.Helpers.stubMessagesApi(langMap, langs);
    messages = messagesApi.preferred(langs.availables());
  }

  @Test
  public void devEnvironment_withEmptyProductionUrlSetting_returnsEmpty() {
    DeploymentType deploymentType = new DeploymentType(/* isDev */ true, /* isStaging */ false);
    assertEnvironmentWithEmptyProductionUrlSettingReturnsEmpty(deploymentType);
  }

  @Test
  public void stagingEnvironment_withEmptyProductionUrlSetting_returnsEmpty() {
    DeploymentType deploymentType = new DeploymentType(/* isDev */ false, /* isStaging */ true);
    assertEnvironmentWithEmptyProductionUrlSettingReturnsEmpty(deploymentType);
  }

  @Test
  public void productionEnvironment_withEmptyProductionUrlSetting_returnsEmpty() {
    DeploymentType deploymentType = new DeploymentType(/* isDev */ false, /* isStaging */ false);
    assertEnvironmentWithEmptyProductionUrlSettingReturnsEmpty(deploymentType);
  }

  private void assertEnvironmentWithEmptyProductionUrlSettingReturnsEmpty(
      DeploymentType deploymentType) {
    when(settingsManifest.getCivicEntityProductionUrl(request)).thenReturn(Optional.empty());

    PageNotProductionBanner component =
        new PageNotProductionBanner(settingsManifest, deploymentType);

    var actual = component.render(request, messages);
    assertThat(actual).isEqualTo(Optional.empty());
  }

  @Test
  public void devEnvironment_withProductionUrlSetting_returnsDivTag() {
    DeploymentType deploymentType = new DeploymentType(/* isDev */ true, /* isStaging */ false);
    assertEnvironmentWithProductionUrlSettingReturnsDivTag(deploymentType);
  }

  @Test
  public void stagingEnvironment_withProductionUrlSetting_returnsDivTag() {
    DeploymentType deploymentType = new DeploymentType(/* isDev */ false, /* isStaging */ true);
    assertEnvironmentWithProductionUrlSettingReturnsDivTag(deploymentType);
  }

  public void assertEnvironmentWithProductionUrlSettingReturnsDivTag(
      DeploymentType deploymentType) {
    String productionUrl = "https://civiform.example.com";
    when(settingsManifest.getCivicEntityProductionUrl(request))
        .thenReturn(Optional.of(productionUrl));
    when(settingsManifest.getWhitelabelCivicEntityShortName(request))
        .thenReturn(Optional.of("shortname"));

    PageNotProductionBanner component =
        new PageNotProductionBanner(settingsManifest, deploymentType);

    var actual = component.render(request, messages);
    assertThat(actual).isNotEqualTo(Optional.empty());

    String actualString = actual.toString();
    assertThat(actualString).contains("h4");
    assertThat(actualString).contains("line1");
    assertThat(actualString).contains("href");
    assertThat(actualString).contains("line2");
    assertThat(actualString).contains(productionUrl);
  }

  @Test
  public void productionEnvironment_withProductionUrlSetting_returnsEmpty() {
    when(settingsManifest.getCivicEntityProductionUrl(request))
        .thenReturn(Optional.of("https://civiform.example.com"));
    DeploymentType deploymentType = new DeploymentType(/* isDev */ false, /* isStaging */ false);

    PageNotProductionBanner component =
        new PageNotProductionBanner(settingsManifest, deploymentType);

    var actual = component.render(request, messages);
    assertThat(actual).isEqualTo(Optional.empty());
  }
}
