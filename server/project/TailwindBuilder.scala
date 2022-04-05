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
          Process("ls node_modules/grunt-cli/bin/grunt", base).run()
        )

        process = Option(
          Process("npx grunt dist", base).run()
        )
      }

      //override def afterStarted() = {
      //  process = Some(
      //    Process("grunt watch", base).run()
      //  )
      //}

      override def afterStopped() = {
        process.foreach(_.destroy())
        process = None
      }
    }

    TailwindBuilderHook
  }
}
