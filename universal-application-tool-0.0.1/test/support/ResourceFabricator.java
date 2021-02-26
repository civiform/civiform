package support;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.Applicant;
import models.Program;
import models.Question;
import play.inject.Injector;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramService;
import services.question.NameQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.QuestionService;
import services.question.TextQuestionDefinition;

public class ResourceFabricator {

  private final Injector injector;
  private final ProgramService programService;
  private final QuestionService questionService;

  public ResourceFabricator(Injector injector) {
    this.injector = checkNotNull(injector);
    this.programService = injector.instanceOf(ProgramService.class);
    this.questionService = injector.instanceOf(QuestionService.class);
  }

  public <T> T instanceOf(Class<T> clazz) {
    return injector.instanceOf(clazz);
  }

  public Question insertQuestion(String path) {
    return insertQuestion(path, 1L);
  }

  public Question insertQuestion(String path, long version) {
    QuestionDefinition definition =
            new TextQuestionDefinition(version, "", path, "", ImmutableMap.of(), ImmutableMap.of());
    Question question = new Question(definition);
    question.save();
    return question;
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
            program.getProgramDefinition().toBuilder().addBlockDefinition(block).build();
    program = programDefinition.toProgram();
    program.update();

    return program;
  }

  public ProgramDefinition insertProgramWithOneBlock(String name) {
    try {
      ProgramDefinition programDefinition =
          programService.createProgramDefinition(name, "desc");
      programDefinition =
          programService.addBlockToProgram(
              programDefinition.id(), "test block", "test block description");
      programDefinition =
          programService.setBlockQuestions(
              programDefinition.id(),
              programDefinition.blockDefinitions().get(0).id(),
              ImmutableList.of(ProgramQuestionDefinition.create(insertQuestion("my.path"))));

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
