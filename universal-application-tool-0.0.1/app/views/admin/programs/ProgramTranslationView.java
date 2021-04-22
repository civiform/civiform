package views.admin.programs;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.select;

import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Langs;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.ToastMessage;

public class ProgramTranslationView extends BaseHtmlView {
  private final AdminLayout layout;
  private final ImmutableList<String> supportedLanguages;

  @Inject
  public ProgramTranslationView(AdminLayout layout, Langs langs) {
    this.layout = layout;
    this.supportedLanguages =
        langs.availables().stream()
            .map(lang -> lang.toLocale().getDisplayLanguage(Locale.US))
            .collect(toImmutableList());
  }

  public Content render(Http.Request request, long programId, Optional<String> errors) {
    ContainerTag form = renderTranslationForm(request, programId);
    errors.ifPresent(s -> form.with(ToastMessage.error(s).setDismissible(false).getContainerTag()));
    return layout.render(form);
  }

  private ContainerTag renderTranslationForm(Http.Request request, long programId) {
    return form()
        .withMethod("POST")
        .with(makeCsrfTokenInputTag(request))
        .withAction(
            controllers.admin.routes.AdminProgramTranslationsController.update(programId).url())
        .with(select().withName("locale").with(each(supportedLanguages, TagCreator::option)))
        .with(FieldWithLabel.input().setFieldName("displayName").getContainer())
        .with(FieldWithLabel.input().setFieldName("displayDescription").getContainer())
        .with(submitButton("Save"));
  }
}
