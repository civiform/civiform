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
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.typesafe.config.Config;
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

/** PdfExporter is meant to generate PDF files. */
public final class PdfExporter {
  private final ApplicantService applicantService;
  private final Provider<LocalDateTime> nowProvider;
  private final String baseUrl;
  private final DateConverter dateConverter;

  // A set of fonts that approximate various heading and text sizes to easily create visual
  // hierarchy in the PDF.
  private static final Font H1_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 30);
  private static final Font H2_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
  private static final Font H3_FONT = FontFactory.getFont(FontFactory.HELVETICA, 16);
  private static final Font PARAGRAPH_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12);
  private static final Font SMALL_GRAY_FONT =
      FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
  private static final Font PREDICATE_FONT =
      FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLUE);
  private static final Font LINK_FONT =
      FontFactory.getFont(FontFactory.HELVETICA, 11, Font.UNDERLINE, new BaseColor(0, 94, 162));

  /**
   * Similar to {@link views.admin.programs.ProgramBlocksView#INDENTATION_FACTOR_INCREASE_ON_LEVEL}:
   * For each level of enumerator question, add another layer of indentation so it's easier to
   * understand which questions are part of enumerators.
   */
  private static final int INDENTATION_PER_LEVEL = 25;

  @Inject
  PdfExporter(
      ApplicantService applicantService,
      @Now Provider<LocalDateTime> nowProvider,
      Config configuration,
      DateConverter dateConverter) {
    this.applicantService = checkNotNull(applicantService);
    this.nowProvider = checkNotNull(nowProvider);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
    this.dateConverter = checkNotNull(dateConverter);
  }

  /**
   * Generates a byte array containing all the values present in the List of AnswerData using
   * itextPDF. This function creates the output document in memory as a byte[] and is part of the
   * inMemoryPDF object. The InMemoryPdf object is passed back to the AdminController Class to
   * generate the required PDF.
   */
  public InMemoryPdf exportApplication(ApplicationModel application, boolean isAdmin)
      throws DocumentException, IOException {
    ReadOnlyApplicantProgramService roApplicantService =
        applicantService
            .getReadOnlyApplicantProgramService(application)
            .toCompletableFuture()
            .join();

    ImmutableList<AnswerData> answersOnlyActive = roApplicantService.getSummaryDataOnlyActive();
    ImmutableList<AnswerData> answersOnlyHidden = ImmutableList.<AnswerData>of();
    if (isAdmin) {
      answersOnlyHidden = roApplicantService.getSummaryDataOnlyHidden();
    }

    // We expect a name to be present at this point. However, if it's not, we use a placeholder
    // rather than throwing an error here.
    String applicantName =
        application.getApplicant().getApplicantDisplayName().orElse("name-unavailable");
    String applicantNameWithApplicationId = String.format("%s (%d)", applicantName, application.id);
    String filename = String.format("%s-%s.pdf", applicantNameWithApplicationId, nowProvider.get());
    byte[] bytes =
        buildApplicationPdf(
            answersOnlyActive,
            answersOnlyHidden,
            applicantNameWithApplicationId,
            application.getApplicant().id,
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

      Paragraph applicant =
          new Paragraph(
              applicantNameWithApplicationId, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
      Paragraph program =
          new Paragraph(
              "Program Name : " + programDefinition.adminName(),
              FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15));
      document.add(applicant);
      document.add(program);
      Paragraph status =
          new Paragraph(
              "Status: " + statusValue.orElse("none"),
              FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
      document.add(status);
      Paragraph submitTimeInformation =
          new Paragraph(
              "Submit Time: " + submitTime, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
      document.add(submitTimeInformation);
      document.add(Chunk.NEWLINE);
      boolean isEligibilityEnabledInProgram = programDefinition.hasEligibilityEnabled();
      for (AnswerData answerData : answersOnlyActive) {
        Paragraph question =
            new Paragraph(
                answerData.questionDefinition().getName(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
        final Paragraph answer;
        if (!answerData.encodedFileKeys().isEmpty()) {
          answer = new Paragraph();
          ImmutableList<String> encodedFileKeys = answerData.encodedFileKeys();
          for (int i = 0; i < encodedFileKeys.size(); i++) {
            String encodedFileKey = encodedFileKeys.get(i);
            String fileName = answerData.fileNames().get(i);
            String fileLink =
                (isAdmin
                        ? controllers.routes.FileController.acledAdminShow(encodedFileKey)
                        : controllers.routes.FileController.show(applicantId, encodedFileKey))
                    .url();
            Anchor anchor = new Anchor(fileName, LINK_FONT);
            anchor.setReference(baseUrl + fileLink);
            Paragraph fileParagraph = new Paragraph();
            fileParagraph.add(anchor);
            answer.add(fileParagraph);
          }
        } else if (answerData.encodedFileKey().isPresent()) {
          String encodedFileKey = answerData.encodedFileKey().get();
          String fileLink =
              (isAdmin
                      ? controllers.routes.FileController.acledAdminShow(encodedFileKey)
                      : controllers.routes.FileController.show(applicantId, encodedFileKey))
                  .url();
          Anchor anchor = new Anchor(answerData.answerText());
          anchor.setReference(baseUrl + fileLink);
          answer = new Paragraph();
          answer.add(anchor);
        } else {
          answer =
              new Paragraph(
                  answerData.answerText(), FontFactory.getFont(FontFactory.HELVETICA, 11));
        }
        LocalDate date =
            Instant.ofEpochMilli(answerData.timestamp())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        Paragraph time =
            new Paragraph("Answered on : " + date, FontFactory.getFont(FontFactory.HELVETICA, 10));
        time.setAlignment(Paragraph.ALIGN_RIGHT);
        Paragraph eligibility = new Paragraph();
        if (isAdmin && isEligibilityEnabledInProgram) {
          try {
            Optional<EligibilityDefinition> eligibilityDef =
                programDefinition.getBlockDefinition(answerData.blockId()).eligibilityDefinition();
            if (eligibilityDef
                .map(
                    definition ->
                        definition
                            .predicate()
                            .getQuestions()
                            .contains(answerData.questionDefinition().getId()))
                .orElse(false)) {

              String eligibilityText =
                  answerData.isEligible() ? "Meets eligibility" : "Doesn't meet eligibility";
              eligibility =
                  new Paragraph(eligibilityText, FontFactory.getFont(FontFactory.HELVETICA, 10));
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
        Paragraph hiddenText =
            new Paragraph(
                "Hidden Questions : ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15));
        document.add(hiddenText);
        document.add(Chunk.NEWLINE);
        for (AnswerData answerData : answersOnlyHidden) {
          Paragraph question =
              new Paragraph(
                  answerData.questionDefinition().getName(),
                  FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
          final Paragraph answer;
          answer =
              new Paragraph(
                  answerData.answerText(), FontFactory.getFont(FontFactory.HELVETICA, 11));
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
   * Generates a byte array containing all the blocks and questions in {@code programDefinition}.
   * This function creates the output document in memory as a byte[] and is part of the inMemoryPDF
   * object.
   *
   * @param allQuestions a list of all questions in the question bank. Used for displaying
   *     predicates correctly.
   */
  public InMemoryPdf exportProgram(
      ProgramDefinition programDefinition, ImmutableList<QuestionDefinition> allQuestions)
      throws DocumentException, IOException, TranslationNotFoundException {
    LocalDateTime timeCreated = nowProvider.get();
    String filename = String.format("%s-%s.pdf", programDefinition.adminName(), timeCreated);
    byte[] bytes = buildProgramPdf(programDefinition, allQuestions, timeCreated);
    return new InMemoryPdf(bytes, filename);
  }

  private byte[] buildProgramPdf(
      ProgramDefinition programDefinition,
      ImmutableList<QuestionDefinition> allQuestions,
      LocalDateTime timeCreated)
      throws DocumentException, IOException {
    ByteArrayOutputStream byteArrayOutputStream = null;
    PdfWriter writer = null;
    Document document = null;
    try {
      byteArrayOutputStream = new ByteArrayOutputStream();
      document = new Document();
      writer = PdfWriter.getInstance(document, byteArrayOutputStream);
      document.open();

      document.add(new Paragraph(programDefinition.localizedName().getDefault(), H1_FONT));
      document.add(new Paragraph("Admin name: " + programDefinition.adminName(), SMALL_GRAY_FONT));
      document.add(
          new Paragraph(
              "Admin description: " + programDefinition.adminDescription(), SMALL_GRAY_FONT));
      document.add(
          new Paragraph(
              "Admin short description: "
                  + programDefinition.localizedShortDescription().getDefault(),
              SMALL_GRAY_FONT));
      document.add(
          new Paragraph(
              "Time of export: "
                  + timeCreated.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")),
              SMALL_GRAY_FONT));
      document.add(new Paragraph("Origin of export: " + baseUrl, SMALL_GRAY_FONT));

      document.add(Chunk.NEWLINE);
      document.add(new LineSeparator());
      Paragraph applicationSteps = new Paragraph();
      programDefinition.applicationSteps().stream()
          .forEach(
              step -> {
                applicationSteps.add(
                    new Paragraph(
                        step.getTitle().getDefault() + " : " + step.getDescription().getDefault(),
                        SMALL_GRAY_FONT));
              });
      if (!applicationSteps.isEmpty()) {
        document.add(new Paragraph("Application steps", PARAGRAPH_FONT));
        document.add(applicationSteps);
      }
      document.add(Chunk.NEWLINE);
      document.add(new LineSeparator());
      document.add(new Paragraph("Application confirmation message", PARAGRAPH_FONT));
      document.add(
          new Paragraph(
              programDefinition.localizedConfirmationMessage().getDefault(), SMALL_GRAY_FONT));

      for (BlockDefinition block : programDefinition.getNonRepeatedBlockDefinitions()) {
        renderProgramBlock(
            document, programDefinition, block, allQuestions, /* indentationLevel= */ 0);
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
   * @param indentationLevel the level of indentation. Should be 0 for most questions, 1 for
   *     questions nested under an enumerator, 2 for doubly-nested enumerator questions, etc. Should
   *     be multiplied by {@code INDENTATION_PER_LEVEL} when adding text into the PDF.
   */
  private void renderProgramBlock(
      Document document,
      ProgramDefinition program,
      BlockDefinition block,
      ImmutableList<QuestionDefinition> allQuestions,
      int indentationLevel)
      throws DocumentException {
    document.add(Chunk.NEWLINE);
    LineSeparator ls = new LineSeparator();
    ls.setAlignment(Element.ALIGN_RIGHT);
    // Approximately indent the line separator by subtracting 5% of its width per level of
    // indentation.
    ls.setPercentage(100 - (indentationLevel * 5));
    document.add(ls);
    document.add(Chunk.NEWLINE);

    // Block-level information
    document.add(text(block.name(), H2_FONT, indentationLevel));
    document.add(
        text("Admin description: " + block.description(), SMALL_GRAY_FONT, indentationLevel));
    document.add(Chunk.NEWLINE);

    // Visibility & eligibility information
    if (block.visibilityPredicate().isPresent()) {
      renderPredicate(
          document, block.visibilityPredicate().get(), block, allQuestions, indentationLevel);
    }
    if (block.eligibilityDefinition().isPresent()) {
      renderPredicate(
          document,
          block.eligibilityDefinition().get().predicate(),
          block,
          allQuestions,
          indentationLevel);
    }

    for (int i = 0; i < block.getQuestionCount(); i++) {
      // Question-level information
      QuestionDefinition question = block.getQuestionDefinition(i);
      document.add(text(question.getQuestionText().getDefault(), H3_FONT, indentationLevel));
      if (!question.getQuestionHelpText().isEmpty()) {
        document.add(
            text(question.getQuestionHelpText().getDefault(), PARAGRAPH_FONT, indentationLevel));
      }

      // Adds a line describing whether the question is optional or not
      Optional<ProgramQuestionDefinition> programQuestionDefinition =
          block.programQuestionDefinitions().stream()
              .filter(pqd -> pqd.id() == question.getId())
              .findFirst();
      if (programQuestionDefinition.isPresent()) {
        document.add(
            text(
                programQuestionDefinition.get().optional()
                    ? "Optional Question"
                    : "Required Question",
                SMALL_GRAY_FONT,
                indentationLevel));
      }

      document.add(text("Admin name: " + question.getName(), SMALL_GRAY_FONT, indentationLevel));
      document.add(
          text(
              "Admin description: " + question.getDescription(),
              SMALL_GRAY_FONT,
              indentationLevel));
      document.add(
          text(
              "Question type: " + question.getQuestionType().name(),
              SMALL_GRAY_FONT,
              indentationLevel));

      // If a question offers options, put those options in the PDF
      if (question.getQuestionType().isMultiOptionType()) {
        MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) question;
        List list = createList(indentationLevel);
        for (QuestionOption option : multiOption.getOptions()) {
          list.add(new ListItem(option.optionText().getDefault()));
        }
        document.add(list);
      } else if (question.getQuestionType() != QuestionType.STATIC) {
        // For questions without options, print an empty box area to indicate
        // that the user will write in a custom answer to the question.
        // (Static questions don't require answers from users, so don't add the empty box area for
        // static questions.)
        document.add(text("[                     ]", PARAGRAPH_FONT, indentationLevel));
      }
      document.add(Chunk.NEWLINE);
    }

    if (block.isEnumerator()) {
      for (BlockDefinition subBlock : program.getBlockDefinitionsForEnumerator(block.id())) {
        // Indent the blocks related to the enumerator so it's clear they're related
        renderProgramBlock(document, program, subBlock, allQuestions, indentationLevel + 1);
      }
    }
  }

  private void renderPredicate(
      Document document,
      PredicateDefinition predicate,
      BlockDefinition block,
      ImmutableList<QuestionDefinition> allQuestions,
      int indentationLevel)
      throws DocumentException {
    ReadablePredicate readablePredicate =
        PredicateUtils.getReadablePredicateDescription(block.name(), predicate, allQuestions);

    document.add(text(readablePredicate.heading(), PREDICATE_FONT, indentationLevel));
    if (readablePredicate.conditionList().isPresent()) {
      List list = createList(indentationLevel);
      for (String condition : readablePredicate.conditionList().get()) {
        list.add(new ListItem(condition, PREDICATE_FONT));
      }
      document.add(list);
    }
    document.add(Chunk.NEWLINE);
  }

  /** Gets a paragraph with the given text, font, and indentation level. */
  private static Paragraph text(String text, Font font, int indentationLevel) {
    Paragraph paragraph = new Paragraph(text, font);
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
