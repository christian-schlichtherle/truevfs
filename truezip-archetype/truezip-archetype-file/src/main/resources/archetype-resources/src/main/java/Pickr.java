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

import de.schlichtherle.truezip.file.swing.TFileChooser;
import javax.swing.SwingUtilities;

/**
 * This command line utility lets you pick a file using a {@link TFileChooser}
 * and prints it's path.
 * Of course, {@code TFileChooser} can browse archive files, too.
 * <p>
 * For example, if the JAR for the module {@code truezip-driver-zip} is present
 * on the run time class path and a ZIP file {@code archive.zip} exists, then
 * you can double click it to browse its entries.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class Pickr extends Application<Exception> {

    /** Equivalent to {@code System.exit(new Pickr().run(args));}. */
    public static void main(String[] args) throws Exception {
        System.exit(new Pickr().run(args));
    }

    @Override
    protected int work(final String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                final TFileChooser pickr = new TFileChooser();
                if (TFileChooser.APPROVE_OPTION == pickr.showDialog(
                        null, "Pick this!"))
                    System.out.println(pickr.getSelectedFile());
            }
        });
        return 0;
    }
}
