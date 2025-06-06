package views.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.iff;
import static j2html.TagCreator.iffElse;
import static j2html.TagCreator.li;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;
import static j2html.TagCreator.ul;
import static views.BaseHtmlView.asRedirectElement;
import static views.ViewUtils.makeSvgTextButton;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import controllers.AssetsFinder;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.NavTag;
import j2html.tags.specialized.UlTag;
import java.util.Optional;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.twirl.api.Content;
import services.DeploymentType;
import services.TranslationLocales;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.BaseHtmlLayout;
import views.HtmlBundle;
import views.JsBundle;
import views.ViewUtils;
import views.components.Icons;
import views.components.SvgTag;
import views.components.TextFormatter;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Contains methods rendering common compoments used across admin pages. */
public final class AdminLayout extends BaseHtmlLayout {

  public enum AdminType {
    CIVI_FORM_ADMIN,
    PROGRAM_ADMIN
  }

  public enum NavPage {
    PROGRAMS,
    QUESTIONS,
    INTERMEDIARIES,
    REPORTING,
    API_KEYS,
    SETTINGS,
    API_DOCS,
  }

  private final NavPage activeNavPage;
  private final TranslationLocales translationLocales;
  private final MessagesApi messagesApi;

  private AdminType primaryAdminType = AdminType.CIVI_FORM_ADMIN;

  AdminLayout(
      ViewUtils viewUtils,
      NavPage activeNavPage,
      SettingsManifest settingsManifest,
      TranslationLocales translationLocales,
      DeploymentType deploymentType,
      AssetsFinder assetsFinder,
      MessagesApi messagesApi) {
    super(viewUtils, settingsManifest, deploymentType, assetsFinder);
    this.activeNavPage = activeNavPage;
    this.translationLocales = checkNotNull(translationLocales);
    this.messagesApi = checkNotNull(messagesApi);
  }

  /**
   * Sets this layout's admin type based on the CiviFormProfile, used to determine which navigation
   * to include.
   */
  public AdminLayout setAdminType(CiviFormProfile profile) {
    primaryAdminType =
        profile.isOnlyProgramAdmin() ? AdminType.PROGRAM_ADMIN : AdminType.CIVI_FORM_ADMIN;
    return this;
  }

  public Content renderCentered(HtmlBundle bundle) {
    return render(bundle, /* isCentered= */ true);
  }

  @Override
  public Content render(HtmlBundle bundle) {
    return render(bundle, /* isCentered= */ false);
  }

  private Content render(HtmlBundle bundle, boolean isCentered) {
    bundle.addMainStyles(
        AdminStyles.MAIN, isCentered ? AdminStyles.MAIN_CENTERED : AdminStyles.MAIN_FULL);
    bundle.addBodyStyles(AdminStyles.BODY);
    addSessionTimeoutModals(bundle, messagesApi.preferred(bundle.getRequest()));

    return super.render(bundle);
  }

  @Override
  protected String getTitleSuffix() {
    return "CiviForm admin console";
  }

  @Override
  public HtmlBundle getBundle(HtmlBundle bundle) {
    return super.getBundle(bundle)
        .addHeaderContent(renderNavBar(bundle.getRequest()))
        .setJsBundle(JsBundle.ADMIN);
  }

  /**
   * Creates a button that will redirect to the translations management page. Returns an empty
   * optional if there are no locales to translate to.
   */
  public Optional<ButtonTag> createManageTranslationsButton(
      String programAdminName, Optional<String> buttonId, String buttonStyles) {
    if (translationLocales.translatableLocales().isEmpty()) {
      return Optional.empty();
    }
    String linkDestination =
        routes.AdminProgramTranslationsController.redirectToFirstLocale(programAdminName).url();
    ButtonTag button =
        makeSvgTextButton("Manage translations", Icons.LANGUAGE).withClass(buttonStyles);
    buttonId.ifPresent(button::withId);
    return Optional.of(asRedirectElement(button, linkDestination));
  }

  /**
   * Creates a button that will take a user back to the previous page.
   *
   * @param goBackUrl the URL to return to
   * @param goBackText the text to display on the button
   * @return a DivTag containing the button
   */
  public DivTag createGoBackButton(String goBackUrl, String goBackText) {
    return div(a(Icons.svg(Icons.ARROW_LEFT).withClasses("w-5", "h-5", "mr-2"))
            .with(text(goBackText))
            .withHref(goBackUrl)
            .withClasses("text-blue-600", "hover:text-blue-500", "inline-flex", "items-center"))
        .withClasses("grid-row");
  }

