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

package de.schlichtherle.truezip.io.archive.controller;

/**
 * This singleton shutdown hook thread class runs
 * {@link Archives.ShutdownHook#SINGLETON} when the JVM terminates.
 * You cannot instantiate this class.
 *
 * @see Archives#addToShutdownHook(java.lang.Runnable)
 */
final class JVMShutdownHook extends Thread {

    /** The singleton instance of this class. */
    public static final JVMShutdownHook SINGLETON = new JVMShutdownHook();

    static {
        Runtime.getRuntime().addShutdownHook(SINGLETON);
    }

    private JVMShutdownHook() {
        super(  Archives.ShutdownHook.SINGLETON,
                "TrueZIP ArchiveController Shutdown Hook");
        setPriority(Thread.MAX_PRIORITY);
    }

    /**
     * Adds the given {@code runnable} to the set of runnables to run by
     * {@link Archives.ShutdownHook#SINGLETON} when the JVM
     * terminates.
     */
    public synchronized void add(final Runnable runnable) {
        Archives.ShutdownHook.SINGLETON.add(runnable);
    }
}
