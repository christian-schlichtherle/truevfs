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

    public abstract TArchiveDetector getArchiveDetector();
    public abstract void setArchiveDetector(TArchiveDetector detector);
    public abstract boolean isLenient();
    public abstract void setLenient(boolean lenient);
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
            if (this != sessions.get().pop())
                throw new IllegalStateException();
        }
    } // class Session
}