  private NavTag renderNavBar(Http.RequestHeader request) {
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();

    DivTag headerIcon =
        div(
            a().withHref(controllers.routes.HomeController.index().url())
                .with(
                    div(span("C"), span("F").withClasses("font-thin"))
                        .withClasses(AdminStyles.ADMIN_NAV_BAR_LOGO)));
    DivTag headerTitle =
        div()
            .withClasses("font-normal", "text-xl", "inline", "pl-10", "py-0", "mr-4")
            .with(span("Civi"), span("Form").withClasses("font-thin"));

    NavTag navBar =
        nav()
            .condWith(
                !settingsManifest.getShowNotProductionBannerEnabled(request),
                getGovBanner(Optional.empty()))
            .withClasses(AdminStyles.NAV_STYLES);

    DivTag adminHeader =
        div().with(headerIcon, headerTitle).withClasses(AdminStyles.INNER_NAV_STYLES);

    String questionLink =
        controllers.admin.routes.AdminQuestionController.index(Optional.empty()).url();
    String programLink = controllers.admin.routes.AdminProgramController.index().url();
    String programAdminProgramsLink = controllers.admin.routes.ProgramAdminController.index().url();
    String intermediaryLink = routes.TrustedIntermediaryManagementController.index().url();
    String apiKeysLink = controllers.admin.routes.AdminApiKeysController.index().url();
    String apiDocsLink = controllers.docs.routes.ApiDocsController.index().url();
    String reportingLink = controllers.admin.routes.AdminReportingController.index().url();
    String settingsLink = controllers.admin.routes.AdminSettingsController.index().url();

    String activeNavStyle =
        StyleUtils.joinStyles(
            BaseStyles.TEXT_CIVIFORM_BLUE,
            "font-medium",
            "border-b-2",
            BaseStyles.BORDER_CIVIFORM_BLUE);

    DomContent reportingHeaderLink =
        headerLink(
            "Reporting",
            reportingLink,
            NavPage.REPORTING.equals(activeNavPage) ? activeNavStyle : "");

    ATag programsHeaderLink =
        headerLink(
            "Programs", programLink, NavPage.PROGRAMS.equals(activeNavPage) ? activeNavStyle : "");

    ATag programAdminProgramsHeaderLink =
        headerLink(
            "Programs",
            programAdminProgramsLink,
            NavPage.PROGRAMS.equals(activeNavPage) ? activeNavStyle : "");

    ATag questionsHeaderLink =
        headerLink(
            "Questions",
            questionLink,
            NavPage.QUESTIONS.equals(activeNavPage) ? activeNavStyle : "");

    ATag intermediariesHeaderLink =
        headerLink(
            "Intermediaries",
            intermediaryLink,
            NavPage.INTERMEDIARIES.equals(activeNavPage) ? activeNavStyle : "");

    ATag apiKeysHeaderLink =
        headerLink(
            "API keys", apiKeysLink, NavPage.API_KEYS.equals(activeNavPage) ? activeNavStyle : "");

    ATag apiDocsHeaderLink =
        headerLink(
            "API docs", apiDocsLink, NavPage.API_DOCS.equals(activeNavPage) ? activeNavStyle : "");

    switch (primaryAdminType) {
      case CIVI_FORM_ADMIN:
        {
          adminHeader
              .with(programsHeaderLink)
              .with(questionsHeaderLink)
              .with(intermediariesHeaderLink)
              .with(reportingHeaderLink)
              .with(apiKeysHeaderLink)
              .condWith(
                  getSettingsManifest().getApiGeneratedDocsEnabled(request), apiDocsHeaderLink);
          break;
        }
      case PROGRAM_ADMIN:
        {
          adminHeader
              .with(programAdminProgramsHeaderLink)
              .with(reportingHeaderLink)
              .condWith(
                  getSettingsManifest().getApiGeneratedDocsEnabled(request), apiDocsHeaderLink);
          break;
        }
    }

    adminHeader.with(
        headerLink("Logout", logoutLink, "float-right").withId("logout-button"),
        primaryAdminType.equals(AdminType.CIVI_FORM_ADMIN)
            ? a(Icons.svg(Icons.COG)
                    .withClasses("h-6", "w-6", "opacity-75", StyleUtils.hover("opacity-100")))
                .withHref(settingsLink)
                .withClasses("float-right")
            : null);

    return navBar.with(adminHeader);
  }

  private ATag headerLink(String text, String href, String... styles) {
    return a(text)
        .withHref(href)
        .withClasses(
            "px-3", "opacity-75", StyleUtils.hover("opacity-100"), StyleUtils.joinStyles(styles));
  }

  // maybeBadgeForImport = empty, maybeDuplicateHandlingForImport = empty

