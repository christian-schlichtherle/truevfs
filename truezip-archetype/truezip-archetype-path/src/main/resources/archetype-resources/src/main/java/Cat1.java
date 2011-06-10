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
import de.schlichtherle.truezip.nio.fsp.TPath;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * @author  Christian Schlichtherle
 * @version ${symbol_dollar}Id${symbol_dollar}
 */
public class Cat1 extends Application<IOException> {

    public static void main(String[] args) throws IOException {
        System.exit(new Cat1().run(args));
    }

    @Override
    protected int work(String[] args) throws IOException {
        for (String arg : args) {
            //Files.copy(new TPath(arg), System.out); // naive read-then-write loop implementation
            try (InputStream in = Files.newInputStream(new TPath(arg))) {
                Streams.cat(in, System.out); // much better: asynchronous I/O!
            }
            
        }
        return 0;
    }
}
