package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import annotations.BindingAnnotations.Now;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provider;
import com.itextpdf.text.Anchor;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.List;
import com.itextpdf.text.ListItem;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.FontSelector;

import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.typesafe.config.Config;
import controllers.LanguageUtils;
import controllers.admin.PredicateUtils;
import controllers.admin.ReadablePredicate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.inject.Inject;
import models.ApplicationModel;
import play.i18n.Lang;
import services.DateConverter;
import services.TranslationNotFoundException;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.predicate.PredicateDefinition;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.statuses.StatusDefinitions;
import services.statuses.StatusService;

/** PdfExporter is meant to generate PDF files. */
public final class PdfExporter {
  private final ApplicantService applicantService;
  private final Provider<LocalDateTime> nowProvider;
  private final String baseUrl;
  private final DateConverter dateConverter;
  private final StatusService statusService;
  private final LanguageUtils languageUtils;

  /**
   * Similar to
   * {@link views.admin.programs.ProgramBlocksView#INDENTATION_FACTOR_INCREASE_ON_LEVEL}:
   * For each level of enumerator question, add another layer of indentation so
   * it's easier to
   * understand which questions are part of enumerators.
   */
  private static final int INDENTATION_PER_LEVEL = 25;

  private static final ImmutableList<String> FONT_PATHS = ImmutableList.of(
      "conf/fonts/NotoSans-Regular.ttf",
      "conf/fonts/NotoSansArabic-Regular.ttf",
      "conf/fonts/NotoSansEthiopic-Regular.ttf",
      "conf/fonts/NotoSansLao-Regular.ttf",
      "conf/fonts/NotoSansTC-Regular.ttf",
      "conf/fonts/NotoSansJP-Regular.ttf",
      "conf/fonts/NotoSansKR-Regular.ttf");
  private final FontSelector h1FontSelector;
  private final FontSelector h2FontSelector;
  private final FontSelector h3FontSelector;
  private final FontSelector paragraphFontSelector;
  private final FontSelector smallGrayFontSelector;
  private final FontSelector predicateFontSelector;
  private final FontSelector linkFontSelector;

  private FontSelector createFontSelector(ImmutableList<BaseFont> baseFonts, int size, int style, BaseColor color) {
    FontSelector selector = new FontSelector();
    if (baseFonts.isEmpty()) {
      selector.addFont(FontFactory.getFont(FontFactory.HELVETICA, size, style, color));
    } else {
      for (BaseFont baseFont : baseFonts) {
        selector.addFont(new Font(baseFont, size, style, color));
      }
      // Always add Helvetica as a final fallback
      selector.addFont(FontFactory.getFont(FontFactory.HELVETICA, size, style, color));
    }
    return selector;
  }

  @Inject
  PdfExporter(
      ApplicantService applicantService,
      @Now Provider<LocalDateTime> nowProvider,
      Config configuration,
      DateConverter dateConverter,
      StatusService statusService, LanguageUtils languageUtils, play.Environment environment) {
    this.applicantService = checkNotNull(applicantService);
    this.nowProvider = checkNotNull(nowProvider);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
    this.dateConverter = checkNotNull(dateConverter);
    this.statusService = checkNotNull(statusService);
    this.languageUtils = checkNotNull(languageUtils);

    ImmutableList.Builder<BaseFont> fontBuilder = ImmutableList.builder();
    for (String fontPath : FONT_PATHS) {
      try {
        java.io.File fontFile = environment.getFile(fontPath);
        if (fontFile.exists()) {
          fontBuilder.add(BaseFont.createFont(fontFile.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED));
        } else {
          System.err.println("Font file not found: " + fontPath);
        }
      } catch (Exception e) {
        System.err.println("Failed to load font " + fontPath + ": " + e.getMessage());
      }
    }
    ImmutableList<BaseFont> baseFonts = fontBuilder.build();

    this.h1FontSelector = createFontSelector(baseFonts, 30, Font.BOLD, BaseColor.BLACK);
    this.h2FontSelector = createFontSelector(baseFonts, 16, Font.BOLD, BaseColor.BLACK);
    this.h3FontSelector = createFontSelector(baseFonts, 16, Font.NORMAL, BaseColor.BLACK);
    this.paragraphFontSelector = createFontSelector(baseFonts, 12, Font.NORMAL, BaseColor.BLACK);
    this.smallGrayFontSelector = createFontSelector(baseFonts, 10, Font.NORMAL, BaseColor.GRAY);
    this.predicateFontSelector = createFontSelector(baseFonts, 11, Font.NORMAL, BaseColor.BLUE);
    this.linkFontSelector = createFontSelector(baseFonts, 11, Font.UNDERLINE, new BaseColor(0, 94, 162));

  }

