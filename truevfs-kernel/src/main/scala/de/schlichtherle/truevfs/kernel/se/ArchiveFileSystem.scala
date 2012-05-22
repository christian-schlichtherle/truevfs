/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import ArchiveFileSystem._
import net.truevfs.kernel._
import net.truevfs.kernel.FsAccessOption._
import net.truevfs.kernel.FsAccessOptions._
import net.truevfs.kernel.FsEntryName._
import net.truevfs.kernel.cio._
import net.truevfs.kernel.cio.Entry._
import net.truevfs.kernel.cio.Entry.Access._
import net.truevfs.kernel.cio.Entry.Type._
import net.truevfs.kernel.util._
import net.truevfs.kernel.util.HashMaps._
import net.truevfs.kernel.util.Paths._
import java.nio.file._
import java.util.Locale
import scala.annotation._

/**
 * A read/write virtual file system for archive entries.
 * Have a look at the online <a href="http://truezip.java.net/faq.html">FAQ</a>
 * to get the concept of how this works.
 * 
 * @param  <E> the type of the archive entries.
 * @see    <a href="http://truezip.java.net/faq.html">Frequently Asked Questions</a>
 * @author Christian Schlichtherle
 */
private class ArchiveFileSystem[E <: FsArchiveEntry](
  driver: FsArchiveDriver[E],
  master: EntryTable[E])
