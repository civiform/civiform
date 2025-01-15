package views.docs;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.a;
import static j2html.TagCreator.aside;
import static j2html.TagCreator.b;
import static j2html.TagCreator.blockquote;
import static j2html.TagCreator.code;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.label;
import static j2html.TagCreator.li;
import static j2html.TagCreator.option;
import static j2html.TagCreator.p;
import static j2html.TagCreator.pre;
import static j2html.TagCreator.select;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;
import static j2html.TagCreator.ul;
import static services.export.JsonPrettifier.asPrettyJsonString;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import controllers.docs.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.AsideTag;
import j2html.tags.specialized.CodeTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.OptionTag;
import j2html.tags.specialized.SelectTag;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;
import play.twirl.api.Content;
import repository.ExportServiceRepository;
import services.TranslationNotFoundException;
import services.export.ProgramJsonSampler;
import services.program.ProgramDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.AccordionFactory;
import views.components.LinkElement;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.StyleUtils;

// TODO: If we're being strict about code organization, access to the ProgramJsonSampler and
// ExportServiceRepository should be brokered by an ApiDocsService, instead of accessed directly
// from the view.

public class ApiDocsView extends BaseHtmlView {
  private static final Logger logger = LoggerFactory.getLogger(ApiDocsView.class);
  private static final String CODE_STYLES =
      StyleUtils.joinStyles("bg-slate-200", "p-0.5", "rounded-md");

  private final ProfileUtils profileUtils;
  private final BaseHtmlLayout unauthenticatedlayout;
  private final AdminLayout authenticatedlayout;
  private final ProgramJsonSampler programJsonSampler;
  private final ExportServiceRepository exportServiceRepository;
  private final SettingsManifest settingsManifest;

  @Inject
  public ApiDocsView(
      ProfileUtils profileUtils,
      BaseHtmlLayout unauthenticatedlayout,
      AdminLayoutFactory layoutFactory,
      ProgramJsonSampler programJsonSampler,
      ExportServiceRepository exportServiceRepository,
      SettingsManifest settingsManifest) {
    this.profileUtils = profileUtils;
    this.unauthenticatedlayout = unauthenticatedlayout;
    this.authenticatedlayout = layoutFactory.getLayout(NavPage.API_DOCS);
    this.programJsonSampler = programJsonSampler;
    this.exportServiceRepository = exportServiceRepository;
    this.settingsManifest = settingsManifest;
  }

  public Content render(
      Http.Request request,
      String selectedProgramSlug,
      Optional<ProgramDefinition> programDefinition,
      ImmutableSet<String> allProgramSlugs) {

    BaseHtmlLayout layout =
        isAuthenticatedAdmin(request) ? authenticatedlayout : unauthenticatedlayout;

    HtmlBundle bundle =
        layout
            .getBundle(request)
            .setTitle("API docs")
            .addMainContent(
                contentDiv(selectedProgramSlug, programDefinition, allProgramSlugs, request))
            .addMainStyles("overflow-hidden");

    return layout.render(bundle);
  }

