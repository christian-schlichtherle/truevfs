/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.io._
import net.java.truecommons.shed._
import java.io._
import java.nio.channels._
import java.nio.file._
import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.FsAccessOption._
import net.java.truevfs.kernel.spec.FsAccessOptions._
import net.java.truevfs.kernel.spec.FsSyncOption._
import net.java.truevfs.kernel.spec.cio._
import net.java.truevfs.kernel.spec.cio.Entry._
import net.java.truevfs.kernel.spec.cio.Entry.Access._
import net.java.truevfs.kernel.spec.cio.Entry.Size._
import net.java.truevfs.kernel.spec.cio.Entry.Type._

/** Manages I/O to the entry which represents the target archive file in its
  * parent file system, detects archive entry collisions and implements a sync
  * of the target archive file.
  * 
  * This controller is an emitter of
  * [[net.java.truevfs.kernel.impl.ControlFlowException]]s, for example
  * when
  * [[net.java.truevfs.kernel.impl.FalsePositiveArchiveException detecting a false positive archive file], or
  * [[net.java.truevfs.kernel.impl.NeedsSyncException requiring a sync].
  *
  * @tparam E the type of the archive entries.
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private abstract class TargetArchiveController[E <: FsArchiveEntry]
(driver: FsArchiveDriver[E], parent: FsController)
extends FileSystemArchiveController[E] {
  controller: LockModelAspect =>

  import TargetArchiveController._

  /** The entry name of the target archive file in the parent file system. */
  private[this] val name = mountPoint.getPath.getNodeName

  /**
   * The (possibly cached) {@link InputArchive} which is used to mount the
   * (virtual) archive file system and read the entries from the target
   * archive file.
   */
  private[this] var _inputArchive: Option[InputArchive[E]] = None

  /**
   * The (possibly cached) {@link OutputArchive} which is used to write the
   * entries to the target archive file.
   */
  private[this] var _outputArchive: Option[OutputArchive[E]] = None

  require(null ne driver)
  require(model.getParent eq parent.getModel, "Parent/member mismatch!")
  assert(invariants)

  private def invariants = {
    assert(null ne driver)
    assert(null ne parent)
    assert(null ne name)
    val fs = fileSystem
    assert(_inputArchive.isEmpty || fs.isDefined)
    assert(_outputArchive.isEmpty || fs.isDefined)
    assert(fs.isEmpty || _inputArchive.isDefined || _outputArchive.isDefined)
    // This is effectively the same than the last three assertions, but is
    // harder to trace in the field on failure.
    //assert null != fs == (null != ia || null != oa);
    true
  }

  private def inputArchive = {
    _inputArchive match {
      case Some(ia) if !ia.clutch.isOpen => throw NeedsSyncException()
      case x => x
    }
  }

  private def inputArchive_=(ia: Option[InputArchive[E]]) {
    assert(ia.isEmpty || _inputArchive.isEmpty)
    ia foreach { _ => mounted = true }
    _inputArchive = ia
  }

  private def outputArchive = {
    _outputArchive match {
      case Some(oa) if !oa.clutch.isOpen => throw NeedsSyncException()
      case x => x
    }
  }

  private def outputArchive_=(oa: Option[OutputArchive[E]]) {
    assert(oa.isEmpty || _outputArchive.isEmpty)
    oa foreach { _ => mounted = true }
    _outputArchive = oa
  }

  def mount(options: AccessOptions, autoCreate: Boolean) {
    try {
      mount0(options, autoCreate)
    } finally {
      assert(invariants)
    }
  }

  private def mount0(options: AccessOptions, autoCreate: Boolean) {
    // HC SVNT DRACONES!

    // Check parent file system entry.
    val pn = {
      try {
        parent node (options, name)
      } catch {
        case ex: FalsePositiveArchiveException =>
          throw new AssertionError(ex)
        case inaccessibleEntry: IOException =>
          if (autoCreate) throw inaccessibleEntry
          throw new FalsePositiveArchiveException(inaccessibleEntry)
      }
    }

    // Obtain file system by creating or loading it from the parent entry.
    val fs = {
      if (null eq pn) {
        if (autoCreate) {
          // This may fail e.g. if the container file is an RAES
          // encrypted ZIP file and the user cancels password prompting.
          outputArchive(options)
          ArchiveFileSystem(new TargetArchiveModel)
        } else {
          throw new FalsePositiveArchiveException(
            new NoSuchFileException(name.toString))
        }
      } else {
        // ro must be init first because the parent archive
        // controller could be a FileController and on Windows this
        // property changes to TRUE once a file is opened for reading!
        val ro = isReadOnlyTarget()
        val is = {
          try {
            driver newInput (model, MOUNT_OPTIONS, parent, name);
          } catch {
            case ex: FalsePositiveArchiveException =>
              throw new AssertionError(ex)
            case ex: IOException =>
              if (pn isType SPECIAL) throw new FalsePositiveArchiveException(ex)
              throw new PersistentFalsePositiveArchiveException(ex)
          }
        }
        val fs = ArchiveFileSystem(new TargetArchiveModel, is, Option(pn), ro)
        inputArchive = Some(new InputArchive(is))
        assert(mounted)
        fs
      }
    }

    // Register file system.
    fileSystem = Some(fs)
  }

  private def isReadOnlyTarget() = {
    try {
      parent checkAccess (MOUNT_OPTIONS, name, WRITE_ACCESS);
      false
    } catch {
      case ex: FalsePositiveArchiveException =>
        throw new AssertionError(ex)
      case ex: IOException =>
        true
    }
  }

  /**
   * Ensures that {@link #outputArchive} does not return {@code None}.
   * This method will use
   * <code>{@link #getContext()}.{@link FsOperationContext#getOutputOptions()}</code>
   * to obtain the output options to use for writing the entry in the parent
   * file system.
   * 
   * @return The output archive.
   */
  private def outputArchive(options: AccessOptions): OutputArchive[E] = {
    outputArchive foreach { oa => assert(mounted); return oa }
    val is = inputArchive match {
      case Some(ia) => ia.driverProduct
      case _ => null
    }
    val os = {
      try {
        driver newOutput (model,
                          options and ACCESS_PREFERENCES_MASK set CACHE,
                          parent, name, is)
      } catch {
        case ex: FalsePositiveArchiveException =>
          throw new AssertionError(ex)
        case ex: ControlFlowException =>
          assert(ex.isInstanceOf[NeedsLockRetryException], ex)
          throw ex
      }
    }
    val oa = new OutputArchive(os)
    outputArchive = Some(oa)
    assert(mounted)
    oa
  }

  def input(name: String) = {
    final class Input extends AbstractInputSocket[E] {
      lazy val socket = inputArchive.get.input(name)

      def target() = syncOn[ClosedInputException] { socket target () }

      override def stream(peer: AnyOutputSocket) =
        syncOn[ClosedInputException] { socket stream peer }

      override def channel(peer: AnyOutputSocket) =
        syncOn[ClosedInputException] { socket channel peer }
    }
    new Input
  }

  def output(options: AccessOptions, entry: E) = {
    final class Output extends AbstractOutputSocket[E] {
      lazy val socket = outputArchive(options) output entry

      def target = entry

      override def stream(peer: AnyInputSocket) =
        syncOn[ClosedOutputException] { socket stream peer }

      override def channel(peer: AnyInputSocket) =
        syncOn[ClosedOutputException] { socket channel peer }
    }
    new Output
  }

  private def syncOn[X <: IOException] = new {
    def apply[A](operation: => A)(implicit mf: ClassManifest[X]) = {
      try {
        operation
      } catch {
        case ex =>
          if (mf.erasure isAssignableFrom ex.getClass)
            throw NeedsSyncException()
          throw ex
      }
    }
  }

  def sync(options: SyncOptions) {
    try {
      val builder = new FsSyncExceptionBuilder
      if (!(options get ABORT_CHANGES))
        copy(builder)
      close(options, builder)
      builder check ()
    } finally {
      assert(invariants)
    }
  }

  /**
   * Synchronizes all entries in the (virtual) archive file system with the
   * (temporary) output archive file.
   *
   * @param handler the strategy for assembling sync exceptions.
   */
  private def copy(handler: FsSyncExceptionBuilder) {
    // Skip (In|Out)putArchive for better performance.
    // This is safe because the ResourceController has already shut down
    // all concurrent access by closing the respective resources (streams,
    // channels etc).
    // The Disconnecting(In|Out)putService should not get skipped however:
    // If these would throw an (In|Out)putClosedException, then this would
    // be an artifact of a bug.
    val is = _inputArchive match {
      case Some(ia) =>
        val clutch = ia.clutch
        if (!clutch.isOpen) return
        clutch
      case _ =>
        new DummyInputService[E]
    }

    val os = _outputArchive match {
      case Some(oa) =>
        val clutch = oa.clutch
        if (!clutch.isOpen) return
        clutch
      case _ =>
        return
    }

    var warning: Option[IOException] = None
    for (cn <- fileSystem.get) {
      for (ae <- cn.getEntries) {
        val aen = ae.getName
        if (null eq (os entry aen)) {
          try {
            if (DIRECTORY eq ae.getType) {
              if (!cn.isRoot) // never output the root directory!
                if (UNKNOWN != ae.getTime(WRITE)) // never output a ghost directory!
                  os.output(ae).stream(null).close()
            } else if (null ne is.entry(aen)) {
              IoSockets.copy(is.input(aen), os.output(ae))
            } else {
              // The file system entry is a newly created
              // non-directory entry which hasn't received any
              // content yet, e.g. as a result of make()
              // => output an empty file system entry.
              for (size <- ALL_SIZES)
                ae.setSize(size, UNKNOWN)
              ae.setSize(DATA, 0)
              os.output(ae).stream(null).close()
            }
          } catch {
            case ex: IOException =>
              if (warning.isDefined || !ex.isInstanceOf[InputException])
                throw handler fail new FsSyncException(mountPoint, ex)
              warning = Some(ex)
              handler warn new FsSyncWarningException(mountPoint, ex)
          }
        }
      }
    }
  }

  /**
   * Discards the file system, closes the input archive and finally the
   * output archive.
   * Note that this order is critical: The parent file system controller is
   * expected to replace the entry for the target archive file with the
   * output archive when it gets closed, so this must be done last.
   * Using a finally block ensures that this is done even in the unlikely
   * event of an exception when closing the input archive.
   * Note that in this case closing the output archive is likely to fail and
   * override the IOException thrown by this method, too.
   *
   * @param handler the strategy for assembling sync exceptions.
   */
  private def close(options: SyncOptions, handler: FsSyncExceptionBuilder) {
    // HC SVNT DRACONES!
    _inputArchive.foreach { ia =>
      try {
        ia.close()
      } catch {
        case ex: ControlFlowException =>
          assert(ex.isInstanceOf[NeedsLockRetryException], ex)
          throw ex
        case ex: IOException =>
          handler warn new FsSyncWarningException(mountPoint, ex)
      }
      inputArchive = None
    }
    _outputArchive.foreach { oa =>
      try {
        oa.close();
      } catch {
        case ex: ControlFlowException =>
          assert(ex.isInstanceOf[NeedsLockRetryException], ex)
          throw ex
        case ex: IOException =>
          handler warn new FsSyncException(mountPoint, ex)
      }
      outputArchive = None
    }
    fileSystem = None
    if (options get ABORT_CHANGES) mounted = false
  }

  def checkSync(options: AccessOptions, name: FsNodeName, intention: Access) {
    // HC SVNT DRACONES!

    // If no file system exists then pass the test.
    val fs = fileSystem match {
      case Some(fs) => fs
      case _        => return
    }

    // If GROWing and the driver supports the respective access method,
    // then pass the test.
    if (options.get(GROW)) {
      intention match {
        case READ  =>
        case WRITE =>
          if (driver.getRedundantContentSupport) {
            outputArchive // side-effect!
            return
          }
        case _     =>
          if (driver.getRedundantMetaDataSupport) return
      }
    }

    // If the file system does not contain an entry with the given name,
    // then pass the test.
    val cn = fs node (options, name) match {
      case Some(cn) => cn
      case _ => return
    }

    // If the entry name addresses the file system root, then pass the test
    // because the root entry cannot get input or output anyway.
    if (name.isRoot) return

    // Check if the entry is already written to the output archive.
    outputArchive match {
      case Some(oa) =>
        val aen = cn.getEntry.getName
        if (null ne (oa entry aen)) throw NeedsSyncException()
      case _ =>
    }

    // If our intention is reading the entry then check if it's present in the
    // input archive.
    if (intention eq READ) inputArchive match {
      case Some(ia) =>
        val aen = cn.getEntry.getName
        if (null eq (ia entry aen)) throw NeedsSyncException()
      case _ =>
        throw NeedsSyncException()
    }
  }

  private class TargetArchiveModel
  extends FsDecoratingModel(model) with DriverModel[E] {
    override def driver = controller.driver
    override def touch(options: AccessOptions) { outputArchive(options) }
  }
} // TargetArchiveController

private object TargetArchiveController {
  private val MOUNT_OPTIONS = BitField.of(CACHE)
  private val WRITE_ACCESS = BitField.of(WRITE)

  private final class InputArchive[E <: FsArchiveEntry]
  (val driverProduct: InputService[E])
  extends LockInputService(new DisconnectingInputService(driverProduct)) {
    def clutch = container.asInstanceOf[DisconnectingInputService[E]]
  }

  private final class OutputArchive[E <: FsArchiveEntry]
  (driverProduct: OutputService[E])
  extends LockOutputService(new DisconnectingOutputService(driverProduct)) {
    def clutch = container.asInstanceOf[DisconnectingOutputService[E]]
  }

  /** A dummy input archive to substitute for `None` when copying.
    * 
    * @tparam E the type of the entries.
    */
  private final class DummyInputService[E <: Entry] extends InputService[E] {
    override def size = 0
    override def iterator = java.util.Collections.emptyList[E].iterator
    override def entry(name: String) = null.asInstanceOf[E]
    override def input(name: String) = throw new AssertionError
    override def close() = throw new AssertionError
  }
} // TargetArchiveController
