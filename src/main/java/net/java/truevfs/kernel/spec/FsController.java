/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.IOException;
import java.lang.annotation.Inherited;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.Entry.Access;
import net.java.truecommons.cio.Entry.Type;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.OutputSocket;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.ImplementationsShouldExtend;
import static net.java.truevfs.kernel.spec.FsAssertion.Level.*;

/**
 * Provides read/write access to a file system.
 *
 * <h3>General Properties</h3>
 * <p>
 * The {@link FsModel#getMountPoint() mount point} of the
 * {@linkplain #getModel() file system model}
 * addresses the file system accessed by this controller.
 * Where the methods of this abstract class accept a
 * {@link FsNodeName file system node name} as a parameter, this MUST get
 * resolved against the {@link FsModel#getMountPoint() mount point} URI of this
 * controller's file system model.
  * <p>
 * As of TrueVFS 0.10, application level transactions are not supported,
 * that is, multiple file system operations cannot get composed into a single
 * application level transaction - support for this feature may be added in a
 * future version.
 * <p>
 * However, individual file system operations do come with assertions about
 * their atomicity, consistency, isolation and durability.
 * Each method of this interface which is expected to access the file system
 * (rather than just memory) is annotated with an {@link FsAssertion}.
 * The annotation is {@link Inherited}, so the assertion forms part of the
 * contract which any implementation of this interface should comply to.
 * <p>
 * Note that file system controllers are generally allowed to buffer any
 * changes made to their file system.
 * This is generally true for file system controllers which operate on archive
 * files.
 * This means that all changes made to the file system via this interface may
 * not be entirely durable until they get committed by calling {@link #sync}.
 * The annotations account for this stipulation by leaving the
 * {@link FsAssertion#durable() durability} property undefined.
 * <p>
 * An implementation which wants to buffer its changes until {@code sync} gets
 * called needs to notify the {@linkplain FsManager file system manager} by
 * calling {@link FsModel#setMounted setMounted(true)} on the controller's
 * file system model before the first change is commenced.
 * Likewise, when {@code sync} gets called, the controller needs to notify the
 * file system manager by calling {@code setMounted(false)} on the controller's
 * file system model if and only if the {@code sync} has been successfully
 * completed.
 * This protocol enables proper management of the controller's life cycle.
 * <p>
 * Implementations should be safe for multi-threaded access.
 *
 * @see    FsManager
 * @see    FsModel
 * @see    <a href="http://www.ietf.org/rfc/rfc2119.txt">RFC 2119: Key words for use in RFCs to Indicate Requirement Levels</a>
 * @author Christian Schlichtherle
 */
@ImplementationsShouldExtend(FsAbstractController.class)
public interface FsController {

    /**
     * Returns the controller for the parent file system or {@code null} if
     * and only if this file system is not federated, i.e. not a member of
     * another file system.
     * Multiple invocations must return the same object.
     *
     * @return The nullable controller for the parent file system.
     */
    @Nullable FsController getParent();

    /**
     * Returns the file system model.
     *
     * @return The file system model.
     */
    FsModel getModel();

    /**
     * Returns the file system node for the given {@code name} or {@code null}
     * if it doesn't exist.
     * Modifying the returned node does not show any effect on the file system
     * and should result in an {@link UnsupportedOperationException}.
     *
     * @param  options the options for accessing the file system node.
     * @param  name the name of the file system node.
     * @return A file system node or {@code null} if no file system node
     *         exists for the given name.
     * @throws IOException on any I/O error.
     */
    @FsAssertion(atomic=YES, consistent=YES, isolated=YES, durable=NOT_APPLICABLE)
    @CheckForNull FsNode node(
            BitField<FsAccessOption> options,
            FsNodeName name)
    throws IOException;

    /**
     * Checks if the file system node for the given {@code name} exists when
     * constrained by the given access {@code options} and permits the given
     * access {@code types}.
     *
     * @param  options the options for accessing the file system node.
     * @param  name the name of the file system node.
     * @param  types the types of the desired access.
     * @throws IOException on any I/O error.
     */
    @FsAssertion(atomic=YES, consistent=YES, isolated=YES, durable=NOT_APPLICABLE)
    void checkAccess(
            BitField<FsAccessOption> options,
            FsNodeName name,
            BitField<Access> types)
    throws IOException;

    /**
     * Sets the named file system node as read-only.
     * This method will fail for typical archive file system controller
     * implementations because they do not support it.
     *
     * @param  options the options for accessing the file system node.
     * @param  name the name of the file system node.
     * @throws IOException on any I/O error or if this operation is not
     *         supported.
     */
    @FsAssertion(atomic=YES, consistent=YES, isolated=YES)
    void setReadOnly(BitField<FsAccessOption> options, FsNodeName name)
    throws IOException;

    /**
     * Makes an attempt to set the last access time of all types in the given
     * map for the file system node with the given name.
     * If {@code false} is returned or an {@link IOException} is thrown, then
     * still some of the last access times may have been set.
     * Whether or not this is an atomic operation is specific to the
     * implementation.
     *
     * @param  options the options for accessing the file system node.
     * @param  name the name of the file system node.
     * @param  times the access times.
     * @return {@code true} if and only if setting the access time for all
     *         types in {@code times} succeeded.
     * @throws IOException on any I/O error.
     * @throws NullPointerException if any key or value in the map is
     *         {@code null}.
     */
    @FsAssertion(atomic=NO, consistent=YES, isolated=YES)
    boolean setTime(
            BitField<FsAccessOption> options,
            FsNodeName name,
            Map<Access, Long> times)
    throws IOException;