  private DivTag contentDiv(
      String selectedProgramSlug,
      Optional<ProgramDefinition> programDefinition,
      ImmutableSet<String> allProgramSlugs,
      Http.Request request) {

    SelectTag slugsDropdown =
        select()
            .withId("select-slug")
            .withClasses("border", "border-gray-300", "rounded-lg", "p-2", "mx-2");
    allProgramSlugs.forEach(
        (String slug) -> {
          OptionTag slugOption = option(slug).withValue(slug);
          if (selectedProgramSlug.equals(slug)) {
            slugOption.isSelected();
          }
          slugsDropdown.with(slugOption);
        });

    SelectTag versionsDropdown =
        select()
            .withId("select-version")
            .withClasses("border", "border-gray-300", "rounded-lg", "p-2", "mx-2")
            .with(
                option("Active")
                    .withValue("active")
                    .withCondSelected(request.uri().contains("/active")))
            .with(
                option("Draft")
                    .withValue("draft")
                    .withCondSelected(request.uri().contains("/draft")));

    DivTag divTag =
        div()
            .withClasses("flex", "flex-col", "flex-grow")
            .with(
                div()
                    .withClasses("items-center", "mx-6", "my-8")
                    .with(h1("API documentation"))
                    .with(buildTabs())
                    .with(
                        p().withClasses("my-2")
                            .with(
                                text(
                                    "This page includes program-specific API documentation for"
                                        + " Active and Draft programs. Detailed documentation about"
                                        + " authentication, pagination, and every field's possible"
                                        + " values is located at "),
                                new LinkElement()
                                    .setHref("https://docs.civiform.us/it-manual/api")
                                    .setText("docs.civiform.us/it-manual/api")
                                    .asAnchorText(),
                                text(".")))
                    .with(div().withClasses("flex", "flex-col").with(getNotes(request))))
            .with(
                div()
                    .withClasses("flex", "flex-row", "items-center", "mx-6")
                    .with(label("Select a program:").withFor("select-slug").with(slugsDropdown))
                    .with(
                        label("Select version:").withFor("select-version").with(versionsDropdown)));

    DivTag fullProgramDiv = div();

    if (programDefinition.isEmpty()) {
      fullProgramDiv.with(h2("Program and version not found").withClasses("mx-5", "my-10"));
    } else {
      fullProgramDiv.withClasses("flex", "flex-row", "gap-4", "m-4");

      DivTag leftSide = div().withClasses("w-full", "flex-grow");
      leftSide.with(h2("Questions").withClasses("pl-4"));
      leftSide.with(programDocsDiv(programDefinition.get()));

      AsideTag rightSide = aside().withClasses("w-full flex-grow");
      rightSide.with(h2("API response preview").withClasses("pl-4"));
      rightSide.with(
          apiResponseSampleDiv(
              programDefinition.get(), settingsManifest.getMultipleFileUploadEnabled(request)));

      fullProgramDiv.with(leftSide);
      fullProgramDiv.with(rightSide);
    }

    divTag.with(fullProgramDiv);
    return divTag;
  }

  private DivTag buildTabs() {
    return div(ul(
                li(a("API documentation").withHref(routes.ApiDocsController.index().url()))
                    .withClasses("border-b-2", AdminStyles.LINK_SELECTED, "mx-0"),
                li(a("API Schema Viewer")
                        .withHref(
                            routes.OpenApiSchemaController.getSchemaUI(
                                    "", Optional.empty(), Optional.empty())
                                .url()))
                    .withClasses(AdminStyles.LINK_NOT_SELECTED))
            .withClasses("flex", "gap-4"))
        .withClasses("my-4", BaseStyles.LINK_TEXT, BaseStyles.LINK_HOVER_TEXT);
  }

  private DivTag apiResponseSampleDiv(
      ProgramDefinition programDefinition, boolean multipleFileUploadEnabled) {
    DivTag apiResponseSampleDiv = div();
    String fullJsonResponsePreview =
        programJsonSampler.getSampleJson(programDefinition, multipleFileUploadEnabled);
    String fullJsonResponsePreviewPretty = asPrettyJsonString(fullJsonResponsePreview);

    apiResponseSampleDiv.with(
        pre(code(fullJsonResponsePreviewPretty))
            .withStyle("max-width: 100ch;")
            .withClasses(
                "m-4", "p-2", "rounded-lg", "bg-slate-200", "break-words", "whitespace-pre-wrap"));

    return apiResponseSampleDiv;
  }

  private DivTag programDocsDiv(ProgramDefinition programDefinition) {
    DivTag programDocsDiv = div().withClasses("flex", "flex-col", "m-4");

    for (QuestionDefinition questionDefinition :
        programDefinition
            .streamQuestionDefinitions()
            .sorted(Comparator.comparing(QuestionDefinition::getQuestionNameKey))
            .collect(toImmutableList())) {
      programDocsDiv.with(questionDocsDiv(questionDefinition));
    }

    return programDocsDiv;
  }

