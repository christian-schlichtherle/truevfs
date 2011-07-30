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
package de.schlichtherle.truezip.key.pbe.console;

import de.schlichtherle.truezip.crypto.KeyStrength;
import de.schlichtherle.truezip.key.KeyPromptingDisabledException;
import de.schlichtherle.truezip.key.pbe.PbeParameters;
import de.schlichtherle.truezip.key.PromptingKeyProvider.Controller;
import de.schlichtherle.truezip.key.PromptingKeyProvider.View;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.Console;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.ResourceBundle;
import net.jcip.annotations.ThreadSafe;

/**
 * A console based user interface to prompt for passwords.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class PbeParametersView<S extends KeyStrength, P extends PbeParameters<S, P>>
implements View<P> {

    /** Used to lock out concurrent prompting. */
    private static class PromptingLock { }

    private static final ResourceBundle resources
            = ResourceBundle.getBundle(PbeParametersView.class.getName());

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

    /**
     * Returns new parameters for password based encryption.
     * 
     * @return New parameters for password based encryption.
     */
    protected abstract P newPbeParameters();

    @Override
    public final void promptWriteKey(final Controller<P> controller)
    throws KeyPromptingDisabledException {
        final Console con = System.console();
        if (null == con)
            throw new KeyPromptingDisabledException();

        synchronized (lock) {
            final URI resource = controller.getResource();
            assert null != resource;
            if (!lastResource.equals(resource))
                con.printf(resources.getString("writeKey.banner"), resource);
            lastResource = resource;

            P param = controller.getKey();
            if (null == param)
                param = newPbeParameters();

            while (true) {
                char[] newPasswd1 = con.readPassword(
                        resources.getString("writeKey.newPasswd1"));
                if (null == newPasswd1 || 0 >= newPasswd1.length)
                    return;
                try {
                    char[] newPasswd2 = con.readPassword(
                            resources.getString("writeKey.newPasswd2"));
                    if (newPasswd2 == null)
                        return;
                    try {
                        if (!Arrays.equals(newPasswd1, newPasswd2)) {
                            con.printf(resources.getString("writeKey.passwd.noMatch"));
                            continue;
                        }
                        if (newPasswd1.length < MIN_PASSWD_LEN) {
                            con.printf(resources.getString("writeKey.passwd.tooShort"));
                            continue;
                        }
                        param.setPassword(newPasswd1);
                        break;
                    } finally {
                        Arrays.fill(newPasswd2, (char) 0);
                    }
                }  finally {
                    Arrays.fill(newPasswd1, (char) 0);
                }
            }

            con.printf(resources.getString("keyStrength.banner"));
            final String selection;
            {
                final PrintWriter writer = con.writer();
                final StringBuilder builder = new StringBuilder();
                for (final S strength : param.getAvailableKeyStrengths()) {
                    writer.println(strength);
                    if (0 < builder.length())
                        builder.append('/');
                    builder.append(strength.getBits());
                }
                selection = builder.toString();
            }

            prompting: while (true) {
                String keyStrength = con.readLine(
                        resources.getString("keyStrength.prompt"),
                        selection,
                        param.getKeyStrength().getBits());
                if (null == keyStrength || keyStrength.length() <= 0)
                    return;
                try {
                    final int bits = Integer.parseInt(keyStrength);
                    for (final S strength : param.getAvailableKeyStrengths()) {
                        if (strength.getBits() == bits) {
                            param.setKeyStrength(strength);
                            break prompting;
                        }
                    }
                } catch (NumberFormatException syntaxError) {
                }
            }

            controller.setKey(param);
        }
    }

    @Override
    public void promptReadKey(
            final Controller<P> controller,
            final boolean invalid)
    throws KeyPromptingDisabledException {
        final Console con = System.console();
        if (null == con)
            throw new KeyPromptingDisabledException();

        synchronized (lock) {
            if (invalid)
                con.printf(resources.getString("readKey.invalid"));

            final URI resource = controller.getResource();
            assert null != resource;
            if (!lastResource.equals(resource))
                con.printf(resources.getString("readKey.banner"), resource);
            lastResource = resource;

            final char[] passwd = con.readPassword(resources.getString("readKey.passwd"));
            if (null == passwd || passwd.length <= 0) {
                controller.setKey(null);
                return;
            }

            final P param = newPbeParameters();
            param.setPassword(passwd);
            Arrays.fill(passwd, (char) 0);
            controller.setKey(param);

            while (true) {
                String changeKey = con.readLine(resources.getString("readKey.change"));
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
