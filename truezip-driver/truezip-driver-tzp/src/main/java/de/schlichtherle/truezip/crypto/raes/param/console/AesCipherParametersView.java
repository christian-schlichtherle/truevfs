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
package de.schlichtherle.truezip.crypto.raes.param.console;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.key.PromptingKeyProvider.Controller;
import de.schlichtherle.truezip.key.PromptingKeyProvider.View;
import java.io.Console;
import java.net.URI;
import java.util.Arrays;
import java.util.ResourceBundle;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength.*;

/**
 * A console based user interface to prompt for passwords.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class AesCipherParametersView
implements View<AesCipherParameters> {

    /** Used to lock out concurrent prompting. */
    private static class PromptingLock { }

    private static final String CLASS_NAME
            = AesCipherParametersView.class.getName();
    static final ResourceBundle resources
            = ResourceBundle.getBundle(CLASS_NAME);

    /**
     * The console to use for I/O.
     * If {@code null}, the prompt methods are never called, so it's
     * safe to assume that it's not {@code null} in these methods.
     */
    static final Console con = System.console();

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
    private static URI lastResource = URI.create(""); // NOI18N
    
    private static final String YES = resources.getString("yes");
    private static final String NO = resources.getString("no");

    @Override
    public final void promptCreateKey(
            final Controller<? super AesCipherParameters> controller) {
        synchronized (lock) {
            final URI resource = controller.getResource();
            assert null != resource : "violation of contract for PromptingKeyProviderUI";
            if (!lastResource.equals(resource))
                con.printf(resources.getString("createKey.banner"), resource);
            lastResource = resource;

            final AesCipherParameters param = new AesCipherParameters();

            while (true) {
                char[] newPasswd1 = con.readPassword(
                        resources.getString("createKey.newPasswd1"));
                if (null == newPasswd1 || newPasswd1.length <= 0)
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

                param.setPassword(newPasswd1);
                break;
            }

            con.printf(resources.getString("keyStrength.banner"));
            con.printf(resources.getString("keyStrength.medium"));
            con.printf(resources.getString("keyStrength.high"));
            con.printf(resources.getString("keyStrength.ultra"));

            prompting: while (true) {
                String keyStrength = con.readLine(
                        resources.getString("keyStrength.prompt"),
                        controller);
                if (null == keyStrength || keyStrength.length() <= 0)
                    return;
                try {
                    switch (Integer.parseInt(keyStrength)) {
                        case 128:
                            param.setKeyStrength(BITS_128);
                            break prompting;

                        case 192:
                            param.setKeyStrength(BITS_192);
                            break prompting;

                        case 256:
                            param.setKeyStrength(BITS_256);
                            break prompting;
                    }
                } catch (NumberFormatException syntaxError) {
                }
            }

            controller.setKey(param);
        }
    }

    @Override
    public void promptOpenKey(
            final Controller<? super AesCipherParameters> controller,
            final boolean invalid) {
        synchronized (lock) {
            if (invalid)
                con.printf(resources.getString("openKey.invalid"));

            final URI resource = controller.getResource();
            assert resource != null : "violation of contract for PromptingKeyProviderUI";
            if (!lastResource.equals(resource))
                con.printf(resources.getString("openKey.banner"), resource);
            lastResource = resource;

            final char[] passwd = con.readPassword(resources.getString("openKey.passwd"));
            if (null == passwd || passwd.length <= 0) {
                controller.setKey(null);
                return;
            }

            final AesCipherParameters param = new AesCipherParameters();
            param.setPassword(passwd);
            controller.setKey(param);

            while (true) {
                String changeKey = con.readLine(resources.getString("openKey.change"));
                controller.setChangeRequested(YES.equalsIgnoreCase(changeKey));
                if (       null == changeKey
                        || changeKey.length() <= 0
                        || YES.equalsIgnoreCase(changeKey)
                        || NO.equalsIgnoreCase(changeKey))
                    return;
            }
        }
    }
}
