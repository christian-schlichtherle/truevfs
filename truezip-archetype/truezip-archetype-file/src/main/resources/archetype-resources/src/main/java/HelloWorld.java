#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import de.schlichtherle.truezip.file.*;
import java.io.*;
import java.nio.charset.Charset;

/**
 * Creates a ZIP file with the name {@code "archive.zip"} in the current
 * directory with the single entry named {@code "HälloWörld.txt"}.
 * The name of the entry will get encoded using the character set CP437.
 * The content of the entry will be the text {@code "Hello World!\n"} which
 * gets encoded using the JVM's default character set.
 * If the entry already exists, it will get overwritten.
 * If present, any other ZIP file entries will remain unchanged.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class HelloWorld extends Application<IOException> {

    public static void main(String[] args) throws IOException {
        System.exit(new HelloWorld().run(args));
    }

    @Override
    protected int work(String[] args) throws IOException {
        // By default, ZIP files use character set CP427 to encode entry names
        // whereas JAR files use UTF-8.
        // This can be changed by configuring the respective archive driver,
        // see Javadoc for TApplication.setup().
        final Writer writer = new TFileWriter(
                new TFile("archive.zip/HälloWörld.txt"),
                Charset.defaultCharset());
        try {
            writer.write("Hello World!\n");
        } finally {
            writer.close();
        }
        return 0;
    }
}
