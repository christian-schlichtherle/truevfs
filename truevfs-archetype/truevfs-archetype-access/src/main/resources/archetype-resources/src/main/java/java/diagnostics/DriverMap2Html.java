#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.java.diagnostics;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import net.java.truecommons.shed.ExtensionSet;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.sl.FsDriverMapLocator;
import net.java.truevfs.kernel.spec.spi.FsDriverMapFactory;
import net.java.truevfs.kernel.spec.spi.FsDriverMapModifier;

/**
 * Pretty-prints a map of file system drivers to an HTML table.
 * You can use the main method of this utility class to diagnose the class path
 * setup or test custom file system driver map modifier implementations.
 * 
 * @param  <O> The type of the resource for output - typically a
 *         {@link PrintStream} or a {@link PrintWriter}.
 * @author Christian Schlichtherle
 */
public final class DriverMap2Html<O extends Appendable>
implements Callable<O> {

    private static final String TABLE_ATTRIBUTES = " border=\"2\" cellpadding=\"4\""; //"";
    private static final String BEGIN_CODE = "<code>"; //"{@code "
    private static final String END_CODE   = "</code>"; //"}"
    private static final String BEGIN_LINK = "<code>"; //"{@link "
    private static final String END_LINK   = "</code>"; //"}"

    private static Map<FsDriver, ExtensionSet> compact(
            final Map<FsScheme, FsDriver> drivers) {
        final Map<FsDriver, ExtensionSet> extensions = new TreeMap<>(
                new Comparator<FsDriver>() {
            @Override public int compare(FsDriver d1, FsDriver d2) {
                final int d = d1.getClass().getName().compareTo(d2.getClass().getName());
                if (0 != d) return d;
                return d1.equals(d2) ? 0 : -1;
            }
        });
        for (final Entry<FsScheme, FsDriver> entry : drivers.entrySet()) {
            final String scheme = entry.getKey().toString();
            final FsDriver driver = entry.getValue();
            ExtensionSet set = extensions.get(driver);
            if (null == set) extensions.put(driver, set = new ExtensionSet());
            set.add(scheme);
        }
        return Collections.unmodifiableMap(extensions);
    }

    private final Map<FsDriver, ExtensionSet> extensions;
    private final O out;

    public DriverMap2Html(final Map<FsScheme, FsDriver> drivers, final O out) {
        this.extensions = compact(drivers);
        this.out = Objects.requireNonNull(out);
    }

    /**
     * Constructs a new instance and pretty-prints the file system driver map.
     * <p>
     * If no parameters are provided to this method, all file system driver
     * mappings which result from locating and applying all file system driver
     * map modifiers on the class path.
     * You can use this feature to diagnose your class path setup.
     * <p>
     * Alternatively, if parameters are provided to this method, they are
     * interpreted as class names of {@link FsDriverMapModifier}
     * implementations.
     * These will be instantiated and applied to an empty file system driver
     * map in order.
     * You can use this feature to document custom file system driver map
     * modifier implementations.
     * 
     * @param  args a may-be-empty array of {@link FsDriverMapModifier}
     *         implementation classes.
     * @throws Exception if instantiating a class fails for some reason.
     */
    public static void main(String[] args) throws Exception {
        Map<FsScheme, FsDriver> drivers;
        if (0 == args.length) {
            drivers = FsDriverMapLocator.SINGLETON.get();
        } else {
            drivers = new FsDriverMapFactory().get();
            for (final String arg : args) {
                final FsDriverMapModifier modifier
                        = (FsDriverMapModifier) Class.forName(arg).newInstance();
                drivers = modifier.apply(drivers);
            }
        }
        new DriverMap2Html<>(drivers, System.out).call().flush();
    }

    /**
     * Pretty-prints the map of file system drivers to an HTML table.
     * 
     * @return the output resource provided to the constructor.
     * @throws IOException on any I/O error.
     */
    @Override
    public O call() throws IOException {
        out     .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n")
                .append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n")
                .append("  <head>\n")
                .append("    <title>File System Driver Map</title>\n")
                .append("  </head>\n")
                .append("  <body>\n")
                .append("    <table").append(TABLE_ATTRIBUTES).append(">\n")
                .append("      <thead>\n")
                .append("        <tr>\n")
                .append("          <th>URI Schemes</th>\n")
                .append("          <th>File System Driver</th>\n")
                .append("        </tr>\n")
                .append("      </thead>\n")
                .append("      <tbody>\n");
        for (final Entry<FsDriver, ExtensionSet> entry : extensions.entrySet()) {
            out .append("        <tr>\n")
                .append("          <td>");
            final FsDriver driver = entry.getKey();
            final ExtensionSet extensions = entry.getValue();
            for (final Iterator<String> it = extensions.iterator(); it.hasNext(); ) {
                final String extension = it.next();
                out.append(BEGIN_CODE).append(extension).append(END_CODE);
                if (it.hasNext()) out.append(", ");
            }
            out .append("</td>\n")
                .append("          <td>").append(BEGIN_LINK).append(driver.toString()).append(END_LINK).append("</td>\n")
                .append("        </tr>\n");
        }
        out     .append("      </tbody>\n")
                .append("    </table>\n")
                .append("  </body>\n")
                .append("</html>\n");
        return out;
    }
}
