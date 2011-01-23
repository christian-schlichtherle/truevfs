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
package de.schlichtherle.truezip.sample.file.app;

import de.schlichtherle.truezip.file.swing.TFileChooser;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Lets you pick a file using {@link TFileChooser} and print it's path.
 * Note that {@link TFileChooser} can browse archive files, too.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class Pickr extends CommandLineUtility {

    /** Equivalent to {@code System.exit(new Pickr().run(args));}. */
    public static void main(String[] args) {
        System.exit(new Pickr().run(args));
    }

    @Override
    public int runChecked(final String[] args)
    throws IllegalUsageException, IOException {
        try {
            final TFileChooser pickr = new TFileChooser();
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    if (TFileChooser.APPROVE_OPTION == pickr.showDialog(
                            null, "Pick this!"))
                        out.println(pickr.getSelectedFile());
                }
            });
            return 0;
        } catch (InterruptedException ex) {
            Logger  .getLogger(Pickr.class.getName())
                    .log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            return 1;
        } catch (InvocationTargetException ex) {
            Logger  .getLogger(Pickr.class.getName())
                    .log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            return 2;
        }
    }
}
