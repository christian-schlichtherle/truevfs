/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.awt;

import de.schlichtherle.truezip.awt.Windows;
import java.awt.EventQueue;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JDialog;
import javax.swing.JFrame;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class WindowsTest {

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