  /**
   * Generates a byte array containing all the values present in the List of
   * AnswerData using
   * itextPDF. This function creates the output document in memory as a byte[] and
   * is part of the
   * inMemoryPDF object. The InMemoryPdf object is passed back to the
   * AdminController Class to
   * generate the required PDF.
   */
  public InMemoryPdf exportApplication(ApplicationModel application, boolean isAdmin)
      throws DocumentException, IOException {
    ReadOnlyApplicantProgramService roApplicantService = applicantService
        .getReadOnlyApplicantProgramService(application)
        .toCompletableFuture()
        .join();

    ImmutableList<AnswerData> answersOnlyActive = isAdmin
        ? roApplicantService.getSummaryDataOnlyActiveForAdmin()
        : roApplicantService.getSummaryDataOnlyActive();

    ImmutableList<AnswerData> answersOnlyHidden = ImmutableList.<AnswerData>of();
    if (isAdmin) {
      answersOnlyHidden = roApplicantService.getSummaryDataOnlyHidden();
    }

    // We expect a name to be present at this point. However, if it's not, we use a
    // placeholder
    // rather than throwing an error here.
    String applicantName = application.getApplicant().getApplicantDisplayName().orElse("name-unavailable");
    String applicantNameWithApplicationId = String.format("%s (%d)", applicantName, application.id);
    String filename = String.format("%s-%s.pdf", applicantNameWithApplicationId, nowProvider.get());
    byte[] bytes = buildApplicationPdf(
        answersOnlyActive,
        answersOnlyHidden,
        applicantNameWithApplicationId,
        application.getOriginalApplicantId().orElse(application.getApplicant().id),
        application.getProgram().getProgramDefinition(),
        application.getLatestStatus(),
        getSubmitTime(application.getSubmitTime()),
        isAdmin);
    return new InMemoryPdf(bytes, filename);
  }

  private String getSubmitTime(Instant submitTime) {
    return submitTime == null
        ? "Application submitted without submission time marked."
        : dateConverter.renderDateTimeHumanReadable(submitTime);
  }

