/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.shed._
import net.java.truecommons.shed.HashMaps._
import net.java.truecommons.shed.Paths._
import java.nio.file._
import java.util.Locale
import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.FsAccessOption._
import net.java.truevfs.kernel.spec.FsAccessOptions._
import net.java.truevfs.kernel.spec.FsNodeName._
import net.java.truevfs.kernel.spec.cio._
import net.java.truevfs.kernel.spec.cio.Entry._
import net.java.truevfs.kernel.spec.cio.Entry.Access._
import net.java.truevfs.kernel.spec.cio.Entry.Type._
import scala.annotation._
import ArchiveFileSystem._

/** A read/write virtual file system for archive entries.
  * 
  * @tparam E the type of the archive entries.
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private class ArchiveFileSystem[E <: FsArchiveEntry] private(
  controller: ArchiveFileSystem.Controller[E],
  master: EntryTable[E])
extends Iterable[FsCovariantNode[E]] {

  private val splitter = new Splitter

  /** Whether or not this file system has been modified. */
  private var touched: Boolean = _

  def this(controller: ArchiveFileSystem.Controller[E]) {
    this(controller, new EntryTable(OVERHEAD_SIZE))
    val root = newEntry(RootPath, DIRECTORY, None)
    val time = System.currentTimeMillis()
    for (access <- ALL_ACCESS)
      root.setTime(access, time)
    master.add(RootPath, root)
    touched = true
  }

  def this(controller: ArchiveFileSystem.Controller[E], archive: Container[E], rootTemplate: Option[Entry]) {
    // Allocate some extra capacity to create missing parent directories.
    this(controller, new EntryTable(archive.size + OVERHEAD_SIZE))
    // Load entries from source archive.
    var paths = List[String]()
    val normalizer = new PathNormalizer(SEPARATOR_CHAR)
    for (ae <- archive) {
      val path = cutTrailingSeparators(
        normalizer.normalize(
          ae.getName.replace('\\', SEPARATOR_CHAR)), // fix illegal Windoze file name separators
        SEPARATOR_CHAR)
      master.add(path, ae)
      if (!path.startsWith(SEPARATOR)
          && !(".." + SEPARATOR).startsWith(path.substring(0, math.min(3, path.length))))
            paths ::= path
    }
    // Setup root file system entry, potentially replacing its previous
    // mapping from the source archive.
    master.add(RootPath, newEntry(RootPath, DIRECTORY, rootTemplate))
    // Now perform a file system check to create missing parent directories
    // and populate directories with their members - this must be done
    // separately!
    for (path <- paths) fix(path)
  }

  private def driver = controller.driver

  /** Called from a constructor in order to fix the parent directories of the
    * file system entry identified by `name`, ensuring that all parent
    * directories of the file system entry exist and that they contain the
    * respective member entry.
    * If a parent directory does not exist, it is created using an unkown time
    * as the last modification time - this is defined to be a
    * ''ghost directory''.
    * If a parent directory does exist, the respective member entry is added
    *
    * @param name the entry name.
    */
  @tailrec
  private def fix(name: String) {
    // When recursing into this method, it may be called with the root
    // directory as its parameter, so we may NOT skip the following test.
    if (!isRoot(name)) {
      splitter.split(name)
      val pp = splitter.getParentPath
      val mn = splitter.getMemberName
      val pce = master.get(pp) match {
        case Some(pce) if pce.isType(DIRECTORY) => pce
        case _ => master.add(pp, newEntry(pp, DIRECTORY, None))
      }
      pce.add(mn)
      fix(pp)
    }
  }

  override def size = master.size

  def iterator = master.iterator

  /** Returns a covariant file system entry or `None` if no file system
    * entry exists for the given name.
    * Modifying the returned object graph is either not supported (i.e. throws
    * an [[java.lang.UnsupportedOperationException]] or does not show any effect
    * on this file system.
    *
    * @param  name the name of the file system entry to look up.
    * @return A covariant file system entry or `None` if no file system
    *         entry exists for the given name.
    */
  def node(options: AccessOptions, name: FsNodeName) = {
    master.get(name.getPath) match {
      case Some(ce) => Some(ce.clone(driver))
      case None => None
    }
  }

  def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access]) {
    if (master.get(name.getPath).isEmpty)
      throw new NoSuchFileException(name.toString)
  }

  def setReadOnly(name: FsNodeName) {
    throw new FileSystemException(name.toString, null,
        "Cannot set read-only state!")
  }

  def setTime(options: AccessOptions, name: FsNodeName, times: Map[Access, Long]) = {
    val ce = master.get(name.getPath) match {
      case Some(ce) => ce
      case _ => throw new NoSuchFileException(name.toString)
    }
    // HC SVNT DRACONES!
    touch(options)
    val ae = ce.getEntry
    var ok = true
    for ((access, value) <- times)
        ok &= 0 <= value && ae.setTime(access, value)
    ok
  }

  def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long) = {
    if (0 > value)
      throw new IllegalArgumentException(name.toString
                                         + " (negative access time)")
    val ce = master.get(name.getPath) match {
      case Some(ce) => ce
      case _ => throw new NoSuchFileException(name.toString)
    }
    // HC SVNT DRACONES!
    touch(options)
    val ae = ce.getEntry
    var ok = true
    for (tµpe <- types)
        ok &= ae.setTime(tµpe, value)
    ok
  }

  /** Begins a ''transaction'' to create or replace and finally link a
    * chain of one or more archive entries for the given `name` into
    * this archive file system.
    *
    * To commit the transaction, you need to call
    * `commit` on the returned
    * [[net.java.truevfs.kernel.impl.ArchiveFileSystem.Make]] object,
    * which will mark this archive file system as touched and set the last
    * modification time of the created and linked archive file system entries
    * to the system's current time at the moment of the call to this method.
    *
    * @param  name the archive file system entry name.
    * @param  tµpe the type of the archive file system entry to create.
    * @param  options if `CREATE_PARENTS` is set, any missing parent
    *         directories will be created and linked into this file
    *         system with its last modification time set to the system's
    *         current time.
    * @param  template if not `None`, then the archive file system entry
    *         at the end of the chain shall inherit as much properties from
    *         this entry as possible - with the exception of its name and type.
    * @throws IOException on any I/O error.
    * @return A new archive file system operation on a chain of one or more
    *         archive file system entries for the given path name which will
    *         be linked into this archive file system upon a call to the
    *         `commit` method of the returned
    *         [[net.java.truevfs.kernel.impl.ArchiveFileSystem.Make]]
    *         object.
    */
  def make(options: AccessOptions, name: FsNodeName, tµpe: Type, template: Option[Entry]) = {
    require(null ne tµpe)
    if (FILE.ne(tµpe) && DIRECTORY.ne(tµpe)) // TODO: Add support for other types.
      throw new FileSystemException(name.toString, null,
                                    "Can only create file or directory entries, but not a " + typeName(tµpe) + " entry!")
    val np = name.getPath
    master.get(np).foreach { ce =>
      if (!ce.isType(FILE))
        throw new FileAlreadyExistsException(name.toString, null,
                                            "Cannot replace a " + typeName(ce) + " entry!")
      if (FILE ne tµpe)
        throw new FileAlreadyExistsException(name.toString, null,
                                            "Can only replace a file entry with a file entry, but not a " + typeName(tµpe) + " entry!")
      if (options.get(EXCLUSIVE))
        throw new FileAlreadyExistsException(name.toString)
    }
    val t = template match {
      case Some(ce: FsCovariantNode[_]) => Some(ce.get(tµpe))
      case x => x
    }
    new Make(options, np, tµpe, t)
  }

  /** Represents a `make` transaction.
    * The transaction get committed by calling `commit`.
    * The state of the archive file system will not change until this method
    * gets called.
    * The head of the chain of covariant file system entries to commit can get
    * obtained by calling `head`.
    *
    * TODO: The current implementation yields a potential issue: The state of
    * the file system may get altered between the construction of this
    * transaction and the call to its `commit` method.
    * However, the change may render this operation illegal and so the file
    * system may get corrupted upon a call to `commit`.
    * To avoid this, the caller must not allow concurrent changes to this
    * archive file system.
    */
  final class Make(options: AccessOptions, path: String, tµpe: Type, template: Option[Entry]) {
    private var time: Long = UNKNOWN
    private val segments = newSegments(path, tµpe, template)

    private def newSegments(path: String, tµpe: Type, template: Option[Entry]): List[Segment[E]] = {
      splitter.split(path)
      val pp = splitter.getParentPath // may equal ROOT_PATH
      val mn = splitter.getMemberName

      // Lookup parent entry, creating it if necessary and allowed.
      master.get(pp) match {
        case Some(pce) =>
          if (!pce.isType(DIRECTORY))
            throw new NotDirectoryException(path)
          var segments = List[Segment[E]]()
          segments ::= Segment(None, pce)
          val mce = new FsCovariantNode[E](path)
          mce.put(tµpe, newEntry(options, path, tµpe, template))
          segments ::= Segment(Some(mn), mce)
          segments
        case _ =>
          if (options.get(CREATE_PARENTS)) {
            var segments = newSegments(pp, DIRECTORY, None)
            val mce = new FsCovariantNode[E](path)
            mce.put(tµpe, newEntry(options, path, tµpe, template))
            segments ::= Segment(Some(mn), mce)
            segments
          } else {
            throw new NoSuchFileException(path, null,
                                          "Missing parent directory entry!")
          }
      }
    }

    def commit() {
      touch(options)
      val size = commit(segments)
      assert(2 <= size)
      val mae = segments.head.entry.getEntry
      if (UNKNOWN == mae.getTime(WRITE))
        mae.setTime(WRITE, getTimeMillis)
    }

    private def commit(segments: List[Segment[E]]): Int = {
      segments match {
        case Segment(mn, mce) :: parentSegments =>
          val parentSize = commit(parentSegments)
          if (0 < parentSize) {
            val pce = parentSegments.head.entry
            val pae = pce.get(DIRECTORY)
            val mae = mce.getEntry
            master.add(mce.getName, mae)
            if (master.get(pce.getName).get.add(mn.get)
                && UNKNOWN != pae.getTime(WRITE)) // never touch ghost directories!
                  pae.setTime(WRITE, getTimeMillis)
          }
          1 + parentSize
        case _ =>
          0
      }
    }

    private def getTimeMillis = {
      if (UNKNOWN == time) time = System.currentTimeMillis
      time
    }

    def head = segments.head.entry
  } // Make

  /** Tests the named file system entry and then - unless its the file system
    * root - notifies the listener and deletes the entry.
    * For the file system root, only the tests are performed but the listener
    * does not get notified and the entry does not get deleted.
    * For the tests to succeed, the named file system entry must exist and
    * directory entries (including the file system root) must be empty.
    *
    * @param  name the archive file system entry name.
    * @throws IOException on any I/O error.
    */
  def unlink(options: AccessOptions, name: FsNodeName) {
    // Test.
    val np = name.getPath
    val mce = master.get(np) match {
      case Some(mce) => mce
      case _ => throw new NoSuchFileException(name.toString)
    }
    if (mce.isType(DIRECTORY)) {
        val size = mce.getMembers.size
        if (0 != size)
          throw new DirectoryNotEmptyException(name.toString)
    }
    if (name.isRoot) {
      // Removing the root entry MUST get silently ignored in order to
      // make the controller logic work.
      return
    }

    // Notify listener and modify.
    touch(options)
    master.remove(np);
    {
      // See http://java.net/jira/browse/TRUEZIP-144 :
      // This is used to signal to the driver that the entry should not
      // be included in the central directory even if the entry is
      // already physically present in the archive file (ZIP).
      // This signal will be ignored by drivers which do no support a
      // central directory (TAR).
      val mae = mce.getEntry
      for (tµpe <- ALL_SIZES)
          mae.setSize(tµpe, UNKNOWN)
      for (tµpe <- ALL_ACCESS)
          mae.setTime(tµpe, UNKNOWN)
    }
    splitter.split(np)
    val pp = splitter.getParentPath
    val pce = master.get(pp).get
    val ok = pce.remove(splitter.getMemberName)
    assert(ok, "The parent directory of \"" + name.toString
                + "\" does not contain this entry - archive file system is corrupted!")
    val pae = pce.get(DIRECTORY)
    if (UNKNOWN != pae.getTime(WRITE)) // never touch ghost directories!
        pae.setTime(WRITE, System.currentTimeMillis)
  }

  /** Returns a new archive entry.
    *
    * Note that this is just a factory method and the returned file system entry
    * is not (yet) linked into this (virtual) archive file system.
    *
    * @param  name the entry name.
    * @param  tµpe the entry type.
    * @param  template if not `None`, then the new entry shall inherit
    *         as much properties from this entry as possible - with the
    *         exception of its name and type.
    * @return A new entry for the given name.
    */
  private def newEntry(name: String, tµpe: Type, template: Option[Entry]) = {
    assert(!isRoot(name) || DIRECTORY.eq(tµpe))
    driver.newEntry(NONE, name, tµpe, template.orNull)
  }

  /** Returns a new archive entry.
    * This version checks that the given entry name can get encoded by the
    * driver's character set.
    *
    * Note that this is just a factory method and the returned file system entry
    * is not (yet) linked into this (virtual) archive file system.
    *
    * @param  name the entry name.
    * @param  options a bit field of access options.
    * @param  tµpe the entry type.
    * @param  template if not `None`, then the new entry shall inherit
    *         as much properties from this entry as possible - with the
    *         exception of its name and type.
    * @return A new entry for the given name.
    * @throws CharConversionException If the entry name contains characters
    *         which cannot get encoded.
    * @see    #make
    */
  private def newEntry(options: AccessOptions, name: String, tµpe: Type, template: Option[Entry]) = {
    assert(!isRoot(name))
    val driver = this.driver
    driver.checkEncodable(name)
    driver.newEntry(options, name, tµpe, template.orNull)
  }

  /** Marks this (virtual) archive file system as touched and notifies the
    * listener if and only if the touch status is changing.
    *
    * @throws IOException If the listener's preTouch implementation vetoed
    *         the operation for any reason.
    */
  private def touch(options: AccessOptions) {
    if (!touched) {
      controller.preTouch(options)
      touched = true
    }
  }
} // ArchiveFileSystem

