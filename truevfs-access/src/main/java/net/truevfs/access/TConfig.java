/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.access;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import net.truevfs.kernel.FsAccessOption;
import static net.truevfs.kernel.FsAccessOption.*;
import net.truevfs.kernel.FsAccessOptions;
import static net.truevfs.kernel.FsAccessOptions.ACCESS_PREFERENCES_MASK;
import net.truevfs.kernel.FsManager;
import net.truevfs.kernel.sl.FsManagerLocator;
import net.truevfs.kernel.util.BitField;
import net.truevfs.kernel.util.InheritableThreadLocalStack;
import net.truevfs.kernel.util.Resource;

/**
 * A container for configuration options with global or inheritable thread
 * local scope.
 * <p>
 * A thread can call {@link #get()} to get access to the
 * <i>current configuration</i> at any time .
 * If no configuration has been pushed onto the inheritable thread local
 * configuration stack before, this will return the <i>global configuration</i>
 * which is shared by all threads (hence its name).
 * Mind that access to the global configuration is <em>not</em> synchronized.
 * <p>
 * To create an <i>inheritable thread local configuration</i>, a thread can
 * simply call {@link #push()}.
 * This will copy the <i>current configuration</i> (which may be identical to
 * the global configuration) and push the copy on top of the inheritable thread
 * local configuration stack.
 * <p>
 * Later, the thread can use {@link #pop()} or {@link #close()} to
 * pop the current configuration or {@code this} configuration respectively
 * off the top of the inheritable thread local configuration stack again.
 * <p>
 * Finally, whenever a child thread gets started, it will share the
 * <em>same</em> current configuration with its parent thread.
 * This is achieved by copying the top level element of the parent's
 * inheritable thread local configuration stack.
 * If the parent's inheritable thread local configuration stack is empty, then
 * the child will share the global configuration as its current configuration
 * with its parent.
 * As an implication, {@link #pop()} or {@link #close()} can be called at most
 * once in the child thread.
 * 
 * <a name="examples"/><h3>Examples</h3>
 *
 * <a name="global"/><h4>Changing The Global Configuration</h4>
 * <p>
 * If the thread local configuration stack is empty, i.e. no {@link #push()}
 * without a corresponding {@link #close()} or {@link #pop()} has been called
 * before, then the {@link #get()} method will return the global configuration.
 * This feature is intended to get used during the application setup to change
 * some configuration options with global scope like this:
 * <pre>{@code
class MyApplication extends TApplication<IOException> {

    \@Override
    protected void setup() {
        // This should obtain the global configuration.
        TConfig config = TConfig.get();
        // Configure custom application file format.
        config.setArchiveDetector(new TArchiveDetector("aff",
                new JarDriver(IOPoolLocator.SINGLETON)));
        // Set FsAccessOption.GROW for appending-to rather than reassembling
        // existing archive files.
        config.setAccessPreferences(
                config.getAccessPreferences.set(FsAccessOption.GROW));
    }

    ...
}
 * }</pre>
 * 
 * <a name="local"/><h4>Setting The Default Archive Detector In The Current Thread</h4>
 * <p>
 * If an application needs to change the configuration of just the current
 * thread rather than changing the global configuration, then the
 * {@link #push()} method needs to get called like this:
 * <pre>{@code
TFile file1 = new TFile("file.aff");
assert !file1.isArchive();

// First, push a new current configuration onto the inheritable thread local
// stack.
TConfig config = TConfig.push();
try {
    // Configure custom application file format "aff".
    config.setArchiveDetector(new TArchiveDetector("aff",
            new JarDriver(IOPoolLocator.SINGLETON)));

    // Now use the current configuration.
    TFile file2 = new TFile("file.aff");
    assert file2.isArchive();
    // Do some I/O here.
    ...
} finally {
    // Pop the current configuration off the inheritable thread local stack.
    config.close();
}
 * }</pre>
 * <p>
 * Using try-with-resources in JSE&nbsp;7, this can get shortened to:
 * <pre>{@code
TFile file1 = new TFile("file.aff");
assert !file1.isArchive();

// First, push a new current configuration onto the inheritable thread local
// stack.
try (TConfig config = TConfig.push()) {
    // Configure custom application file format "aff".
    config.setArchiveDetector(new TArchiveDetector("aff",
            new JarDriver(IOPoolLocator.SINGLETON)));

    // Now use the current configuration.
    TFile file2 = new TFile("file.aff");
    assert file2.isArchive();
    // Do some I/O here.
    ...
}
 * }</pre>
 *
 * <a name="appending"/><h4>Appending To Archive Files In The Current Thread</h4>
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
 * <pre>{@code
// We are going to append "entry" to "archive.zip".
TFile file = new TFile("archive.zip/entry");

// First, push a new current configuration on the inheritable thread local
// stack.
TConfig config = TConfig.push();
try {
    // Set FsAccessOption.GROW for appending-to rather than reassembling
    // existing archive files.
    config.setAccessPreferences(
            config.getAccessPreferences.set(FsAccessOption.GROW));

    // Now use the current configuration and append the entry to the archive
    // file even if it's already present.
    TFileOutputStream out = new TFileOutputStream(file);
    try {
        // Do some output here.
        ...
    } finally {
        out.close();
    }
} finally {
    // Pop the current configuration off the inheritable thread local stack.
    config.close();
}
 * }</pre>
 * <p>
 * Note that it's specific to the archive file system driver if this output
 * option preference is supported or not.
 * If it's not supported, then it gets silently ignored, thereby falling back
 * to the default strategy of performing a full archive update whenever
 * required to avoid writing redundant archive entry data.
 * <p>
 * As of TrueVFS 7.5, the support is like this:
 * <ul>
 * <li>The drivers of the module TrueVFS Driver ZIP fully support this output
 *     option preference, so it's available for EAR, JAR, WAR, ZIP etc.</li>
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
 * <pre>{@code
public class AppTest {

    \@Before
    public void setUp() {
        TConfig config = TConfig.push();
        // Let's just recognize ZIP files.
        config.setArchiveDetector(new TArchiveDetector("zip"));
    }

    \@After
    public void shutDown() {
        TConfig.pop();
    }

    \@Test
    public void testMethod() {
        // Test accessing some ZIP files here.
        ...
    }
}
 * }</pre>
 * <p>
 * Note that it's not necessary to save the reference to the new pushed
 * configuration in {@code setUp()}.
 * {@code shutDown()} will just pop the top configuration off the inheritable
 * thread local configuration stack.
 * <p>
 * The most important feature of this code is that it's thread-safe, which
 * enables you to run your unit tests in parallel!
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
@CleanupObligation
public final class TConfig extends Resource<RuntimeException> {

    /**
     * The default value of the
     * {@link #getAccessPreferences output preferences} property, which is
     * <code>{@link BitField}.of({@link FsAccessOption#CREATE_PARENTS})</code>.
     */
    public static final BitField<FsAccessOption>
            DEFAULT_ACCESS_PREFERENCES = BitField.of(CREATE_PARENTS);

    private static final BitField<FsAccessOption>
            NOT_ACCESS_PREFERENCES_MASK = ACCESS_PREFERENCES_MASK.not();

    private static final InheritableThreadLocalStack<TConfig>
            configs = new InheritableThreadLocalStack<>();

    private static final TConfig GLOBAL = new TConfig();

    // I don't think these fields should be volatile.
    // This would make a difference if and only if two threads were changing
    // the GLOBAL configuration concurrently, which is discouraged.
    // Instead, the global configuration should only get changed once at
    // application startup and then each thread should modify only its thread
    // local configuration which has been obtained by a call to TConfig.push().
    private FsManager manager;
    private TArchiveDetector detector;
    private BitField<FsAccessOption> accessPreferences;

    /**
     * Returns the current configuration.
     * First, this method peeks the inheritable thread local configuration
     * stack.
     * If no configuration has been {@link #push() pushed} yet, the global
     * configuration is returned.
     * Mind that the global configuration is shared by all threads.
     * 
     * @return The current configuration.
     * @see    #push()
     */
    public static TConfig get() {
        return configs.peekOrElse(GLOBAL);
    }

    /**
     * Creates a new current configuration by copying the current configuration
     * and pushing the copy onto the inheritable thread local configuration
     * stack.
     * 
     * @return The new current configuration.
     * @see    #get()
     */
    @CreatesObligation
    public static TConfig push() {
        return configs.push(new TConfig(get()));
    }

    /**
     * Pops the {@linkplain #get() current configuration} off the inheritable thread
     * local configuration stack.
     * 
     * @throws IllegalStateException If the {@linkplain #get() current configuration}
     *         is the global configuration.
     */
    @DischargesObligation
    public static void pop() {
        configs.popIf(get());
    }

    /** Default constructor for the global configuration. */
    private TConfig() {
        this.manager = FsManagerLocator.SINGLETON.getManager();
        this.detector = TArchiveDetector.ALL;
        this.accessPreferences = DEFAULT_ACCESS_PREFERENCES;
    }

    /** Copy constructor for inheritable thread local configurations. */
    private TConfig(final TConfig template) {
        this.manager = template.getFsManager();
        this.detector = template.getArchiveDetector();
        this.accessPreferences = template.getAccessPreferences();
    }

    /**
     * Returns the file system manager.
     * 
     * @return The file system manager.
     */
    FsManager getFsManager() {
        return this.manager;
    }

    /**
     * Sets the file system manager.
     * 
     * @param manager The file system manager.
     */
    void setManager(final FsManager manager) {
        this.manager = Objects.requireNonNull(manager);
    }

    /**
     * Returns the value of the property {@code lenient}.
     *
     * @return The value of the property {@code lenient}.
     * @see    #setLenient(boolean)
     */
    public boolean isLenient() {
        return this.accessPreferences.get(CREATE_PARENTS);
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
     * @param lenient the value of the property {@code lenient}.
     * @see   #isLenient()
     */
    public void setLenient(final boolean lenient) {
        this.accessPreferences = this.accessPreferences
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
        return this.detector;
    }

    /**
     * Sets the default {@link TArchiveDetector} to use for scanning path
     * names for prospective archive files if no {@code TArchiveDetector} has
     * been explicitly provided to a {@link TFile} constructor.
     * Because the {@code TFile} class is immutable, changing the value of this
     * property will affect the scanning of path names of subsequently
     * constructed {@code TFile} objects only - existing {@code TFile} objects
     * will <em>not</em> be affected.
     *
     * @param detector the default {@link TArchiveDetector} to use for scanning
     *        path names for prospective archive files.
     * @see   #getArchiveDetector()
     */
    public void setArchiveDetector(final TArchiveDetector detector) {
        this.detector = Objects.requireNonNull(detector);
    }

    /**
     * Returns the access preferences.
     * 
     * @return The access preferences.
     */
    public BitField<FsAccessOption> getAccessPreferences() {
        return this.accessPreferences;
    }

    /**
     * Sets the access preferences.
     * Changing this property will show imediate effect upon the next effective
     * access to the (virtual federated) file system.
     * 
     * @param  preferences the access preferences.
     * @throws IllegalArgumentException if an option is present in
     *         {@code preferences} which is not present in
     *         {@link FsAccessOptions#ACCESS_PREFERENCES_MASK} or if both
     *         {@link FsAccessOption#STORE} and
     *         {@link FsAccessOption#COMPRESS} have been set.
     */
    public void setAccessPreferences(final BitField<FsAccessOption> preferences) {
        final BitField<FsAccessOption>
                illegal = preferences.and(NOT_ACCESS_PREFERENCES_MASK);
        if (!illegal.isEmpty())
            throw new IllegalArgumentException(illegal + " (illegal output preference(s))");
        if (preferences.get(STORE) && preferences.get(COMPRESS))
            throw new IllegalArgumentException(preferences + " (either STORE or COMPRESS may be set, but not both)");
        this.accessPreferences = preferences;
    }

    // This one is just for you, NetBeans.
    @Override
    @DischargesObligation
    public void close() {
        super.close();
    }

    @Override
    protected void onClose() {
        configs.popIf(this);
    }
}
