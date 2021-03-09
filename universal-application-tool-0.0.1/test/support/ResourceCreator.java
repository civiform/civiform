package support;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.Applicant;
import models.Program;
import models.Question;
import play.inject.Injector;
import services.Path;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.question.QuestionDefinition;
import services.question.QuestionService;
import services.question.TextQuestionDefinition;

public class ResourceCreator {

  private final ProgramService programService;
  private final QuestionService questionService;

  public ResourceCreator(Injector injector) {
    this.programService = injector.instanceOf(ProgramService.class);
    this.questionService = injector.instanceOf(QuestionService.class);
  }

  public Question insertQuestion(String pathString) {
    return insertQuestion(pathString, 1L);
  }

  public Question insertQuestion(String pathString, long version) {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            version, "", Path.create(pathString), "", ImmutableMap.of(), ImmutableMap.of());
    Question question = new Question(definition);
    question.save();
    return question;
  }

  public QuestionDefinition insertQuestionDefinition() {
    return questionService
        .create(
            new TextQuestionDefinition(
                1L,
                "question name",
                Path.create("applicant.my.path.name"),
                "description",
                ImmutableMap.of(Locale.ENGLISH, "question?"),
                ImmutableMap.of(Locale.ENGLISH, "help text")))
        .getResult();
  }

  public Program insertProgram(String name) {
    Program program = new Program(name, "description");
    program.save();
    return program;
  }

  public Program insertProgram(String name, BlockDefinition block) {
    Program program = new Program(name, "description");

    program.save();

    ProgramDefinition programDefinition =
        program.getProgramDefinition().toBuilder()
            .setBlockDefinitions(ImmutableList.of(block))
            .build();
    program = programDefinition.toProgram();
    program.update();

    return program;
  }

  public ProgramDefinition insertProgramWithOneBlock(String name) {
    try {
      ProgramDefinition programDefinition = programService.createProgramDefinition(name, "desc");
      programDefinition =
          programService.addQuestionsToBlock(
              programDefinition.id(), 1L, ImmutableList.of(insertQuestionDefinition().getId()));

      return programDefinition;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Applicant insertApplicant() {
    Applicant applicant = new Applicant();
    applicant.save();
    return applicant;
  }
}
