package views.admin.importexport;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.ul;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.admin.AdminImportExportController;
import controllers.admin.PredicateUtils;
import controllers.admin.ReadablePredicate;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;

import java.util.List;
import java.util.Optional;

import j2html.tags.specialized.LiTag;
import j2html.tags.specialized.UlTag;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.twirl.api.Content;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.predicate.PredicateDefinition;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.QuestionType;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.fileupload.FileUploadViewStrategy;

public class AdminImportView extends BaseHtmlView {
  private final AdminLayout layout;
  private final MessagesApi messagesApi;

  @Inject
  public AdminImportView(AdminLayoutFactory layoutFactory, MessagesApi messagesApi) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.IMPORT);
    this.messagesApi = messagesApi;
  }

  public Content render(
      Http.Request request, Optional<AdminImportExportController.JsonExportingClass> dataToImport) {
    String title = "Import programs";
    DivTag contentDiv = div().with(h1(title));

    contentDiv.with(importProgram(request));


    if (dataToImport.isPresent()) {
      contentDiv.with(renderImportedProgram(dataToImport.get().getPrograms().get(0), dataToImport.get().getQuestions()));
    }

    /*
    if (dataToImport.isPresent()) {
      contentDiv.with(h2("Programs:"));
      contentDiv.with(p(dataToImport.get().getPrograms().toString()));
      contentDiv.with(h2("Questions:"));
      for (QuestionDefinition question : dataToImport.get().getQuestions()) {
        contentDiv.with(h3(question.getQuestionText().getDefault() + "  [Type: " + question.getQuestionType() + "]"));
        contentDiv.with(p(question.getConfig().toString()));
      }
    } else {
      contentDiv.with(p("Nothing imported yet"));
    }

     */

    HtmlBundle bundle = layout.getBundle(request).setTitle(title).addMainContent(contentDiv);
    return layout.render(bundle);
  }

  private DomContent importProgram(Http.Request request) {
    DivTag fileUploadElement =
        FileUploadViewStrategy.createUswdsFileInputFormElement(
            "fake-id",
            Http.MimeTypes.JSON,
            ImmutableList.of(),
            /* disabled= */ false,
            /* fileLimitMb= */ 5,
            messagesApi.preferred(request));
    return div()
        .with(h2("Import program"))
        .with(
            form()
                .withEnctype("multipart/form-data")
                .withMethod("POST")
                .with(makeCsrfTokenInputTag(request), fileUploadElement)
                .with(submitButton("Import content"))
                .withAction(routes.AdminImportExportController.importPrograms().url()));
  }

  private DomContent renderImportedProgram(
          ProgramDefinition programDefinition,
          List<QuestionDefinition> questionsWithinProgram) {
    DivTag content = div();

    // TODO: Similar to PdfExporter
    content.with(h2(programDefinition.localizedName().getDefault()));
      content.with(p("Admin name: " + programDefinition.adminName()));
    content.with(p( "Admin description: " + programDefinition.adminDescription()));

      for (BlockDefinition block : programDefinition.getNonRepeatedBlockDefinitions()) {
        content.with(renderProgramBlock(
                programDefinition, block, questionsWithinProgram, /* indentationLevel= */ 0));
      }
    return content;
  }

  /**
   * Renders the given block in the program preview PDF.
   *
   * @param indentationLevel the level of indentation. Should be 0 for most questions, 1 for
   *     questions nested under an enumerator, 2 for doubly-nested enumerator questions, etc. Should
   *     be multiplied by {@code INDENTATION_PER_LEVEL} when adding text into the PDF.
   */
  private DomContent renderProgramBlock(
          ProgramDefinition program,
          BlockDefinition block,
          List<QuestionDefinition> allQuestions,
          int indentationLevel) {
    DivTag content = div();
    // Block-level information
    content.with(h3(block.name()));
    content.with(p("Admin description: " + block.description()));
    // Visibility & eligibility information
    if (block.visibilityPredicate().isPresent()) {
      renderPredicate(block.visibilityPredicate().get(), block, allQuestions, indentationLevel);
    }
    if (block.eligibilityDefinition().isPresent()) {
      renderPredicate(block.eligibilityDefinition().get().predicate(),
              block,
              allQuestions,
              indentationLevel);
    }

    for (ProgramQuestionDefinition pqd : block.programQuestionDefinitions()) {
      // Question-level information
      QuestionDefinition question = allQuestions.stream().filter(qdef -> qdef.getId() == pqd.id()).findFirst().get();
      content.with(h4(question.getQuestionText().getDefault()));
      if (!question.getQuestionHelpText().isEmpty()) {
        content.with(p(question.getQuestionHelpText().getDefault()));
      }
      content.with(p("Admin name: " + question.getName()));
      content.with(p("Admin description: " + question.getDescription()));
      content.with(p("Question type: " + question.getQuestionType().name()));

      // If a question offers options, put those options in the PDF
      if (question.getQuestionType().isMultiOptionType()) {
        MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) question;
        UlTag optionList = ul();
        for (QuestionOption option : multiOption.getOptions()) {
          optionList.with(li(option.optionText().getDefault()));
        }
        content.with(optionList);
      }
    }

    /* TODO: Block.isEnumerator calls ProgramQuestionDefinition.getQuestionDefinition which we're not actually including in the export, so it can't find it. Maybe we should just include it?
    if (block.isEnumerator()) {
      // TODO: Use indentation?
      for (BlockDefinition subBlock : program.getBlockDefinitionsForEnumerator(block.id())) {
        // Indent the blocks related to the enumerator so it's clear they're related
        renderProgramBlock(program, subBlock, allQuestions, indentationLevel + 1);
      }
    }

     */
    return content;
  }

  private DomContent renderPredicate(
          PredicateDefinition predicate,
          BlockDefinition block,
          List<QuestionDefinition> allQuestions,
          int indentationLevel) {
    DivTag content = div();
    ReadablePredicate readablePredicate =
            PredicateUtils.getReadablePredicateDescription(block.name(), predicate, ImmutableList.copyOf(allQuestions));

    content.with(p(readablePredicate.heading()));
    if (readablePredicate.conditionList().isPresent()) {
      UlTag conditionList = ul();
      for (String condition : readablePredicate.conditionList().get()) {
        conditionList.with(li(condition));
      }
      content.with(conditionList);
    }
    return content;
  }
}