  /**
   * Renders a question for import, including info about the question and possibly its
   * duplicate-handling options
   *
   * @param questionDefinition the question definition to render
   * @param badgeForImport a badge indicating if the question is new or a duplicate
   * @param maybeDuplicateHandlingForImport a div tag containing the duplicate handling options, if
   *     this question is a duplicate
   * @return a div tag containing the rendered question
   */
  public static DivTag renderQuestionForImport(
      QuestionDefinition questionDefinition,
      DivTag badgeForImport,
      Optional<FieldsetTag> maybeDuplicateHandlingForImport) {
    return renderQuestion(
        questionDefinition,
        /* malformedQuestionDefinition= */ false,
        /* editButtonsForProgramPage= */ ImmutableList.of(),
        Optional.of(badgeForImport),
        maybeDuplicateHandlingForImport);
  }

  /**
   * Renders a question for a program, including info about the question and possibly some editing
   * controls
   *
   * @param questionDefinition the question definition to render
   * @param malformedQuestionDefinition whether there is an issue with the definition
   * @param editButtonsForProgramPage the div tags containing buttons/icons showing the options to
   *     edit the question
   * @return a div tag containing the rendered question
   */
  public static DivTag renderQuestionForProgramPage(
      QuestionDefinition questionDefinition,
      boolean malformedQuestionDefinition,
      ImmutableList<DomContent> editButtonsForProgramPage) {
    return renderQuestion(
        questionDefinition,
        malformedQuestionDefinition,
        editButtonsForProgramPage,
        /* maybeBadgeForImport= */ Optional.empty(),
        /* maybeDuplicateHandlingForImport= */ Optional.empty());
  }

  /**
   * Renders an individual question, including the description and any toggles or tags that should
   * be shown next to the question in the list of questions.
   */
  private static DivTag renderQuestion(
      QuestionDefinition questionDefinition,
      boolean malformedQuestionDefinition,
      ImmutableList<DomContent> editButtonsForProgramPage,
      Optional<DivTag> maybeBadgeForImport,
      Optional<FieldsetTag> maybeDuplicateHandlingForImport) {
    DivTag cardDiv =
        div()
            .withData("testid", "question-admin-name-" + questionDefinition.getName())
            .withClasses(
                ReferenceClasses.PROGRAM_QUESTION,
                "my-2",
                iffElse(malformedQuestionDefinition, "border-2", "border"),
                iffElse(malformedQuestionDefinition, "border-red-500", "border-gray-200"),
                "px-4",
                "py-2",
                "items-center",
                "rounded-md",
                StyleUtils.hover("text-gray-800", "bg-gray-100"))
            .with(
                div()
                    .withClasses("flex", "mt-2", "mb-4")
                    .condWith(
                        !malformedQuestionDefinition && questionDefinition.isUniversal(),
                        ViewUtils.makeUniversalBadge(questionDefinition, "mr-2"))
                    .condWith(maybeBadgeForImport.isPresent(), maybeBadgeForImport.get()));
    SvgTag icon =
        Icons.questionTypeSvg(questionDefinition.getQuestionType())
            .withClasses("shrink-0", "h-12", "w-6");
    String questionHelpText =
        questionDefinition.getQuestionHelpText().isEmpty()
            ? ""
            : questionDefinition.getQuestionHelpText().getDefault();

    DivTag content =
        div()
            .withClass("flex-grow")
            .with(
                iff(
                    malformedQuestionDefinition,
                    p("This is not pointing at the latest version")
                        .withClasses("text-red-500", "font-bold")),
                iff(
                    malformedQuestionDefinition,
                    p("Edit the program and try republishing").withClass("text-red-500")),
                div()
                    .with(
                        TextFormatter.formatTextForAdmins(
                            questionDefinition.getQuestionText().getDefault()))
                    .withData("testid", "question-div"),
                div()
                    .with(TextFormatter.formatTextForAdmins(questionHelpText))
                    .withClasses("mt-1", "text-sm"),
                p(String.format("Admin ID: %s", questionDefinition.getName()))
                    .withClasses("mt-1", "text-sm"),
                iff(
                    // Only show multi-option text during program import
                    maybeDuplicateHandlingForImport.isPresent()
                        && questionDefinition.getQuestionType().isMultiOptionType(),
                    getOptions((MultiOptionQuestionDefinition) questionDefinition)));

    DivTag row =
        div()
            .withClasses("flex", "gap-4", "items-center")
            .with(icon, content)
            .condWith(
                maybeDuplicateHandlingForImport.isPresent(), maybeDuplicateHandlingForImport.get())
            .with(editButtonsForProgramPage);

    return cardDiv.with(row);
  }

  private static UlTag getOptions(MultiOptionQuestionDefinition question) {
    UlTag options = ul().withClasses("list-disc", "mx-4", "mt-2");
    for (QuestionOption option : question.getOptions()) {
      options.with(li(option.optionText().getDefault()));
    }
    return options;
  }
}
