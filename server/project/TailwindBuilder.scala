import play.sbt.PlayRunHook
import sbt.File

import scala.sys.process.Process

object TailwindBuilder {
  def apply(base: File): PlayRunHook = {
    object TailwindBuilderHook extends PlayRunHook {
      var process: Option[Process] = None

      override def beforeStarted() = {
        process = Option(
          Process(
            "npx tailwindcss build -i ./app/assets/stylesheets/styles.css -o ./public/stylesheets/tailwind.css --watch=always",
            base
          ).run()
        )
      }

      override def afterStopped() = stop()

      // Play calls onError, not afterStopped, when the server fails to start.
      override def onError() = stop()

      private def stop(): Unit = {
        process.foreach(_.destroy())
        process = None
      }
    }

    TailwindBuilderHook
  }
}