extends Iterable[FsCovariantEntry[E]] {

  private val splitter = new Splitter

  /** Whether or not this file system has been modified. */
  private var touched: Boolean = _

  private var _touchListener: Option[TouchListener] = None

  private def this(driver: FsArchiveDriver[E]) {
    this(driver, new EntryTable(OVERHEAD_SIZE))
    val root = newEntry(ROOT_PATH, DIRECTORY, None)
    val time = System.currentTimeMillis()
    for (access <- ALL_ACCESS)
      root.setTime(access, time)
    master.add(ROOT_PATH, root)
    touched = true;
  }

  def this(driver: FsArchiveDriver[E], archive: Container[E], rootTemplate: Option[Entry]) {
    // Allocate some extra capacity to create missing parent directories.
    this(driver, new EntryTable(archive.size + OVERHEAD_SIZE))
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
    master.add(ROOT_PATH, newEntry(ROOT_PATH, DIRECTORY, rootTemplate))
    // Now perform a file system check to create missing parent directories
    // and populate directories with their members - this must be done
    // separately!
    for (path <- paths) fix(path)
  }

  /**
   * Called from a constructor in order to fix the parent directories of the
   * file system entry identified by {@code name}, ensuring that all parent
   * directories of the file system entry exist and that they contain the
   * respective member entry.
   * If a parent directory does not exist, it is created using an unkown time
   * as the last modification time - this is defined to be a
   * <i>ghost directory<i>.
   * If a parent directory does exist, the respective member entry is added
   * (possibly yet again) and the process is continued.
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

  /**
   * Returns a covariant file system entry or {@code null} if no file system
   * entry exists for the given name.
   * Modifying the returned object graph is either not supported (i.e. throws
   * an {@link UnsupportedOperationException}) or does not show any effect on
   * this file system.
   * 
   * @param  name the name of the file system entry to look up.
   * @return A covariant file system entry or {@code null} if no file system
   *         entry exists for the given name.
   */
  def stat(options: AccessOptions, name: FsEntryName) = {
    master.get(name.getPath) match {
      case Some(ce) => Some(ce.clone(driver))
      case x => x
    }
  }

  def checkAccess(options: AccessOptions, name: FsEntryName, types: BitField[Access]) {
    if (master.get(name.getPath).isEmpty)
      throw new NoSuchFileException(name.toString)
  }

  def setReadOnly(name: FsEntryName) {
    throw new FileSystemException(name.toString, null,
        "Cannot set read-only state!");
  }

  def setTime(options: AccessOptions, name: FsEntryName, times: Map[Access, Long]) = {
    val ce = master.get(name.getPath) match {
      case Some(ce) => ce
      case _ => throw new NoSuchFileException(name.toString)
    }
    // HC SVNT DRACONES!
    touch(options)
    val ae = ce.getEntry
    var ok = true
    import collection.JavaConversions._
    for ((access, value) <- times)
        ok &= 0 <= value && ae.setTime(access, value)
    ok
  }

  def setTime(options: AccessOptions, name: FsEntryName, types: BitField[Access], value: Long) = {
    if (0 > value)
      throw new IllegalArgumentException(name.toString
                                         + " (negative access time)")
    val ce = master.get(name.getPath) match {
      case Some(ce) => ce
      case _ => throw new NoSuchFileException(name.toString)
    }
    // HC SVNT DRACONES!
    touch(options)
    val ae = ce.getEntry()
    var ok = true
    for (tµpe <- types)
        ok &= ae.setTime(tµpe, value)
    ok
  }

  /**
   * Begins a <i>transaction</i> to create or replace and finally link a
   * chain of one or more archive entries for the given {@code path} into
   * this archive file system.
   * <p>
   * To commit the transaction, you need to call
   * {@link ArchiveFileSystemOperation#commit} on the returned object, which
   * will mark this archive file system as touched and set the last
   * modification time of the created and linked archive file system entries
   * to the system's current time at the moment of the call to this method.
   *
   * @param  name the archive file system entry name.
   * @param  type the type of the archive file system entry to create.
   * @param  options if {@code CREATE_PARENTS} is set, any missing parent
   *         directories will be created and linked into this file
   *         system with its last modification time set to the system's
   *         current time.
   * @param  template if not {@code null}, then the archive file system entry
   *         at the end of the chain shall inherit as much properties from
   *         this entry as possible - with the exception of its name and type.
   * @throws IOException on any I/O error.
   * @return A new archive file system operation on a chain of one or more
   *         archive file system entries for the given path name which will
   *         be linked into this archive file system upon a call to its
   *         {@link ArchiveFileSystemOperation#commit} method.
   */
  def mknod(options: AccessOptions, name: FsEntryName, tµpe: Type, template: Option[Entry]) = {
    require(null ne tµpe)
    if (FILE.ne(tµpe) && DIRECTORY.ne(tµpe)) // TODO: Add support for other types.
      throw new FileSystemException(name.toString, null,
                                    "Can only create file or directory entries, but not a " + typeName(tµpe) + " entry!")
    val path = name.getPath
    master.get(path).foreach { ce =>
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
      case Some(ce: FsCovariantEntry[_]) => Some(ce.getEntry(tµpe))
      case x => x
    }
    new Mknod(options, path, tµpe, t)
  }

  /**
   * Represents an {@linkplain #mknod} transaction.
   * The transaction get committed by calling {@link #commit}.
   * The state of the archive file system will not change until this method
   * gets called.
   * The head of the chain of covariant file system entries to commit can get
   * obtained by calling {@link #head}.
   * <p>
   * TODO: The current implementation yields a potential issue: The state of
   * the file system may get altered between the construction of this
   * transaction and the call to its {@link #commit} method.
   * However, the change may render this operation illegal and so the file
   * system may get corrupted upon a call to {@link #commit}.
   * To avoid this, the caller must not allow concurrent changes to this
   * archive file system.
   */
  final class Mknod(options: AccessOptions, path: String, tµpe: Type, template: Option[Entry]) {
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
          val mce = new FsCovariantEntry[E](path)
          mce.putEntry(tµpe, newEntry(options, path, tµpe, template))
          segments ::= Segment(Some(mn), mce)
          segments
        case _ =>
          if (options.get(CREATE_PARENTS)) {
            var segments = newSegments(pp, DIRECTORY, None)
            val mce = new FsCovariantEntry[E](path)
            mce.putEntry(tµpe, newEntry(options, path, tµpe, template))
            segments ::= Segment(Some(mn), mce)
            segments
          } else {
            throw new NoSuchFileException(path, null,
                                          "Missing parent directory entry!")
          }
      }
    }

    def commit() {
      touch(options);
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
            val pae = pce.getEntry(DIRECTORY)
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
  } // Mknod

  /**
   * Tests the named file system entry and then - unless its the file system
   * root - notifies the listener and deletes the entry.
   * For the file system root, only the tests are performed but the listener
   * does not get notified and the entry does not get deleted.
   * For the tests to succeed, the named file system entry must exist and
   * directory entries (including the file system root) must be empty.
   *
   * @param  name the archive file system entry name.
   * @throws IOException on any I/O error.
   */
  def unlink(options: AccessOptions, name: FsEntryName) {
    // Test.
    val path = name.getPath
    val mce = master.get(path) match {
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
    master.remove(path);
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
    splitter.split(path)
    val pp = splitter.getParentPath
    val pce = master.get(pp).get
    val ok = pce.remove(splitter.getMemberName)
    assert(ok, "The parent directory of \"" + name.toString
                + "\" does not contain this entry - archive file system is corrupted!")
    val pae = pce.getEntry(DIRECTORY)
    if (UNKNOWN != pae.getTime(WRITE)) // never touch ghost directories!
        pae.setTime(WRITE, System.currentTimeMillis)
  }

  /**
   * Returns a new archive entry.
   * This is just a factory method and the returned file system entry is not
   * (yet) linked into this (virtual) archive file system.
   *
   * @param  name the entry name.
   * @param  options a bit field of access options.
   * @param  type the entry type.
   * @param  template if not {@code null}, then the new entry shall inherit
   *         as much properties from this entry as possible - with the
   *         exception of its name and type.
   * @return A new entry for the given name.
   */
  private def newEntry(name: String, tµpe: Type, template: Option[Entry]) = {
    assert(!isRoot(name) || DIRECTORY.eq(tµpe))
    driver.newEntry(NONE, name, tµpe, template.orNull)
  }

  /**
   * Like {@link #entry entry(name, type, options, template)},
   * but checks that the given entry name can get encoded by the driver's
   * character set.
   *
   * @param  name the entry name.
   * @param  options a bit field of access options.
   * @param  type the entry type.
   * @param  template if not {@code null}, then the new entry shall inherit
   *         as much properties from this entry as possible - with the
   *         exception of its name and type.
   * @return A new entry for the given name.
   * @throws CharConversionException If the entry name contains characters
   *         which cannot get encoded.
   * @see    #mknod
   */
  private def newEntry(options: AccessOptions, name: String, tµpe: Type, template: Option[Entry]) = {
    assert(!isRoot(name))
    driver.checkEncodable(name)
    driver.newEntry(options, name, tµpe, template.orNull)
  }

  /**
   * Marks this (virtual) archive file system as touched and notifies the
   * listener if and only if the touch status is changing.
   *
   * @throws IOException If the listener's preTouch implementation vetoed
   *         the operation for any reason.
   */
  private def touch(options: AccessOptions) {
    if (!touched) {
      _touchListener.foreach(_.preTouch(options))
      touched = true
    }
  }

  /** Gets the archive file system touch listener. */
  final def touchListener = _touchListener

  /**
   * Sets the archive file system touch listener.
   *
   * @param  listener the listener for archive file system events.
   * @throws IllegalStateException if {@code listener} is not null and the
   *         touch listener has already been set.
   */
  final def touchListener_=(listener: Option[TouchListener]) {
    if (listener.isDefined && _touchListener.isDefined)
      throw new IllegalStateException("The touch listener has already been set!")
    _touchListener = listener
  }
} // ArchiveFileSystem

