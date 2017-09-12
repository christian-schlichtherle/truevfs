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

  val TruecommonsVersion: String = "2.5.0"
  val Slf4jVersion: String = "1.7.25"

  val BcprovJdk15on: ModuleID = "org.bouncycastle" % "bcprov-jdk15on" % "1.58"
  val CommonsCompress: ModuleID = "org.apache.commons" % "commons-compress" % "1.14"
  val FindbugsAnnotations: ModuleID = "com.google.code.findbugs" % "annotations" % "3.0.1u2" exclude("com.google.code.findbugs", "jsr305") exclude("net.jcip", "jcip-annotations")
  val Httpclient: ModuleID = "org.apache.httpcomponents" % "httpclient" % "4.5.3" exclude("commons-logging", "commons-logging")
  val JclOverSlf4j: ModuleID = "org.slf4j" % "jcl-over-slf4j" % Slf4jVersion
  val Junit: ModuleID = "junit" % "junit" % "4.12"
  val JunitInterface: ModuleID = "com.novocode" % "junit-interface" % "0.11"
  val MockitoCore: ModuleID = "org.mockito" % "mockito-core" % "2.9.0"
  val Scalacheck: ModuleID = "org.scalacheck" %% "scalacheck" % "1.13.5"
  def scalaLibrary(scalaVersion: String): ModuleID = "org.scala-lang" % "scala-library" % scalaVersion
  val ScalaPlus: ModuleID = "global.namespace.scala-plus" %% "scala-plus" % "0.1"
  val Scalatest: ModuleID = "org.scalatest" %% "scalatest" % "3.0.4"
  val Slf4jSimple: ModuleID = "org.slf4j" % "slf4j-simple" % Slf4jVersion
  val TruecommonsAnnotations: ModuleID = "net.java.truecommons" % "truecommons-annotations" % TruecommonsVersion exclude("com.google.code.findbugs", "jsr305") exclude("net.jcip", "jcip-annotations")
  val TruecommonsCIO: ModuleID = "net.java.truecommons" % "truecommons-cio" % TruecommonsVersion
  val TruecommonsIO: ModuleID = "net.java.truecommons" % "truecommons-io" % TruecommonsVersion
  val TruecommonsJMX: ModuleID = "net.java.truecommons" % "truecommons-jmx" % TruecommonsVersion
  val TruecommonsKeyConsole: ModuleID = "net.java.truecommons" % "truecommons-key-console" % TruecommonsVersion
  val TruecommonsKeyDefault: ModuleID = "net.java.truecommons" % "truecommons-key-default" % TruecommonsVersion
  val TruecommonsKeyHurlfb: ModuleID = "net.java.truecommons" % "truecommons-key-hurlfb" % TruecommonsVersion
  val TruecommonsKeyMacosx: ModuleID = "net.java.truecommons" % "truecommons-key-macosx" % TruecommonsVersion
  val TruecommonsKeySpec: ModuleID = "net.java.truecommons" % "truecommons-key-spec" % TruecommonsVersion
  val TruecommonsKeySwing: ModuleID = "net.java.truecommons" % "truecommons-key-swing" % TruecommonsVersion
  val TruecommonsServices: ModuleID = "net.java.truecommons" % "truecommons-services" % TruecommonsVersion
  val TruecommonsShed: ModuleID = "net.java.truecommons" % "truecommons-shed" % TruecommonsVersion
  val Xz: ModuleID = "org.tukaani" % "xz" % "1.6"

  val ScalaVersion_2_10: String = sys.props.getOrElse("SCALA_VERSION_2.10", "2.10.6")
  val ScalaVersion_2_11: String = sys.props.getOrElse("SCALA_VERSION_2.11", "2.11.11")
  val ScalaVersion_2_12: String = sys.props.getOrElse("SCALA_VERSION_2.12", "2.12.3")
}
