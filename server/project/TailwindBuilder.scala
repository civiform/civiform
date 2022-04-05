import play.sbt.PlayRunHook
import sbt.File

import scala.sys.process.Process

object TailwindBuilder {
  def apply(base: File): PlayRunHook = {
    object TailwindBuilderHook extends PlayRunHook {
      var process: Option[Process] = None

      override def beforeStarted() = {
        process = Option(
          Process("npx tailwindcss build -i ./app/assets/stylesheets/styles.css -o ./public/stylesheets/tailwind.css", base).run()
        )

        process = Option(
          Process("npx grunt dist", base).run()
        )

        process = Option(
          Process("npx grunt watch", base).run()
        )
      }

      override def afterStopped() = {
        process.foreach(_.destroy())
        process = None
      }
    }

    TailwindBuilderHook
  }
}