private object ArchiveFileSystem {
  private val ROOT_PATH = ROOT.getPath

  /**
   * Returns a new empty archive file system and ensures its integrity.
   * Only the root directory is created with its last modification time set
   * to the system's current time.
   * The file system is modifiable and marked as touched!
   *
   * @param  <E> The type of the archive entries.
   * @param  driver the archive driver to use.
   * @return A new archive file system.
   * @throws NullPointerException If {@code factory} is {@code null}.
   */
  def apply[E <: FsArchiveEntry](driver: FsArchiveDriver[E]) =
    new ArchiveFileSystem(driver)

  /**
   * Returns a new archive file system which populates its entries from
   * the given {@code archive} and ensures its integrity.
   * <p>
   * First, the entries from the archive are loaded into the file system.
   * <p>
   * Second, a root directory with the given last modification time is
   * created and linked into the filesystem (so it's never loaded from the
   * archive).
   * <p>
   * Finally, the file system integrity is checked and fixed: Any missing
   * parent directories are created using the system's current time as their
   * last modification time - existing directories will never be replaced.
   * <p>
   * Note that the entries in the file system are shared with the given
   * archive entry {@code container}.
   *
   * @param  <E> The type of the archive entries.
   * @param  driver the archive driver to use.
   * @param  archive The archive entry container to read the entries for
   *         the population of the archive file system.
   * @param  rootTemplate The nullable template to use for the root entry of
   *         the returned archive file system.
   * @param  readOnly If and only if {@code true}, any subsequent
   *         modifying operation on the file system will result in a
   *         {@link FsReadOnlyFileSystemException}.
   * @return A new archive file system.
   * @throws NullPointerException If {@code factory} or {@code archive} are
   *         {@code null}.
   */
  def apply[E <: FsArchiveEntry](driver: FsArchiveDriver[E], archive: Container[E], rootTemplate: Option[Entry], readOnly: Boolean) = {
    if (readOnly) new ReadOnlyArchiveFileSystem(driver, archive, rootTemplate)
    else new ArchiveFileSystem(driver, archive, rootTemplate)
  }

