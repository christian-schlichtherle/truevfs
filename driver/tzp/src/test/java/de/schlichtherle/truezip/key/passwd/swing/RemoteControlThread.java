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
package de.schlichtherle.truezip.key.passwd.swing;

import java.net.URI;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class RemoteControlThread extends Thread {
    private final RemoteControl rc;

    public RemoteControlThread(final URI id) {
        this(new RemoteControl(id));
    }

    public RemoteControlThread(RemoteControl rc) {
        super(rc, "Remote Control Thread for \"" + rc.id + "\"");
        setDaemon(true); // thread may die.
        //setPriority(Thread.MAX_PRIORITY);
        this.rc = rc;
    }

    /**
     * Returns non-{@code null} if and only if this thread has
     * terminated because an assertion error happened.
     */
    public Throwable getThrowable() {
        return rc.throwable;
    }

    @Override
    @SuppressWarnings({"CallToThreadRun", "CallToThreadDumpStack"})
    public void run() {
        super.run();
        final Throwable t = getThrowable();
        if (t != null)
            t.printStackTrace();
    }
}