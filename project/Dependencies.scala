/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services
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

  val Bali: ModuleID = "global.namespace.bali" % "bali-java" % "0.8.0"
  val BcprovJdk15on: ModuleID = "org.bouncycastle" % "bcprov-jdk15on" % "1.68"
  val CommonsCompress: ModuleID = "org.apache.commons" % "commons-compress" % "1.20"
  val FindbugsAnnotations: ModuleID = "com.google.code.findbugs" % "annotations" % "3.0.1u2" exclude("com.google.code.findbugs", "jsr305") exclude("net.jcip", "jcip-annotations")
  val FunIoBIOS: ModuleID = "global.namespace.fun-io" % "fun-io-bios" % Versions.FunIo
  val FunIoScalaApi: ModuleID = "global.namespace.fun-io" %% "fun-io-scala-api" % Versions.FunIo
  val Httpclient: ModuleID = "org.apache.httpcomponents" % "httpclient" % "4.5.13" exclude("commons-logging", "commons-logging")
  val JclOverSlf4j: ModuleID = "org.slf4j" % "jcl-over-slf4j" % Versions.Slf4j
  val Junit: ModuleID = "junit" % "junit" % "4.13.1"
  val JunitInterface: ModuleID = "com.novocode" % "junit-interface" % "0.11"
  val Lombok: ModuleID = "org.projectlombok" % "lombok" % "1.18.16"
  val MockitoCore: ModuleID = "org.mockito" % "mockito-core" % "3.7.7"
  val Scalacheck: ModuleID = "org.scalacheck" %% "scalacheck" % "1.15.2"
  val Scalatest: ModuleID = "org.scalatest" %% "scalatest" % "3.0.9"
  val ServiceWightAnnotation: ModuleID = "global.namespace.service-wight" % "service-wight-annotation" % Versions.ServiceWight
  val ServiceWightCore: ModuleID = "global.namespace.service-wight" % "service-wight-core" % Versions.ServiceWight
  val Slf4jSimple: ModuleID = "org.slf4j" % "slf4j-simple" % Versions.Slf4j
  val TrueCommonsCIO: ModuleID = "net.java.truecommons" % "truecommons-cio" % Versions.TrueCommons
  val TrueCommonsIO: ModuleID = "net.java.truecommons" % "truecommons-io" % Versions.TrueCommons
  val TrueCommonsJMX: ModuleID = "net.java.truecommons" % "truecommons-jmx" % Versions.TrueCommons
  val TrueCommonsKeyConsole: ModuleID = "net.java.truecommons" % "truecommons-key-console" % Versions.TrueCommons
  val TrueCommonsKeyDefault: ModuleID = "net.java.truecommons" % "truecommons-key-default" % Versions.TrueCommons
  val TrueCommonsKeyHurlfb: ModuleID = "net.java.truecommons" % "truecommons-key-hurlfb" % Versions.TrueCommons
  val TrueCommonsKeyMacosx: ModuleID = "net.java.truecommons" % "truecommons-key-macosx" % Versions.TrueCommons
  val TrueCommonsKeySpec: ModuleID = "net.java.truecommons" % "truecommons-key-spec" % Versions.TrueCommons
  val TrueCommonsKeySwing: ModuleID = "net.java.truecommons" % "truecommons-key-swing" % Versions.TrueCommons
  val TrueCommonsLogging: ModuleID = "net.java.truecommons" % "truecommons-logging" % Versions.TrueCommons
  val TrueCommonsServices: ModuleID = "net.java.truecommons" % "truecommons-services" % Versions.TrueCommons
  val TrueCommonsShed: ModuleID = "net.java.truecommons" % "truecommons-shed" % Versions.TrueCommons
  val Xz: ModuleID = "org.tukaani" % "xz" % "1.8"
}