  private def typeName(entry: FsCovariantEntry[_ <: Entry]): String = {
    val types = entry.getTypes
    if (1 == types.cardinality) typeName(types.iterator.next)
    else types.toString.toLowerCase(Locale.ENGLISH)
  }

  private def typeName(tµpe: Type) = tµpe.toString.toLowerCase(Locale.ENGLISH)

  /**
   * The master archive entry table.
   * 
   * @param <E> The type of the archive entries.
   */
  final class EntryTable[E <: FsArchiveEntry](_initialSize: Int)
  extends Iterable[FsCovariantEntry[E]] {

    /**
     * The map of covariant file system entries.
     * <p>
     * Note that the archive entries in the covariant file system entries
     * in this map are shared with the {@link Container} object
     * provided to the constructor of this class.
     */
    private[this] val map = new collection.mutable.LinkedHashMap[String, FsCovariantEntry[E]] {
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
          val ce = new FsCovariantEntry[E](name)
          map.put(name, ce)
          ce
      }
      ce.putEntry(ae.getType, ae)
      ce
    }

    def get(name: String) = map.get(name)

    def remove(name: String) = map.remove(name)
  } // EntryTable

  private final class Splitter extends PathSplitter(SEPARATOR_CHAR, false) {
    override def getParentPath = {
      val path = super.getParentPath
      if (null ne path) path else ROOT_PATH
    }
  } // Splitter

  /** Used to notify implementations of an event in this file system. */
  trait TouchListener {

    /**
     * Called immediately before the source archive file system is going to
     * get modified (touched) for the first time.
     * If this method throws an {@code IOException}), then the modification
     * is effectively vetoed.
     *
     * @throws IOException at the discretion of the implementation.
     */
    def preTouch(options: AccessOptions)
  } // TouchListener

  /**
   * A case class which represents a path segment for use by {@link Mknod}.
   * 
   * @param name the nullable member name for the covariant file system entry.
   * @param entry the covariant file system entry for the nullable member name.
   */
  private final case class Segment[E <: FsArchiveEntry](
    name: Option[String],
    entry: FsCovariantEntry[E])
} // ArchiveFileSystem
