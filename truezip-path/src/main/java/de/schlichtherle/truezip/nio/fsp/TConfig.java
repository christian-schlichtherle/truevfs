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
package de.schlichtherle.truezip.nio.fsp;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * An interface to the global TrueZIP configuration or a thread local stack of
 * configurations.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class TConfig implements AutoCloseable {

    private static final ThreadLocalSessionStack
            sessions = new ThreadLocalSessionStack();

    /**
     * Peeks the current configuration on top of the thread local stack of
     * configurations.
     * If no configuration has been {@link #push() pushed} yet, an adapter for
     * the global configuration is returned.
     * 
     * @return The current configuration.
     * @see    #push()
     */
    public static TConfig get() {
        final Session session = sessions.get().peek();
        return null != session ? session : Holder.GLOBAL;
    }

    /**
     * Pushes a new configuration on the thread local stack of configurations.
     * The new configuration will have 
     * 
     * @return The new configuration.
     * @see    #get()
     */
    public static TConfig push() {
        return new Session();
    }

    /** You cannot instantiate this class. */
    private TConfig() {
    }

    /**
     * Returns the {@link TArchiveDetector} to use for scanning a URI
     * for prospective archive files.
     *
     * @return The {@link TArchiveDetector} to use for scanning a URI
     *         for prospective archive files.
     * @see #setArchiveDetector
     */
    public TArchiveDetector getArchiveDetector() {
        return TFile.getDefaultArchiveDetector();
    }

    /**
     * Sets the {@link TArchiveDetector} to use for scanning a URI
     * for prospective archive files.
     * Changing the value of this class property affects only subsequently
     * constructed {@code TPath} instances - not any existing ones.
     *
     * @param detector the {@link TArchiveDetector} to use for scanning a URI
     *        for prospective archive files.
     * @see   #getArchiveDetector()
     */
    public void setArchiveDetector(TArchiveDetector detector) {
        TFile.setDefaultArchiveDetector(detector);
    }

    /**
     * Returns the value of the property {@code lenient}.
     *
     * @return The value of the property {@code lenient}.
     * @see    #setLenient(boolean)
     */
    public boolean isLenient() {
        return TFile.isLenient();
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
    public void setLenient(boolean lenient) {
        TFile.setLenient(lenient);
    }

    /**
     * Pops this configuration from the thread local stack of configurations.
     * 
     * @throws UnsupportedOperationException If this configuration is the
     *         global configuration.
     * @throws IllegalStateException If this configuration is not the top
     *         element of the thread local stack of configurations.
     */
    public void close() {
        throw new UnsupportedOperationException();
    }

    private static final class ThreadLocalSessionStack
    extends ThreadLocal<Deque<Session>> {
        @Override
        protected Deque<Session> initialValue() {
            return new LinkedList<>();
        }
    } // class ThreadLocalSessionStack

    private static final class Holder {
        static final TConfig GLOBAL = new TConfig();

        /** Make lint happy. */
        private Holder() {
        }
    } // class Global

    private static final class Session extends TConfig {
        private TArchiveDetector detector;
        private boolean lenient;

        @SuppressWarnings("LeakingThisInConstructor")
        Session() {
            final TConfig config = get();
            this.detector = config.getArchiveDetector();
            this.lenient = config.isLenient();
            TConfig.sessions.get().push(this);
        }

        @Override
        public TArchiveDetector getArchiveDetector() {
            return detector;
        }

        @Override
        public void setArchiveDetector(TArchiveDetector detector) {
            if (null == detector)
                throw new NullPointerException();
            this.detector = detector;
        }

        @Override
        public boolean isLenient() {
            return lenient;
        }

        @Override
        public void setLenient(boolean lenient) {
            this.lenient = lenient;
        }

        @Override
        public void close() {
            final Deque<Session> locals = sessions.get();
            final Session session;
            try {
                session = locals.pop();
            } catch (NoSuchElementException ex) {
                throw new IllegalStateException(ex);
            }
            if (this != session) {
                locals.push(session);
                throw new IllegalStateException("Not the top element of the thread local stack of configurations.");
            }
        }
    } // class Session
}
