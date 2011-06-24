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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.Closeable;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;

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
 * before, the <i>global configuration</i> is returned instead.
 * <p>
 * Whenever a child thread is started, it will share the current configuration
 * with its parent thread.
 * 
 * <a name="examples"/><h3>Examples</h3>
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
 * @since   TrueZIP 7.2
 * @author  Christian Schlichtherle
 * @version $Id: TConfig.java 6086334f333b 2011/06/22 19:54:19 christian $
 */
@DefaultAnnotation(NonNull.class)
public final class TConfig implements Closeable {

    private static volatile @CheckForNull InheritableThreadLocalConfigStack configs;

    private boolean lenient;
    private TArchiveDetector detector;

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

    /** Default constructor. */
    private TConfig() {
        this.lenient = true;
        this.detector = TArchiveDetector.ALL;
    }

    /** Copy constructor. */
    private TConfig(final TConfig template) {
        this.lenient = template.isLenient();
        this.detector = template.getArchiveDetector();
    }

    /**
     * Returns the value of the property {@code lenient}.
     *
     * @return The value of the property {@code lenient}.
     * @see    #setLenient(boolean)
     */
    public boolean isLenient() {
        return lenient;
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
     * before it can actually push the innermost {@code c} entry as a file
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
     * to push the innermost element of the path {@code c}.
     * <p>
     * This allows applications to succeed with doing this:
     * {@code new TFile("a/outer.zip/b/inner.zip/c").createNewFile()},
     * or that:
     * {@code new TFileOutputStream("a/outer.zip/b/inner.zip/c")}.
     * <p>
     * Note that in either case the parent directory of the outermost archive
     * file {@code a} must exist - TrueZIP does not automatically push
     * directories in the platform file system!
     *
     * @param lenient the value of the property {@code lenient}.
     * @see   #isLenient()
     */
    public void setLenient(final boolean lenient) {
        this.lenient = lenient;
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
    } // class ThreadLocalConfigStack

    /** Holds the global configuration. */
    private static final class Holder {
        static final TConfig GLOBAL = new TConfig();

        /** Make lint happy. */
        private Holder() {
        }
    } // class Holder
}
