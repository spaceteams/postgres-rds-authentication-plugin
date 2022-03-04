val scalaVersion = "2.12.15"

ThisBuild / organization := "de.spaceteams"
ThisBuild / organizationName := "Spaceteams GmbH"
ThisBuild / organizationHomepage := Some(url("https://spaceteams.de"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    browseUrl  = url("https://github.com/spaceteams/postgres-rds-authentication-plugin"),
    connection = "scm:git@github.com:spaceteams/postgres-rds-authentication-plugin.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "kaaas",
    name  = "Kassem Tohme",
    email = "kassem.tohme@spaceteams.de",
    url   = url("https://spaceteams.de")
  )
)

ThisProject / name := "postgres-rds-authentication-plugin"
ThisBuild / description := "RDS authentication plugin for PostreSQL JDBC driver."
ThisBuild / licenses := List("BSD 3-Clause License" -> new URL("https://opensource.org/licenses/BSD-3-Clause"))
ThisBuild / homepage := Some(url("https://github.com/spaceteams/postgres-rds-authentication-plugin"))

ThisBuild / version := "1.0.0"
ThisBuild / versionScheme := Some("semver-spec")

ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true

val `postgres-rds-authentication-plugin` = project
  .in(file("."))
  .settings(
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql"                      % "42.3.3"   % "provided",
      "com.amazonaws"  % "aws-java-sdk-rds"                % "1.12.167" % "provided",
      "org.scalatest" %% "scalatest"                       % "3.2.11"   % Test,
      "com.dimafeng"  %% "testcontainers-scala-postgresql" % "0.40.1"   % Test
    ),
  )
