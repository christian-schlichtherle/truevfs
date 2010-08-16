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

package de.schlichtherle.key.passwd.swing;

import java.util.logging.*;

/**
 * Simulates the typical life cycle of a protected resource and its
 * associated key using the API in the package {@link de.schlichtherle.key}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.1
 */
public class KeyMgmtLifeCycleThread extends Thread {

    private static final Logger logger
            = Logger.getLogger(KeyMgmtLifeCycleThread.class.getName());

    private final KeyMgmtLifeCycle rlc;

    public KeyMgmtLifeCycleThread(final String id) {
        this(new KeyMgmtLifeCycle(id));
    }

    public KeyMgmtLifeCycleThread(KeyMgmtLifeCycle rlc) {
        super(rlc, "Key Management Life Cycle Thread for \"" + rlc.id + "\"");
        this.rlc = rlc;
        //setPriority(Thread.MIN_PRIORITY);
        //setDaemon(true); // thread may die.
    }

    public String getResourceID() {
        return rlc.id;
    }

    /**
     * Returns non-<code>null</code> if and only if this thread has
     * terminated because the user cancelled the key prompting.
     */
    public Throwable getThrowable() {
        return rlc.throwable;
    }

    public void start() {
        logger.fine(rlc.id + ": Starting Key Management Life Cycle Thread...");
        super.start();
    }

    public void run() {
        super.run();
        final Throwable t = getThrowable();
        if (t != null)
            t.printStackTrace();
    }
}