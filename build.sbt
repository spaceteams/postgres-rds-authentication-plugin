val settings = Seq(
  scalaVersion := "2.11.12",
  libraryDependencies ++= Seq(
    "org.postgresql" % "postgresql"                      % "42.3.3"   % "provided",
    "com.amazonaws"  % "aws-java-sdk-rds"                % "1.12.167" % "provided",
    "org.scalatest" %% "scalatest"                       % "3.2.11"   % Test,
    "com.dimafeng"  %% "testcontainers-scala-postgresql" % "0.40.1"   % Test
  ),
  version := "1.0.0",
  organization := "de.spaceteams",
  artifactName := { (_: ScalaVersion, module: ModuleID, artifact: Artifact) =>
    artifact.name + "-" + module.revision + "." + artifact.extension
  }
)

val `postgres-rds-authentication-plugin` = project
  .in(file("."))
  .settings(settings)
