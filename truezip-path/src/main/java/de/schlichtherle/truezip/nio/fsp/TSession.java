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

/**
 * An interface to the global TrueZIP configuration or a thread local 
 * configuration of this package.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class TSession implements AutoCloseable {

    private static final ThreadLocalSessions
            sessions = new ThreadLocalSessions();

    /**
     * Peeks the current session on top of the thread local stack of sessions.
     * If no session has been pushed yet, an adapter for the global
     * configuration is returned.
     * 
     * @return The current session.
     * @see    #newSession()
     */
    public static TSession getSession() {
        final Local session = sessions.get().peek();
        return null != session ? session : Global.SINGLETON;
    }

    /**
     * Pushes a new session on the thread local stack of sessions.
     * 
     * @return The new session.
     * @see    #getSession()
     */
    public static TSession newSession() {
        return new Local();
    }

    /** You cannot instantiate this class. */
    private TSession() {
    }

    /**
     * Returns the {@link TArchiveDetector} to use for scanning a URI
     * for prospective archive files.
     *
     * @return The {@link TArchiveDetector} to use for scanning a URI
     *         for prospective archive files.
     * @see #setArchiveDetector
     */
    public abstract TArchiveDetector getArchiveDetector();

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
    public abstract void setArchiveDetector(TArchiveDetector detector);

    /**
     * Returns the value of the property {@code lenient}.
     *
     * @return The value of the property {@code lenient}.
     * @see    #setLenient(boolean)
     */
    public abstract boolean isLenient();

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
    public abstract void setLenient(boolean lenient);

    /**
     * Pops this session from the thread local stack of sessions.
     * 
     * @throws IllegalStateException
     */
    public abstract void close();

    private static final class ThreadLocalSessions
    extends ThreadLocal<Deque<Local>> {
        @Override
        protected Deque<Local> initialValue() {
            return new LinkedList<>();
        }
    } // class ThreadLocalSessions

    private static final class Global extends TSession {
        static final Global SINGLETON = new Global();

        @Override
        public TArchiveDetector getArchiveDetector() {
            return TFile.getDefaultArchiveDetector();
        }

        @Override
        public void setArchiveDetector(TArchiveDetector detector) {
            TFile.setDefaultArchiveDetector(detector);
        }

        @Override
        public boolean isLenient() {
            return TFile.isLenient();
        }

        @Override
        public void setLenient(boolean lenient) {
            TFile.setLenient(lenient);
        }

        @Override
        public void close() {
        }
    } // class Global

    private static final class Local extends TSession {
        boolean closed;
        private TArchiveDetector detector;
        private boolean lenient;

        @SuppressWarnings("LeakingThisInConstructor")
        Local() {
            final TSession config = getSession();
            this.detector = config.getArchiveDetector();
            this.lenient = config.isLenient();
            TSession.sessions.get().push(this);
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
            if (closed)
                return;
            closed = true;
            final Deque<Local> locals = sessions.get();
            final Local session = locals.pop();
            if (this != session) {
                locals.push(session);
                throw new IllegalStateException();
            }
        }
    } // class Local
}
