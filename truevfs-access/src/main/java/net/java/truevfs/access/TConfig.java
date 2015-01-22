/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.util.Objects;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.InheritableThreadLocalStack;
import net.java.truecommons.shed.Resource;
import net.java.truevfs.kernel.spec.FsAccessOption;
import static net.java.truevfs.kernel.spec.FsAccessOption.*;
import net.java.truevfs.kernel.spec.FsAccessOptions;
import static net.java.truevfs.kernel.spec.FsAccessOptions.ACCESS_PREFERENCES_MASK;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;

/**
 * A mutable container for configuration options with global or inheritable
 * thread local scope.
 * <p>
 * At any time, a thread can call {@link #current()} to get access to the
 * mutable <i>current configuration</i>.
 * If no configuration has been pushed onto the inheritable thread local
 * configuration stack before, then this will return the
 * <i>global configuration</i> which is shared by all threads.
 * As an implication, accessing the global configuration may not be thread-safe.
 * <p>
 * To create an <i>inheritable thread local configuration</i>, a thread may
 * call {@link #open()}.
 * This will copy the <i>current configuration</i> (which may be the global
 * configuration) and push the copy on top of the inheritable thread
 * local configuration stack.
 * <p>
 * Later, the thread can use {@link #close()} to pop this configuration
 * off the top of the inheritable thread local configuration stack again.
 * <p>
 * Finally, whenever a child thread gets started, it will share the
 * <em>same</em> current configuration with its parent thread.
 * If the parent's inheritable thread local configuration stack is empty, then
 * the child will share the global configuration as its current configuration
 * with its parent.
 * Note that the child thread cannot {@link #close()} the inherited current
 * configuration - this would result in an {@link IllegalStateException}.
 *
 * <a name="examples"/><h3>Examples</h3>
 *
 * <a name="global"/><h4>Changing The Global Configuration</h4>
 * <p>
 * If the thread local configuration stack is empty, i.e. no {@link #open()}
 * without a corresponding {@link #close()} has been called before, then the
 * {@link #current()} method will return the global configuration.
 * This feature is intended to get used during application setup to change some
 * configuration options with global scope like this:
 * <pre><code>
class MyApplication extends TApplication<IOException> {

    &#64;Override
    protected void setup() {
        // This should obtain the global configuration.
        TConfig config = TConfig.current();
        // Configure custom application file format.
        config.setArchiveDetector(new TArchiveDetector("aff", new JarDriver()));
        // Set FsAccessOption.GROW for appending-to rather than reassembling
        // existing archive files.
        config.setAccessPreference(FsAccessOption.GROW, true);
    }

    ...
}
 * </code></pre>
 *
 * <a name="local"/><h4>Setting The Archive Detector For The Current Thread</h4>
 * <p>
 * If an application needs to change the configuration for just the current
 * thread rather than changing the global configuration, then the
 * {@link #open()} method needs to get called like this:
 * <pre><code>
TFile file1 = new TFile("file.aff");
assert !file1.isArchive();

// First, push a new current configuration onto the inheritable thread local
// stack.
try (TConfig config = TConfig.open()) {
    // Configure custom application file format "aff".
    config.setArchiveDetector(new TArchiveDetector("aff", new JarDriver()));

    // Now use the current configuration.
    TFile file2 = new TFile("file.aff");
    assert file2.isArchive();
    // Do some I/O here.
    ...
}
 * </code></pre>
 *
 * <a name="appending"/><h4>Appending To Archive Files For The Current Thread</h4>
 * <p>
 * By default, TrueVFS is configured to produce the smallest possible archive
 * files.
 * This is achieved by selecting the maximum compression ratio in the archive
 * drivers and by performing an archive update whenever an existing archive
 * entry is going to get overwritten with new contents or updated with new meta
 * data in order to avoid the writing of redundant data to the resulting
 * archive file.
 * An archive update is basically a copy operation where all archive entries
 * which haven't been written yet get copied from the input archive file to the
 * output archive file.
 * However, while this strategy produces the smallest possible archive files,
 * it may yield bad performance if the number and contents of the archive
 * entries to create or update are pretty small compared to the total size of
 * the resulting archive file.
 * <p>
 * Therefore, you can change this strategy by setting the
 * {@link FsAccessOption#GROW} output option preference when writing archive
 * entry contents or updating their meta data.
 * When set, this output option allows archive files to grow by appending new
 * or updated archive entries to their end and inhibiting archive update
 * operations.
 * You can set this preference in the global configuration as shown above or
 * you can set it on a case-by-case basis as follows:
 * <pre><code>
// We are going to append "entry" to "archive.zip".
TFile file = new TFile("archive.zip/entry");

// First, push a new current configuration on the inheritable thread local
// stack.
try (TConfig config = TConfig.open()) {
    // Set FsAccessOption.GROW for appending-to rather than reassembling
    // existing archive files.
    config.setAccessPreference(FsAccessOption.GROW, true);

    // Now use the current configuration and append the entry to the archive
    // file even if it's already present.
    try (TFileOutputStream out = new TFileOutputStream(file)) {
        // Do some output here.
        ...
    }
}
 * </code></pre>
 * <p>
 * Note that it's specific to the archive file system driver if this output
 * option preference is supported or not.
 * If it's not supported, then it gets silently ignored, thereby falling back
 * to the default strategy of performing a full archive update whenever
 * required to avoid writing redundant archive entry data.
 * <p>
 * As of TrueVFS 0.11, the support is like this:
 * <ul>
 * <li>The drivers of the module TrueVFS Driver JAR fully support this output
 *     option preference, so it's available for EAR, JAR, WAR files.</li>
 * <li>The drivers of the module TrueVFS Driver ZIP fully support this output
 *     option preference, so it's available for ZIP files.</li>
 * <li>The drivers of the module TrueVFS Driver ZIP.RAES only allow redundant
 *     archive entry contents and meta data.
 *     You cannot append to an existing ZIP.RAES file, however.</li>
 * <li>The drivers of the module TrueVFS Driver TAR only allow redundant
 *     archive entry contents.
 *     You cannot append to an existing TAR file, however.</li>
 * </ul>
 *
 * <a name="unit-testing"/><h4>Unit Testing</h4>
 * <p>
 * Using the thread local inheritable configuration stack comes in handy when
 * unit testing, e.g. with JUnit. Consider this pattern:
 * <pre><code>
public class AppTest {

    private TConfig config;

    &#64;Before
    public void setUp() {
        config = TConfig.open();
        // Let's just recognize ZIP files.
        config.setArchiveDetector(new TArchiveDetector("zip"));
    }

    &#64;After
    public void shutDown() {
        config.close();
    }

    &#64;Test
    public void testMethod() {
        // Test accessing some ZIP files here.
        ...
    }
}
 * </code></pre>
 * <p>
 * <b>Disclaimer</b>: Although this class internally uses an
 * {@link InheritableThreadLocal}, it does not leak memory in multi class
 * loader environments when used appropriately.
 *
 * @author Christian Schlichtherle
 */
