name := "Semeru"

version := "2.0"

incOptions := incOptions.value.withNameHashing(true)

scalaVersion := "2.11.7"

scalacOptions += "-deprecation"

scalacOptions += "-feature"

scalacOptions += "-unchecked"

scalacOptions += "-optimise"

scalacOptions += "-Yinline-warnings"

libraryDependencies ++= Seq(
  "org.scala-lang"              % "scala-compiler"        % scalaVersion.value,
  "org.neo4j"                   % "neo4j-kernel"          % "2.2.5",
  "org.neo4j"                   % "neo4j-lucene-index"    % "2.2.5",
  "com.google.guava"            % "guava"                 % "18.0",
  "mysql"                       % "mysql-connector-java"  % "5.1.37",
  "org.apache.commons"          % "commons-dbcp2"         % "2.1.1",
  "com.google.code.findbugs"    % "jsr305"                % "1.3.9",
  "org.scalatest"               %% "scalatest"             % "2.2.0" % "test",
  "junit"                       % "junit"                 % "4.8.1" % "test",
  "jfree"                       % "jfreechart"            % "1.0.12",
  "nz.ac.waikato.cms.weka"      % "weka-stable"           % "3.6.8",
  "org.apache.lucene"           % "lucene-core"           % "3.5.0",
  "org.apache.lucene"           % "lucene-wordnet"        % "3.3.0",
  "org.scalatra"                %% "scalatra"         % "2.3.0",
  "org.scalatra"                %% "scalatra-scalate" % "2.3.0",
  "org.scalatra"                %% "scalatra-json"    % "2.3.0",
  "org.json4s"                  %% "json4s-native"    % "3.2.9",
  "ch.qos.logback"              %  "logback-classic"      % "1.1.1",
  "org.eclipse.jetty"           %  "jetty-plus"           % "9.1.3.v20140225",
  "org.eclipse.jetty"           %  "jetty-webapp"         % "9.1.3.v20140225", 
  "org.eclipse.jetty.websocket" %  "websocket-server"     % "9.1.3.v20140225", 
  "javax.servlet"               %  "javax.servlet-api"    % "3.1.0"          artifacts Artifact("javax.servlet-api", "jar", "jar")
)

mainClass in (Compile,run) := Some("edu.mit.csail.cap.query.web.Server")

fork in run := true

connectInput in run := true

javaOptions in run += "-XX:+UseConcMarkSweepGC"

javaOptions in run += "-XX:+UseNUMA"

javaOptions in run += "-XX:+DoEscapeAnalysis"

javaOptions in run += "-XX:+UseCompressedOops"

javaOptions in run += "-Xmx8G"

parallelExecution in Test := false

logBuffered in Test := false

showSuccess := false

onLoadMessage := ""

logLevel in compile := Level.Warn

outputStrategy := Some(StdoutOutput)
