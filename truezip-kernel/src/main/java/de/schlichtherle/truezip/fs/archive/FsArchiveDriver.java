/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.FsConcurrentModel;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsCachingController;
import de.schlichtherle.truezip.fs.FsConcurrentController;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.Icon;
import net.jcip.annotations.Immutable;

/**
 * An abstract archive driver which provides access to a federated file system
 * which is persistet in an archive file format like ZIP, JAR, TZP, TAR,
 * TAR.GZ, TAR.BZ2 etc.
 *
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public abstract class FsArchiveDriver<E extends FsArchiveEntry>
extends FsDriver {

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link FsArchiveDriver} always returns
     * {@code true}.
     * This can't get overridden.
     */
    @Override
    public final boolean isFederated() {
        return true;
    }

    /**
     * Returns the I/O pool to use for allocating temporary I/O entries.
     *
     * @return The I/O pool to use for allocating temporary I/O entries.
     */
    protected abstract IOPool<?> getPool();

    /**
     * Returns a new thread-safe file system controller for the given mount
     * point and parent file system controller.
     * <p>
     * When called, the following expression is a precondition:
     * {@code mountPoint.getParent().equals(parent.getModel().getMountPoint())}
     * <p>
     * Note that an archive file system is always federated and therefore
     * its parent file system controller is never {@code null}.
     * <p>
     * Furthermore, an archive driver implementation is <em>not</em> expected
     * to consider the scheme of the given mount point to determine the class
     * of the returned file system controller.
     * Consequently, it is an error to call this method with a mount point
     * which has a scheme which is not supported by this archive driver.
     * <p>
     * Note again that the returned file system controller must be thread-safe!
     *
     * @param  model the file system model.
     * @param  parent the nullable parent file system controller.
     * @return A new thread-safe file system controller for the given mount
     *         point and parent file system controller.
     */
    @Override
    public FsController<?>
    newController(FsModel model, FsController<?> parent) {
        return  new FsConcurrentController(
                   new FsCachingController(
                        new FsDefaultArchiveController<E>(
                            new FsConcurrentModel(model),
                            this, parent, false),
                        getPool()));
    }

    /**
     * Creates a new input shop for reading the archive entries for the
     * given {@code model} from the given {@code input} socket's target.
     * 
     * @param  model the file system model.
     * @param  input the input socket for reading the contents of the archive
     *         from its target.
     * @return A new input shop.
     * @throws TabuFileException if the target archive file is temporarily not
     *         accessible, e.g. if a key for decryption is currently not
     *         available.
     *         The client application will recognize the target archive file
     *         as a <i>tabu file</i> until a subsequent repetition of this
     *         method call succeeds.
     *         A tabu file will not be accessible through the API although it
     *         exists.
     * @throws FileNotFoundException if the target archive file does not exist
     *         or is (temporarily) not accessible.
     *         An exception of this type marks a <em>transient</em> cause,
     *         i.e. this method may get subsequently called until it succeeds.
     * @throws IOException if the target archive file is a
     *         <i>false positive</i> archive file.
     *         An exception of this type marks a <em>persistent</em> cause,
     *         i.e. this method will not get called again until the archive
     *         file system is synchronized with its parent file system.
     */
    public abstract InputShop<E>
    newInputShop(   FsModel model,
                    InputSocket<?> input)
    throws IOException;

    /**
     * Creates a new output shop for writing archive entries for the
     * given {@code model} to the given {@code output} socket's target.
     * 
     * @param  model the file system model.
     * @param  output the output socket for writing the contents of the
     *         archive to its target.
     * @param  source the {@link InputShop} if {@code archive} is going to get
     *         updated.
     *         If not {@code null}, this is guaranteed to be a product
     *         of this driver's {@link #newInputShop} factory method, which may
     *         be used to copy some meta data which is specific to the type of
     *         archive this driver supports.
     *         For example, this could be used to copy the comment of a ZIP
     *         file.
     * @return A new output shop.
     * @throws FileNotFoundException if the target archive file does not exist
     *         or is (temporarily) not accessible.
     *         An exception of this type marks a <em>transient</em> cause,
     *         i.e. this method may get subsequently called until it succeeds.
     * @throws IOException if the target archive file is a
     *         <i>false positive</i> archive file.
     *         An exception of this type marks a <em>persistent</em> cause,
     *         i.e. this method will not get called again until the archive
     *         file system is synchronized with its parent file system.
     */
    public abstract OutputShop<E>
    newOutputShop(  FsModel model,
                    OutputSocket<?> output,
                    @CheckForNull InputShop<E> source)
    throws IOException;

    /**
     * Returns a new archive entry for the given name.
     * The implementation may need to fix this name in order to 
     * form a valid {@link Entry#getName() entry name} for their
     * particular requirements.
     * <p>
     * If {@code template} is not {@code null}, then the returned entry shall
     * inherit as much properties from this template as possible - with the
     * exception of its name and type.
     * Furthermore, if {@code name} and {@code type} are equal to the name and
     * type of this template, then the returned entry shall be a (deep) clone
     * of the template which shares no mutable objects with the template.
     *
     * @param  name an entry name.
     * @param  type an entry type.
     * @param  template if not {@code null}, then the new entry shall inherit
     *         as much properties from this entry as possible - with the
     *         exception of its name and type.
     * @return A new entry for the given name.
     * @throws CharConversionException if {@code name} contains characters
     *         which are invalid.
     */
    public abstract E
    newEntry(String name, Type type, @CheckForNull Entry template)
    throws CharConversionException;

    /**
     * Returns the icon that should be displayed for the given archive file
     * if it's open/expanded in the view.
     * <p>
     * The implementation in the abstract class {@code FsArchiveDriver} simply
     * returns {@code null}.
     *
     * @param  model the file system model.
     * @return The icon that should be displayed for the given archive file
     *         if it's open/expanded in the view.
     *         If {@code null} is returned, a default icon should be displayed.
     */
    public @CheckForNull Icon getOpenIcon(FsModel model) {
        return null;
    }

    /**
     * Returns the icon that should be displayed for the given archive file
     * if it's closed/collapsed in the view.
     * <p>
     * The implementation in the abstract class {@code FsArchiveDriver} simply
     * returns {@code null}.
     *
     * @param  model the file system model.
     * @return The icon that should be displayed for the given archive file
     *         if it's closed/collapsed in the view.
     *         If {@code null} is returned, a default icon should be displayed.
     */
    public @CheckForNull Icon getClosedIcon(FsModel model) {
        return null;
    }
}
