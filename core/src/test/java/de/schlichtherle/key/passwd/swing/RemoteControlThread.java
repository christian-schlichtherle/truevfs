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

/**
 * @author Christian Schlichtherle
 * @version $Revision$
 * @since TrueZIP 6.1
 */
public class RemoteControlThread extends Thread {
    private final RemoteControl rc;

    public RemoteControlThread(final String id) {
        this(new RemoteControl(id));
    }

    public RemoteControlThread(RemoteControl rc) {
        super(rc, "Remote Control Thread for \"" + rc.id + "\"");
        this.rc = rc;
        //setPriority(Thread.MAX_PRIORITY);
        //setDaemon(true); // thread may die.
    }

    /**
     * Returns non-<code>null</code> if and only if this thread has
     * terminated because an assertion error happened.
     */
    public Throwable getThrowable() {
        return rc.throwable;
    }

    public void run() {
        super.run();
        final Throwable t = getThrowable();
        if (t != null)
            t.printStackTrace();
    }
}