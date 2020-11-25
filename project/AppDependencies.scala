import sbt._

object AppDependencies {

  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val bootstrapPlayVersion = "1.6.0"
  private val domainVersion = "5.10.0-play-26"
  private val hmrcTestVersion = "3.9.0-play-26"
  private val scalaTestVersion = "3.0.8"
  private val mockitoCoreVersion = "3.6.0"
  private val pegdownVersion = "1.6.0"
  private val jsoupVersion = "1.13.1"
  private val scalatestPlusPlayVersion = "3.1.2"
  private val authClientVersion = "3.2.0-play-26"
  private val wiremockVersion = "2.25.1"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalatestPlusPlayVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.jsoup" % "jsoup" % jsoupVersion % scope,
        "org.mockito" % "mockito-core" % mockitoCoreVersion % scope,
        "com.github.tomakehurst" % "wiremock-jre8" % wiremockVersion % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val scope: String = "it"
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalatestPlusPlayVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.jsoup" % "jsoup" % jsoupVersion % scope,
        "com.github.tomakehurst" % "wiremock" % wiremockVersion % scope,
        "uk.gov.hmrc" %% "auth-client" % authClientVersion
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}