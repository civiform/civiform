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

/** Custom plugin to compile and bundle TypeScript and Sass using Webpack. We
  * are not using sbt-typescript
  * https://github.com/joost-de-vries/sbt-typescript plugin because it hasn't
  * been updated since 2018 and uses Typescript 2 while current Typescript
  * version is 4.
  *
  * Our custom plugin invokes `tsc` compiler directly through the Webpack
  * ts-loader. `tsconfig.json` and `webpack.config.js` are set up to compile
  * code from `app/assets/javascripts` and 'node_modules/@uswds/uswds/dist/js'
  * directories and output the result to `target/web/dist` folder. We also use
  * Webpack to compile the USWDS Sass into CSS.
  *
  * This plugin introduces `bundleWebAssets` task which will be used as a
  * pipeline stage. Read about Asset Pipeline tasks
  * https://github.com/sbt/sbt-web#asset-pipeline The task implements Asset
  * Pipeline task and not Source File task because the latter doesn't allow us
  * to keep directory structure: we want that file `assets/javascripts/foo.ts`
  * be compiled to `assets/javascripts/foo.js`. So we are using Pipeline.Stage
  * for that.
  *
  * This plugin is enabled by adding `bundleWebAssets` as first task in
  * pipelineStages in build.sbt.
  */
object WebAssetsBundler extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport {
    // bundleWebAssets is the entry point of this plugin. It is a task
    // which is called by sbt-web when it prepares assets. This task
    // receives a seq of all assets (images, css, TS files, Sass), compiles TS and
    // Sass files and returns seq of all input assets plus newly compiled JS
    // and CSS files.
    //
    // This is task declaration. Implementation is below.
    val bundleWebAssets =
      taskKey[Pipeline.Stage]("Uses Webpack to compile and bundle TypeScript, custom Sass files, and the USWDS Sass.")
  }
  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    bundleWebAssets := { inputFiles: Seq[PathMapping] =>
      val streamsVal = (Assets / streams).value: @sbtUnchecked
      // targetDir will contain compiled JS and CSS files.
      val targetDir = new File(webTarget.value, "dist")
      val cacheDir = new File(streamsVal.cacheDirectory, "run")
      val compiledFiles: Seq[File] =
        recompileIfFilesChanged(inputFiles, targetDir, cacheDir, streamsVal.log)
      // transform compiled JS and CSS files to PathMapping and adds "dist" as
      // part of the file name.  That way, the bundled files will be added in
      // /assets/dist/ in final output.
      val jsFiles: Seq[PathMapping] =
        compiledFiles.map({ f => (f, "dist/" + f.getName) })

      inputFiles ++ jsFiles
    }
  )

  /** Given list of all assets (css, image, TS, Sass files) figures out whether
    * any TS or Sass files have been changed and recompiles them. If no files
    * have been changed assumes JS and CSS files have been compiled at earlier
    * iterations and returns them.
    *
    * @param inputFiles
    *   All assets files passed by sbt-web.
    * @param targetDir
    *   Directory that contains compiled JS and CSS files.
    * @param cacheDir
    *   Directory that contains cache file, used by syncIncremental.
    * @param log
    *   Logger object to output compilation errors to sbt console.
    * @return
    *   Seq of compiled JS and CSS files and sourcemaps.
    */
  def recompileIfFilesChanged(
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
    // we only watch for changes in TS files.
    val changedFiles = inputFiles map { p => p._1 } filter { f =>
      f.getName.endsWith(".ts") || f.getName.endsWith(".scss")
    }
    // we are using sbt-web syncIncremental function. It's somewhat complicated. For our
    // use case we just want to see if any of TS or Sass files changed and if so - rerun Webpack
    // bundling.
    val (_, compiledFiles) =
      incremental.syncIncremental(cacheDir, changedFiles)({ modifiedFiles =>
        if (modifiedFiles.nonEmpty || !targetDir.exists()) {
          log.info("Typescript and/or Sass files changed. Recompiling...")
          runWebpack(targetDir, log)
        }
        val compiledFiles: Seq[File] = targetDir.listFiles()
        // ignore opResults. We are not using full functionality of syncIncremental
        // so we always return dummy opResults with OpSuccess for each compiled file.
        val opResults: Map[File, OpResult] = modifiedFiles
          .map(f => f -> OpSuccess(Set.empty[File], Set.empty[File]))
          .toMap

        (opResults, compiledFiles)
      })(fileHasher)

    compiledFiles
  }

  def runWebpack(targetDir: File, log: ManagedLogger) = {
    val compilationCommand =
      "npx webpack --config webpack.config.js --output-path " + targetDir
    val res = Process(compilationCommand) ! ProcessLogger(
      stdout => log.info(stdout),
      stderr => log.error(stderr)
    )
    if (res != 0) {
      log.info(
        "To debug Webpack compilation run the following command from 'server' folder:\n    " + compilationCommand
      )
      throw new sbt.MessageOnlyException(
        "Webpack compilation failed. Check console to see compilation errors."
      )
    }
  }
}