@CleanupObligation
public final class TConfig extends Resource<IllegalStateException> {

    private static final BitField<FsAccessOption>
            NOT_ACCESS_PREFERENCES_MASK = ACCESS_PREFERENCES_MASK.not();

    private static final InheritableThreadLocalStack<TConfig>
            configs = new InheritableThreadLocalStack<>();

    static final TConfig GLOBAL = new TConfig();

    /**
     * Returns the current configuration.
     * First, this method peeks the inheritable thread local configuration
     * stack.
     * If no configuration has been {@link #open()}ed yet, the global
     * configuration gets returned.
     * Note that accessing the global configuration is not thread-safe!
     *
     * @return The current configuration.
     * @see    #open()
     */
    public static TConfig current() { return configs.peekOrElse(GLOBAL); }

    /**
     * Creates a new current configuration by copying the current configuration
     * and pushing the copy onto the inheritable thread local configuration
     * stack.
     *
     * @return The new current configuration.
     * @see    #current()
     */
    @CreatesObligation
    public static TConfig open() { return configs.push(new TConfig(current())); }

    // I don't think these fields should be volatile.
    // This would make a difference if and only if two threads were changing
    // the GLOBAL configuration concurrently, which is discouraged.
    // Instead, the global configuration should only get changed once at
    // application startup and then each thread should modify only its thread
    // local configuration which has been obtained by a call to TConfig.open().
    private FsManager manager;
    private TArchiveDetector detector;
    private BitField<FsAccessOption> preferences;

