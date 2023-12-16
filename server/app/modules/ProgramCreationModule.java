package modules;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import views.admin.programs.BlockListPartial;
import views.admin.programs.ProgramBlocksView;

/*
 * This class is a Guice module which handles bindings and dependency injections
 * related to the program creation and editing flow for civiform admins. The Guice
 * module is created when the Play application starts.
 **/
public class ProgramCreationModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder().build(ProgramBlocksView.Factory.class));
    install(new FactoryModuleBuilder().build(BlockListPartial.Factory.class));
  }
}
