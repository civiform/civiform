import play.sbt.PlayRunHook
import sbt.File

import scala.sys.process.Process

object TailwindBuilder {
  def apply(base: File): PlayRunHook = {
    object TailwindBuilderHook extends PlayRunHook {
      var processTailwind: Option[Process] = None
      var processViteBuild: Option[Process] = None

      override def beforeStarted() = {
        processTailwind = Option(
          Process(
            "npx tailwindcss build -i ./app/assets/stylesheets/styles.css -o ./public/stylesheets/tailwind.css --watch=always",
            base
          ).run()
        )

        processViteBuild = Option(Process("npm run build", base).run())
      }

      override def afterStopped() = {
        processTailwind.foreach(_.destroy())
        processTailwind = None

        processViteBuild.foreach(_.destroy())
        processViteBuild = None
      }
    }

    TailwindBuilderHook
  }
}
