import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.SbtArtifactory
import sbt.Keys._
import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "pensions-lifetime-allowance"

lazy val plugins : Seq[Plugins] = Seq.empty
lazy val playSettings : Seq[Setting[_]] = Seq.empty

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;uk.gov.hmrc.BuildInfo;app.*;prod.*;config.*;com.*;.*(AuthService|BuildInfo|Routes).*",
    ScoverageKeys.coverageMinimum := 90,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val root = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins : _*)
  .settings(playSettings ++ scoverageSettings : _*)
  .settings(playSettings : _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    targetJvm := "jvm-1.8",
    scalaVersion := "2.11.12",
    libraryDependencies ++= AppDependencies(),
    parallelExecution in Test := false,
    fork in Test := false,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    resolvers += Resolver.bintrayRepo("hmrc", "releases"),
    resolvers += Resolver.jcenterRepo
  )
  .settings(majorVersion := 1)