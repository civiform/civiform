package modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import services.ObjectMapperProvider;

/**
 * Replaces the default Play ObjectMapper configuration with a customized version that includes
 * support for additional common types such as: {@link java.util.Optional}, {@link
 * com.google.common.collect.ImmutableList}, {@link com.google.common.collect.ImmutableMap}.
 *
 * <p>Classes that need an ObjectMapper should get it via Dependency Injection and not create new
 * instances of their own.
 *
 * <p>See Play docs <a
 * href="https://www.playframework.com/documentation/3.0.x/JavaJsonActions#Custom-binding-for-ObjectMapper">on
 * Custom binding for ObjectMapper</a>.
 */
public class ObjectMapperModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class).asEagerSingleton();
  }
}
