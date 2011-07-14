/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsInputOptions;
import static de.schlichtherle.truezip.fs.FsInputOptions.*;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.fs.sl.FsManagerLocator;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.Closeable;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import net.jcip.annotations.ThreadSafe;

/**
 * A container for some configuration options.
 * <p>
 * A client application can use {@link #push()} to create a new
 * <i>current configuration</i> by copying the current configuration and
 * pushing the copy on top of an inheritable thread local stack.
 * <p>
 * A client application can use {@link #pop()} or {@link #close()} to
 * pop the current configuration or this configuration respectively from the
 * top of the inheritable thread local stack.
 * <p>
 * Finally, a client application can use {@link #get()} to get access to the
 * current configuration.
 * If no configuration has been pushed on the inheritable thread local stack
 * before, the <i>global configuration</i> is returned.
 * <p>
 * Whenever a child thread is started, it will share the current configuration
 * with its parent thread.
 * 
 * <a name="examples"/><h3>Examples</h3>

 * <h4>Setting The Default Archive Detector</h4>
 * <p>
 * The standard use case looks like this:
 * <pre>{@code
TFile file1 = new TFile("file.mok");
assert !file1.isArchive();
// Push a new current configuration on the inheritable thread local stack.
TConfig config = TConfig.push();
try {
    // Change the inheritable thread local configuration.
    config.setArchiveDetector(new TArchiveDetector("mok", new MockArchiveDriver()));
    // Use the inheritable thread local configuration.
    TFile file2 = new TFile("file.mok");
    assert file2.isArchive();
    // Do some I/O here.
    ...
} finally {
    // Pop the configuration off the inheritable thread local stack.
    config.close();
}
 * }</pre>
 * <p>
 * Using try-with-resources in JSE 7, this can get shortened to:
 * <pre>{@code
TFile file1 = new TFile("file.mok");
assert !file1.isArchive();
// Push a new current configuration on the inheritable thread local stack.
try (TConfig config = TConfig.push()) {
    // Change the inheritable thread local configuration.
    config.setArchiveDetector(new TArchiveDetector("mok", new MockArchiveDriver()));
    // Use the inheritable thread local configuration.
    TFile file2 = new TFile("file.mok");
    assert file2.isArchive();
    // Do some I/O here.
    ...
}
 * }</pre>
 *
 * <h4>Appending To Archive Files</h4>
 * <p>
 * By default, TrueZIP is configured to produce the smalled possible archive
 * files. This is achieved by setting the maximum compression rate in the
 * archive driver and by performing a <i>full update</i> if an entry is going
 * to get written to an archive file which is already present in it.
 * <p>
 * This default <i>collision strategy</i> usually involves some copying and
 * could be deemed as an unacceptable &quot;performance penalty&quot; if the
 * archive entries to write are rather small compared to the total size of the
 * archive file.
 * <p>
 * You can change the collision strategy by allowing archive files to grow by
 * simply <i>appending</i> entries to their end as follows:
 * <pre>{@code
TFile file = new TFile("archive.zip/entry");
// Push a new current configuration on the inheritable thread local stack.
TConfig config = TConfig.push();
try {
    // Change the inheritable thread local configuration.
    config.setOutputPreferences(BitField.of(FsOutputOption.GROW, FsOutputOption.CREATE_PARENTS));
    // Append the entry to the archive file even if it's already present.
    TFileOutputStream out = new TFileOutputStream(file);
    try {
        // Do some I/O here.
        ...
    } finally {
        out.close();
    }
} finally {
    // Pop the configuration off the inheritable thread local stack.
    config.close();
}
 * }</pre>
 * <p>
 * Here, {@link FsOutputOption#GROW} is used by the application to express a
 * preference to inhibit full updates and simply append entries to an archive
 * file's end instead.
 * <p>
 * Note that it's specific to the archive file system driver if this output
 * option is supported or not.
 * If it's not supported, it gets silently ignored,
 * thereby falling back to the default collision strategy.
 * In this example, the archive file system driver for ZIP files is used,
 * which is known to support this output option.
 * 
 * @since   TrueZIP 7.2
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class TConfig implements Closeable {

    /**
     * The default value of the
     * {@link #getInputPreferences input preferences} property, which is
     * {@link FsInputOptions#NO_INPUT_OPTIONS}.
     */
    public static final BitField<FsInputOption>
            DEFAULT_INPUT_PREFERENCES = NO_INPUT_OPTIONS;

    /**
     * The mask of allowed {@link #setInputPreferences input preferences},
     * which is
     * <code>{@link BitField}.of({@link FsInputOption#CACHE})</code>.
     */
    public static final BitField<FsInputOption>
            INPUT_PREFERENCES_MASK = BitField
                .of(FsInputOption.CACHE);

    private static final BitField<FsInputOption>
            INPUT_PREFERENCES_COMPLEMENT_MASK = INPUT_PREFERENCES_MASK.not();

    /**
     * The default value of the
     * {@link #getOutputPreferences output preferences} property, which is
     * <code>{@link BitField}.of({@link FsOutputOption#CREATE_PARENTS})</code>.
     */
    public static final BitField<FsOutputOption>
            DEFAULT_OUTPUT_PREFERENCES = BitField.of(CREATE_PARENTS);

    /**
     * The mask of allowed {@link #setOutputPreferences output preferences},
     * which is
     * <code>{@link BitField}.of({@link FsOutputOption#CACHE}, {@link FsOutputOption#CREATE_PARENTS}, {@link FsOutputOption#COMPRESS}, {@link FsOutputOption#STORE})</code>.
     */
    public static final BitField<FsOutputOption>
            OUTPUT_PREFERENCES_MASK = BitField
                .of(FsOutputOption.CACHE, CREATE_PARENTS, COMPRESS, STORE, GROW);

    private static final BitField<FsOutputOption>
            OUTPUT_PREFERENCES_COMPLEMENT_MASK = OUTPUT_PREFERENCES_MASK.not();

    private static volatile @CheckForNull InheritableThreadLocalConfigStack configs;

    /** The file system manager to use within this package. */
    private static final FsManager manager = FsManagerLocator.SINGLETON.get();

    private TArchiveDetector detector;
    private BitField<FsInputOption> inputPreferences;
    private BitField<FsOutputOption> outputPreferences;

    /**
     * Returns the current configuration.
     * First, this method peeks the inheritable thread local configuration stack.
     * If no configuration has been {@link #push() pushed} yet, the global
     * configuration is returned.
     * Mind that the global configuration is shared by all threads.
     * 
     * @return The current configuration.
     * @see    #push()
     */
    public static TConfig get() {
        final InheritableThreadLocalConfigStack configs = TConfig.configs;
        if (null == configs)
            return Holder.GLOBAL;
        final TConfig session = configs.get().peek();
        return null != session ? session : Holder.GLOBAL;
    }

    /**
     * Creates a new current configuration by copying the current configuration
     * and pushing the copy on the inheritable thread local stack.
     * 
     * @return The new current configuration.
     * @see    #get()
     */
    public static TConfig push() {
        InheritableThreadLocalConfigStack configs;
        synchronized (TConfig.class) {
            configs = TConfig.configs;
            if (null == configs)
                configs = TConfig.configs = new InheritableThreadLocalConfigStack();
        }
        final Deque<TConfig> stack = configs.get();
        TConfig template = stack.peek();
        if (null == template)
            template = Holder.GLOBAL;
        final TConfig config = new TConfig(template);
        stack.push(config);
        return config;
    }

    /**
     * Pops the current configuration off the inheritable thread local stack.
     * 
     * @throws IllegalStateException If the current configuration is the global
     *         configuration.
     */
    public static void pop() {
        get().close();
    }

    private static void pop(final TConfig config) {
        final InheritableThreadLocalConfigStack configs = TConfig.configs;
        if (null == configs)
            throw new IllegalStateException("Inheritable thread local configuration stack is empty.");
        final Deque<TConfig> stack = configs.get();
        final TConfig found;
        try {
            found = stack.pop();
        } catch (NoSuchElementException ex) {
            throw new IllegalStateException("Inheritable thread local configuration stack is empty.", ex);
        }
        if (config != found) {
            stack.push(found);
            throw new IllegalStateException("Not the top element of the inheritable thread local configuration stack.");
        }
        if (stack.isEmpty())
            configs.remove();
    }

    /** Default constructor for the global configuration. */
    private TConfig() {
        this.detector = TArchiveDetector.ALL;
        this.inputPreferences = DEFAULT_INPUT_PREFERENCES;
        this.outputPreferences = DEFAULT_OUTPUT_PREFERENCES;
    }

    /** Copy constructor for inheritable thread local configurations. */
    private TConfig(final TConfig template) {
        this.detector = template.getArchiveDetector();
        this.inputPreferences = template.getInputPreferences();
        this.outputPreferences = template.getOutputPreferences();
    }

    FsManager getManager() {
        return manager;
    }

    /**
     * Returns the value of the property {@code lenient}.
     *
     * @return The value of the property {@code lenient}.
     * @see    #setLenient(boolean)
     */
    public boolean isLenient() {
        return outputPreferences.get(CREATE_PARENTS);
    }

    /**
     * Sets the value of the property {@code lenient}.
     * This property controls whether archive files and their member
     * directories get automatically created whenever required.
     * By default, the value of this class property is {@code true}!
     * <p>
     * Consider the following path: {@code a/outer.zip/b/inner.zip/c}.
     * Now let's assume that {@code a} exists as a plain directory in the
     * platform file system, while all other segments of this path don't, and
     * that the module TrueZIP Driver ZIP is present on the run-time class path
     * in order to detect {@code outer.zip} and {@code inner.zip} as ZIP files
     * according to the initial setup.
     * <p>
     * Now, if this property is set to {@code false}, then an application
     * needs to call {@code new TFile("a/outer.zip/b/inner.zip").mkdirs()}
     * before it can actually create the innermost {@code c} entry as a file
     * or directory.
     * <p>
     * More formally, before an application can access an entry in a federated
     * file system, all its parent directories need to exist, including archive
     * files.
     * This emulates the behaviour of the platform file system.
     * <p>
     * If this property is set to {@code true} however, then any missing
     * parent directories (including archive files) up to the outermost archive
     * file {@code outer.zip} get automatically created when using operations
     * to create the innermost element of the path {@code c}.
     * <p>
     * This allows applications to succeed with doing this:
     * {@code new TFile("a/outer.zip/b/inner.zip/c").createNewFile()},
     * or that:
     * {@code new TFileOutputStream("a/outer.zip/b/inner.zip/c")}.
     * <p>
     * Note that in either case the parent directory of the outermost archive
     * file {@code a} must exist - TrueZIP does not automatically create
     * directories in the platform file system!
     *
     * @param lenient the value of the property {@code lenient}.
     * @see   #isLenient()
     */
    public void setLenient(final boolean lenient) {
        this.outputPreferences = this.outputPreferences
                .set(CREATE_PARENTS, lenient);
    }

    /**
     * Returns the default {@link TArchiveDetector} to use for scanning path
     * names for prospective archive files if no {@code TArchiveDetector} has
     * been explicitly provided to a constructor.
     *
     * @return The default {@link TArchiveDetector} to use for scanning
     *         path names for prospective archive files.
     * @see #setArchiveDetector
     */
    public TArchiveDetector getArchiveDetector() {
        return detector;
    }

    /**
     * Sets the default {@link TArchiveDetector} to use for scanning path
     * names for prospective archive files if no {@code TArchiveDetector} has
     * been explicitly provided to a constructor.
     * Changing the value of this property affects the scanning of path names
     * of subsequently constructed {@link TFile} objects only.
     * Any existing {@code TFile} objects are <em>not</em> affected.
     *
     * @param detector the default {@link TArchiveDetector} to use for scanning
     *        path names for prospective archive files.
     * @see   #getArchiveDetector()
     */
    public void setArchiveDetector(TArchiveDetector detector) {
        if (null == detector)
            throw new NullPointerException();
        this.detector = detector;
    }

    /**
     * Returns the input preferences.
     * 
     * @return The input preferences.
     */
    public BitField<FsInputOption> getInputPreferences() {
        return inputPreferences;
    }

    /**
     * Sets the input preferences.
     * These preferences are usually not cached, so changing them should take
     * effect immediately.
     * 
     * @param  preferences the input preferences.
     * @throws IllegalArgumentException if an option is present in
     *         {@code preferences} which is not present in
     *         {@link #INPUT_PREFERENCES_MASK}.
     */
    public void setInputPreferences(final BitField<FsInputOption> preferences) {
        final BitField<FsInputOption> illegal = preferences
                .and(INPUT_PREFERENCES_COMPLEMENT_MASK);
        if (!illegal.isEmpty())
            throw new IllegalArgumentException(preferences + " (illegal input preferences)");
        this.inputPreferences = preferences;
    }

    /**
     * Returns the output preferences.
     * 
     * @return The output preferences.
     */
    public BitField<FsOutputOption> getOutputPreferences() {
        return outputPreferences;
    }

    /**
     * Sets the output preferences.
     * These preferences are usually not cached, so changing them should take
     * effect immediately.
     * 
     * @param  preferences the output preferences.
     * @throws IllegalArgumentException if an option is present in
     *         {@code preferences} which is not present in
     *         {@link #OUTPUT_PREFERENCES_MASK} or if both
     *         {@link FsOutputOption#STORE} and
     *         {@link FsOutputOption#COMPRESS} have been set.
     */
    public void setOutputPreferences(final BitField<FsOutputOption> preferences) {
        final BitField<FsOutputOption> illegal = preferences
                .and(OUTPUT_PREFERENCES_COMPLEMENT_MASK);
        if (!illegal.isEmpty())
            throw new IllegalArgumentException(preferences + " (illegal output preferences)");
        if (preferences.get(STORE) && preferences.get(COMPRESS))
            throw new IllegalArgumentException(preferences + " (either STORE or COMPRESS can be set, but not both)");
        this.outputPreferences = preferences;
    }

    /**
     * Pops this configuration off the inheritable thread local stack.
     * 
     * @throws IllegalStateException If this configuration is not the top
     *         element of the inheritable thread local stack.
     */
    @Override
    public void close() {
        pop(this);
    }

    /**
     * An inheritable thread local configuration stack.
     * Whenever a child thread is started, it will share the current
     * configuration with its parent thread by creating a new inheritable
     * thread local stack with the parent thread's current configuration as the
     * only element.
     */
    private static final class InheritableThreadLocalConfigStack
    extends InheritableThreadLocal<Deque<TConfig>> {
        @Override
        protected Deque<TConfig> initialValue() {
            return new LinkedList<TConfig>();
        }

        @Override
        protected Deque<TConfig> childValue(final Deque<TConfig> parent) {
            final Deque<TConfig> child = new LinkedList<TConfig>();
            final TConfig element = parent.peek();
            if (null != element)
                child.push(element);
            return child;
        }
    } // InheritableThreadLocalConfigStack

    /** Holds the global configuration. */
    private static final class Holder {
        static final TConfig GLOBAL = new TConfig();

        /** Make lint happy. */
        private Holder() {
        }
    } // Holder
}
