package views.dev.componentcatalog;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import views.admin.BaseViewModel;
import views.dev.componentcatalog.fragments.ButtonControl;
import views.dev.componentcatalog.fragments.CheckboxControl;
import views.dev.componentcatalog.fragments.InputControl;
import views.dev.componentcatalog.fragments.RadioControl;
import views.dev.componentcatalog.fragments.SelectControl;
import views.dev.componentcatalog.fragments.TextareaControl;

@Builder
public record CatalogPageViewModel(String controlName) implements BaseViewModel {
  public record ControlInfo(String controlName, String label, Object controlModel) {
    public String getUrl() {
      return controllers.dev.routes.ComponentCatalogController.controlIndex(controlName).url();
    }

    public String getTemplateFileName() {
      return controlModel.getClass().getSimpleName();
    }
  }

  private static ImmutableList<ControlInfo> controlList =
      ImmutableList.of(
          new ControlInfo("button", "Button Component", new ButtonControl()),
          new ControlInfo("checkbox", "Checkbox Component", new CheckboxControl()),
          new ControlInfo("input", "Input Component", new InputControl()),
          new ControlInfo("radio", "Radio Component", new RadioControl()),
          new ControlInfo("select", "Select Component", new SelectControl()),
          new ControlInfo("textarea", "Textarea Component", new TextareaControl()));

  public ImmutableList<ControlInfo> getControlList() {
    return controlList;
  }

  public static boolean controlExists(String controlName) {
    return controlList.stream().anyMatch(x -> x.controlName().equalsIgnoreCase(controlName));
  }

  /** Get the {@link ControlInfo} for the current controlName */
  private ControlInfo getControlInfo() {
    try {
      return controlList.stream()
          .filter(x -> x.controlName.equalsIgnoreCase(controlName()))
          .findFirst()
          .orElseThrow();
    } catch (RuntimeException ex) {
      var msg = "Cannot find control for: '%s'".formatted(controlName());
      throw new RuntimeException(msg, ex);
    }
  }

  /** Get the template file name of the control to load */
  public String getTemplateFileName() {
    return getControlInfo().getTemplateFileName();
  }

  /** Get the label of the control to load */
  public String getLabel() {
    return getControlInfo().label();
  }

  /** Get the control configuration object */
  public Object getControlModel() {
    return getControlInfo().controlModel();
  }
}
