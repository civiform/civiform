package support.thymeleaf;

/**
 * Stands in for the question page view models in {@code .thtest} context sections.
 *
 * <p>Fragments that generate their own field ids call {@code model.randomFieldId()}. The real
 * implementations return random ids, which can't be asserted on, so this one hands out {@code
 * field-1}, {@code field-2}, ... in call order.
 */
public final class FakeFieldIdModel {

  private int nextId = 0;

  public String randomFieldId() {
    nextId++;
    return "field-" + nextId;
  }
}