  private DivTag questionDocsDiv(QuestionDefinition questionDefinition) {
    DivTag divTag =
        div()
            .withClasses(
                "pl-4",
                "border",
                "rounded-lg",
                "mb-2",
                "border-gray-300",
                "pt-2",
                "pb-2",
                "flex",
                "flex-col");

    DivTag questionCardHeader = div();

    questionCardHeader.with(
        span(
            h2(b(questionDefinition.getName())).withClasses("inline", "mr-1"),
            codeWithStyles(questionDefinition.getQuestionNameKey().toLowerCase(Locale.US))));
    questionCardHeader.withClasses("mb-4");

    DivTag questionCardBodyTopLeftSide = div().withClasses("w-2/5", "flex", "flex-col", "mr-4");
    DivTag questionCardBodyTopRightSide = div().withClasses("w-3/5", "flex", "flex-col", "mr-4");

    questionCardBodyTopLeftSide.with(
        div(h3(b("Type")), text(questionDefinition.getQuestionType().toString())));

    try {
      questionCardBodyTopRightSide.with(
          h3(b("Text")).withClasses("inline"),
          blockquote(questionDefinition.getQuestionText().get(Locale.US)).withClasses("inline"));

    } catch (TranslationNotFoundException e) {
      logger.error("No translation found for locale US in question text: " + e.getMessage());
    }
    DivTag questionCardBodyTop = div().withClasses("flex", "flex-row");
    questionCardBodyTop.with(questionCardBodyTopLeftSide, questionCardBodyTopRightSide);

    DivTag questionCardBody = div().withClasses("flex", "flex-column");
    questionCardBody.with(questionCardBodyTop);

    if (questionDefinition.getQuestionType().isMultiOptionType()) {
      DivTag questionCardBodyBottom = div();

      MultiOptionQuestionDefinition multiOptionQD =
          (MultiOptionQuestionDefinition) questionDefinition;

      Stream<DomContent> currentOptionElements =
          asCommaSeparatedCodeElementStream(multiOptionQD.getOptionAdminNames());

      Stream<DomContent> allPossibleOptionElements =
          asCommaSeparatedCodeElementStream(
              exportServiceRepository.getAllHistoricMultiOptionAdminNames(multiOptionQD));

      questionCardBodyBottom.with(
          div()
              .with(h3(b("Options")), h4("Current options:"))
              .with(currentOptionElements)
              .with(h4("All possible options:").withClasses("mt-2"))
              .with(allPossibleOptionElements)
              .withClasses("mt-4"));

      questionCardBody.with(questionCardBodyBottom);
    }

    divTag.with(questionCardHeader);
    divTag.with(questionCardBody);

    return divTag;
  }

  private static Stream<DomContent> asCommaSeparatedCodeElementStream(
      ImmutableList<String> codeElements) {
    return codeElements.stream()
        .map(ApiDocsView::codeWithStyles)
        .flatMap(element -> Stream.<DomContent>of(element, span(", ").withClass("ml-0.5")))
        // trim the trailing comma element
        .limit(codeElements.size() * 2L - 1);
  }

  private static CodeTag codeWithStyles(String text) {
    return code(text).withClass(CODE_STYLES);
  }

  private boolean isAuthenticatedAdmin(Http.Request request) {
    // CiviFormProfileFilter does not apply to API docs views, so there may be no profile
    Optional<CiviFormProfile> currentUserProfile = profileUtils.optionalCurrentUserProfile(request);
    return currentUserProfile.isPresent()
        && (currentUserProfile.get().isCiviFormAdmin()
            || currentUserProfile.get().isProgramAdmin());
  }

  private DivTag getNotes(Http.Request request) {
    String apiDocsLink = controllers.docs.routes.ApiDocsController.index().absoluteURL(request);

    DivTag notesTag = div().withClasses("mt-2");

    notesTag.with(
        p().withClasses("my-2")
            .with(
                text(
                    "The API response preview is a sample of what the API response might look like"
                        + " for a given program. All data is fake. Single-select and multi-select"
                        + " questions have sample answers that are selected from the available"
                        + " responses.")),
        p().withClasses("my-2")
            .with(
                text("You may share this link, "),
                code(new LinkElement().setHref(apiDocsLink).setText(apiDocsLink).asAnchorText())
                    .withClass(CODE_STYLES),
                text(
                    ", with anyone who needs to see API docs. It is accessible without logging in,"
                        + " so they do not need to be a CiviForm Admin or Program Admin to view"
                        + " this page.")),
        p().withClasses("my-2")
            .with(
                text(
                    "Note: These API docs do not currently support enumerator or repeated"
                        + " questions. See GitHub Issue "),
                new LinkElement()
                    .setHref("https://github.com/civiform/civiform/issues/5238")
                    .setText("#5238")
                    .asAnchorText(),
                text(" for progress on this.")));

    return AccordionFactory.buildAccordion(
        Optional.of("How does this work?"), Optional.of(notesTag));
  }
}