private object ArchiveFileSystem {
  private val RootPath = ROOT.getPath

  /** Returns a new empty archive file system and ensures its integrity.
    * Only the root directory is created with its last modification time set
    * to the system's current time.
    * The file system is modifiable and marked as touched!
    *
    * @tparam E The type of the archive entries.
    * @param  driver the archive driver to use.
    * @return A new archive file system.
    */
  def apply[E <: FsArchiveEntry](controller: Controller[E]) =
    new ArchiveFileSystem(controller)

  /** Returns a new archive file system which populates its entries from
    * the given `archive` and ensures its integrity.
    *
    * First, the entries from the archive are loaded into the file system.
    *
    * Second, a root directory with the given last modification time is
    * created and linked into the filesystem (so it's never loaded from the
    * archive).
    *
    * Finally, the file system integrity is checked and fixed: Any missing
    * parent directories are created using the system's current time as their
    * last modification time - existing directories will never be replaced.
    *
    * Note that the entries in the file system are shared with the given
    * `archive`.
    *
    * @tparam E The type of the archive entries.
    * @param  driver the archive driver to use.
    * @param  archive The archive entry container to read the entries for
    *         the population of the archive file system.
    * @param  rootTemplate The optional template to use for the root entry of
    *         the returned archive file system.
    * @param  readOnly If and only if `true`, any subsequent
    *         modifying operation on the file system will result in a
    *         [[net.java.truevfs.kernel.impl.FsReadOnlyFileSystemException]].
    *@return A new archive file system.
    */
  def apply[E <: FsArchiveEntry](controller: ArchiveFileSystem.Controller[E], archive: Container[E], rootTemplate: Option[Entry], readOnly: Boolean) = {
    if (readOnly) new ReadOnlyArchiveFileSystem(controller, archive, rootTemplate)
    else new ArchiveFileSystem(controller, archive, rootTemplate)
  }

