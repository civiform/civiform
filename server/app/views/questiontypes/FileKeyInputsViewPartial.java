package views.questiontypes;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.applicant.question.FileUploadQuestion;

/**
 * Renders the {@code file-key-field} and {@code file-list-display} fragments from {@link
 * views.questiontypes.FileUploadQuestionFragment} as HTMX out-of-band partials. Used by {@code
 * hxSelectFileForUpload} so newly-uploaded files are reflected in both the hidden Continue-form
 * inputs and the visible file list without a full page reload.
 */
public final class FileKeyInputsViewPartial {

  private static final String TEMPLATE = "questiontypes/FileUploadQuestionFragment";

  private final TemplateEngine templateEngine;
  private final ThymeleafModule.PlayThymeleafContextFactory contextFactory;

  @Inject
  public FileKeyInputsViewPartial(
      TemplateEngine templateEngine, ThymeleafModule.PlayThymeleafContextFactory contextFactory) {
    this.templateEngine = templateEngine;
    this.contextFactory = contextFactory;
  }

  public String renderOob(FileUploadQuestion fileUploadQuestion) {
    ThymeleafModule.PlayThymeleafContext context = contextFactory.create();
    context.setVariable("fileUploadQuestion", fileUploadQuestion);
    context.setVariable("clearData", false);
    context.setVariable("oob", true);
    String inputs = templateEngine.process(TEMPLATE, ImmutableSet.of("file-key-field"), context);
    String list = templateEngine.process(TEMPLATE, ImmutableSet.of("file-list-display"), context);
    return inputs + list;
  }
}
