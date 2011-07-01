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

import de.schlichtherle.truezip.crypto.raes.param.swing.HurlingWindowFeedback;
import de.schlichtherle.truezip.crypto.raes.param.swing.InvalidKeyFeedback;
import de.schlichtherle.truezip.file.TApplication;

/**
 * An abstract class which configures the TrueZIP FSP JSE7 module.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
abstract class Application<E extends Exception> extends TApplication<E> {

    /**
     * Runs the setup phase.
     * <p>
     * This method is {@link ${symbol_pound}run run} only once at the start of the life
     * cycle.
     * Its task is to configure the default behavior of the TrueZIP FSP JSE7 API
     * in order to answer the following questions:
     * <ul>
     * <li>What are the file suffixes which shall be recognized as archive
     *     files and hence as virtual directories?
     * <li>Shall missing archive files and directory entries get automatically
     *     created whenever required?
     * </ul>
     * <p>
     * The implementation in the class {@link Application} configures
     * the type of the feedback when prompting the user for keys for RAES
     * encrypted ZIP alias ZIP.RAES alias TZP files by the Swing based
     * prompting key manager.
     * If this JVM is running in headless mode, then this configuration is
     * ignored and the user is prompted by the console I/O based prompting
     * key manager.
     */
    @Override
    protected void setup() {
        String spec = InvalidKeyFeedback.class.getName();
        String impl = HurlingWindowFeedback.class.getName();
        System.setProperty(spec, System.getProperty(spec, impl));
    }

    /*@Override
    protected void sync() throws FsSyncException {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ex) {
            Logger  .getLogger(Application.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        super.sync();
    }*/
}
