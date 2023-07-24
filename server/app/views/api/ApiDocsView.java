package views.api;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.a;
import static j2html.TagCreator.b;
import static j2html.TagCreator.blockquote;
import static j2html.TagCreator.br;
import static j2html.TagCreator.code;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.option;
import static j2html.TagCreator.pre;
import static j2html.TagCreator.select;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.OptionTag;
import j2html.tags.specialized.SelectTag;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;
import play.twirl.api.Content;
import services.CfJsonDocumentContext;
import services.TranslationNotFoundException;
import services.export.QuestionJsonSampler;
import services.program.ProgramDefinition;
import services.question.LocalizedQuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.Accordion;

public class ApiDocsView extends BaseHtmlView {
  private static final Logger logger = LoggerFactory.getLogger(ApiDocsView.class);

  private final ProfileUtils profileUtils;
  private final BaseHtmlLayout unauthenticatedlayout;
  private final AdminLayout authenticatedlayout;
  private final QuestionJsonSampler.Factory questionJsonSamplerFactory;

  @Inject
  public ApiDocsView(
      ProfileUtils profileUtils,
      BaseHtmlLayout unauthenticatedlayout,
      AdminLayoutFactory layoutFactory,
      QuestionJsonSampler.Factory questionJsonSamplerFactory) {
    this.profileUtils = profileUtils;
    this.unauthenticatedlayout = unauthenticatedlayout;
    this.authenticatedlayout = layoutFactory.getLayout(NavPage.API_DOCS);
    this.questionJsonSamplerFactory = questionJsonSamplerFactory;
  }

  public Content render(
      Http.Request request,
      ProgramDefinition programDefinition,
      ImmutableSet<String> allProgramSlugs) {

    BaseHtmlLayout layout =
        isAuthenticatedAdmin(request) ? authenticatedlayout : unauthenticatedlayout;

    HtmlBundle bundle =
        layout
            .getBundle(request)
            .setTitle("API Docs")
            .addMainContent(contentDiv(programDefinition, allProgramSlugs, request))
            .addMainStyles("overflow-hidden");

    return layout.render(bundle);
  }

  private DivTag contentDiv(
      ProgramDefinition programDefinition,
      ImmutableSet<String> allProgramSlugs,
      Http.Request request) {

    SelectTag slugsDropdown =
        select()
            .withId("select-slug")
            .withClasses("border", "border-gray-300", "rounded-lg", "p-2", "ml-2")
            .attr("onchange", "window.location.href = '/api/docs/v1/' + this.value");

    allProgramSlugs.forEach(
        (String slug) -> {
          OptionTag slugOption = option(slug).withValue(slug);
          if (programDefinition.slug().equals(slug)) {
            slugOption.isSelected();
          }
          slugsDropdown.with(slugOption);
        });

    DivTag divTag =
        div()
            .withClasses("flex", "flex-col", "flex-grow")
            .with(
                div()
                    .withClasses("items-center", "mx-6", "my-8")
                    .with(h1("API Documentation"))
                    .with(div().withClasses("flex", "flex-col").with(getNotes(request))))
            .with(
                div()
                    .withClasses("flex", "flex-row", "items-center", "mx-6")
                    .with(text("Select a program:   "))
                    .with(slugsDropdown));

    DivTag fullProgramDiv = div();
    fullProgramDiv.withClasses("flex", "flex-row", "gap-4", "m-4");

    DivTag leftSide = div().withClasses("w-full", "flex-grow");
    leftSide.with(h1("Questions").withClasses("pl-4"));
    leftSide.with(programDocsDiv(programDefinition));

    DivTag rightSide = div().withClasses("w-full flex-grow");
    rightSide.with(h1("API Response Preview").withClasses("pl-4"));
    rightSide.with(apiResponseSampleDiv(programDefinition));

    fullProgramDiv.with(leftSide);
    fullProgramDiv.with(rightSide);

    divTag.with(fullProgramDiv);

    return divTag;
  }

