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
package de.schlichtherle.truezip.swing;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.EventListener;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JDialogOperator;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class EnhancedPanelTest {
    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }

    private EnhancedPanel instance;

    @Before
    public void setUp() {
        instance = new EnhancedPanel();
        instance.add(new JLabel("Hello world!"));
    }

    @Test
    public void testGetAncestorWindow()
    throws InterruptedException, InvocationTargetException {
        assertNull(new EnhancedPanel().getAncestorWindow());

        JFrame frame = showInNewFrame(instance);
        Window window1 = instance.getAncestorWindow();
        assertSame(frame, window1);
        disposeFrame(frame);

        frame = showInNewFrame(instance); // change enclosing frame
        Window window2 = instance.getAncestorWindow();
        assertSame(frame, window2);
        disposeFrame(frame);

        assertNotSame(window1, window2);
    }

    private static JFrame showInNewFrame(final Component component)
    throws InterruptedException, InvocationTargetException {
        final JFrame frame = new JFrame();
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.add(component);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
        return frame;
    }

    private static void disposeFrame(final JFrame frame)
    throws InterruptedException, InvocationTargetException {
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                //frame.setVisible(false);
                frame.dispose();
            }
        });
    }

    @Test
    public void testPanelListeners0() {
        PanelListener listener;
        EventListener[] listeners;

        listeners = instance.getPanelListeners();
        assertEquals(0, listeners.length);

        listener = null;

        try {
            instance.addPanelListener(listener);
            fail("Previous method call should throw an NPE!");
        } catch (NullPointerException expected) {
        }
        listeners = instance.getPanelListeners();
        assertEquals(0, listeners.length);

        try {
            instance.removePanelListener(listener);
            fail("Previous method call should throw an NPE!");
        } catch (NullPointerException expected) {
        }
        listeners = instance.getPanelListeners();
        assertEquals(0, listeners.length);

        listener = new CountingPanelListener();

        instance.addPanelListener(listener);
        listeners = instance.getPanelListeners();
        assertEquals(1, listeners.length);
        assertSame(listener, listeners[0]);

        instance.addPanelListener(listener);
        listeners = instance.getPanelListeners();
        assertEquals(2, listeners.length);
        assertSame(listener, listeners[0]);
        assertSame(listener, listeners[1]);

        instance.removePanelListener(listener);
        listeners = instance.getPanelListeners();
        assertEquals(1, listeners.length);
        assertSame(listener, listeners[0]);

        instance.removePanelListener(listener);
        listeners = instance.getPanelListeners();
        assertEquals(0, listeners.length);
    }

    private static class CountingPanelListener implements PanelListener {
        public int shown, hidden;

        @Override
        public void ancestorWindowShown(PanelEvent evt) {
            shown++;
        }

        @Override
        public void ancestorWindowHidden(PanelEvent evt) {
            hidden++;
        }
    }

    @Test
    public void testPanelListeners1() {
        PanelListener listener;
        EventListener[] listeners;

        listeners = instance.getListeners(PanelListener.class);
        assertEquals(0, listeners.length);

        listener = null;

        try {
            instance.addPanelListener(listener);
            fail("Previous method call should throw an NPE!");
        } catch (NullPointerException expected) {
        }
        listeners = instance.getListeners(PanelListener.class);
        assertEquals(0, listeners.length);

        try {
            instance.removePanelListener(listener);
            fail("Previous method call should throw an NPE!");
        } catch (NullPointerException expected) {
        }
        listeners = instance.getListeners(PanelListener.class);
        assertEquals(0, listeners.length);

        listener = new CountingPanelListener();

        instance.addPanelListener(listener);
        listeners = instance.getListeners(PanelListener.class);
        assertEquals(1, listeners.length);
        assertSame(listener, listeners[0]);

        instance.addPanelListener(listener);
        listeners = instance.getListeners(PanelListener.class);
        assertEquals(2, listeners.length);
        assertSame(listener, listeners[0]);
        assertSame(listener, listeners[1]);

        instance.removePanelListener(listener);
        listeners = instance.getListeners(PanelListener.class);
        assertEquals(1, listeners.length);
        assertSame(listener, listeners[0]);

        instance.removePanelListener(listener);
        listeners = instance.getListeners(PanelListener.class);
        assertEquals(0, listeners.length);
    }

    @Test
    public void testFireAncestorWindowShown() {
        final CountingPanelListener l = new CountingPanelListener();
        instance.addPanelListener(l);
        instance.addPanelListener(l); // add again to receive same event twice!
        assertEquals(0, l.shown);

        PanelEvent event = null;
        instance.fireAncestorWindowShown(event);
        assertEquals(2, l.shown);
    }

    @Test
    public void testFireAncestorWindowHidden() {
        final CountingPanelListener l = new CountingPanelListener();
        instance.addPanelListener(l);
        instance.addPanelListener(l); // add again to receive same event twice!
        assertEquals(0, l.hidden);

        PanelEvent event = null;
        instance.fireAncestorWindowHidden(event);
        assertEquals(2, l.hidden);
    }

    /**
     * Adds the EnhancedPanel instance to a frame, shows and hides this frame
     * multiple times and counts these events.
     * Then puts the same EnhancedPanel instance into a second frame and repeats
     * the test.
     */
    @Test
    public void testEvents4JFrame() throws Exception {
        final CountingPanelListener l = new CountingPanelListener();
        instance.addPanelListener(l);

        class FrameHolder {
            protected final JFrame frame;

            protected FrameHolder(JFrame frame) {
                this.frame = frame;
            }
        }

        class SetupFrame extends FrameHolder implements Runnable {
            SetupFrame(JFrame frame) {
                super(frame);
            }

            @Override
            public void run() {
                frame.add(instance);
                frame.pack();
                frame.setLocationRelativeTo(null);
            }
        }

        class MakeFrameVisible extends FrameHolder implements Runnable {
            MakeFrameVisible(JFrame frame) {
                super(frame);
            }

            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                // Use multiple ways to make the frame visible.
                // Depending on the version of the Swing implementation, this may
                // create multiple PanelEvents with an ID equal to
                // PanelEvent.ANCESTOR_WINDOW_SHOWN.
                // However, all these events are coalesced into one.
                frame.show();
                frame.setVisible(true);
            }
        }

        class MakeFrameInvisible extends FrameHolder implements Runnable {
            MakeFrameInvisible(JFrame frame) {
                super(frame);
            }

            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                // Use multiple ways to make the frame invisible.
                // Depending on the version of the Swing implementation, this may
                // create multiple PanelEvents with an ID equal to
                // PanelEvent.ANCESTOR_WINDOW_HIDDEN.
                // However, all these events are coalesced into one.
                frame.setVisible(false);
                frame.hide();
                frame.dispose();
            }
        }

        // Test in first frame.
        final JFrame frame1 = new JFrame();
        EventQueue.invokeAndWait(new SetupFrame(frame1));
        events(new MakeFrameVisible(frame1), new MakeFrameInvisible(frame1), l);

        // Test in second frame.
        final JFrame frame2 = new JFrame();
        EventQueue.invokeAndWait(new SetupFrame(frame2));
        events(new MakeFrameVisible(frame2), new MakeFrameInvisible(frame2), l);
    }

    private void events(
            final Runnable makeVisible,
            final Runnable makeInvisible,
            final CountingPanelListener l)
    throws  Exception {
        l.shown = l.hidden = 0; // reset counters

        for (int i = 1; i <= 3; i++) {
            EventQueue.invokeAndWait(makeVisible);
            assertEquals(i, l.shown);
            assertEquals(i - 1, l.hidden);

            EventQueue.invokeAndWait(makeInvisible);
            assertEquals(i, l.shown);
            assertEquals(i, l.hidden);
        }
    }

    /**
     * Shows and hides the EnhancedPanel instance several times in a
     * JOptionPane and counts these events.
     */
    @Test
    public void testEvents4JOptionPane() throws Exception {
        final CountingPanelListener l = new CountingPanelListener();
        instance.addPanelListener(l);

        final Runnable makeVisible = new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, instance);
            }
        };

        for (int i = 1; i <= 3; i++) {
            EventQueue.invokeLater(makeVisible);
            JDialogOperator dialogOp = new JDialogOperator(); // wait for JOptionPane
            assertEquals(i, l.shown);
            assertEquals(i - 1, l.hidden);

            JButtonOperator buttonOp = new JButtonOperator(dialogOp);
            buttonOp.push();
            assertEquals(i, l.shown);
            assertEquals(i, l.hidden);
        }
    }
}