  private byte[] buildApplicationPdf(
      ImmutableList<AnswerData> answersOnlyActive,
      ImmutableList<AnswerData> answersOnlyHidden,
      String applicantNameWithApplicationId,
      Long applicantId,
      ProgramDefinition programDefinition,
      Optional<String> statusValue,
      String submitTime,
      boolean isAdmin)
      throws DocumentException, IOException {
    ByteArrayOutputStream byteArrayOutputStream = null;
    PdfWriter writer = null;
    Document document = null;

    try {
      byteArrayOutputStream = new ByteArrayOutputStream();
      document = new Document();
      writer = PdfWriter.getInstance(document, byteArrayOutputStream);
      document.open();

      Paragraph applicant = new Paragraph(h2FontSelector.process(applicantNameWithApplicationId));
      Paragraph program = new Paragraph(h2FontSelector.process("Program Name : " + programDefinition.adminName()));
      document.add(applicant);
      document.add(program);
      Paragraph status = new Paragraph(paragraphFontSelector.process("Status: " + statusValue.orElse("none")));
      document.add(status);
      Paragraph submitTimeInformation = new Paragraph(paragraphFontSelector.process("Submit Time: " + submitTime));
      document.add(submitTimeInformation);
      document.add(Chunk.NEWLINE);
      boolean isEligibilityEnabledInProgram = programDefinition.hasEligibilityEnabled();
      for (AnswerData answerData : answersOnlyActive) {
        Paragraph question = new Paragraph(paragraphFontSelector.process(answerData.questionDefinition().getName()));
        final Paragraph answer;
        if (!answerData.encodedFileKeys().isEmpty()) {
          answer = new Paragraph();
          ImmutableList<String> encodedFileKeys = answerData.encodedFileKeys();
          for (int i = 0; i < encodedFileKeys.size(); i++) {
            String encodedFileKey = encodedFileKeys.get(i);
            String fileName = answerData.fileNames().get(i);
            String fileLink = (isAdmin
                ? controllers.routes.FileController.acledAdminShow(encodedFileKey)
                : controllers.routes.FileController.show(applicantId, encodedFileKey))
                .url();
            Anchor anchor = new Anchor();
            anchor.add(linkFontSelector.process(fileName));
            anchor.setReference(baseUrl + fileLink);
            Paragraph fileParagraph = new Paragraph();
            fileParagraph.add(anchor);
            answer.add(fileParagraph);
          }
        } else if (answerData.encodedFileKey().isPresent()) {
          String encodedFileKey = answerData.encodedFileKey().get();
          String fileLink = (isAdmin
              ? controllers.routes.FileController.acledAdminShow(encodedFileKey)
              : controllers.routes.FileController.show(applicantId, encodedFileKey))
              .url();
          Anchor anchor = new Anchor();
          anchor.add(linkFontSelector.process(answerData.answerText()));
          anchor.setReference(baseUrl + fileLink);
          answer = new Paragraph();
          answer.add(anchor);
        } else {
          answer = new Paragraph(paragraphFontSelector.process(answerData.answerText()));
        }
        LocalDate date = Instant.ofEpochMilli(answerData.timestamp())
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
        Paragraph time = new Paragraph(smallGrayFontSelector.process("Answered on : " + date));
        time.setAlignment(Paragraph.ALIGN_RIGHT);
        Paragraph eligibility = new Paragraph();
        if (isAdmin && isEligibilityEnabledInProgram) {
          try {
            Optional<EligibilityDefinition> eligibilityDef = programDefinition.getBlockDefinition(answerData.blockId())
                .eligibilityDefinition();
            if (eligibilityDef
                .map(
                    definition -> definition
                        .predicate()
                        .getQuestions()
                        .contains(answerData.questionDefinition().getId()))
                .orElse(false)) {

              String eligibilityText = answerData.isEligible() ? "Meets eligibility" : "Doesn't meet eligibility";
              eligibility = new Paragraph(smallGrayFontSelector.process(eligibilityText));
              eligibility.setAlignment(Paragraph.ALIGN_RIGHT);
            }
          } catch (ProgramBlockDefinitionNotFoundException e) {
            throw new RuntimeException(e);
          }
        }

        document.add(question);
        document.add(answer);
        document.add(time);
        if (!eligibility.isEmpty()) {
          document.add(eligibility);
        }
      }
      if (!answersOnlyHidden.isEmpty()) {
        document.add(Chunk.NEWLINE);
        Paragraph hiddenText = new Paragraph(h2FontSelector.process("Hidden Questions : "));
        document.add(hiddenText);
        document.add(Chunk.NEWLINE);
        for (AnswerData answerData : answersOnlyHidden) {
          Paragraph question = new Paragraph(paragraphFontSelector.process(answerData.questionDefinition().getName()));
          final Paragraph answer;
          answer = new Paragraph(paragraphFontSelector.process(answerData.answerText()));
          document.add(question);
          document.add(answer);
        }
      }
    } finally {
      document.close();
      writer.close();
      byteArrayOutputStream.close();
    }
    return byteArrayOutputStream.toByteArray();
  }

