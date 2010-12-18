/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.archive.driver;

import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.filesystem.MountPoint;
import de.schlichtherle.truezip.io.archive.controller.CachingArchiveController;
import de.schlichtherle.truezip.io.archive.controller.ConcurrentArchiveController;
import de.schlichtherle.truezip.io.archive.controller.UpdatingArchiveController;
import de.schlichtherle.truezip.io.archive.model.ArchiveModel;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.entry.EntryFactory;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.filesystem.FileSystemEntryName.*;
import static de.schlichtherle.truezip.io.Paths.*;

/**
 * An abstract archive driver implementation to ease the task of developing
 * an archive driver.
 * It provides default implementations for character sets and icon handling.
 * <p>
 * This class is serializable in order to meet the requirements of some client
 * classes.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class AbstractArchiveDriver<E extends ArchiveEntry>
implements ArchiveDriver<E>, Serializable {

    private static final long serialVersionUID = 6546816446546846516L;

    private static final String CLASS_NAME
            = AbstractArchiveDriver.class.getName();
    private static final Logger LOGGER
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    private final String charset;

    /**
     * This field should be considered to be {@code final}!
     *
     * @see #assertEncodable
     */
    private transient ThreadLocalEncoder encoder; // never transmit this over the wire!

    /**
     * Constructs a new abstract archive driver.
     *
     * @param charset The name of a character set to use by default for all
     *        entry names and probably other meta data when reading or writing
     *        archive files.
     * @throws NullPointerException If {@code charset} is {@code null}.
     * @throws UnsupportedCharsetException If {@code charset} is not
     *         supported by both the JSE 1.1 API and JSE 1.4 API.
     * @throws InconsistentCharsetSupportError If {@code charset} is
     *         supported by the JSE 1.1 API, but not the JSE 1.4 API,
     *         or vice versa.
     */
    protected AbstractArchiveDriver(final String charset) {
        this.charset = charset;
        this.encoder = new ThreadLocalEncoder();

        // Perform fail fast tests for character set charsets using both
        // JSE 1.1 API and the NIO API.
        final UnsupportedEncodingException uee = testJSE11Support(charset);
        final UnsupportedCharsetException  uce = testJSE14Support(charset);
        if (uee != null || uce != null) {
            if (uee == null)
                throw new InconsistentCharsetSupportError(charset, uce);
            if (uce == null)
                throw new InconsistentCharsetSupportError(charset, uee);
            throw uce; // throw away uee - it has same reason
        }

        assert invariants();
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    private static UnsupportedEncodingException testJSE11Support(
            final String charset) {
        try {
            new String(new byte[0], charset);
        } catch (UnsupportedEncodingException ex) {
            return ex;
        }
        return null;
    }

    private static UnsupportedCharsetException testJSE14Support(
            final String charset) {
        try {
            final Charset impl = Charset.forName(charset);
            LOGGER.log(Level.CONFIG, "charset.class", new Object[] { // NOI18N
                charset,
                impl.name(),
                impl.getClass().getName(),
            });
        } catch (UnsupportedCharsetException ex) {
            return ex;
        }
        return null;
    }

    /**
     * Checks the invariants of this class and throws an AssertionError if
     * any is violated even if assertion checking is disabled.
     * <p>
     * The constructors call this method like this:
     * <pre>{@code assert invariants(); }</pre>
     * This calls the method if and only if assertions are enabled in order
     * to assert that the instance invariants are properly obeyed.
     * If assertions are disabled, the call to this method is thrown away by
     * the HotSpot compiler, so there is no performance penalty.
     * <p>
     * When deserializing however, this method is called regardless of the
     * assertion status. On error, the {@link AssertionError} is wrapped
     * in an {@link InvalidObjectException} and thrown instead.
     *
     * @throws AssertionError If any invariant is violated even if assertions
     *         are disabled.
     * @return {@code true}
     */
    private boolean invariants() {
        if (charset == null)
            throw new AssertionError("character set not initialized");
        try {
            assertEncodable("");
        } catch (CharConversionException ex) {
            throw new AssertionError(ex);
        }
        return true;
    }

    /**
     * Postfixes the instance after its default deserialization.
     *
     * @throws InvalidObjectException If the instance invariants are not met.
     */
    private void readObject(final ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        assert encoder == null;
        encoder = new ThreadLocalEncoder();

        try {
            invariants();
        } catch (AssertionError ex) {
            throw (InvalidObjectException) new InvalidObjectException(ex.toString()).initCause(ex);
        }
    }

    /**
     * Maps the given <i>path name</i> to an <i>entry name</i> for ZIP or TAR
     * files by ensuring that the returned entry name ends with the separator
     * character {@code '/'} if and only if {@code type} is {@code DIRECTORY}.
     * <p>
     * First, {@link #assertEncodable(String) assertEncodable(path)} is called.
     *
     * @see    EntryFactory#newEntry Common Requirements For Path Names
     * @param  path a non-{@code null} <i>path name</i>.
     * @param  type a non-{@code null} entry type.
     * @return A non-{@code null} <i>entry name</i>.
     */
    protected final String toZipOrTarEntryName(
            final String path,
            final Type type)
    throws CharConversionException {
        assertEncodable(path);
        switch (type) {
            case DIRECTORY:
                return path.endsWith(SEPARATOR) ? path : path + SEPARATOR_CHAR;
            default:
                return cutTrailingSeparators(path, SEPARATOR_CHAR);
        }
    }

    /**
     * Ensures that the given path name can be encoded by this driver's
     * character set.
     * Should be called by sub classes in their implementation of the method
     * {@link EntryFactory#newEntry}.
     * 
     * @see    EntryFactory#newEntry Common Requirements For Path Names
     * @param  path a non-{@code null} path name.
     * @see    #getCharset
     * @throws CharConversionException If the path name contains characters
     *         which cannot get encoded.
     */
    protected final void assertEncodable(String path)
    throws CharConversionException {
        if (!encoder.canEncode(path))
            throw new CharConversionException(path +
                    " (illegal characters in entry name)");
    }

    /**
     * Returns the value of the property {@code charset} which was
     * provided to the constructor.
     */
    public final String getCharset() {
        return charset;
    }

    @Override
    public FileSystemController<? extends ArchiveModel> newController(
            @NonNull MountPoint mountPoint,
            @NonNull FileSystemController<?> parent) {
        return new ConcurrentArchiveController(
                    new CachingArchiveController(
                        new UpdatingArchiveController<E>( // TODO: Support append strategy.
                            new ArchiveModel(mountPoint, parent.getModel()),
                            this, parent)));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@code AbstractArchiveDriver} simply
     * returns {@code null}.
     *
     * @param model ignored.
     */
    @Override
    public Icon getOpenIcon(ArchiveModel model) {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@code AbstractArchiveDriver} simply
     * returns {@code null}.
     */
    @Override
    public Icon getClosedIcon(ArchiveModel model) {
        return null;
    }

    private final class ThreadLocalEncoder extends ThreadLocal<CharsetEncoder> {
        @Override
        protected CharsetEncoder initialValue() {
            return Charset.forName(charset).newEncoder();
        }

        boolean canEncode(CharSequence cs) {
            return get().canEncode(cs);
        }
    }

    /**
     * Thrown to indicate that the character set implementation in the Java
     * Runtime Environment (JRE) for the Java Standard Edition (JSE) is broken
     * and needs fixing.
     * <p>
     * This error is thrown if and only if the character set provided to the
     * constructor of the enclosing class is either supported by the JSE 1.1
     * style API ({@link String#String(byte[], String)}), but not the JSE 1.4
     * style API ({@link Charset#forName(String)}), or vice versa.
     * This implies that this error is <em>not</em> thrown if the character
     * set is consistently supported or not supported by both APIs!
     * <p>
     * Most of the time, this error happens when accessing regular ZIP files.
     * The respective archive drivers require &quot;IBM437&quot; as the
     * character set.
     * Unfortunately, this character set is optional and Sun's JSE
     * implementations usually only install it if the JSE has been fully
     * installed. Its provider is then located in
     * <i>$JAVA_HOME/lib/charsets.jar</i>, where <i>$JAVA_HOME</i> is the
     * path name of the installed JRE.
     * <p>
     * To assert that &quot;IBM437&quot; is always available regardless of
     * the JRE installation, TrueZIP provides its own provider for this charset.
     * This provider is configured in
     * <i>truezip.jar/META-INF/services/java.nio.charset.spi.CharsetProvider</i>.
     * So you should actually never see this happening (cruel world - sigh...).
     * <p>
     * Because the detected inconsistency would cause subtle bugs in archive
     * drivers and may affect other applications, too, it needs fixing.
     * Your options in order of preference:
     * <ol>
     * <li>Upgrade to a more recent JRE or reinstall it.
     *     When asked during installation, make sure to do a "full install".
     * <li>Fix the JRE by copying <i>$JAVA_HOME/lib/charsets.jar</i> from some
     *     other distribution.
     * </ol>
     * This should assert that $JAVA_HOME/lib/charsets.jar is present in the
     * JRE, which contains the provider for the &quot;IBM437&quot; character
     * set.
     * Although this should not be necessary due to TrueZIP's own provider,
     * this seems to fix the issue.
     * <p>
     * This error class has protected visibility solely for the purpose of
     * documenting it in the Javadoc.
     */
    protected static final class InconsistentCharsetSupportError extends Error {
        private static final long serialVersionUID = 5976345821010992606L;

        private InconsistentCharsetSupportError(String charset, Exception cause) {
            super(message(charset, cause), cause);
        }

        private static String message(  final String charset,
                                        final Exception cause) {
            assert cause instanceof UnsupportedEncodingException
                || cause instanceof UnsupportedCharsetException;
            final String[] api = cause instanceof UnsupportedEncodingException
                    ? new String[] { "J2SE 1.4", "JSE 1.1" }
                    : new String[] { "JSE 1.1", "J2SE 1.4" };
            return "The character set '" + charset
                    + "' is supported by the " + api[0]
                    + " API, but not the " + api[1] + " API."
                    + "\nThis requires fixing the Java Runtime Environment!"
                    + "\nPlease read the Javadoc of this error class for more information.";
        }
    }
}
