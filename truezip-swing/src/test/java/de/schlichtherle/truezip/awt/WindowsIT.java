/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.awt;

import de.schlichtherle.truezip.awt.Windows;
import java.awt.EventQueue;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class WindowsIT {

    @Test
    public void testParentWindow()
    throws InterruptedException, InvocationTargetException {
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                Window result = Windows.getParentWindow();
                assertNotNull(result);
                assertFalse(result.isVisible());

                final JFrame frame = new JFrame();
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                final JDialog dialog = new JDialog(frame);
                dialog.pack();
                dialog.setLocationRelativeTo(null);

                assertFalse(dialog.isVisible());
                result = Windows.getParentWindow();
                assertSame(frame, result);

                dialog.setVisible(true);
                result = Windows.getParentWindow();
                assertSame(frame, result);

                dialog.dispose();
                frame.dispose();
            }
        });
    }
}
