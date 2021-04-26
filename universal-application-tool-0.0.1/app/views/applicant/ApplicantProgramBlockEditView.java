package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.body;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.p;

import com.google.auto.value.AutoValue;
import com.google.inject.Inject;
import controllers.applicant.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Http.HttpVerbs;
import play.twirl.api.Content;
import services.applicant.Block;
import services.applicant.question.ApplicantQuestion;
import views.BaseHtmlView;
import views.components.ToastMessage;
import views.questiontypes.ApplicantQuestionRendererFactory;

public final class ApplicantProgramBlockEditView extends BaseHtmlView {

  private final ApplicantLayout layout;
  private final ApplicantQuestionRendererFactory applicantQuestionRendererFactory;

  @Inject
  public ApplicantProgramBlockEditView(
      ApplicantLayout layout, ApplicantQuestionRendererFactory applicantQuestionRendererFactory) {
    this.layout = checkNotNull(layout);
    this.applicantQuestionRendererFactory = checkNotNull(applicantQuestionRendererFactory);
  }

  public Content render(Params params) {
    String formAction =
        routes.ApplicantProgramBlocksController.update(
                params.applicantId(), params.programId(), params.block().getId(), params.inReview())
            .url();
    ContainerTag body =
        body()
            .with(h1(params.block().getName()))
            .with(p(params.block().getDescription()))
            .with(
                form()
                    .withAction(formAction)
                    .withMethod(HttpVerbs.POST)
                    .with(makeCsrfTokenInputTag(params.request()))
                    .with(each(params.block().getQuestions(), this::renderQuestion))
                    .with(submitButton(params.messages().at("button.nextBlock"))));

    if (!params.preferredLanguageSupported()) {
      body.with(
          renderLocaleNotSupportedToast(
              params.applicantId(), params.programId(), params.messages()));
    }

    return layout.render(body);
  }

  private Tag renderQuestion(ApplicantQuestion question) {
    return applicantQuestionRendererFactory.getRenderer(question).render();
  }

 /**
   * If the applicant's preferred language is not supported for this program, render a toast
   * warning. Allow them to dismiss the warning, and once it is dismissed it does not reappear for
   * the same program.
   */
  private ContainerTag renderLocaleNotSupportedToast(
      long applicantId, long programId, Messages messages) {
    // Note: we include applicantId and programId in the ID, so that the applicant sees the warning
    // for each program that is not properly localized. Otherwise, once dismissed, this toast would
    // never appear for other programs. Additionally, including the applicantId ensures that this
    // warning still appears across applicants, so that (for example) a Trusted Intermediary
    // handling multiple applicants will see the toast displayed.
    return ToastMessage.warning(messages.at("toast.localeNotSupported"))
        .setId(String.format("locale-not-supported-%d-%d", applicantId, programId))
        .setDismissible(true)
        .setIgnorable(true)
        .setDuration(0)
        .getContainerTag();
  }

  @AutoValue
  public abstract static class Params {
    public static Builder builder() {
      return new AutoValue_ApplicantProgramBlockEditView_Params.Builder();
    }

    abstract boolean inReview();

    abstract Http.Request request();

    abstract Messages messages();

    abstract long applicantId();

    abstract long programId();

    abstract Block block();

    abstract boolean preferredLanguageSupported();

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setRequest(Http.Request request);

      public abstract Builder setInReview(boolean inReview);

      public abstract Builder setMessages(Messages messages);

      public abstract Builder setApplicantId(long applicantId);

      public abstract Builder setProgramId(long programId);

      public abstract Builder setBlock(Block block);

      public abstract Builder setPreferredLanguageSupported(boolean preferredLanguageSupported);

      public abstract Params build();
    }
  }
}
