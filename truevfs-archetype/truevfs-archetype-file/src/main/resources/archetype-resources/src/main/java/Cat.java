#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import net.truevfs.file.TFile;
import java.io.IOException;

/**
 * This command line utility concatenates the contents of the parameter paths
 * on the standard output.
 * 
 * @see     <a href="http://www.gnu.org/software/wget/">GNU Cat - Home Page</a>
 * @author  Christian Schlichtherle
 */
public class Cat extends Application<IOException> {

    public static void main(String[] args) throws IOException {
        System.exit(new Cat().run(args));
    }

    @Override
    protected int work(String[] args) throws IOException {
        for (String arg : args)
            new TFile(arg).output(System.out);
        return 0;
    }
}
