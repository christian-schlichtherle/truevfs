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
package de.schlichtherle.truezip.key.passwd.console;

import de.schlichtherle.truezip.key.PromptingKeyProvider;
import java.io.Console;
import java.net.URI;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 * A out I/O based user interface to prompt for passwords.
 * This class is thread safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ConsolePromptingKeyProviderUI<P extends PromptingKeyProvider<char[]>>
implements de.schlichtherle.truezip.key.PromptingKeyProviderUI<char[], P> {

    private static final String CLASS_NAME
            = ConsolePromptingKeyProviderUI.class.getName();
    protected static final ResourceBundle resources
            = ResourceBundle.getBundle(CLASS_NAME);

    /**
     * The console to use for I/O.
     * If {@code null}, the prompt methods are never called, so it's
     * safe to assume that it's not {@code null} in these methods.
     */
    protected static final Console con = System.console();

    /**
     * Used to lock out prompting by multiple threads.
     */
    private static final PromptingLock lock = new PromptingLock();

    /** The minimum acceptable lenght of a password. */
    private static final int MIN_PASSWD_LEN = 6;

    /**
     * The last resource ID used when prompting.
     * Initialized to the empty string.
     */
    private static URI lastResource = URI.create(""); // NOI18N

    @Override
    public final void promptCreateKey(final P provider) {
        synchronized (lock) {
            final URI resource = provider.getResource();
            assert resource != null : "violation of contract for PromptingKeyProviderUI";
            if (!resource.equals(lastResource))
                con.printf(resources.getString("createKey.banner"),
                        provider.getResource());
            lastResource = resource;

            while (true) {
                char[] newPasswd1 = con.readPassword(
                        resources.getString("createKey.newPasswd1"));
                if (newPasswd1 == null || newPasswd1.length <= 0)
                    return;

                char[] newPasswd2 = con.readPassword(
                        resources.getString("createKey.newPasswd2"));
                if (newPasswd2 == null)
                    return;

                if (!Arrays.equals(newPasswd1, newPasswd2)) {
                    con.printf(resources.getString("createKey.passwd.noMatch"));
                    continue;
                }

                if (newPasswd1.length < MIN_PASSWD_LEN) {
                    con.printf(resources.getString("createKey.passwd.tooShort"));
                    continue;
                }

                provider.setKey(newPasswd1);
                break;
            }

            promptExtraData(provider);
        }
    }

    protected void promptExtraData(P provider) {
    }

    @Override
    public boolean promptOpenKey(final P provider, final boolean invalid) {
        synchronized (lock) {
            if (invalid)
                con.printf(resources.getString("openKey.invalid"));

            final URI resource = provider.getResource();
            assert resource != null : "violation of contract for PromptingKeyProviderUI";
            if (!resource.equals(lastResource))
                con.printf(resources.getString("openKey.banner"),
                        provider.getResource());
            lastResource = resource;

            char[] passwd = con.readPassword(resources.getString("openKey.passwd"));
            if (passwd == null || passwd.length <= 0) {
                provider.setKey(null);
                return false;
            }

            provider.setKey(passwd);

            while (true) {
                String changeKey = con.readLine(resources.getString("openKey.change"));
                if (changeKey == null)
                    return false;
                changeKey = changeKey.toLowerCase();
                if (changeKey.length() <= 0 || changeKey.equals(resources.getString("no")))
                    return false;
                else if (changeKey.equals(resources.getString("yes")))
                    return true;
            }
        }
    }

    //
    // Miscellaneous.
    //

    private static class PromptingLock { }
}
