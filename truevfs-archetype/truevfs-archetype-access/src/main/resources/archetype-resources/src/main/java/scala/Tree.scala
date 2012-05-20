#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.scala

import net.truevfs.access.TPath
import java.nio.file.Files._
import java.nio.file.Path
import java.util.TreeSet
import scala.collection.JavaConversions._

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
    listfiles foreach (path => graph(new TPath(path)))
    0
  }

  private def graph(file: Path, padding: String = "", prefix: String = "") {
    require(exists(file), file + " (file or directory does not exist)")
    System.out.append(padding).append(prefix).println(file.getFileName)
    if (isDirectory(file)) {
      // Create a sorted set from the directory stream.
      val entries = new TreeSet[Path] {
        val stream = newDirectoryStream(file)
        try {
          for (member <- stream) super.add(member)
        } finally {
          stream.close
        }
      }
      // Graph the sorted set.
      if (!entries.isEmpty) {
        val nextPadding = padding + (
          if (prefix.isEmpty) ""
          else if (prefix == lastPrefix) lastPadding
          else defaultPadding)
        entries.dropRight(1).foreach(graph(_, nextPadding, defaultPrefix))
        graph(entries.last, nextPadding, lastPrefix)
      }
    }
  }
}
