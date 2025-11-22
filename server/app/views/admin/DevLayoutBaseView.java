package views.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import controllers.AssetsFinder;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http;
import services.ViteService;
import services.settings.SettingsManifest;
import views.admin.shared.AdminCommonHeader;

/**
 * {@link DevLayoutBaseView} is used to render the supplied Thymeleaf page template. This view is
 * tied to layout template {@link LayoutTemplate#ADMIN_LAYOUT}.
 *
 * @param <TModel> A class or record that implements {@link BaseViewModel}
 */
public abstract class DevLayoutBaseView<TModel extends BaseViewModel> extends BaseView<TModel> {
  private final ViteService viteService;
  protected final AssetsFinder assetsFinder;

  public DevLayoutBaseView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest,
      ViteService viteService,
      AssetsFinder assetsFinder) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest);
    this.viteService = checkNotNull(viteService);
    this.assetsFinder = checkNotNull(assetsFinder);
  }

  /** Override to set the active page for top header navigation. */
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.NULL_PAGE;
  }

  @Override
  protected void customizeContext(
      Http.Request request, ThymeleafModule.PlayThymeleafContext context) {

    context.setVariable(
        "adminCommonHeader",
        AdminCommonHeader.builder()
            .activeNavPage(activeNavigationPage())
            .isOnlyProgramAdmin(false)
            .isApiBridgeEnabled(settingsManifest.getApiBridgeEnabled(request))
            .build());
  }

  @Override
  protected final String layoutTemplate() {
    return LayoutTemplate.ADMIN_LAYOUT;
  }

  @Override
  protected final ImmutableList<String> getSiteStylesheets() {
    return ImmutableList.<String>builder().add(viteService.getUswdsStylesheet()).build();
  }

  @Override
  protected final ImmutableList<ScriptElementSettings> getSiteHeadScripts() {
    return ImmutableList.<ScriptElementSettings>builder()
        .add(ScriptElementSettings.builder().src(viteService.getAdminJsBundle()).build())
        .add(ScriptElementSettings.builder().src(viteService.getUswdsJsInit()).build())
        .build();
  }

  @Override
  protected final ImmutableList<ScriptElementSettings> getSiteBodyScripts() {
    return ImmutableList.<ScriptElementSettings>builder()
        .add(ScriptElementSettings.builder().src(viteService.getUswdsJsBundle()).build())
        .build();
  }
}
