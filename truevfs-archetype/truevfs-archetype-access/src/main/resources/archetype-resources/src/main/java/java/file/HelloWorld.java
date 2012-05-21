#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.java.file;

import ${package}.java.Application;
import net.truevfs.access.TFile;
import net.truevfs.access.TFileWriter;
import java.io.IOException;
import java.io.Writer;

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
 */
public class HelloWorld extends Application<IOException> {

    public static void main(String[] args) throws IOException {
        System.exit(new HelloWorld().run(args));
    }

    @Override
    protected int work(String[] args) throws IOException {
        // By default, ZIP files use character set IBM437 to encode entry names
        // whereas JAR files use UTF-8.
        // This can be changed by configuring the respective archive driver,
        // see Javadoc for TApplication.setup().
        final Writer writer = new TFileWriter(
                new TFile("archive.zip/dir/HälloWörld.txt"));
        try {
            writer.write("Hello world!\n");
        } finally {
            writer.close();
        }
        return 0;
    }
}
