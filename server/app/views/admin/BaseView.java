package views.admin;

import static autovalue.shaded.com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.AssetsFinder;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http;
import services.settings.SettingsManifest;
import views.CspUtil;
import views.admin.shared.LayoutParams;
import views.admin.shared.TemplateGlobals;
import views.html.helper.CSRF;

/**
 * Base view class containing the structure and bare essentials to render the supplied Thymeleaf
 * template.
 *
 * @param <TModel> A class or record that implements {@link BaseViewModel}
 */
public abstract class BaseView<TModel extends BaseViewModel> {
  private static final String FAVICON_DATAURI =
      "data:image/x-icon;base64,AAABAAEAEBAAAAEAIACcAQAAFgAAAIlQTkcNChoKAAAADUlIRFIAAAAQAAAAEAgGAAAAH/P/YQAAAWNJREFUOE9j/A8EDBQAxsFjwKuPPxgK5h1n2HnhCdhDDtqSDJNSrBikhbgYDIrWMVx78gHFo7vrPRnsgWrgXvBp28lw5s4bhuZIYwY+LjaGltXnGXg4WRmOt/uBDZAT5WEo9tOFG6KvIMwgwM0GMeDui88MmnmrGWZn2TLEO6iCFd169pHh/P23DCGWigzGJesZzNXEGGZm2GAEN9gAkLN923YxXOoPZtCQ5sdQBHKBiYoowxSgl2CAnZWZgZGRAeKC3RefMni37mS40BfEoCUjgNUA9DC4NSWMQUGMB2LAozdfGVSyVjJMT7dmSHZWBxtw+dF7hjXH7jGUB+ozWFVuYlCS4GMoD9CDG26gKMwAdgUsHUT07WPYf+UZQ1OECYMQDztD06pzDGwsTAynuwIYjAiFAcjYD19/MRTNP8Gw+cwjhj///oGjcTLQzzLC3OBYwBuIFKRkhBfINWQQ5QVyvQAAuEmo0TDmRP4AAAAASUVORK5CYII=";
  private final TemplateEngine templateEngine;
  private final ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory;
  private final SettingsManifest settingsManifest;
  protected final AssetsFinder assetsFinder;
  protected final ProfileUtils profileUtils;