  private def typeName(entry: FsCovariantNode[_ <: Entry]): String = {
    val types = entry.getTypes
    if (1 == types.cardinality) typeName(types.iterator.next)
    else types.toString.toLowerCase(Locale.ROOT)
  }

  private def typeName(tµpe: Type) = tµpe.toString.toLowerCase(Locale.ROOT)

  /** The master archive entry table.
    *
    * @tparam E The type of the archive entries.
    */
  final class EntryTable[E <: FsArchiveEntry](_initialSize: Int)
  extends Iterable[FsCovariantNode[E]] {

    /** The map of covariant file system entries.
      *
      * Note that the archive entries in the covariant file system entries
      * in this map are shared with the constructor parameter  `archive` of
      * the archive file system object.
      */
    private[this] val map = new collection.mutable.LinkedHashMap[String, FsCovariantNode[E]] {
      // See https://issues.scala-lang.org/browse/SI-5804 .
      table = new Array(initialCapacity(_initialSize))
      threshold = (table.size * 3L / 4).toInt
    }

    override def size = map.size

    override def iterator = map.values.iterator

    def add(name: String, ae: E) = {
      val ce = map.get(name) match {
        case Some(ce) => ce
        case _ =>
          val ce = new FsCovariantNode[E](name)
          map.put(name, ce)
          ce
      }
      ce.put(ae.getType, ae)
      ce
    }

    def get(name: String) = map.get(name)

    def remove(name: String) = map.remove(name)
  } // EntryTable