    /** Default constructor for the global configuration. */
    private TConfig() {
        this.manager = FsManagerLocator.SINGLETON.get();
        this.detector = TArchiveDetector.ALL;
        this.preferences = BitField.of(CREATE_PARENTS);
    }

    /** Copy constructor for inheritable thread local configurations. */
    private TConfig(final TConfig template) {
        this.manager = template.getManager();
        this.detector = template.getArchiveDetector();
        this.preferences = template.getAccessPreferences();
    }

    private void checkOpen() {
        if (!isOpen())
            throw new IllegalStateException("This configuration has already been close()d.");
    }

    /**
     * Returns the file system manager.
     *
     * @return The file system manager.
     */
    FsManager getManager() {
        checkOpen();
        return manager;
    }

    /**
     * Sets the file system manager.
     * This method is solely provided for testing purposes.
     * Changing this property will show effect upon the next access to the
     * virtual file system.
     *
     * @param manager The file system manager.
     */
    void setManager(final FsManager manager) {
        checkOpen();
        this.manager = Objects.requireNonNull(manager);
    }

    /**
     * Returns the {@link TArchiveDetector} to use for scanning path names for
     * prospective archive files.
     *
     * @return The {@link TArchiveDetector} to use for scanning path names for
     *         prospective archive files.
     * @see #getArchiveDetector
     */
    public TArchiveDetector getArchiveDetector() {
        checkOpen();
        return detector;
    }

    /**
     * Sets the default {@link TArchiveDetector} to use for scanning path
     * names for prospective archive files.
     * Changing this property will show effect when a new {@link TFile} or
     * {@link TPath} gets created.
     *
     * @param detector the default {@link TArchiveDetector} to use for
     *        scanning path names for prospective archive files.
     * @see   #getArchiveDetector()
     */
    public void setArchiveDetector(final TArchiveDetector detector) {
        checkOpen();
        this.detector = Objects.requireNonNull(detector);
    }

    /**
     * Returns the access preferences to apply for file system operations.
     *
     * @return The access preferences to apply for file system operations.
     */
    public BitField<FsAccessOption> getAccessPreferences() {
        checkOpen();
        return preferences;
    }

    /**
     * Sets the access preferences to apply for file system operations.
     * Changing this property will show effect upon the next access to the
     * virtual file system.
     *
     * @param  preferences the access preferences.
     * @throws IllegalArgumentException if an option is present in
     *         {@code accessPreferences} which is not present in
     *         {@link FsAccessOptions#ACCESS_PREFERENCES_MASK} or if both
     *         {@link FsAccessOption#STORE} and
     *         {@link FsAccessOption#COMPRESS} have been set.
     */
    public void setAccessPreferences(final BitField<FsAccessOption> preferences) {
        checkOpen();
        if (preferences.equals(this.preferences)) return;
        final BitField<FsAccessOption>
                illegal = preferences.and(NOT_ACCESS_PREFERENCES_MASK);
        if (!illegal.isEmpty())
            throw new IllegalArgumentException(illegal + " (illegal access preference(s))");
        if (preferences.get(STORE) && preferences.get(COMPRESS))
            throw new IllegalArgumentException(preferences + " (either STORE or COMPRESS may be set, but not both)");
        this.preferences = preferences;
    }

    /**
     * Returns {@code true} if and only if the given access option is set in
     * the access preferences.
     *
     * @param  option the access option to test.
     * @return {@code true} if and only if the given access option is set in
     *         the access preferences.
     */
    public boolean getAccessPreference(FsAccessOption option) {
        return getAccessPreferences().get(option);
    }