    /**
     * Makes an attempt to set the last access time of all types in the given
     * bit field for the file system node with the given name.
     * If {@code false} is returned or an {@link IOException} is thrown, then
     * still some of the last access times may have been set.
     *
     * @param  options the options for accessing the file system node.
     * @param  name the name of the file system node.
     * @param  types the access types.
     * @param  value the last access time.
     * @return {@code true} if and only if setting the access time for all
     *         types in {@code types} succeeded.
     * @throws IOException on any I/O error.
     */
    @FsAssertion(atomic=NO, consistent=YES, isolated=YES)
    boolean setTime(
            BitField<FsAccessOption> options,
            FsNodeName name,
            BitField<Access> types,
            long value)
    throws IOException;

    /**
     * Returns an input socket for reading the contents of the file system
     * node addressed by the given name from the file system.
     * Note that the assertions for this file system operation equally apply to
     * any channel or stream created by the returned input socket!
     *
     * @param  options the options for accessing the file system node.
     * @param  name the name of the file system node.
     * @return An {@code InputSocket}.
     */
    @FsAssertion(atomic=YES, consistent=YES, isolated=YES, durable=NOT_APPLICABLE)
    InputSocket<? extends Entry> input(
            BitField<FsAccessOption> options,
            FsNodeName name);

    /**
     * Returns an output socket for writing the contents of the node addressed
     * by the given name to the file system.
     * Note that the assertions for this file system operation equally apply to
     * any channel or stream created by the returned output socket!
     *
     * @param  options the options for accessing the file system node.
     *         If {@link FsAccessOption#CREATE_PARENTS} is set, any missing
     *         parent directories shall get created with an undefined last
     *         modification time.
     * @param  name the name of the file system node.
     * @param  template if not {@code null}, then the file system node
     *         at the end of the chain shall inherit as much properties from
     *         this node as possible - with the exception of its name and type.
     * @return An {@code OutputSocket}.
     */
    @FsAssertion(atomic=YES, consistent=YES, isolated=YES)
    OutputSocket<? extends Entry> output(
            BitField<FsAccessOption> options,
            FsNodeName name,
            @CheckForNull Entry template);

    /**
     * Creates or replaces and finally links a chain of one or more entries
     * for the given node {@code name} into the file system.
     *
     * @param  options the options for accessing the file system node.
     *         If {@link FsAccessOption#CREATE_PARENTS} is set, any missing
     *         parent directories shall get created with an undefined last
     *         modification time.
     * @param  name the name of the file system node.
     * @param  type the file system node type.
     * @param  template if not {@code null}, then the file system node
     *         at the end of the chain shall inherit as much properties from
     *         this node as possible - with the exception of its name and type.
     * @throws IOException on any I/O error, including but not limited to
     *         these reasons:
     *         <ul>
     *         <li>The file system is read only.
     *         <li>{@code name} contains characters which are not
     *             supported by the file system.
     *         <li>The node already exists and either the option
     *             {@link FsAccessOption#EXCLUSIVE} is set or the node is a
     *             directory.
     *         <li>The node exists as a different type.
     *         <li>A parent node exists but is not a directory.
     *         <li>A parent node is missing and {@code createParents} is
     *             {@code false}.
     *         </ul>
     */
    @FsAssertion(atomic=YES, consistent=YES, isolated=YES)
    void make(
            BitField<FsAccessOption> options,
            FsNodeName name,
            Type type,
            @CheckForNull Entry template)
    throws IOException;

    /**
     * Removes the named file system node from the file system.
     * If the named file system node is a directory, it must be empty.
     *
     * @param  options the options for accessing the file system node.
     * @param  name the name of the file system node.
     * @throws IOException on any I/O error.
     */
    @FsAssertion(atomic=YES, consistent=YES, isolated=YES)
    void unlink(BitField<FsAccessOption> options, FsNodeName name)
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
     * <p>
     * An implementation may ignore calls to this method if its stateless.
     *
     * @param  options the options for synchronizing the file system.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if an unclosed archive entry
     *         stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     */
    @FsAssertion(atomic=NO, consistent=YES, isolated=YES)
    void sync(BitField<FsSyncOption> options) throws FsSyncException;

    /**
     * A factory for {@linkplain FsController file system controllers}.
     * <p>
     * Implementations should be safe for multi-threaded access.
     *
     * @param  <Context> The type of the calling context.
     * @author Christian Schlichtherle
     */
    interface Factory<Context> {

        /**
         * Returns a new file system controller for the mount point of the
         * given file system model.
         * This is a pure function without side effects.
         * <p>
         * When called, you may assert the following precondition:
         * <pre>{@code
         * assert null == parent
         *         ? null == model.getParent()
         *         : parent.getModel().equals(model.getParent())
         * }</pre>
         *
         * @param  context the calling context.
         * @param  model the file system model.
         * @param  parent the nullable parent file system controller.
         * @return A new file system controller for the mount point of the
         *         given file system model.
         */
        FsController newController(
                Context context,
                FsModel model,
                @CheckForNull FsController parent);
    }
}
