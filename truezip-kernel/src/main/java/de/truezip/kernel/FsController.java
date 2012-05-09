/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.util.BitField;
import java.io.IOException;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Provides read/write access to a file system.
 * Implementations of this interface are typically organized in a chain of
 * responsibility for file system federation and a decorator chain for
 * implementing different aspects of the management of the file system state,
 * e.g. lock management for concurrent access.
 * 
 * <h3>General Properties</h3>
 * <p>
 * The {@link FsModel#getMountPoint() mount point} of the
 * {@link #getModel() file system model} addresses the file system at the head
 * of this chain of federated file systems.
 * Where the methods of this abstract class accept a
 * {@link FsEntryName file system entry name} as a parameter, this MUST get
 * resolved against the {@link FsModel#getMountPoint() mount point} URI of this
 * controller's {@link #getModel() file system model}.
 * 
 * <h3>Transaction Support</h3>
 * <p>
 * Even on modern computers, I/O operations are inherently unreliable: They
 * can fail on hardware errors, network timeouts, third party interactions etc.
 * In an ideal world, we would like all file system operations to be truly
 * transactional like some relational database services.
 * However, file system have to cope with really big data, much more than most
 * relational databases will ever see.
 * Its not uncommon these days to store some gigabytes of data in a single
 * file, for example a video file.
 * However, buffering gigabytes of data just for an eventual rollback of a
 * transaction is still not a realistic option and considering the fact that
 * faster computers have always been used to store even bigger data then its
 * getting clear that it never will be.
 * Therefore, the contract of this abstract class strives for only limited
 * transactional support as follows.
 * <ol>
 * <li>
 * Generally all file system operations may fail with either a
 * {@link RuntimeException} or an {@link IOException} to respectively indicate
 * wrong input parameters or a file system operation failure.
 * Where the following terms consider a failure, the term equally applies to
 * both exception types.
 * <li>
 * With the exception of {@link #sync}, all file system operations SHOULD be
 * <i>atomic</i>, that is they either succeed or fail completely as if they had
 * not been called.
 * <li>
 * All file system operations MUST be <i>consistent</i>, that is they MUST
 * leave their resources in a state so that they can get retried, even after a
 * failure.
 * <li>
 * All file system operations SHOULD be <i>isolated</i> with respect to any
 * threads which share the same definition of the implementing class, that is
 * two such threads SHOULD NOT interfere with each other's file system
 * operations in any other way than the operation's defined side effect on the
 * stored data.
 * In general, this simply means that file system operations SHOULD be
 * thread-safe.
 * Note that some factory methods declare this as a MUST requirement for their
 * generated file system controllers, for example
 * {@link FsDriver#newController} and {@link FsCompositeDriver#newController}.
 * <li>
 * All file system operations SHOULD be <i>durable</i>, that is their side
 * effect on the stored data SHOULD be permanent in the parent file system or
 * storage system.
 * <li>
 * Once a call to {@link #sync} has succeeded, all previous file system
 * operations MUST be durable.
 * Furthermore, any changes to the stored data in the parent file system or
 * storage system which have been made by third parties up to this point in
 * time MUST be visible to the users of this class.
 * This enables file system operations to use I/O buffers most of the time and
 * eventually synchronize their contents with the parent file system or storage
 * system upon a call to {@code sync}.
 * </ol>
 * 
 * @param  <M> the type of the file system model.
 * @see    FsManager
 * @see    <a href="http://www.ietf.org/rfc/rfc2119.txt">RFC 2119: Key words for use in RFCs to Indicate Requirement Levels</a>
 * @author Christian Schlichtherle
 */
public interface FsController<M extends FsModel> {

    /**
     * Returns the file system model.
     * 
     * @return The file system model.
     */
    M getModel();

    /**
     * Returns the controller for the parent file system or {@code null} if
     * and only if this file system is not federated, i.e. not a member of
     * another file system.
     * Multiple invocations must return the same object.
     * 
     * @return The nullable controller for the parent file system.
     */
    @Nullable FsController<?> getParent();

    /**
     * Returns {@code true} if and only if the file system is read-only.
     * 
     * @return {@code true} if and only if the file system is read-only.
     * @throws IOException on any I/O error.
     */
    boolean isReadOnly() throws IOException;

    /**
     * Returns the file system entry for the given {@code name} or {@code null}
     * if it doesn't exist.
     * Modifying the returned entry does not show any effect on the file system
     * and should result in an {@link UnsupportedOperationException}.
     * 
     * @param  options the options for accessing the file system entry.
     * @param  name the name of the file system entry.
     * @return A file system entry or {@code null} if no file system entry
     *         exists for the given name.
     * @throws IOException on any I/O error.
     */
    @CheckForNull FsEntry stat(
            BitField<FsAccessOption> options,
            FsEntryName name)
    throws IOException;

    /**
     * Checks if the file system entry for the given {@code name} exists when
     * constrained by the given access {@code options} and permits the given
     * access {@code types}.
     * 
     * @param  options the options for accessing the file system entry.
     * @param  name the name of the file system entry.
     * @param  types the types of the desired access.
     * @throws IOException on any I/O error.
     */
    void checkAccess(
            BitField<FsAccessOption> options,
            FsEntryName name,
            BitField<Access> types)
    throws IOException;

    /**
     * Sets the named file system entry as read-only.
     * This method will fail for typical federated (archive) file system
     * controller implementations because they do not support it.
     * 
     * @param  name the name of the file system entry.
     * @throws IOException on any I/O error or if this operation is not
     *         supported.
     */
    void setReadOnly(FsEntryName name) throws IOException;

    /**
     * Makes an attempt to set the last access time of all types in the given
     * map for the file system entry with the given name.
     * If {@code false} is returned or an {@link IOException} is thrown, then
     * still some of the last access times may have been set.
     * Whether or not this is an atomic operation is specific to the
     * implementation.
     * 
     * @param  options the options for accessing the file system entry.
     * @param  name the name of the file system entry.
     * @param  times the access times.
     * @return {@code true} if and only if setting the access time for all
     *         types in {@code times} succeeded.
     * @throws IOException on any I/O error.
     * @throws NullPointerException if any key or value in the map is
     *         {@code null}.
     */
    boolean setTime(
            BitField<FsAccessOption> options,
            FsEntryName name,
            Map<Access, Long> times)
    throws IOException;

    /**
     * Makes an attempt to set the last access time of all types in the given
     * bit field for the file system entry with the given name.
     * If {@code false} is returned or an {@link IOException} is thrown, then
     * still some of the last access times may have been set.
     * Whether or not this is an atomic operation is specific to the
     * implementation.
     * 
     * @param  options the options for accessing the file system entry.
     * @param  name the name of the file system entry.
     * @param  types the access types.
     * @param  value the last access time.
     * @return {@code true} if and only if setting the access time for all
     *         types in {@code types} succeeded.
     * @throws IOException on any I/O error.
     */
    boolean setTime(
            BitField<FsAccessOption> options,
            FsEntryName name,
            BitField<Access> types,
            long value)
    throws IOException;

    /**
     * Returns an input socket for reading the contents of the file system
     * entry addressed by the given name from the file system.
     *
     * @param  options the options for accessing the file system entry.
     * @param  name the name of the file system entry.
     * @return An {@code InputSocket}.
     */
    InputSocket<? extends Entry> input(
            BitField<FsAccessOption> options,
            FsEntryName name);

    /**
     * Returns an output socket for writing the contents of the entry addressed
     * by the given name to the file system.
     *
     * @param  options the options for accessing the file system entry.
     *         If {@link FsAccessOption#CREATE_PARENTS} is set, any missing
     *         parent directories shall get created with an undefined last
     *         modification time.
     * @param  name the name of the file system entry.
     * @param  template if not {@code null}, then the file system entry
     *         at the end of the chain shall inherit as much properties from
     *         this entry as possible - with the exception of its name and type.
     * @return An {@code OutputSocket}.
     */
    OutputSocket<? extends Entry> output(
            BitField<FsAccessOption> options,
            FsEntryName name,
            @CheckForNull Entry template);

    /**
     * Creates or replaces and finally links a chain of one or more entries
     * for the given entry {@code name} into the file system.
     *
     * @param  options the options for accessing the file system entry.
     *         If {@link FsAccessOption#CREATE_PARENTS} is set, any missing
     *         parent directories shall get created with an undefined last
     *         modification time.
     * @param  name the name of the file system entry.
     * @param  type the file system entry type.
     * @param  template if not {@code null}, then the file system entry
     *         at the end of the chain shall inherit as much properties from
     *         this entry as possible - with the exception of its name and type.
     * @throws IOException on any I/O error, including but not limited to
     *         these reasons:
     *         <ul>
     *         <li>The file system is read only.
     *         <li>{@code name} contains characters which are not
     *             supported by the file system.
     *         <li>The entry already exists and either the option
     *             {@link FsAccessOption#EXCLUSIVE} is set or the entry is a
     *             directory.
     *         <li>The entry exists as a different type.
     *         <li>A parent entry exists but is not a directory.
     *         <li>A parent entry is missing and {@code createParents} is
     *             {@code false}.
     *         </ul>
     */
    void mknod(
            BitField<FsAccessOption> options,
            FsEntryName name,
            Type type,
            @CheckForNull Entry template)
    throws IOException;

    /**
     * Removes the named file system entry from the file system.
     * If the named file system entry is a directory, it must be empty.
     * 
     * @param  options the options for accessing the file system entry.
     * @param  name the name of the file system entry.
     * @throws IOException on any I/O error.
     */
    void unlink(
            BitField<FsAccessOption> options,
            FsEntryName name)
    throws IOException;

    /**
     * Commits all unsynchronized changes to the contents of this file system
     * to its parent file system,
     * releases the associated resources (e.g. target archive files) for
     * access by third parties (e.g. other processes), cleans up any temporary
     * allocated resources (e.g. temporary files) and purges any cached data.
     * Note that temporary resources may get allocated even if the federated
     * file systems were accessed read-only.
     * If this is not a federated file system, i.e. if its not a member of a
     * parent file system, then nothing happens.
     * Otherwise, the state of this file system controller is reset.
     *
     * @param  options the options for synchronizing the file system.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if an unclosed archive entry
     *         stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     */
    void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException;
}
