enablePlugins(GatlingPlugin)

name := "wayperf"
organization := "waypoint"
version := "1.0"

scalaVersion := "2.13.4"

scalacOptions := Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps")

val gatlingVersion = "3.5.1"
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion 
libraryDependencies += "io.gatling"            % "gatling-test-framework"    % gatlingVersion 
