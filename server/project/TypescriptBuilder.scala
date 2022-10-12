import com.typesafe.sbt.web.Import.WebKeys.webTarget
import com.typesafe.sbt.web.{PathMapping, incremental}
import com.typesafe.sbt.web.pipeline.Pipeline
import sbt.Keys.streams
import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web.incremental.{
  OpInputHash,
  OpInputHasher,
  OpResult,
  OpSuccess
}
import sbt.internal.util.ManagedLogger
import sbt.{AutoPlugin, File, Setting, sbtUnchecked, taskKey}

import scala.sys.process.{Process, ProcessLogger}

/** Custom plugin to compile TypeScript code. We are not using sbt-typescript
  * https://github.com/joost-de-vries/sbt-typescript plugin because it hasn't
  * been updated since 2018 and uses Typescript 2 while current Typescript
  * version is 4.
  *
  * Our custom plugin invokes `tsc` compiler directly. `tsconfig.json` is setup
  * to compile code from `app/assets/javascripts` directory and outputs result
  * to `target/web/typescript/javascripts` folder.
  *
  * This plugin introduces `compileTypescript` task which will be used as a
  * pipeline stage. Read about Asset Pipeline tasks
  * https://github.com/sbt/sbt-web#asset-pipeline The task implements Asset
  * Pipeline task and not Source File task because the latter doesn't allow us
  * to keep directory structure: we want that file `assets/javascripts/foo.ts`
  * be compiled to `assets/javascripts/foo.js`. So we are using Pipeline.Stage
  * for that.
  *
  * This plugin is enabled by adding `compileTypescript` as first task in
  * pipelineStages in build.sbt.
  */
object TypescriptBuilder extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport {
    // compileTypescript is the entry point of this plugin. It is a task
    // which is called by sbt-web when it prepares assets. This task
    // receives a seq of all assets (images, css, TS files), compiles TS files
    // and returns seq of all input assets plus newly compiled JS files.
    //
    // This is task declaration. Implementation is below.
    val compileTypescript = taskKey[Pipeline.Stage](
      "Compiles typescript from assets/javascript folder"
    )
  }
  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    compileTypescript := { inputFiles: Seq[PathMapping] =>
      val streamsVal = (Assets / streams).value: @sbtUnchecked
      // targetDir will contain compiled JS files.
      val targetDir = new File(webTarget.value, "typescript/javascripts")
      val cacheDir = new File(streamsVal.cacheDirectory, "run")
      val compiledJsFiles: Seq[File] = recompileTypescriptIfFilesChanged(
        inputFiles,
        targetDir,
        cacheDir,
        streamsVal.log
      )
      // transform compiles JS files to PathMapping and add "javascripts" as part of the file name.
      // That way JS files fille be added in /assets/javascripts/*.js in final output.
      val jsFiles: Seq[PathMapping] =
        compiledJsFiles.map({ f => (f, "javascripts/" + f.getName) })

      inputFiles ++ jsFiles
    }
  )

  /** Given list of all assets (css, image, TS files) figures out whether any TS
    * files have been changed and recompiles them. If no files have been changed
    * assumes JS files have been compiled at earlier iterations and returns
    * them.
    *
    * @param inputFiles
    *   All assets files passed by sbt-web.
    * @param targetDir
    *   Directory that contains compiled JS files.
    * @param cacheDir
    *   Directory that contains cache file, used by syncIncremental.
    * @param log
    *   Logger object to output compilation errors to sbt console.
    * @return
    *   Seq of compiled JS files and sourcemaps.
    */
  def recompileTypescriptIfFilesChanged(
    inputFiles: Seq[PathMapping],
    targetDir: File,
    cacheDir: File,
    log: ManagedLogger
  ): Seq[File] = {
    // function that provides hash for each file. Includes fileName + timestamp.
    val fileHasher: OpInputHasher[File] =
      OpInputHasher[File](f =>
        OpInputHash.hashString(f.getCanonicalPath + f.lastModified)
      )
    // we are interested only in TS files.
    val tsFiles = inputFiles map { p => p._1 } filter { f =>
      f.getName.endsWith(".ts")
    }
    // we are using sbt-web syncIncremental function. It's somewhat complicated. For our
    // use case we just want to see if any of TS files changed and if so - recompile
    // everything.
    val (_, compiledJsFiles) =
      incremental.syncIncremental(cacheDir, tsFiles)({ modifiedFiles =>
        if (modifiedFiles.nonEmpty || !targetDir.exists()) {
          log.info("Typescript files changed. Recompiling...")
          compileTypescriptInternal(targetDir, log)
        }
        val compiledJsFiles: Seq[File] = targetDir.listFiles()
        // ignore opResults. We are not using full functionality of syncIncremental
        // so we always return dummy opResults with OpSuccess for each TS file.
        val opResults: Map[File, OpResult] = modifiedFiles
          .map(f => f -> OpSuccess(Set.empty[File], Set.empty[File]))
          .toMap

        (opResults, compiledJsFiles)
      })(fileHasher)

    compiledJsFiles
  }

  /** Function that compiles TS files. It relies on tsconfig.json to contain all
    * necessary settings including which source files to compile. Note that we
    * don't pass input source files. Instead we simply compile everything in
    * server/app/assets/javascripts folder (check server/tsconfig.json).
    *
    * @param targetDir
    *   Directory in which compiled JS and sourcemap files will be added.
    * @param log
    *   Logger object to output compilation errors to sbt console.
    */
  def compileTypescriptInternal(targetDir: File, log: ManagedLogger) = {
    val compilationCommand =
      "npx tsc --pretty --project tsconfig.json --outDir " + targetDir
    val res = Process(compilationCommand) ! ProcessLogger(
      line => log.error(line),
      line => log.error(line)
    )
    if (res != 0) {
      log.info(
        "To debug TS code compilation can run the following command from 'server' folder:\n    " + compilationCommand
      )
      throw new sbt.MessageOnlyException(
        "TypeScript compilation failed. Check console to see compilation errors."
      )
    }
  }
}