    /**
     * Sets or clears the given access option in the access preferences.
     * Changing this property will show effect upon the next access to the
     * virtual file system.
     *
     * @param option the access option to set or clear.
     * @param set {@code true} if you want the option to be set or
     *        {@code false} if you want it to be cleared.
     */
    public void setAccessPreference(FsAccessOption option, boolean set) {
        setAccessPreferences(getAccessPreferences().set(option, set));
    }

    /**
     * Returns the value of the property {@code lenient}, which is {@code true}
     * if and only if the access preference {@link FsAccessOption#CREATE_PARENTS}
     * is set in the {@linkplain #getAccessPreferences() access preferences}.
     * This property controls whether archive files and their member
     * directories get automatically created whenever required.
     * <p>
     * Consider the following path: {@code a/outer.zip/b/inner.zip/c}.
     * Now let's assume that {@code a} exists as a plain directory in the
     * platform file system, while all other segments of this path don't, and
     * that the module TrueVFS Driver ZIP is present on the run-time class path
     * in order to enable the detection of {@code outer.zip} and
     * {@code inner.zip} as prospective ZIP files.
     * <p>
     * Now, if this property is set to {@code false}, then a client application
     * needs to call {@code new TFile("a/outer.zip/b/inner.zip").mkdirs()}
     * before it can actually create the innermost entry {@code c} as a file
     * or directory.
     * More formally, before an application can access an entry in an archive
     * file system, all its parent directories need to exist, including archive
     * files.
     * <p>
     * This emulates the behaviour of the platform file system.
     * <p>
     * If this property is set to {@code true} however, then any missing
     * parent directories (including archive files) up to the outermost archive
     * file {@code outer.zip} get automatically created when using operations
     * to create the innermost entry {@code c}.
     * This enables applications to succeed with doing this:
     * {@code new TFile("a/outer.zip/b/inner.zip/c").createNewFile()},
     * or that:
     * {@code new TFileOutputStream("a/outer.zip/b/inner.zip/c")}.
     * <p>
     * A most desirable side effect of <i>being lenient</i> is that it will
     * safe space in the target archive file.
     * This is because the directory entry {@code b} in this exaple does not
     * need to get output because there is no meta data associated with it.
     * This is called a <em>ghost directory</em>.
     * <p>
     * Note that in either case the parent directory of the outermost archive
     * file {@code a} must exist - TrueVFS does not automatically create
     * directories in the platform file system!
     *
     * @return The value of the property {@code lenient}, which is {@code true}
     *         if and only if the access preference
     *         {@link FsAccessOption#CREATE_PARENTS} is set in the
     *         {@linkplain #getAccessPreferences() access accessPreferences}.
     */
    public boolean isLenient() { return getAccessPreference(CREATE_PARENTS); }

    /**
     * Sets the value of the property {@code lenient}.
     * Changing this property will show effect upon the next access to the
     * virtual file system.
     *
     * @param lenient the value of the property {@code lenient}.
     */
    public void setLenient(final boolean lenient) {
        setAccessPreference(CREATE_PARENTS, lenient);
    }

    @Override
    @DischargesObligation
    public void close() throws IllegalStateException { super.close(); }

    /**
     * Pops this configuration off the inheritable thread local configuration
     * stack.
     *
     * @throws IllegalStateException If this configuration is not the
     *         {@linkplain #current() current configuration}.
     */
    @Override protected void onBeforeClose() throws IllegalStateException {
        configs.popIf(this);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TConfig)) return false;
        final TConfig that = (TConfig) other;
        return this.manager.equals(that.getManager())
                && this.detector.equals(that.getArchiveDetector())
                && this.preferences.equals(that.getAccessPreferences());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + manager.hashCode();
        hash = 89 * hash + detector.hashCode();
        hash = 89 * hash + preferences.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return String.format("%s[manager=%s, detector=%s, preferences=%s]",
                getClass().getName(), manager, detector, preferences);
    }
}
