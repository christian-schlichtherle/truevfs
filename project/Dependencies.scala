/*
 * Copyright © 2017 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._

object Dependencies {

  val TrueCommonsVersion: String = "2.5.0"
  val SLF4JVersion: String = "1.7.25"

  val BCProvJDK15On: ModuleID = "org.bouncycastle" % "bcprov-jdk15on" % "1.58"
  val CommonsCompress: ModuleID = "org.apache.commons" % "commons-compress" % "1.14"
  val FindBugsAnnotations: ModuleID = "com.google.code.findbugs" % "annotations" % "3.0.1u2" exclude("com.google.findbugs", "jsr305") exclude("net.jcip", "jcip-annotations")
  val Httpclient: ModuleID = "org.apache.httpcomponent" % "httpclient" % "4.5.3" exclude("commons-logging", "commons-logging")
  val JCLOverSLF4J: ModuleID = "org.slf4j" % "jcl-over-slf4j" % SLF4JVersion
  val JUnit: ModuleID = "junit" % "junit" % "4.12"
  val JUnitInterface: ModuleID = "com.novocode" % "junit-interface" % "0.11"
  val MockitoCore: ModuleID = "org.mockito" % "mockito-core" % "2.9.0"
  val ScalaCheck: ModuleID = "org.scalacheck" %% "scalacheck" % "1.13.5"
  def scalaLibrary(scalaVersion: String): ModuleID = "org.scala-lang" % "scala-library" % scalaVersion
  val ScalaPlus: ModuleID = "global.namespace.scala-plus" %% "scala-plus" % "0.1"
  val ScalaTest: ModuleID = "org.scalatest" %% "scalatest" % "3.0.4"
  val SLF4JSimple: ModuleID = "org.slf4j" % "slf4j-simple" % SLF4JVersion
  val TrueCommonsAnnotations: ModuleID = "net.java.truecommons" % "truecommons-annotations" % TrueCommonsVersion
  val TrueCommonsCIO: ModuleID = "net.java.truecommons" % "truecommons-cio" % TrueCommonsVersion
  val TrueCommonsIO: ModuleID = "net.java.truecommons" % "truecommons-io" % TrueCommonsVersion
  val TrueCommonsJMX: ModuleID = "net.java.truecommons" % "truecommons-jmx" % TrueCommonsVersion
  val TrueCommonsKeyDefault: ModuleID = "net.java.truecommons" % "truecommons-key-default" % TrueCommonsVersion
  val TrueCommonsKeySpec: ModuleID = "net.java.truecommons" % "truecommons-key-spec" % TrueCommonsVersion
  val TrueCommonsServices: ModuleID = "net.java.truecommons" % "truecommons-services" % TrueCommonsVersion
  val TrueCommonsShed: ModuleID = "net.java.truecommons" % "truecommons-shed" % TrueCommonsVersion
  val XZ: ModuleID = "org.tukaani" % "xz" % "1.6"

  val ScalaVersion_2_10: String = sys.props.getOrElse("SCALA_VERSION_2.10", "2.10.6")
  val ScalaVersion_2_11: String = sys.props.getOrElse("SCALA_VERSION_2.11", "2.11.11")
  val ScalaVersion_2_12: String = sys.props.getOrElse("SCALA_VERSION_2.12", "2.12.3")
}
