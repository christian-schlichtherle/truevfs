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

package de.schlichtherle.key.passwd.console;

import de.schlichtherle.key.*;

import java.io.*;
import java.util.*;

/**
 * A out I/O based user interface to prompt for passwords.
 * This class is thread safe.
 * 
 * @author Christian Schlichtherle
 * @version $Revision$
 * @since TrueZIP 6.4
 */
public class PromptingKeyProviderUI
        implements de.schlichtherle.key.PromptingKeyProviderUI {

    private static final String CLASS_NAME
            = "de/schlichtherle/key/passwd/console/PromptingKeyProviderUI".replace('/', '.'); // support code obfuscation!
    protected static final ResourceBundle resources
            = ResourceBundle.getBundle(CLASS_NAME);

    /**
     * The console to use for I/O.
     * If <code>null</code>, the prompt methods are never called, so it's
     * safe to assume that it's not <code>null</code> in these methods.
     */
    protected static final Console con = System.console();

    /**
     * Used to lock out prompting by multiple threads.
     */
    private static final PromptingLock lock = new PromptingLock();

    /** The minimum acceptable length of a password. */
    private static final int MIN_PASSWD_LEN = 6;

    /**
     * The last resource ID used when prompting.
     * Initialized to the empty string.
     */
    private static String lastResourceID = "";

    public final void promptCreateKey(final PromptingKeyProvider provider) {
        synchronized (lock) {
            final String resourceID = provider.getResourceID();
            if (!resourceID.equals(lastResourceID))
                printf(resources.getString("createKey.banner"),
                        provider.getResourceID());
            lastResourceID = resourceID;

            while (true) {
                char[] newPasswd1 = readPassword(
                        resources.getString("createKey.newPasswd1"));
                if (newPasswd1 == null || newPasswd1.length <= 0)
                    return;

                char[] newPasswd2 = readPassword(
                        resources.getString("createKey.newPasswd2"));
                if (newPasswd2 == null)
                    return;

                if (!Arrays.equals(newPasswd1, newPasswd2)) {
                    printf(resources.getString("createKey.passwd.noMatch"));
                    continue;
                }

                if (newPasswd1.length < MIN_PASSWD_LEN) {
                    printf(resources.getString("createKey.passwd.tooShort"));
                    continue;
                }

                provider.setKey(newPasswd1);
                break;
            }

            promptExtraData(provider);
        }
    }

    protected void promptExtraData(PromptingKeyProvider provider)
    {
    }

    public final boolean promptUnknownOpenKey(PromptingKeyProvider provider) {
        synchronized (lock) {
            return promptOpenKey(provider, false);
        }
    }

    public final boolean promptInvalidOpenKey(PromptingKeyProvider provider) {
        synchronized (lock) {
            return promptOpenKey(provider, true);
        }
    }

    private boolean promptOpenKey(final PromptingKeyProvider provider, final boolean invalid) {
        if (invalid)
            printf(resources.getString("openKey.invalid"));

        final String resourceID = provider.getResourceID();
        if (!resourceID.equals(lastResourceID))
            printf(resources.getString("openKey.banner"),
                    provider.getResourceID());
        lastResourceID = resourceID;

        char[] passwd = readPassword(resources.getString("openKey.passwd"));
        if (passwd == null || passwd.length <= 0) {
            provider.setKey(null);
            return false;
        }

        provider.setKey(passwd);

        while (true) {
            String changeKey = readLine(resources.getString("openKey.change"));
            if (changeKey == null)
                return false;
            changeKey = changeKey.toLowerCase();
            if (changeKey.length() <= 0 || changeKey.equals(resources.getString("no")))
                return false;
            else if (changeKey.equals(resources.getString("yes")))
                return true;
        }
    }

    //
    // TrueZIP 6.4 is still Java source level 1.4, so we need these helpers
    // as a substitute for the varargs calls.
    //

    protected final Console printf(String format) {
        return con.printf(format, null);
    }

    protected final Console printf(String format, Object arg) {
        return con.printf(format, new Object[] { arg });
    }

    protected final String readLine(String format) {
        return con.readLine(format, null);
    }

    protected final String readLine(String format, Object arg) {
        return con.readLine(format, new Object[] { arg });
    }

    protected final char[] readPassword(String format) {
        return con.readPassword(format, null);
    }

    //
    // Miscellaneous.
    //

    private static class PromptingLock { }
}
