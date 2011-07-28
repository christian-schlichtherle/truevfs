#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package ${package};

import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.nio.file.TPath;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This command line utility concatenates the contents of the parameter paths
 * on the standard output.
 * 
 * @see     <a href="http://www.gnu.org/software/wget/">GNU Cat - Home Page</a>
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class Cat1 extends Application<IOException> {

    public static void main(String[] args) throws IOException {
        System.exit(new Cat1().run(args));
    }

    @Override
    protected int work(String[] args) throws IOException {
        for (String arg : args) {
            Path path = new TPath(arg);
            //Files.copy(path, System.out); // naive read-then-write loop implementation
            try (InputStream in = Files.newInputStream(path)) {
                // Much faster: Uses multithreaded I/O with pooled threads and
                // ring buffers!
                Streams.cat(in, System.out);
            }
            
        }
        return 0;
    }
}
