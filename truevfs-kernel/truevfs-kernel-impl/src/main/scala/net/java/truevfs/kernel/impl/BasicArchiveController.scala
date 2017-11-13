/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.io._
import net.java.truecommons.logging._
import net.java.truecommons.shed._
import java.io._
import java.nio.channels._
import java.nio.file._
import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.FsAccessOption._
import net.java.truevfs.kernel.spec.FsAccessOptions._
import net.java.truecommons.cio._
import net.java.truecommons.cio.Entry._
import net.java.truecommons.cio.Entry.Access._
import net.java.truecommons.cio.Entry.Type._

import scala.{None, Option, Some}

/** An abstract base class for any archive file system controller which
  * provide all the essential services required for accessing a prospective
  * archive file.
  * This base class encapsulates all the code which is not depending on a
  * particular archive update strategy and the corresponding state of this
  * file system controller.
  * <p>
  * Each instance of this class manages an archive file - the
  * ''target archive file'' - in order to allow random access to it as if it
  * were a regular directory in its parent file system.
  * <p>
  * Note that in general all of the methods in this class are reentrant on
  * exceptions.
  * This is important because client applications may repeatedly call them.
  * Of course, depending on the calling context, some or all of the archive
  * file's data may be lost in this case.
  *
  * @tparam E the type of the archive entries.
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private abstract class BasicArchiveController[E <: FsArchiveEntry]
extends ArchiveController[E] {
  controller: ArchiveModelAspect[E] =>

  import BasicArchiveController._

  private def fullPath(name: FsNodeName) = path(name).toString

  def node(options: AccessOptions, name: FsNodeName): Option[FsNode] =
    autoMount(options) node (options, name)

  def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access]): Unit =
    autoMount(options) checkAccess (options, name, types)

  def setReadOnly(options: AccessOptions, name: FsNodeName): Unit =
    autoMount(NONE) setReadOnly (options, name)

  def setTime(options: AccessOptions, name: FsNodeName, times: Map[Access, Long]): Boolean = {
    checkSync(options, name, CREATE) // alias for UPDATE
    autoMount(options) setTime (options, name, times)
  }

  def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long): Boolean = {
    checkSync(options, name, CREATE) // alias for UPDATE
    autoMount(options) setTime (options, name, types, value)
  }

  def input(options: AccessOptions, name: FsNodeName): AnyInputSocket = {
    require(null ne options)
    require(null ne name)

    final class Input extends AbstractInputSocket[E] {
      def target(): E = {
        checkSync(options, name, READ)
        autoMount(options) node (options, name) match {
          case Some(ce) =>
            val ae = ce get FILE
            if (null eq ae)
              throw new FileSystemException(fullPath(name), null,
                                            "Expected a FILE entry, but is a " + ce.getTypes + " entry!")
            ae
          case _ => throw new NoSuchFileException(fullPath(name))
        }
      }

      override def stream(peer: AnyOutputSocket): InputStream = socket(peer) stream peer

      override def channel(peer: AnyOutputSocket): SeekableByteChannel = socket(peer) channel peer

      def socket(peer: AnyOutputSocket): InputSocket[E] = {
        Option(peer) foreach (_ target ()) // may sync() if in same target archive file!
        input(target().getName)
      }
    } // Input

    new Input
  }: AnyInputSocket

  def input(name: String): InputSocket[E]

  def output(options: AccessOptions, name: FsNodeName, template: Option[Entry]): AnyOutputSocket = {
    require(null ne options)
    require(null ne name)

    final class Output extends AbstractOutputSocket[FsArchiveEntry] {
      def target(): FsArchiveEntry = {
        val ae = make().head.getEntry
        if (options get APPEND) {
          // A proxy entry must get returned here in order to inhibit
          // a peer target to recognize the type of this entry and
          // switch to Raw Data Copy (RDC) mode.
          // This would not work when APPENDing.
          new ProxyEntry(ae)
        } else {
          ae
        }
      }

      override def stream(peer: AnyInputSocket): OutputStream = {
        val tx = make()
        val ae = tx.head.getEntry
        val in = append()
        var ex: Option[Throwable] = None
        try {
          val os = output(options, ae)
          val out = os stream (in match {
            case Some(_) => null // do NOT bind when appending!
            case None    => peer
          })
          try {
            tx.commit()
            in foreach (Streams cat (_, out))
          } catch {
            case ex2: Throwable =>
              try {
                out.close()
              } catch {
                case ex3: Throwable =>
                  ex2 addSuppressed ex3
              }
              throw ex2
          }
          out
        } catch {
          case ex2: Throwable =>
            ex = Some(ex2)
            throw ex2
        } finally {
          in foreach { in =>
            try {
              in close ()
            } catch {
              case t: Throwable =>
                ex match {
                  case Some(x) => x addSuppressed t
                  case _ => throw t
                }
            }
          }
        }
      }

      def make(): ArchiveFileSystem[E]#Make = {
        checkSync(options, name, WRITE)
        // Start creating or overwriting the archive entry.
        // This will fail if the entry already exists as a directory.
        autoMount(options, !name.isRoot && (options get CREATE_PARENTS))
        .make(options, name, FILE, template)
      }

      def append(): Option[InputStream] = {
        if (options get APPEND) {
          try {
            return Some(input(options, name) stream null)
          } catch {
            // When appending, there is no need for the entry to be
            // readable or even exist, so this can get safely ignored.
            case _: IOException =>
          }
        }
        None
      }
    } // Output

    new Output
  }: AnyOutputSocket

  def output(options: AccessOptions, entry: E): OutputSocket[E]

  def make(options: AccessOptions, name: FsNodeName, tµpe: Type, template: Option[Entry]) {
    if (name.isRoot) { // TODO: Is this case differentiation still required?
      try {
        autoMount(options) // detect false positives!
      } catch {
        case ex: FalsePositiveArchiveException =>
          if (DIRECTORY ne tµpe)
            throw ex
          autoMount(options, autoCreate = true)
          return
      }
      throw new FileAlreadyExistsException(fullPath(name), null,
                                           "Cannot replace a directory entry!")
    } else {
      checkSync(options, name, CREATE)
      autoMount(options, options get CREATE_PARENTS)
      .make(options, name, tµpe, template)
      .commit()
    }
  }

  def unlink(options: AccessOptions, name: FsNodeName) {
    checkSync(options, name, DELETE)
    val fs = autoMount(options)
    fs.unlink(options, name)
    if (name.isRoot) {
      // Check for any archive entries with absolute entry names.
      val size = fs.size - 1 // mind the ROOT entry
      if (0 != size) logger warn ("unlink.absolute", mountPoint, size)
    }
  }

  /** Checks if the intended access to the named archive entry in the virtual
    * file system is possible without performing a
    * [[FsController#sync(BitField, ExceptionHandler)]] sync} operation in
    * advance.
    *
    * @param  options the options for accessing the file system entry.
    * @param  name the name of the file system entry.
    * @param  intention the intended I/O operation on the archive entry.
    * @throws NeedsSyncException If a sync operation is required before the
    *         intended access could succeed.
    */
  def checkSync(options: AccessOptions, name: FsNodeName, intention: Access)

  /** Returns the (virtual) archive file system mounted from the target
    * archive file.
    *
    * @param  options the options for accessing the file system entry.
    * @param  autoCreate If this is `true` and the archive file does not
    *         exist, then a new archive file system with only a virtual root
    *         directory is created with its last modification time set to the
    *         system's current time.
    * @return An archive file system.
    * @throws IOException on any I/O error.
    */
  def autoMount(options: AccessOptions, autoCreate: Boolean = false): ArchiveFileSystem[E]
}

private object BasicArchiveController {
  private val logger = new LocalizedLogger(classOf[BasicArchiveController[_]])

  private final class ProxyEntry(entry: FsArchiveEntry)
  extends DecoratingEntry[FsArchiveEntry](entry) with FsArchiveEntry {
    def getType: Type = entry.getType

    def setSize(tµpe: Size, value: Long): Boolean = entry setSize (tµpe, value)

    def setTime(tµpe: Access, value: Long): Boolean = entry setTime (tµpe, value)

    def setPermitted(tµpe: Access, entity: Entity, value: java.lang.Boolean): Boolean =
      entry.setPermitted(tµpe, entity, value)
  }
}
