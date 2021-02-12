package services;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.lang.RuntimeException;

class ErrorAnd<T, E> {
  private final ImmutableSet<E> errors;
  private final T wrapped;

  public ErrorAnd(ImmutableSet<E> errors, T wrapped) {
    this.errors = errors;
    this.wrapped = checkNotNull(wrapped);
  }

  public ErrorAnd(T wrapped) {
    this.wrapped = checkNotNull(wrapped);
    this.errors = null;
  }

  public T getWrapped() {
    return wrapped;
  }

  public boolean isError() {
    return errors != null;
  }

  public E getErrors() {
    if (errors == null) {
      throw new RuntimeException("There are no errors");
    }

    return errors;
  }
}
