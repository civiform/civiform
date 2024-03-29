package views.admin.importexport;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.input;
import static j2html.TagCreator.li;
import static j2html.TagCreator.p;
import static j2html.TagCreator.ul;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import controllers.admin.AdminImportExportController;
import controllers.admin.PredicateUtils;
import controllers.admin.ReadablePredicate;
import controllers.admin.routes;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;

import java.util.List;
import java.util.Optional;

import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.FormTag;
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
import views.components.FieldWithLabel;
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
      Http.Request request, Optional<AdminImportExportController.JsonExportingClass> dataToImport, Optional<String> pureString, Optional<ImmutableList<QuestionDefinition>> currentQuestionsWithinInstance) {
    String title = "Import programs";
    DivTag contentDiv = div().with(h1(title));

    contentDiv.with(importProgram(request));


    if (dataToImport.isPresent()) {
      contentDiv.with(renderImportedProgram(request, pureString.get(), dataToImport.get().getPrograms().get(0), dataToImport.get().getQuestions(), currentQuestionsWithinInstance.get()));
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
          Http.Request request,
          String pureString,
          ProgramDefinition programDefinition,
          List<QuestionDefinition> questionsWithinProgram,           ImmutableList<QuestionDefinition> currentQuestionsInInstance) {


    ImmutableMap.Builder<String, QuestionDefinition> questionsByAdminName = ImmutableMap.builder();
    currentQuestionsInInstance.forEach(q -> questionsByAdminName.put(q.getName(), q));
    System.out.println("questions by admin name map: "+ questionsByAdminName);

    FormTag form = form()
            .withMethod("POST")
                    .withAction(routes.AdminImportExportController.createProgramsAndQuestions().url())
                            .with(makeCsrfTokenInputTag(request));

    form.with(input().isHidden().withValue(pureString).withName("pureString"));

    // TODO: Similar to PdfExporter
            // TODO: Error if there's an existing program with that admin name
    form.with(h2(programDefinition.localizedName().getDefault()));
    form.with(p("Admin name: " + programDefinition.adminName()));
    form.with(p( "Admin description: " + programDefinition.adminDescription()));

      for (BlockDefinition block : programDefinition.getNonRepeatedBlockDefinitions()) {
        form.with(renderProgramBlock(
                programDefinition, block, questionsWithinProgram, questionsByAdminName.build(), /* indentationLevel= */ 0));
      }

      form.with(submitButton("Import programs and questions"));
    return form;
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
          List<QuestionDefinition> questionsForImportedProgram,
          ImmutableMap<String, QuestionDefinition> currentQuestionsInInstance,
          int indentationLevel) {
    DivTag blockDiv = div().withClasses("border", "border-gray-200", "p-4", "flex", "flex-col");
    // Block-level information
    blockDiv.with(h3(block.name()).withClass("w-full"));
    blockDiv.with(p("Admin description: " + block.description()).withClass("w-full"));
    // Visibility & eligibility information
    if (block.visibilityPredicate().isPresent()) {
      blockDiv.with(renderPredicate(block.visibilityPredicate().get(), block, questionsForImportedProgram, indentationLevel));
    }
    if (block.eligibilityDefinition().isPresent()) {
      blockDiv.with(renderPredicate(block.eligibilityDefinition().get().predicate(),
              block,
              questionsForImportedProgram,
              indentationLevel));
    }

    for (ProgramQuestionDefinition pqd : block.programQuestionDefinitions()) {
      DivTag fullQuestionRow = div().withClasses("border", "border-gray-200", "p-4", "flex");
      // Question-level information
      QuestionDefinition question = questionsForImportedProgram.stream().filter(qdef -> qdef.getId() == pqd.id()).findFirst().get();
      boolean hasExistingQuestion = currentQuestionsInInstance.containsKey(question.getName());


      // TODO: Better handling if existing q and imported q are identical
      // TODO: Show validation predicate?
      // TODO: Make a table with the rows being adminName, questionText, questionHelpText, etc. and
      // the columns being the existing q and imported q and highlight the differences so it's easy to see what's happening?
      fullQuestionRow.with(renderQuestion(question, /*wide= */ !hasExistingQuestion, /* heading= */ "Imported question"));
      if (hasExistingQuestion) {
        /* wide= */
        fullQuestionRow.with(renderQuestion(currentQuestionsInInstance.get(question.getName()), /* wide= */ false, /* heading= */ "Existing question") );
      }
      DomContent adminSelectionOptions = createAdminSelectionOptionsForQuestion(question, currentQuestionsInInstance);
      fullQuestionRow.with(adminSelectionOptions);
      blockDiv.with(fullQuestionRow);
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
    return blockDiv;
  }

  private DomContent renderQuestion(QuestionDefinition question, boolean wide, String heading) {
    String width;
    if (wide) { width = "w-2/3";} else {width = "w-1/3";}
    DivTag questionDiv = div().withClasses("border-gray-200", "p-4", width);
    questionDiv.with(p(heading).withClass("font-bold"));
    questionDiv.with(h4(question.getQuestionText().getDefault()));
    if (!question.getQuestionHelpText().isEmpty()) {
      questionDiv.with(p(question.getQuestionHelpText().getDefault()));
    }
    questionDiv.with(p("Admin name: " + question.getName()));
    questionDiv.with(p("Admin description: " + question.getDescription()));
    questionDiv.with(p("Question type: " + question.getQuestionType().name()));

    // TODO: Seems like the help text isn't being imported?
    // should also blank line if no help text so everything aligns
    // Or maybe just align things in grid if wording changes

    // If a question offers options, put those options in the PDF
    if (question.getQuestionType().isMultiOptionType()) {
      MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) question;
      UlTag optionList = ul();
      for (QuestionOption option : multiOption.getOptions()) {
        optionList.with(li(option.optionText().getDefault()));
      }
      questionDiv.with(optionList);
    }
    return questionDiv;
  }

  private DomContent renderPredicate(
          PredicateDefinition predicate,
          BlockDefinition block,
          List<QuestionDefinition> allQuestions,
          int indentationLevel) {
    DivTag content = div().withClass("w-full");
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

  private static final String QUESTION_CHOICE_PREFIX = "question-choice-"; // Should append the admin name to the end

  private DomContent createAdminSelectionOptionsForQuestion(QuestionDefinition questionDefinition, ImmutableMap<String, QuestionDefinition> currentQuestionsInInstance) {
    String fieldName = QUESTION_CHOICE_PREFIX + questionDefinition.getName();
    DivTag selectionOptions = div().withClass("w-1/3");
    FieldsetTag fields = fieldset();
    if (currentQuestionsInInstance.containsKey(questionDefinition.getName())) {
      // Admin needs to choose between keeping the current instance version or using the new version
      fields.with(FieldWithLabel.radio()
              .setFieldName(fieldName)
              .setLabelText("Keep existing version")
              .setValue("keep-existing")
                      .setRequired(true)
              .getRadioTag());
      fields.with(FieldWithLabel.radio()
              .setFieldName(fieldName)
              .setLabelText("Replace with imported version")
              .setValue("replace-with-imported")
              .setRequired(true)

              .getRadioTag());
      fields.with(FieldWithLabel.radio()
              .setFieldName(fieldName)
              .setLabelText("Remove from program")
              .setValue("remove-from-program")
              .setRequired(true)

              .getRadioTag());
    } else {
      // Admin needs to confirm they want this question in the program
      fields.with(FieldWithLabel.radio()
              .setFieldName(fieldName)
              .setLabelText("Use in program")
              .setValue("use-in-program")
                      .setChecked(true) // Assume any new questions will be kept in the program
              .setRequired(true)

              .getRadioTag());
      fields.with(FieldWithLabel.radio()
              .setFieldName(fieldName)
              .setLabelText("Remove from program")
              .setValue("remove-from-program")
              .setRequired(true)

              .getRadioTag());
    }
    // TODO: Prevent form submission unless something has been selected for every question
    return selectionOptions.with(fields);
  }
}
