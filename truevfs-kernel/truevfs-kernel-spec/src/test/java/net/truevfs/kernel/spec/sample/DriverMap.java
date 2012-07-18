/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.sample;

import java.io.PrintStream;
import java.util.Map.Entry;
import java.util.*;
import net.truevfs.kernel.spec.FsDriver;
import net.truevfs.kernel.spec.FsScheme;
import net.truevfs.kernel.spec.sl.FsDriverMapLocator;
import net.truevfs.kernel.spec.spi.FsDriverMapFactory;
import net.truevfs.kernel.spec.spi.FsDriverMapModifier;
import net.truevfs.kernel.spec.util.ExtensionSet;

/**
 * Pretty-prints an HTML table of the driver map of a given file system driver
 * provider.
 * 
 * @author Christian Schlichtherle
 */
public final class DriverMap implements Runnable {

    private static final String TABLE_ATTRIBUTES = " border=\"2\" cellpadding=\"4\""; //"";
    private static final String BEGIN_CODE = "<code>"; //"{@code "
    private static final String END_CODE   = "</code>"; //"}"
    private static final String BEGIN_LINK = "<code>"; //"{@link "
    private static final String END_LINK   = "</code>"; //"}"

    private final PrintStream out;
    private final Map<FsScheme, FsDriver> map;

    public DriverMap(final PrintStream out, final Map<FsScheme, FsDriver> map) {
        this.out = Objects.requireNonNull(out);
        this.map = Objects.requireNonNull(new LinkedHashMap<>(map));
    }

    public static void main(String[] args) throws Exception {
        Map<FsScheme, FsDriver> map;
        if (0 == args.length) {
            map = FsDriverMapLocator.SINGLETON.get();
        } else {
            map = new FsDriverMapFactory().get();
            for (final String arg : args) {
                final FsDriverMapModifier modifier
                        = (FsDriverMapModifier) Class.forName(arg).newInstance();
                map = modifier.apply(map);
            }
        }
        new DriverMap(System.out, map).run();
    }

    @Override
    public void run() {
        final Map<String, ExtensionSet> compact = compact(map);
        out     .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n")
                .append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n")
                .append("  <head>\n")
                .append("    <title>File System Driver Map</title>\n")
                .append("  </head>\n")
                .append("  <body>\n")
                .append("    <table").append(TABLE_ATTRIBUTES).append(">\n")
                //.append("      <caption>File System Driver Provider Class ").append(BEGIN_LINK).append(provider.getClass().getName()).append(END_LINK).append("</caption>\n")
                .append("      <thead>\n")
                .append("        <tr>\n")
                .append("          <th>URI Schemes</th>\n")
                .append("          <th>Archive Driver?</th>\n")
                .append("          <th>File System Driver Class</th>\n")
                .append("        </tr>\n")
                .append("      </thead>\n")
                .append("      <tbody>\n");
        for (Entry<String, ExtensionSet> entry : compact.entrySet()) {
            String clazz = entry.getKey();
            List<String> set = new ArrayList<>(entry.getValue());
            String federated = Boolean.toString(
                    map .get(FsScheme.create(set.iterator().next()))
                        .isArchiveDriver());
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

    private static Map<String, ExtensionSet> compact(
            final Map<FsScheme, FsDriver> input) {
        final Map<String, ExtensionSet> output = new TreeMap<>();
        for (final Entry<FsScheme, FsDriver> entry : input.entrySet()) {
            final String scheme = entry.getKey().toString();
            final String clazz = entry.getValue().getClass().getName();
            ExtensionSet extensions = output.get(clazz);
            if (null == extensions) {
                extensions = new ExtensionSet();
                output.put(clazz, extensions);
            }
            extensions.add(scheme);
        }
        return Collections.unmodifiableMap(output);
    }
}