  public BaseView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest,
      AssetsFinder assetsFinder,
      ProfileUtils profileUtils) {
    this.templateEngine = checkNotNull(templateEngine);
    this.playThymeleafContextFactory = checkNotNull(playThymeleafContextFactory);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.assetsFinder = checkNotNull(assetsFinder);
    this.profileUtils = checkNotNull(profileUtils);
  }

  /** Page title text */
  protected String pageTitle() {
    return "";
  }

  /** Override to set the active page for top header navigation. */
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.NULL_PAGE;
  }

  /**
   * Primary page thymeleaf template file to render. This is a relative path from the {@link views}
   * directory.
   */
  protected abstract String pageTemplate();

  /**
   * Override to specify the Thymeleaf template page layout file. This is a relative path from the
   * {@link views} directory.
   *
   * <p>If not set the page will render without a full layout.
   *
   * <p>HTMX partials would be a reason not to set a layout.
   */
  protected String layoutTemplate() {
    return null;
  }

  /**
   * Override if needing to add additional configuration to the Thymeleaf context.
   *
   * <p>Most page level values should be added to your {@code TModel} instead of being added as
   * separate context variables.
   */
  protected void customizeContext(
      Http.Request request, ThymeleafModule.PlayThymeleafContext context) {
    // no-op
  }

  /**
   * Entrypoint for rendering a Thymeleaf template
   *
   * @param request Current Play HTTP request
   * @param model Model class implementing the {@link BaseViewModel} interface
   * @return Rendered template
   */
  public final String render(Http.Request request, TModel model) {
    ThymeleafModule.PlayThymeleafContext context = playThymeleafContextFactory.create(request);

    // Set layout specific values. These should not need to be used outside of
    // layout templates.
    context.setVariable(
        "layoutParams",
        LayoutParams.builder()
            .pageTemplate(pageTemplate())
            .civiformImageTag(settingsManifest.getCiviformImageTag().orElse("UNKNOWN"))
            .addNoIndexMetaTag(settingsManifest.getStagingAddNoindexMetaTag())
            .favicon(FAVICON_DATAURI)
            .measurementId(settingsManifest.getMeasurementId())
            .stylesheets(getStylesheets())
            .headScripts(getHeadScripts())
            .bodyScripts(getBodyScripts())
            .build());

    // Set global values that are available to all page templates
    context.setVariable(
        "templateGlobals",
        TemplateGlobals.builder()
            .pageTitle(pageTitle())
            .cspNonce(CspUtil.getNonce(request))
            .csrfToken(CSRF.getToken(request.asScala()).value())
            .build());

    // This gives the Thymeleaf template a reference to this view class. Methods can be added
    // to the view to aid in custom formatting. Ideally keep these to a minimum and prefer
    // using Thymeleaf directly.
    context.setVariable("view", this);

    // This gives the Thymeleaf template the typed data model for the page template. Methods
    // can be added to the model to aid in more complex logic needs of the template.
    context.setVariable("model", model);

    // Allow each view to add custom items to the Thymeleaf content. In most cases prefer not
    // adding extra variables to the context. Instead prefer adding data fields/methods to the model
    // and custom formatting methods to the view.
    customizeContext(request, context);

    // If a layout template is set render the page template with the layout
    if (layoutTemplate() != null) {
      return templateEngine.process(layoutTemplate(), context);
    }

    // Render the page template without a layout
    return templateEngine.process(pageTemplate(), context);
  }

  /**
   * Returns a list of all stylesheet files to be added in the <b>head</b>. This includes site-wide
   * and page specific stylesheets.
   *
   * <p>They will be added to the page in order.
   */
  private ImmutableList<String> getStylesheets() {
    return ImmutableList.<String>builder()
        .addAll(getSiteStylesheets())
        .addAll(getPageStylesheets())
        .build();
  }

  /** Returns a list of site-wide stylesheets */
  protected ImmutableList<String> getSiteStylesheets() {
    return ImmutableList.of();
  }

  /**
   * Returns a list of page specific stylesheet to be added in the <b>head</b>.
   *
   * <p>They will be added to the page in order.
   *
   * <p>Override to add page specific stylesheet files. Use <b>sparringly</b>.
   */
  protected ImmutableList<String> getPageStylesheets() {
    // no-op
    return ImmutableList.of();
  }

  /**
   * Returns a list of all javascript files to be added in the <b>head</b>. This includes site-wide
   * and page specific scripts.
   *
   * <p>They will be added to the page in order.
   */
  private ImmutableList<String> getHeadScripts() {
    return ImmutableList.<String>builder()
        .addAll(getSiteHeadScripts())
        .addAll(getPageHeadScripts())
        .build();
  }

  /**
   * Returns a list of side-wide scripts to be added in the <b>head</b>.
   *
   * <p>They will be added to the page in order.
   *
   * <p>Most script files should be added with {@link #getSiteBodyScripts()} or {@link
   * #getPageBodyScripts()}.
   */
  protected ImmutableList<String> getSiteHeadScripts() {
    return ImmutableList.of();
  }

  /**
   * Returns a list of page specific javascript files to be added in the <b>head</b>.
   *
   * <p>They will be added to the page in order.
   *
   * <p>Override to add page specific javascript files. Use <b>sparringly</b>. Most page specific
   * files should be added with {@link #getPageBodyScripts()}.
   */
  protected ImmutableList<String> getPageHeadScripts() {
    return ImmutableList.of();
  }

  /**
   * Returns a list of all javascript files to be added right before the closing of the <b>body</b>.
   * This includes site-wide and page specific scripts.
   *
   * <p>They will be added to the page in order.
   */
  private ImmutableList<String> getBodyScripts() {
    return ImmutableList.<String>builder()
        .addAll(getSiteBodyScripts())
        .addAll(getPageBodyScripts())
        .build();
  }

  /**
   * Returns a list of side-wide scripts to be added right before the closing of the <b>body</b>.
   *
   * <p>They will be added to the page in order.
   */
  protected ImmutableList<String> getSiteBodyScripts() {
    return ImmutableList.of();
  }

  /**
   * Returns a list of page specific javascript files to be added right before the closing of the
   * <b>body</b>.
   *
   * <p>They will be added to the page in order.
   *
   * <p>Override to add page specific javascript files. Use <b>sparringly</b>.
   */
  protected ImmutableList<String> getPageBodyScripts() {
    return ImmutableList.of();
  }
}
