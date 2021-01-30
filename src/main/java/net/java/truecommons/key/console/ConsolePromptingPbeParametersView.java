/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.console;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.java.truecommons.key.spec.KeyStrength;
import net.java.truecommons.key.spec.prompting.KeyPromptingDisabledException;
import net.java.truecommons.key.spec.prompting.PromptingKey;
import net.java.truecommons.key.spec.prompting.PromptingKey.Controller;
import net.java.truecommons.key.spec.prompting.PromptingPbeParameters;

import javax.annotation.concurrent.ThreadSafe;
import java.io.Console;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * A console based user interface for prompting for passwords.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@ThreadSafe
abstract class ConsolePromptingPbeParametersView<
        P extends PromptingPbeParameters<P, S>,
        S extends KeyStrength>
implements PromptingKey.View<P> {

    private static final ResourceBundle resources = ResourceBundle
            .getBundle(ConsolePromptingPbeParametersView.class.getName());

    /**
     * Used to lock out prompting by multiple threads.
     */
    private static final Object lock = new Object();

    /** The minimum acceptable length of a password. */
    private static final int MIN_PASSWD_LEN = 8;

    /**
     * The last resource ID used when prompting.
     * Initialized to the empty string.
     */
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    private static URI lastResource = URI.create(""); // NOI18N

    private static final String YES = resources.getString("yes");
    private static final String NO = resources.getString("no");

    /**
     * Returns new parameters for safe password based encryption.
     *
     * @return New parameters for safe password based encryption.
     */
    protected abstract P newPbeParameters();

    @Override
    public final void promptKeyForWriting(final Controller<P> controller)
    throws KeyPromptingDisabledException {
        promptKey(new KeyPrompt() {
            @Override
            public void promptKey(final Console con) {
                final URI resource = controller.getResource();
                assert null != resource;
                if (!lastResource.equals(resource))
                    con.format(resources.getString("writeKey.banner"), resource);
                lastResource = resource;

                P param = controller.getKeyClone();
                if (null == param)
                    param = newPbeParameters();

                while (true) {
                    char[] input1 = con.readPassword(
                            resources.getString("writeKey.newPasswd1"));
                    if (null == input1 || 0 >= input1.length)
                        return;
                    if (MIN_PASSWD_LEN > input1.length) {
                        con.format(resources.getString("writeKey.passwd.tooShort"), MIN_PASSWD_LEN);
                        continue;
                    }
                    try {
                        char[] input2 = con.readPassword(
                                resources.getString("writeKey.newPasswd2"));
                        if (input2 == null)
                            return;
                        try {
                            if (!Arrays.equals(input1, input2)) {
                                con.format(resources.getString("writeKey.passwd.noMatch"));
                                continue;
                            }
                            param.setPassword(input1);
                            break;
                        } finally {
                            Arrays.fill(input2, (char) 0);
                        }
                    }  finally {
                        Arrays.fill(input1, (char) 0);
                    }
                }

                con.format(resources.getString("keyStrength.banner"));
                final String selection;
                final Map<Integer, S> map;
                {
                    final StringBuilder builder = new StringBuilder();
                    final S[] array = param.getAllKeyStrengths();
                    map = new HashMap<>(array.length / 3 * 4 + 1);
                    final PrintWriter writer = con.writer();
                    for (final S strength : array) {
                        if (0 < builder.length())
                            builder.append('/');
                        builder.append(strength.getBits());
                        map.put(strength.getBits(), strength);
                        writer.println(strength);
                    }
                    selection = builder.toString();
                }

                while (true) {
                    final S keyStrength = param.getKeyStrength();
                    String input = con.readLine(
                            resources.getString("keyStrength.prompt"),
                            selection,
                            null == keyStrength ? 0 : keyStrength.getBits());
                    if (null == input || input.length() <= 0)
                        break;
                    try {
                        final int bits = Integer.parseInt(input);
                        final S strength = map.get(bits);
                        if (null != strength) {
                            assert strength.getBits() == bits;
                            param.setKeyStrength(strength);
                            break;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }

                controller.setKeyClone(param);
            }
        });
    }

    @Override
    public void promptKeyForReading(
            final Controller<P> controller,
            final boolean invalid)
    throws KeyPromptingDisabledException {
        promptKey(new KeyPrompt() {
            @Override
            public void promptKey(final Console con) {
                if (invalid)
                    con.format(resources.getString("readKey.invalid"));

                final URI resource = controller.getResource();
                assert null != resource;
                if (!lastResource.equals(resource))
                    con.format(resources.getString("readKey.banner"), resource);
                lastResource = resource;

                final char[] passwd = con.readPassword(resources.getString("readKey.passwd"));
                if (null == passwd || passwd.length <= 0) {
                    controller.setKeyClone(null);
                    return;
                }

                final P param = newPbeParameters();
                param.setPassword(passwd);
                Arrays.fill(passwd, (char) 0);

                while (true) {
                    String changeKey = con.readLine(resources.getString("readKey.change"));
                    param.setChangeRequested(YES.equalsIgnoreCase(changeKey));
                    if (null == changeKey ||
                            changeKey.length() <= 0 ||
                            YES.equalsIgnoreCase(changeKey) ||
                            NO.equalsIgnoreCase(changeKey)) {
                        controller.setKeyClone(param);
                        return;
                    }
                }
            }
        });
    }

    private void promptKey(final KeyPrompt keyPrompt)
    throws KeyPromptingDisabledException {
        final Console con = System.console();
        if (null == con)
            throw new KeyPromptingDisabledException();

        synchronized (lock) {
            keyPrompt.promptKey(con);
        }
    }

    private interface KeyPrompt {
        void promptKey(Console con);
    }
}
