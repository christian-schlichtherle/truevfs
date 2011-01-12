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
package de.schlichtherle.truezip.awt;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import net.jcip.annotations.ThreadSafe;

/**
 * Provide static utility methods for the AWT Event Queue.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class EventQueueHelper {

    private static final byte RESET = 0, CANCELLED = 1, STARTED = 2, DONE = 3;

    private EventQueueHelper() {
    }

    /**
     * Equivalent to {@link #invokeAndWaitUninterruptibly(Runnable, long)
     * invokeAndWaitUninterruptibly(task, 0)}, but cannot throw an
     * {@code EventDispatchTimeoutException}.
     */
    public static void invokeAndWaitUninterruptibly(Runnable task)
    throws  InvocationTargetException {
        try {
            invokeAndWait(task, false, 0);
        } catch (EventDispatchTimeoutException cannotHappen) {
            throw new AssertionError(cannotHappen);
        } catch (InterruptedException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    /**
     * Invokes the given {@code task} on the AWT Event Dispatching Thread
     * (EDT) and waits until it's finished.
     * If this method is called on the EDT itself, it will just invoke the
     * given {@code task}.
     * <p>
     * If the current thread gets interrupted while waiting for the EDT to
     * finish the task, then waiting is continued normally, but the current
     * thread's interrupt status is {@link Thread#interrupt() set} upon return.
     *
     * @param task The {@link Runnable} whose {@code run}
     *        method should be executed synchronously in the EDT.
     * @param startTimeout If positive, then this parameter specifies the
     *        maximum time to wait before the EDT starts to process
     *        {@code task} in milliseconds.
     * @throws IllegalArgumentException If {@code startTimeout} is
     *         negative.
     * @throws EventDispatchTimeoutException If {@code startTimeout} is
     *         positive and waiting for the EDT to start processing the
     *         {@code task} timed out.
     *         The task has been cancelled, i.e. it will not be executed.
     * @throws InvocationTargetException If an exception is thrown when
     *         running {@code task}.
     *         {@code getCause()} yields the cause of this exception,
     *         which must be a {@link RuntimeException} or an {@link Error}.
     * @see Thread#interrupted
     */
    public static void invokeAndWaitUninterruptibly(Runnable task,
                                                    long startTimeout)
    throws  EventDispatchTimeoutException,
            InvocationTargetException {
        try {
            invokeAndWait(task, false, startTimeout);
        } catch (InterruptedException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    private static void invokeAndWait(  final Runnable task,
                                        final boolean interruptibly,
                                        final long startTimeout)
    throws  EventDispatchTimeoutException,
            InterruptedException,
            InvocationTargetException {
        if (startTimeout < 0)
            throw new IllegalArgumentException("Timeout must not be negative!");

        if (EventQueue.isDispatchThread()) {
            try {
                task.run();
            } catch (Throwable throwable) {
                throw new InvocationTargetException(throwable);
            }
        } else { // !isDispatchThread()
            class MonitoredAction implements Runnable {
                Thread edt;
                Throwable throwable;
                byte status = RESET;

                @Override
				public void run() {
                    assert EventQueue.isDispatchThread();
                    if (start()) {
                        try {
                            task.run();
                            finished(null);
                        } catch (Throwable throwable) {
                            finished(throwable);
                        }
                    }
                }

                private synchronized boolean start() {
                    if (status == CANCELLED)
                        return false;

                    edt = Thread.currentThread();
                    status = STARTED;
                    notifyAll();
                    return true;
                }

                private synchronized void finished(final Throwable t) {
                    throwable = t;
                    status = DONE;
                    Thread.interrupted(); // clear status
                    notifyAll();
                }
            }

            final MonitoredAction action = new MonitoredAction();
            EventQueue.invokeLater(action);
            synchronized (action) {
                InterruptedException interrupted = null;
                while (action.status < DONE) {
                    try {
                        action.wait(action.status < STARTED ? startTimeout : 0);
                        if (action.status < STARTED) {
                            action.status = CANCELLED;
                            throw new EventDispatchTimeoutException(startTimeout);
                        }
                    } catch (InterruptedException ex) {
                        interrupted = ex;
                        if (interruptibly)
                            break;
                    }
                }
                if (interrupted != null) {
                    if (interruptibly) {
                        if (action.status >= STARTED)
                            action.edt.interrupt();
                        throw interrupted;
                    } else {
                        Thread.currentThread().interrupt();
                    }
                }
                if (action.throwable != null)
                    throw new InvocationTargetException(action.throwable);
            }
        }
    }
}
