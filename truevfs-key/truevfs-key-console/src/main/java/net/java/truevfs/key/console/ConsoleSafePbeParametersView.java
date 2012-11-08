/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.console;

import java.io.Console;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.KeyPromptingDisabledException;
import net.java.truevfs.key.spec.PromptingKeyProvider;
import net.java.truevfs.key.spec.PromptingKeyProvider.Controller;
import net.java.truevfs.key.spec.param.KeyStrength;
import net.java.truevfs.key.spec.param.SafePbeParameters;

/**
 * A console based user interface for prompting for passwords.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
abstract class ConsoleSafePbeParametersView<
        P extends SafePbeParameters<P, S>,
        S extends KeyStrength>
implements PromptingKeyProvider.View<P> {

    private static final ResourceBundle resources = ResourceBundle
            .getBundle(ConsoleSafePbeParametersView.class.getName());

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
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
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
    public final void promptForWriting(final Controller<P> controller)
    throws KeyPromptingDisabledException {
        final Console con = System.console();
        if (null == con) throw new KeyPromptingDisabledException();

        synchronized (lock) {
            final URI resource = controller.getResource();
            assert null != resource;
            if (!lastResource.equals(resource))
                con.printf(resources.getString("writeKey.banner"), resource);
            lastResource = resource;

            P param = controller.getKey();
            if (null == param) param = newPbeParameters();

            while (true) {
                char[] input1 = con.readPassword(
                        resources.getString("writeKey.newPasswd1"));
                if (null == input1 || 0 >= input1.length) return;
                if (MIN_PASSWD_LEN > input1.length) {
                    con.printf(resources.getString("writeKey.passwd.tooShort"), MIN_PASSWD_LEN);
                    continue;
                }
                try {
                    char[] input2 = con.readPassword(
                            resources.getString("writeKey.newPasswd2"));
                    if (input2 == null) return;
                    try {
                        if (!Arrays.equals(input1, input2)) {
                            con.printf(resources.getString("writeKey.passwd.noMatch"));
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

            con.printf(resources.getString("keyStrength.banner"));
            final String selection;
            final Map<Integer, S> map;
            {
                final StringBuilder builder = new StringBuilder();
                final S[] array = param.getKeyStrengthValues();
                map = new HashMap<>(array.length / 3 * 4 + 1);
                final PrintWriter writer = con.writer();
                for (final S strength : array) {
                    if (0 < builder.length()) builder.append('/');
                    builder.append(strength.getBits());
                    map.put(strength.getBits(), strength);
                    writer.println(strength);
                }
                selection = builder.toString();
            }

            while (true) {
                String input = con.readLine(
                        resources.getString("keyStrength.prompt"),
                        selection,
                        param.getKeyStrength().getBits());
                if (null == input || input.length() <= 0) break;
                try {
                    final int bits = Integer.parseInt(input);
                    final S strength = map.get(bits);
                    if (null != strength) {
                        assert strength.getBits() == bits;
                        param.setKeyStrength(strength);
                        break;
                    }
                } catch (NumberFormatException ex) {
                }
            }

            controller.setKey(param);
        }
    }

    @Override
    public void promptForReading(
            final Controller<P> controller,
            final boolean invalid)
    throws KeyPromptingDisabledException {
        final Console con = System.console();
        if (null == con) throw new KeyPromptingDisabledException();

        synchronized (lock) {
            if (invalid) con.printf(resources.getString("readKey.invalid"));

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
