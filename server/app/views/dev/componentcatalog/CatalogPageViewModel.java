package views.dev.componentcatalog;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import helpers.Pair;
import lombok.Builder;
import views.admin.BaseViewModel;
import views.dev.componentcatalog.fragments.AlertControl;
import views.dev.componentcatalog.fragments.ButtonControl;
import views.dev.componentcatalog.fragments.CheckboxControl;
import views.dev.componentcatalog.fragments.InputControl;
import views.dev.componentcatalog.fragments.RadioControl;
import views.dev.componentcatalog.fragments.SelectControl;
import views.dev.componentcatalog.fragments.TextareaControl;

@Builder
public record CatalogPageViewModel(String controlName) implements BaseViewModel {
  private record ControlInfo(String templateFileName, String label, Object controlModel) {}

  private static ImmutableMap<String, ControlInfo> e =
      ImmutableMap.of(
//          "alert",
//          new ControlInfo("AlertControl", "Alert Control", new AlertControl()),
          "button",
          new ControlInfo("ButtonControl", "Button Control", new ButtonControl()),
          "checkbox",
          new ControlInfo("CheckboxControl", "Checkbox Control", new CheckboxControl()),
          "input",
          new ControlInfo("InputControl", "Input Control", new InputControl()),
          "radio",
          new ControlInfo("RadioControl", "Radio Control", new RadioControl()),
          "select",
          new ControlInfo("SelectControl", "Select Control", new SelectControl()),
          "textarea",
          new ControlInfo("TextareaControl", "Textarea Control", new TextareaControl()));

  public ImmutableList<Pair> getUrls() {
    return e.entrySet().stream()
        .map(
            x ->
                new Pair<>(
                    x.getValue().label(),
                    controllers.dev.routes.ComponentCatalogController.index2(x.getKey()).url()))
        .collect(ImmutableList.toImmutableList());
  }

  public String getTemplateFileName() {
    return e.get(controlName()).templateFileName();
  }

  // In your model class or a utility class
  public Object getControlModel() {
    try {
      return e.get(controlName()).controlModel();
    } catch (RuntimeException e) {
      var msg = "Cannot find getter for: %s. Added the new control wrong or on wrong url.".formatted(controlName());
      throw new RuntimeException(msg, e);
    }
  }
}