  private final class Splitter extends PathSplitter(SEPARATOR_CHAR, false) {
    override def getParentPath = {
      val path = super.getParentPath
      if (null ne path) path else RootPath
    }
  } // Splitter

  /**
    * Introduced to decouple the
    * [[net.java.truevfs.kernel.impl.TargetArchiveController]] from
    * this archive file system.
    */
  trait Controller[E <: FsArchiveEntry] {

    def driver: FsArchiveDriver[E]

    /** Returns the mount point of the (federated virtual) file system.
      *
      * @return The mount point of the (federated virtual) file system.
      */
    def mountPoint: FsMountPoint

    /** Called immediately before the source archive file system is going to
      * get modified (touched) for the first time.
      * If this method throws an [[java.io.IOException]], then the modification
      * is effectively vetoed.
      *
      * @throws IOException at the discretion of the implementation.
      */
    def preTouch(options: AccessOptions)
  } // Callback

  /** A case class which represents a path segment for use by
    * [[net.java.truevfs.kernel.impl.ArchiveFileSystem.Make]].
    *
    * @param name the nullable member name for the covariant file system entry.
    * @param entry the covariant file system entry for the nullable member name.
    */
  private final case class Segment[E <: FsArchiveEntry](
    name: Option[String],
    entry: FsCovariantNode[E])
} // ArchiveFileSystem
