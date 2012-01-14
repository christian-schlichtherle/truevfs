/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.swing;

import java.awt.Component;
import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.operators.JFrameOperator;

/**
 * A helper class for JUnit tests with JemmyUtils.
 * Note that using this class causes Jemmy to output nothing as a permanent
 * side effect!
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class JemmyUtils {
    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }

    /**
     * Displays a new {@link JFrame} with the given {@code component} and
     * returns an operator for it.
     *
     * @param  component the component to display in the new frame.
     * @return An operator for the new frame which shows the given component.
     */
    public static JFrameOperator showFrameWith(final Component component)
    throws InterruptedException {
        class Show implements Runnable {
            JFrame frame;

            @Override
            public void run() {
                frame = new JFrame();
                frame.add(component);
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        } // Show
        
        final Show show = new Show();
        try {
            EventQueue.invokeAndWait(show);
        } catch (InvocationTargetException ex) {
            throw new AssertionError(ex);
        }
        return new JFrameOperator(show.frame);
    }

    /** You cannot instantiate this class. */
    private JemmyUtils() { }
}
