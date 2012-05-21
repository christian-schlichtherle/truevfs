#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.scala.file

import ${package}.scala.Application
import net.truevfs.access.TFile
import java.io.File
import java.util.Arrays

/**
 * This command line utility prints the tree graph of the directory structure
 * of its file or directory arguments to the standard output.
 * Instead of a directory, you can name any configured archive file type as an
 * argument, too.
 * <p>
 * For example, if the JAR for the module {@code truevfs-driver-zip} is present
 * on the run time class path and the path name argument is {@code archive.zip}
 * and this file actually exists as a ZIP file, then the tree graph of the
 * directory structure of this ZIP file gets printed.
 * 
 * @author Christian Schlichtherle
 */
object Tree extends Application {
  private val defaultPrefix  = "|-- "
  private val lastPrefix     = "`-- "
  private val defaultPadding = "|   "
  private val lastPadding    = "    "

  def main(args: Array[String]) {
    System.exit(Tree.run(args))
  }

  override protected def work(args: Array[String]) = {
    val listfiles = if (0 < args.length) args else Array(".")
    listfiles foreach (path => graph(new TFile(path)))
    0
  }

  private def graph(file: File, padding: String = "", prefix: String = "") {
    require(file.exists(), file + " (file or directory does not exist)")
    System.out.append(padding).append(prefix).println(file.getName)
    if (file.isDirectory()) {
      val entries = file.listFiles()
      Arrays.sort(entries.asInstanceOf[Array[Object]])
      val nextPadding = padding + (
        if (prefix.isEmpty) ""
        else if (prefix == lastPrefix) lastPadding
        else defaultPadding)
      import scala.collection.JavaConversions._
      entries.dropRight(1).foreach(graph(_, nextPadding, defaultPrefix))
      graph(entries.last, nextPadding, lastPrefix)
    }
  }
}
