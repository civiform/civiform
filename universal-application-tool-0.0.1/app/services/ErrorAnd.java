package services;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;

import java.util.Optional;

/**
 * Wraps two values, labeled "errors" and "result". Errors is intended to be an immutable collection
 * of error objects while result is the result of an action. If the action was successful only
 * result need be provided.
 */
public class ErrorAnd<T, E> {
  public static <T, E> ErrorAnd<T, E> error(ImmutableSet<E> errors) {
    return new ErrorAnd<>(errors);
  }

  public static <T, E> ErrorAnd<T, E> errorAnd(ImmutableSet<E> errors, T result) {
    return new ErrorAnd<>(errors, result);
  }

  public static <T, E> ErrorAnd<T, E> of(T result) {
    return new ErrorAnd<>(result);
  }

  private final ImmutableSet<E> errors;
  private final Optional<T> result;

  /** Constructor for the error case. */
  private ErrorAnd(ImmutableSet<E> errors) {
    this.errors = checkNotNull(errors);
    this.result = Optional.empty();
  }

  /** Constructor for the error case but when result is also useful. */
  private ErrorAnd(ImmutableSet<E> errors, T result) {
    this.errors = checkNotNull(errors);
    this.result = Optional.of(checkNotNull(result));
  }

  /** Constructor for the success case. */
  private ErrorAnd(T result) {
    this.errors = ImmutableSet.of();
    this.result = Optional.of(checkNotNull(result));
  }

  /** Returns true if there is a result */
  public boolean hasResult() {
    return result.isPresent();
  }

  /** Returns the result, throws a RuntimeException if there is no result. */
  public T getResult() {
    if (result.isEmpty()) {
      throw new RuntimeException("There is no result");
    }

    return result.get();
  }

  /** Returns true if there are errors. */
  public boolean isError() {
    return !errors.isEmpty();
  }

  /** Returns the errors. */
  public ImmutableSet<E> getErrors() {
    return errors;
  }
}
