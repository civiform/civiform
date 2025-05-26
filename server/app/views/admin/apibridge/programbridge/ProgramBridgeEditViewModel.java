package views.admin.apibridge.programbridge;

import play.data.DynamicForm;

public record ProgramBridgeEditViewModel(Long id, String bridgeDefinitions) {
  public static ProgramBridgeEditViewModel create(DynamicForm form) {
    return new ProgramBridgeEditViewModel(
        Long.parseLong(form.get("id")), form.get("bridgeDefinitions"));
  }
}
