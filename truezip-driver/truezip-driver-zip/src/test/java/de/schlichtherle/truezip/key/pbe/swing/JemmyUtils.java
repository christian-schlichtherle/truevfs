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
package de.schlichtherle.truezip.key.pbe.swing;

import java.awt.Component;
import java.awt.EventQueue;
import javax.swing.JFrame;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.operators.JFrameOperator;

/**
 * A helper class for JUnit tests with JemmyUtils.
 * Note that using this class causes Jemmy to output nothing as a permanent
 * side effect!
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class JemmyUtils {
    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }

    /**
     * Displays a new {@link JFrame} with the given {@code component} and
     * returns an operator for it.
     *
     * @param  component the component to display in the new frame.
     * @return a new operator for the new frame which displays the given
     *         component.
     */
    public static JFrameOperator showInNewFrame(final Component component) {
        final JFrame frame = new JFrame();
        class ShowInNewFrame implements Runnable {
            @Override
            public void run() {
                frame.add(component);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        } // class ShowInNewFrame
        EventQueue.invokeLater(new ShowInNewFrame());
        return new JFrameOperator(frame); // waits for the frame to appear
    }
}
