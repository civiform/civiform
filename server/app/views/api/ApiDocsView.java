package views.api;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.a;
import static j2html.TagCreator.b;
import static j2html.TagCreator.br;
import static j2html.TagCreator.code;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.option;
import static j2html.TagCreator.pre;
import static j2html.TagCreator.select;
import static j2html.TagCreator.text;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.OptionTag;
import j2html.tags.specialized.SelectTag;
import java.util.List;
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
            .addMainContent(contentDiv(programDefinition, allProgramSlugs))
            .addMainStyles("overflow-hidden");

    return layout.render(bundle);
  }

  private DivTag contentDiv(
      ProgramDefinition programDefinition, ImmutableSet<String> allProgramSlugs) {

    SelectTag slugsDropdown =
        select()
            .withId("select-slug")
            .withClasses("border", "border-gray-300", "rounded-lg", "p-2", "ml-2")
            .attr("onchange", "window.location.href = '/api/v1/docs/' + this.value");

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
                    .with(div().withClasses("flex", "flex-col").with(getNotes())))
            .with(
                div()
                    .withClasses("flex", "flex-row", "items-center", "mx-6")
                    .with(text("Select a program:   "))
                    .with(slugsDropdown));

    DivTag fullProgramDiv = div();
    fullProgramDiv.withClasses("flex", "flex-row", "gap-4", "m-4");

    DivTag leftSide = div().withClasses("w-full", "flex-grow");
    leftSide.with(h2("Questions"));
    leftSide.with(programDocsDiv(programDefinition));

    DivTag rightSide = div().withClasses("w-full flex-grow");
    rightSide.with(h2("API Response Preview"));
    rightSide.with(apiResponseSampleDiv(programDefinition));

    fullProgramDiv.with(leftSide);
    fullProgramDiv.with(rightSide);

    divTag.with(fullProgramDiv);

    return divTag;
  }

  private DivTag apiResponseSampleDiv(ProgramDefinition programDefinition) {
    DivTag apiResponseSampleDiv = div();

    List<QuestionDefinition> questionDefinitions =
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
            .withStyle("background-color: lightgray;")
            .withClass("m-4"));

    return apiResponseSampleDiv;
  }

  private DivTag programDocsDiv(ProgramDefinition programDefinition) {
    DivTag programDocsDiv = div().withClasses("flex", "flex-col", "gap-4");

    for (QuestionDefinition questionDefinition :
        programDefinition.streamQuestionDefinitions().collect(toImmutableList())) {
      programDocsDiv.with(questionDocsDiv(questionDefinition));
    }

    return programDocsDiv;
  }

  private DivTag questionDocsDiv(QuestionDefinition questionDefinition) {
    DivTag divTag = div().withClasses("pl-4", "border", "border-gray-500", "rounded-lg");

    divTag.with(h2(b("Question Name: "), text(questionDefinition.getName())));
    divTag.with(h2(b("Question Type: "), text(questionDefinition.getQuestionType().toString())));

    try {
      divTag.with(
          h2(b("Question Text: "), text(questionDefinition.getQuestionText().get(Locale.US))));
    } catch (TranslationNotFoundException e) {
      logger.error("No translation found for locale US in question text: " + e.getMessage());
    }

    if (questionDefinition.getQuestionType().isMultiOptionType()) {
      MultiOptionQuestionDefinition multiOptionQuestionDefinition =
          (MultiOptionQuestionDefinition) questionDefinition;
      divTag.with(
          h2(b("Question Options: "), text(getOptionsString(multiOptionQuestionDefinition))));
    }

    return divTag;
  }

  private static String getOptionsString(MultiOptionQuestionDefinition questionDefinition) {
    ImmutableList<LocalizedQuestionOption> options =
        questionDefinition.getOptionsForDefaultLocale();
    ImmutableList<String> optionsText =
        options.stream().map(LocalizedQuestionOption::optionText).collect(toImmutableList());

    StringBuilder stringBuilder = new StringBuilder();

    String commaSpace = ", ";
    optionsText.forEach(
        option -> stringBuilder.append("\"").append(option).append("\"").append(commaSpace));

    String optionsString = stringBuilder.toString();

    // Remove the trailing comma if there is one.
    if (optionsString.endsWith(commaSpace)) {
      optionsString = optionsString.substring(0, optionsString.length() - commaSpace.length());
    }

    return optionsString;
  }

  private boolean isAuthenticatedAdmin(Http.Request request) {
    Optional<CiviFormProfile> currentUserProfile = profileUtils.currentUserProfile(request);
    return currentUserProfile.isPresent()
        && (currentUserProfile.get().isCiviFormAdmin()
            || currentUserProfile.get().isProgramAdmin());
  }

  private DivTag getNotes() {
    String link = "https://docs.civiform.us/it-manual/api.";

    DivTag notesTag = div().withClasses("mt-6");

    notesTag.with(h2(b("How does this work?")));
    notesTag.with(
        text(
            "The API Response Preview is a sample of what the API response might look like for a"
                + " given program. All data is fake. Single-select and multi-select questions have"
                + " sample answers that are selected from the available responses. General"
                + " information about using the API is located at "));
    notesTag.with(
        a(link)
            .withHref(link)
            .withClasses("text-blue-500", "underline")); // create a clickable link
    notesTag.with(br());
    notesTag.with(br());
    notesTag.with(
        text(
            "Note that API docs do not currently support enumerated nor enumerator questions."
                + " Static content questions are not shown on the API Response Preview because"
                + " they do not include answers to questions."));

    return notesTag;
  }
}