  /**
   * Generates a byte array containing all the blocks and questions in
   * {@code programDefinition}.
   * This function creates the output document in memory as a byte[] and is part
   * of the inMemoryPDF
   * object.
   *
   * @param allQuestions a list of all questions in the question bank. Used for
   *                     displaying
   *                     predicates correctly.
   */
  public ImmutableList<InMemoryPdf> exportProgram(
      ProgramDefinition programDefinition,
      ImmutableList<QuestionDefinition> allQuestions,
      boolean expandedFormLogicEnabled) throws DocumentException, IOException, TranslationNotFoundException {

    LocalDateTime timeCreated = nowProvider.get();
    ImmutableList.Builder<InMemoryPdf> pdfListBuilder = ImmutableList.builder();

    // Use the concrete type (e.g., Locale or String) instead of var, and separate
    // with a colon
    for (var preferredLocale : languageUtils.getApplicantEnabledLanguages()) {

      String filename = String.format("%s-%s.pdf",
          programDefinition.adminName(),
          preferredLocale.language());

      byte[] bytes = buildProgramPdf(
          programDefinition,
          allQuestions,
          timeCreated,
          expandedFormLogicEnabled,
          preferredLocale);

      System.out.println("Generated PDF for " + filename);
      // Add each generated PDF to the builder instead of returning early
      pdfListBuilder.add(new InMemoryPdf(bytes, filename));
    }

    // Return the completed list after processing all available locales
    return pdfListBuilder.build();
  }

