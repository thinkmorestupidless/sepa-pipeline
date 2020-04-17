import sbt._
import sbt.Keys._

lazy val root =
  Project(id = "root", base = file("."))
    .enablePlugins(ScalafmtPlugin)
    .settings(
      name := "root",
      scalafmtOnCompile := true,
      skip in publish := true,
    )
    .withId("root")
    .settings(commonSettings)
    .aggregate(
      sepaPipeline,
      datamodel,
      xCloudflow,
      akkaScala,
      akkaJava,
      sparkAggregation
    )

//tag::docs-CloudflowApplicationPlugin-example[]
lazy val sepaPipeline = appModule("sepa-pipeline")
  .enablePlugins(CloudflowApplicationPlugin)
  .settings(commonSettings)
  .settings(
    name := "sepa-pipeline"
  )
  .dependsOn(akkaScala, akkaJava, sparkAggregation)
//end::docs-CloudflowApplicationPlugin-example[]

lazy val datamodel = appModule("datamodel")
  .enablePlugins(CloudflowLibraryPlugin)

lazy val akkaScala = appModule("akka-scala")
    .enablePlugins(CloudflowAkkaStreamsLibraryPlugin)
    .settings(
      commonSettings,
      libraryDependencies ++= Seq(
        "com.typesafe.akka"         %% "akka-http-spray-json"   % "10.1.10",
        "ch.qos.logback"            %  "logback-classic"        % "1.2.3",
        "org.scalatest"             %% "scalatest"              % "3.0.8"    % "test"
      )
    )
  .dependsOn(datamodel)

lazy val xCloudflow = appModule("cloudflow-extensions")
  .enablePlugins(CloudflowAkkaStreamsLibraryPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % "2.5.24",
      "com.lightbend.akka.management" %% "akka-management" % "1.0.3",
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % "1.0.3",
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % "1.0.3",
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.3",
      "com.typesafe.akka" %% "akka-discovery" % "2.5.24",
      "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.24",
      "com.typesafe.akka"         %% "akka-http-spray-json"   % "10.1.10",
      "ch.qos.logback"            %  "logback-classic"        % "1.2.3",
      "org.scalatest"             %% "scalatest"              % "3.0.8"    % "test"
    )
  )
  .dependsOn(datamodel)

lazy val akkaJava = appModule("akka-java")
  .enablePlugins(CloudflowAkkaStreamsLibraryPlugin)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.typesafe.akka"      %% "akka-http-spray-json"   % "10.1.10",
      "ch.qos.logback"         %  "logback-classic"        % "1.2.3",
      "org.scalatest"          %% "scalatest"              % "3.0.8"    % "test"
    )
  )
  .dependsOn(datamodel)

lazy val sparkAggregation = appModule("spark-aggregation")
    .enablePlugins(CloudflowSparkLibraryPlugin)
    .settings(
      commonSettings,
      Test / parallelExecution := false,
      Test / fork := true,
      libraryDependencies ++= Seq(
        "ch.qos.logback" %  "logback-classic" % "1.2.3",
        "org.scalatest"  %% "scalatest"       % "3.0.8"  % "test"
      )
    )
  .dependsOn(datamodel)

def appModule(moduleID: String): Project = {
  Project(id = moduleID, base = file(moduleID))
    .settings(
      name := moduleID
    )
    .withId(moduleID)
    .settings(commonSettings)
}

lazy val commonSettings = Seq(
  organization := "com.lightbend.cloudflow",
  headerLicense := Some(HeaderLicense.ALv2("(C) 2016-2020", "Lightbend Inc. <https://www.lightbend.com>")),
  scalaVersion := "2.12.10",
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-target:jvm-1.8",
    "-Xlog-reflective-calls",
    "-Xlint",
    "-Ywarn-unused",
    "-Ywarn-unused-import",
    "-deprecation",
    "-feature",
    "-language:_",
    "-unchecked"
  ),

  scalacOptions in (Compile, console) --= Seq("-Ywarn-unused", "-Ywarn-unused-import"),
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value

)
