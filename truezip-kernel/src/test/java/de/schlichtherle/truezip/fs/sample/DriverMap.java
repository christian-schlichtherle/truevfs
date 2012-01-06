/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.sample;

import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsDriverProvider;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.sl.FsDriverLocator;
import de.schlichtherle.truezip.util.SuffixSet;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Pretty-prints an HTML table of the map of a file system driver provider.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public final class DriverMap implements Runnable {

    private static final String TABLE_ATTRIBUTES = " border=\"2\" cellpadding=\"4\""; //"";
    private static final String BEGIN_CODE = "<code>"; //"{@code "
    private static final String END_CODE   = "</code>"; //"}"
    private static final String BEGIN_LINK = "<code>"; //"{@link "
    private static final String END_LINK   = "</code>"; //"}"

    private final PrintStream out;
    private final FsDriverProvider provider;

    public DriverMap(final PrintStream out, final FsDriverProvider provider) {
        if (null == out || null == provider)
            throw new NullPointerException();
        this.out = out;
        this.provider = provider;
    }

    public static void main(String[] args) throws Exception {
        final FsDriverProvider provider;
        provider = 0 == args.length
                ? FsDriverLocator.SINGLETON
                : (FsDriverProvider) Class.forName(args[0]).newInstance();
        new DriverMap(System.out, provider).run();
    }

    @Override
    public void run() {
        final Map<FsScheme, FsDriver> map = provider.get();
        final Map<String, SuffixSet> compact = compact(map);
        out     .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n")
                .append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n")
                .append("  <head>\n")
                .append("    <title>File System Driver Map</title>\n")
                .append("  </head>\n")
                .append("  <body>\n")
                .append("    <table").append(TABLE_ATTRIBUTES).append(">\n")
                .append("      <caption>File System Driver Provider Class ").append(BEGIN_LINK).append(provider.getClass().getName()).append(END_LINK).append("</caption>\n")
                .append("      <thead>\n")
                .append("        <tr>\n")
                .append("          <th>URI Schemes</th>\n")
                .append("          <th>Archive File System?</th>\n")
                .append("          <th>File System Driver Class</th>\n")
                .append("        </tr>\n")
                .append("      </thead>\n")
                .append("      <tbody>\n");
        for (Entry<String, SuffixSet> entry : compact.entrySet()) {
            String clazz = entry.getKey();
            List<String> set = new ArrayList<String>(entry.getValue());
            String federated = Boolean.toString(
                    map .get(FsScheme.create(set.iterator().next()))
                        .isFederated());
            out .append("        <tr>\n")
                .append("          <td>");
            for (int i = 0; i < set.size(); i++) {
                if (0 < i)
                    out.append(", ");
                out.append(BEGIN_CODE).append(set.get(i)).append(END_CODE);
            }
            out .append("</td>\n")
                .append("          <td>").append(BEGIN_CODE).append(federated).append(END_CODE).append("</td>\n")
                .append("          <td>").append(BEGIN_LINK).append(clazz).append(END_LINK).append("</td>\n")
                .append("        </tr>\n");
        }
        out     .append("      </tbody>\n")
                .append("    </table>\n")
                .append("  </body>\n")
                .append("</html>\n");
    }

    private static Map<String, SuffixSet> compact(
            final Map<FsScheme, FsDriver> input) {
        final Map<String, SuffixSet>
                output = new TreeMap<String, SuffixSet>();
        for (final Entry<FsScheme, FsDriver> entry : input.entrySet()) {
            final String scheme = entry.getKey().toString();
            final String clazz = entry.getValue().getClass().getName();
            SuffixSet suffixes = output.get(clazz);
            if (null == suffixes) {
                suffixes = new SuffixSet();
                output.put(clazz, suffixes);
            }
            suffixes.add(scheme);
        }
        return Collections.unmodifiableMap(output);
    }
}
