package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Locale;
import javax.inject.Inject;
import models.Applicant;
import models.Person;
import models.Program;
import models.Question;
import play.db.ebean.EbeanConfig;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramService;
import services.question.NameQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.QuestionService;
import views.J2HtmlDemoView;

public class J2HtmlDemoController extends Controller {

  private final J2HtmlDemoView view;
  private final QuestionService questionService;
  private final ProgramService programService;
  private final EbeanConfig ebeanConfig;

  private int counter;

  @Inject
  public J2HtmlDemoController(
      J2HtmlDemoView view,
      QuestionService questionService,
      ProgramService programService,
      EbeanConfig ebeanConfig) {
    this.view = checkNotNull(view);

    // DO NOT SUBMIT THE CHANGES TO THIS FILE.
    this.questionService = questionService;
    this.programService = programService;
    this.ebeanConfig = ebeanConfig;
    this.counter = 0;
  }

  public Result newOne(Request request) {
    this.truncateTables();
    insertApplicant();
    insertProgramWithOneBlock("Natalie and Caroline's Program", counter + "");
    counter++;
    return ok(view.render("Let's get started!", request));
  }

  public Result create(Request request) {
    return ok(request.toString());
  }

  public Program insertProgram(String name) {
    Program program = new Program(name, "description");
    program.save();
    return program;
  }

  public QuestionDefinition insertNameQuestionDefinition(String path) {
    return questionService
        .create(
            new NameQuestionDefinition(
                1L,
                "my name",
                path,
                "description",
                ImmutableMap.of(Locale.ENGLISH, "question?"),
                ImmutableMap.of(Locale.ENGLISH, "help text")))
        .get();
  }

  public ProgramDefinition insertProgramWithOneBlock(String name, String path) {
    try {
      ProgramDefinition programDefinition = programService.createProgramDefinition(name, "desc");
      programDefinition =
          programService.addBlockToProgram(
              programDefinition.id(), "test block", "test block description");
      programDefinition =
          programService.setBlockQuestions(
              programDefinition.id(),
              programDefinition.blockDefinitions().get(0).id(),
              ImmutableList.of(
                  ProgramQuestionDefinition.create(insertNameQuestionDefinition(path))));

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

  private void truncateTables() {
    EbeanServer server = Ebean.getServer(ebeanConfig.defaultServer());
    server.truncate(Applicant.class, Person.class, Program.class, Question.class);
  }
}