  private byte[] buildProgramPdf(
    ProgramDefinition programDefinition,
    ImmutableList<QuestionDefinition> allQuestions,
    LocalDateTime timeCreated,
    boolean expandedFormLogicEnabled, Lang prefferedLocale)
      throws DocumentException, IOException {
    ByteArrayOutputStream byteArrayOutputStream = null;
    PdfWriter writer = null;
    Document document = null;
    try {
      byteArrayOutputStream = new ByteArrayOutputStream();
      document = new Document();
      writer = PdfWriter.getInstance(document, byteArrayOutputStream);
      document.open();

      document.add(new Paragraph(h1FontSelector.process(programDefinition.localizedName().getOrDefault(prefferedLocale.toLocale()))));
      document.add(new Paragraph(smallGrayFontSelector.process("Admin name: " + programDefinition.adminName())));
      document.add(
          new Paragraph(smallGrayFontSelector.process(
              "Admin description: " + programDefinition.adminDescription())));
      document.add(
          new Paragraph(smallGrayFontSelector.process(
              "Admin short description: "
                  + programDefinition.localizedShortDescription().getOrDefault(prefferedLocale.toLocale()))));
      document.add(
          new Paragraph(smallGrayFontSelector.process(
              "Admin long description: " + programDefinition.localizedDescription().getOrDefault(prefferedLocale.toLocale()))));
      document.add(
          new Paragraph(smallGrayFontSelector.process(
              "Time of export: "
                  + timeCreated.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")))));
      document.add(new Paragraph(smallGrayFontSelector.process("Origin of export: " + baseUrl)));

      document.add(Chunk.NEWLINE);
      document.add(new LineSeparator());
      Paragraph applicationSteps = new Paragraph();
      programDefinition.applicationSteps().stream()
          .forEach(
              step -> {
                applicationSteps.add(
                    new Paragraph(smallGrayFontSelector.process(
                        step.getTitle().getOrDefault(prefferedLocale.toLocale()) + " : " + step.getDescription().getOrDefault(prefferedLocale.toLocale()))));
              });
      document.add(new Paragraph(paragraphFontSelector.process("Application steps")));
      document.add(applicationSteps);
      document.add(Chunk.NEWLINE);
      document.add(new LineSeparator());
      document.add(new Paragraph(paragraphFontSelector.process("Application confirmation message")));
      document.add(
          new Paragraph(smallGrayFontSelector.process(
              programDefinition.localizedConfirmationMessage().getOrDefault(prefferedLocale.toLocale()))));
      document.add(Chunk.NEWLINE);
      document.add(new LineSeparator());
      document.add(new Paragraph(paragraphFontSelector.process("Statuses")));
      document.add(Chunk.NEWLINE);
      StatusDefinitions statusDefinitions =
          statusService.lookupActiveStatusDefinitions(programDefinition.adminName());

      if (!statusDefinitions.getStatuses().isEmpty()) {

        for (StatusDefinitions.Status status : statusDefinitions.getStatuses()) {
          if (status.computedDefaultStatus()) {
            document.add(new Paragraph(smallGrayFontSelector.process("(Default status)")));
          }
          document.add(new Paragraph(paragraphFontSelector.process(status.localizedStatusText().getOrDefault(prefferedLocale.toLocale()))));
          if (status.localizedEmailBodyText().isPresent()) {
            document.add(
                new Paragraph(smallGrayFontSelector.process(status.localizedEmailBodyText().get().getOrDefault(prefferedLocale.toLocale()))));
          }
          document.add(Chunk.NEWLINE);
        }
      }
      for (BlockDefinition block : programDefinition.getNonRepeatedBlockDefinitions()) {
        renderProgramBlock(
            document,
            programDefinition,
            block,
            allQuestions,
            /* indentationLevel= */ 0,
            expandedFormLogicEnabled,prefferedLocale);
      }
    } finally {
      if (document != null) {
        document.close();
      }
      if (writer != null) {
        writer.close();
      }
      if (byteArrayOutputStream != null) {
        byteArrayOutputStream.close();
      }
    }
    return byteArrayOutputStream.toByteArray();
  }

  /**
   * Renders the given block in the program preview PDF.
   *
   * @param indentationLevel the level of indentation. Should be 0 for most
   *                         questions, 1 for
   *                         questions nested under an enumerator, 2 for
   *                         doubly-nested enumerator questions, etc. Should
   *                         be multiplied by {@code INDENTATION_PER_LEVEL} when
   *                         adding text into the PDF.
   */
  private void renderProgramBlock(
      Document document,
      ProgramDefinition program,
      BlockDefinition block,
      ImmutableList<QuestionDefinition> allQuestions,
      int indentationLevel,
      boolean expandedFormLogicEnabled, Lang prefferedLocale)
      throws DocumentException {
    LineSeparator ls = new LineSeparator();
    ls.setAlignment(Element.ALIGN_RIGHT);
    // Approximately indent the line separator by subtracting 5% of its width per
    // level of
    // indentation.
    ls.setPercentage(100 - (indentationLevel * 5));
    document.add(ls);
    document.add(Chunk.NEWLINE);

    // Block-level information
    document.add(text(block.name(), h2FontSelector, indentationLevel));
    document.add(
        text("Admin description: " + block.description(), smallGrayFontSelector, indentationLevel));
    document.add(Chunk.NEWLINE);

    // Visibility & eligibility information
    if (block.visibilityPredicate().isPresent()) {
      renderPredicate(
          document,
          block.visibilityPredicate().get(),
          block,
          allQuestions,
          indentationLevel,
          expandedFormLogicEnabled);
    }
    if (block.eligibilityDefinition().isPresent()) {
      renderPredicate(
          document,
          block.eligibilityDefinition().get().predicate(),
          block,
          allQuestions,
          indentationLevel,
          expandedFormLogicEnabled);
    }

    for (int i = 0; i < block.getQuestionCount(); i++) {
      // Question-level information
      QuestionDefinition question = block.getQuestionDefinition(i);
      document.add(
          text(question.getQuestionText().getOrDefault(prefferedLocale.toLocale()), h3FontSelector, indentationLevel));
      if (!question.getQuestionHelpText().isEmpty()) {
        document.add(
            text(question.getQuestionHelpText().getOrDefault(prefferedLocale.toLocale()), paragraphFontSelector,
                indentationLevel));
      }

      // Adds a line describing whether the question is optional or not
      Optional<ProgramQuestionDefinition> programQuestionDefinition = block.programQuestionDefinitions().stream()
          .filter(pqd -> pqd.id() == question.getId())
          .findFirst();
      if (programQuestionDefinition.isPresent()) {
        document.add(
            text(
                programQuestionDefinition.get().optional()
                    ? "Optional Question"
                    : "Required Question",
                smallGrayFontSelector,
                indentationLevel));
      }

      document.add(text("Admin name: " + question.getName(), smallGrayFontSelector, indentationLevel));
      document.add(
          text(
              "Admin description: " + question.getDescription(),
              smallGrayFontSelector,
              indentationLevel));
      document.add(
          text(
              "Question type: " + question.getQuestionType().name(),
              smallGrayFontSelector,
              indentationLevel));

      // If a question offers options, put those options in the PDF
      if (question.getQuestionType().isMultiOptionType()) {
        MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) question;
        List list = createList(indentationLevel);
        for (QuestionOption option : multiOption.getDisplayableOptions()) {
          list.add(new ListItem(
              paragraphFontSelector.process(option.optionText().getOrDefault(prefferedLocale.toLocale()))));
        }
        document.add(list);
      } else if (question.getQuestionType() != QuestionType.STATIC) {
        // For questions without options, print an empty box area to indicate
        // that the user will write in a custom answer to the question.
        // (Static questions don't require answers from users, so don't add the empty
        // box area for
        // static questions.)
        document.add(text("[                     ]", paragraphFontSelector, indentationLevel));
      }
      document.add(Chunk.NEWLINE);
    }

    if (block.hasEnumeratorQuestion()) {
      for (BlockDefinition subBlock : program.getBlockDefinitionsForEnumerator(block.id())) {
        // Indent the blocks related to the enumerator so it's clear they're related
        renderProgramBlock(
            document,
            program,
            subBlock,
            allQuestions,
            indentationLevel + 1,
            expandedFormLogicEnabled, prefferedLocale);
      }
    }
  }

  private void renderPredicate(
      Document document,
      PredicateDefinition predicate,
      BlockDefinition block,
      ImmutableList<QuestionDefinition> allQuestions,
      int indentationLevel,
      boolean expandedFormLogicEnabled)
      throws DocumentException {
    ReadablePredicate readablePredicate = PredicateUtils.getReadablePredicateDescription(
        block.name(), predicate, allQuestions, expandedFormLogicEnabled);

    document.add(text(readablePredicate.heading(), predicateFontSelector, indentationLevel));
    if (readablePredicate.conditionList().isPresent()) {
      List list = createList(indentationLevel);
      for (String condition : readablePredicate.conditionList().get()) {
        list.add(new ListItem(predicateFontSelector.process(condition)));
      }
      document.add(list);
    }
    document.add(Chunk.NEWLINE);
  }

  /** Gets a paragraph with the given text, font, and indentation level. */
  private Paragraph text(String text, FontSelector fontSelector, int indentationLevel) {
    Paragraph paragraph = new Paragraph(fontSelector.process(text));
    paragraph.setIndentationLeft(indentationLevel * INDENTATION_PER_LEVEL);
    return paragraph;
  }

  private static List createList(int indentationLevel) {
    List list = new List();
    list.setIndentationLeft(indentationLevel * INDENTATION_PER_LEVEL);
    return list;
  }

  public static final class InMemoryPdf {
    private final byte[] byteArray;
    private final String fileName;

    InMemoryPdf(byte[] byteArray, String fileName) {
      this.byteArray = byteArray;
      this.fileName = fileName;
    }

    public byte[] getByteArray() {
      return byteArray;
    }

    public String getFileName() {
      return fileName;
    }
  }
}
