package views.admin.programs;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.select;

import com.google.common.collect.ImmutableList;
import forms.ProgramTranslationForm;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import java.util.Locale;
import javax.inject.Inject;
import play.i18n.Langs;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;

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

  public Content render(ProgramTranslationForm form, ProgramDefinition program) {
    return layout.render();
  }

  private ContainerTag renderCurrentSupportedLanguages() {

  }

  private ContainerTag renderTranslationForm() {
    // Locale dropdown + display name + display description
    return div()
        .with(select().withName("locale").with(each(supportedLanguages, TagCreator::option)))
        .with(FieldWithLabel.input().setFieldName("displayName").getContainer())
        .with(FieldWithLabel.input().setFieldName("displayDescription").getContainer());
  }
}