  private DivTag apiResponseSampleDiv(ProgramDefinition programDefinition) {
    DivTag apiResponseSampleDiv = div();

    ImmutableList<QuestionDefinition> questionDefinitions =
        programDefinition.streamQuestionDefinitions().collect(toImmutableList());

    CfJsonDocumentContext sampleJson = new CfJsonDocumentContext();
    for (QuestionDefinition questionDefinition : questionDefinitions) {
      sampleJson.mergeFrom(
          questionJsonSamplerFactory
              .create(questionDefinition.getQuestionType())
              .getSampleJson(questionDefinition));
    }

    apiResponseSampleDiv.with(
        pre(code(sampleJson.asPrettyJsonString()))
            .withStyle(
                "background-color: lightgray; max-width: 100ch; overflow-wrap: break-word;"
                    + " white-space: pre-wrap;")
            .withClass("m-4"));

    return apiResponseSampleDiv;
  }

  private DivTag programDocsDiv(ProgramDefinition programDefinition) {
    DivTag programDocsDiv =
        div().withClasses("flex", "flex-col", "border", "border-gray-300", "rounded-lg", "m-4");

    for (QuestionDefinition questionDefinition :
        programDefinition.streamQuestionDefinitions().collect(toImmutableList())) {
      programDocsDiv.with(questionDocsDiv(questionDefinition));
    }

    return programDocsDiv;
  }

  private DivTag questionDocsDiv(QuestionDefinition questionDefinition) {
    DivTag divTag = div().withClasses("pl-4", "border-b", "border-gray-300", "pt-2", "pb-2");

    divTag.with(
        span(
            h3(b(questionDefinition.getName())).withClasses("inline"),
            text(" (" + questionDefinition.getQuestionNameKey().toLowerCase(Locale.US) + ")")));
    divTag.with(br(), br());

    divTag.with(h3("Type: " + questionDefinition.getQuestionType().toString()));
    try {
      divTag.with(
          span(
              h3(text("Text:  ")).withClasses("inline"),
              blockquote(questionDefinition.getQuestionText().get(Locale.US))
                  .withClasses("inline")));

    } catch (TranslationNotFoundException e) {
      logger.error("No translation found for locale US in question text: " + e.getMessage());
    }

    if (questionDefinition.getQuestionType().isMultiOptionType()) {
      MultiOptionQuestionDefinition multiOptionQuestionDefinition =
          (MultiOptionQuestionDefinition) questionDefinition;
      divTag.with(h3("Options: " + getOptionsString(multiOptionQuestionDefinition)));
    }

    return divTag;
  }

  private static String getOptionsString(MultiOptionQuestionDefinition questionDefinition) {
    ImmutableList<LocalizedQuestionOption> options =
        questionDefinition.getOptionsForDefaultLocale();
    ImmutableList<String> optionsText =
        options.stream().map(LocalizedQuestionOption::optionText).collect(toImmutableList());

    return "\"" + String.join("\", \"", optionsText) + "\"";
  }

  private boolean isAuthenticatedAdmin(Http.Request request) {
    Optional<CiviFormProfile> currentUserProfile = profileUtils.currentUserProfile(request);
    return currentUserProfile.isPresent()
        && (currentUserProfile.get().isCiviFormAdmin()
            || currentUserProfile.get().isProgramAdmin());
  }

  private DivTag getNotes(Http.Request request) {
    String generalDocsLink = "https://docs.civiform.us/it-manual/api.";
    String apiDocsLink = controllers.api.routes.ApiDocsController.index().absoluteURL(request);

    DivTag notesTag = div().withClasses("mt-6");

    notesTag.with(
        text(
            "The API Response Preview is a sample of what the API response might look like for a"
                + " given program. All data is fake. Single-select and multi-select questions have"
                + " sample answers that are selected from the available responses. General"
                + " information about using the API is located at "));
    notesTag.with(
        a(generalDocsLink)
            .withHref(generalDocsLink)
            .withClasses("text-blue-500", "underline")); // create a clickable link
    notesTag.with(br());
    notesTag.with(br());
    notesTag.with(text("You may share this link, "));
    notesTag.with(
        a(apiDocsLink)
            .withHref(apiDocsLink)
            .withClasses("text-blue-500", "underline")); // create a clickable link
    notesTag.with(
        text(
            ", with anyone in your organization who needs to see API docs, even "
                + "if they are not a CiviForm Admin or Program Admin."));
    notesTag.with(br());
    notesTag.with(br());

    notesTag.with(
        text(
            "Note that API docs do not currently support enumerated nor enumerator questions."
                + " Static content questions are not shown on the API Response Preview because"
                + " they do not include answers to questions."));

    Accordion accordion = new Accordion().setTitle("How does this work?");
    accordion.addContent(notesTag);

    return accordion.getContainer();
  }
}
