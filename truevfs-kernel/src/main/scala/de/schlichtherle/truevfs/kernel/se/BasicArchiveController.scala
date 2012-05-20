/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import de.schlichtherle.truevfs.kernel._
import net.truevfs.kernel._
import net.truevfs.kernel.FsAccessOption._
import net.truevfs.kernel.FsAccessOptions._
import net.truevfs.kernel.cio._
import net.truevfs.kernel.cio.Entry._;
import net.truevfs.kernel.cio.Entry.Access._;
import net.truevfs.kernel.cio.Entry.Type._;
import net.truevfs.kernel.io._
import net.truevfs.kernel.util._
import java.io._
import java.nio.channels._
import java.nio.file._
import java.util.logging._

/**
 * An abstract base class for any archive file system controller which
 * provide all the essential services required for accessing a prospective
 * archive file.
 * This base class encapsulates all the code which is not depending on a
 * particular archive update strategy and the corresponding state of this
 * file system controller.
 * <p>
 * Each instance of this class manages an archive file - the <i>target file</i>
 * - in order to allow random access to it as if it were a regular directory in
 * its parent file system.
 * <p>
 * Note that in general all of the methods in this class are reentrant on
 * exceptions.
 * This is important because client applications may repeatedly call them.
 * Of course, depending on the calling context, some or all of the archive
 * file's data may be lost in this case.
 * 
 * @param  <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
private abstract class BasicArchiveController[E <: FsArchiveEntry](m: LockModel)
extends FsAbstractController[LockModel](m) {
  import BasicArchiveController._

  require(null ne m.getParent)

  override def stat(options: AccessOptions, name: FsEntryName): FsEntry =
    autoMount(options).stat(options, name).orNull

  override def checkAccess(options: AccessOptions, name: FsEntryName, types: BitField[Access]) =
    autoMount(options).checkAccess(options, name, types)

  override def setReadOnly(name: FsEntryName) =
    autoMount(NONE).setReadOnly(name)

  override def setTime(options: AccessOptions, name: FsEntryName, times: java.util.Map[Access, java.lang.Long]) = {
    checkSync(options, name, None)
    autoMount(options).setTime(options, name, times)
  }

  override def setTime(options: AccessOptions, name: FsEntryName, types: BitField[Access], value: Long) = {
    checkSync(options, name, None)
    autoMount(options).setTime(options, name, types, value)
  }

  override def input(options: AccessOptions, name: FsEntryName) = {
    require(null ne options)
    require(null ne name)

    final class Input extends DelegatingInputSocket[E] {
      override def socket() = {
        val ae = localTarget()
        val tµpe = ae.getType()
        if (FILE ne tµpe)
          throw new FileSystemException(name.toString, null,
                                        "Expected a FILE entry, but is a " + tµpe + " entry!");
        input(ae.getName)
      }

      override def localTarget() = {
        peerTarget() // may sync() if in same target archive file!
        checkSync(options, name, Some(READ))
        autoMount(options).stat(options, name) match {
          case Some(ce) => ce.getEntry
          case _ => throw new NoSuchFileException(name.toString)
        }
      }
    }
    new Input
  }: AnyInputSocket

  def input(name: String): InputSocket[E]

  override def output(options: AccessOptions, name: FsEntryName, template: Entry) = {
    require(null ne options)
    require(null ne name)

    final class Output extends AbstractOutputSocket[FsArchiveEntry] {
      override def localTarget() = {
        val ae = mknod().head.getEntry
        if (options.get(APPEND)) {
          // A proxy entry must get returned here in order to inhibit
          // a peer target to recognize the type of this entry and
          // switch to Raw Data Copy (RDC) mode.
          // This would not work when APPENDing.
          new ProxyEntry(ae)
        } else {
          ae
        }
      }

      override def stream() = {
        val tx = mknod()
        val ae = tx.head.getEntry
        val in = append()
        var ex: Option[Throwable] = None
        try {
          val os = output(options, ae)
          in match {
            case None => os.bind(this)
            case _ => // do NOT bind when appending!
          }
          val out = os.stream()
          try {
            tx.commit()
            in.foreach(Streams.cat(_, out))
          } catch {
            case ex2: Throwable =>
              try {
                out.close()
              } catch {
                case ex3: Throwable =>
                  ex2.addSuppressed(ex3)
              }
              throw ex2
          }
          out
        } catch {
          case ex2: Throwable =>
            ex = Some(ex2)
            throw ex2
        } finally {
          in.foreach { in =>
            try {
              in.close()
            } catch {
              case ex2: IOException =>
                val ex3 = new InputException(ex2)
                ex match {
                  case Some(ex) => ex.addSuppressed(ex3)
                  case _ => throw ex3
                }
              case ex2: Throwable =>
                ex match {
                  case Some(ex) => ex.addSuppressed(ex2)
                  case _ => throw ex2
                }
            }
          }
        }
      }

      def mknod() = {
        checkSync(options, name, Some(WRITE))
        // Start creating or overwriting the archive entry.
        // This will fail if the entry already exists as a directory.
        autoMount(options, !name.isRoot && options.get(CREATE_PARENTS))
        .mknod(options, name, FILE, Option(template))
      }

      def append(): Option[InputStream] = {
        if (options.get(APPEND)) {
          try {
            return Some(input(options, name).stream())
          } catch {
            // When appending, there is no need for the entry to be
            // readable or even exist, so this can get safely ignored.
            case _: IOException =>
          }
        }
        None
      }
    }
    new Output
  }: AnyOutputSocket

  def output(options: AccessOptions, entry: E): OutputSocket[E]

  override def mknod(options: AccessOptions, name: FsEntryName, tµpe: Type, template: Entry): Unit = {
    if (name.isRoot) { // TODO: Is this case differentiation still required?
      try {
        autoMount(options) // detect false positives!
      } catch {
        case ex: FalsePositiveArchiveException =>
          if (DIRECTORY ne tµpe)
            throw ex
          autoMount(options, true);
          return
      }
      throw new FileAlreadyExistsException(name.toString, null,
                                           "Cannot replace a directory entry!");
    } else {
      checkSync(options, name, None)
      autoMount(options, options.get(CREATE_PARENTS))
      .mknod(options, name, tµpe, Option(template))
      .commit()
    }
  }

  override def unlink(options: AccessOptions, name: FsEntryName) = {
    checkSync(options, name, None)
    val fs = autoMount(options)
    fs.unlink(options, name)
    if (name.isRoot) {
      // Check for any archive entries with absolute entry names.
      val size = fs.size - 1 // mind the ROOT entry
      if (0 != size)
          logger.log(Level.WARNING, "unlink.absolute",
                     Array[AnyRef](getMountPoint, size.asInstanceOf[AnyRef]));
    }
  }

  /**
   * Checks if the intended access to the named archive entry in the virtual
   * file system is possible without performing a
   * {@link FsController#sync(BitField, ExceptionHandler) sync} operation in
   * advance.
   *
   * @param  options the options for accessing the file system entry.
   * @param  name the name of the file system entry.
   * @param  intention the intended I/O operation on the archive entry.
   *         If {@code None}, then only an update to the archive entry meta
   *         data (i.e. a pure virtual file system operation with no I/O)
   *         is intended.
   * @throws NeedsSyncException If a sync operation is required before the
   *         intended access could succeed.
   */
  def checkSync(options: AccessOptions, name: FsEntryName, intention: Option[Access])

  /**
   * Returns the (virtual) archive file system mounted from the target
   * archive file.
   *
   * @param  options the options for accessing the file system entry.
   * @param  autoCreate If this is {@code true} and the archive file does not
   *         exist, then a new archive file system with only a virtual root
   *         directory is created with its last modification time set to the
   *         system's current time.
   * @return An archive file system.
   * @throws IOException on any I/O error.
   */
  def autoMount(options: AccessOptions, autoCreate: Boolean = false): ArchiveFileSystem[E]
}

private object BasicArchiveController {
  private val logger = Logger.getLogger(
    classOf[BasicArchiveController[_]].getName,
    classOf[BasicArchiveController[_]].getName)

  private final class ProxyEntry(entry: FsArchiveEntry)
  extends DecoratingEntry[FsArchiveEntry](entry) with FsArchiveEntry {
    override def getType: Type = entry.getType

    override def setSize(tµpe: Size, value: Long) = entry.setSize(tµpe, value)

    override def setTime(tµpe: Access, value: Long) = entry.setTime(tµpe, value)

    override def isPermitted(tµpe: Access, entity: Entity) =
      entry.isPermitted(tµpe, entity)

    override def setPermitted(tµpe: Access, entity: Entity, value: java.lang.Boolean) =
      entry.setPermitted(tµpe, entity, value)
  }
}
