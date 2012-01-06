/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
