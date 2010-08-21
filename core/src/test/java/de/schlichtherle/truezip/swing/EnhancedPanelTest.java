/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.swing.event.PanelEvent;
import de.schlichtherle.truezip.swing.event.PanelListener;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.EventListener;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import junit.framework.TestCase;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JDialogOperator;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class EnhancedPanelTest extends TestCase {
    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }

    private EnhancedPanel instance;

    public EnhancedPanelTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        instance = new EnhancedPanel();
        instance.add(new JLabel("Hello world!"));
    }

    @Override
    protected void tearDown() throws Exception {
    }

    /**
     * Test of getAncestorWindow method, of class de.schlichtherle.truezip.swing.EnhancedPanel.
     */
    public void testGetAncestorWindow() throws Exception {
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

    private static JFrame showInNewFrame(final Component instance)
    throws InterruptedException, InvocationTargetException {
        final JFrame frame = new JFrame();
        invokeAndWait(new Runnable() {
            public void run() {
                frame.getContentPane().add(instance);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
        return frame;
    }

    private static void disposeFrame(final JFrame frame)
    throws InterruptedException, InvocationTargetException {
        invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(false);
                frame.dispose();
            }
        });
    }

    /**
     * Test of getPanelListeners method, of class de.schlichtherle.truezip.swing.EnhancedPanel.
     */
    public void testPanelListeners1() {
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

        public void ancestorWindowShown(PanelEvent evt) {
            shown++;
        }

        public void ancestorWindowHidden(PanelEvent evt) {
            hidden++;
        }
    }

    /**
     * Test of getListeners method, of class de.schlichtherle.truezip.swing.EnhancedPanel.
     */
    public void testPanelListeners2() {
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

    /**
     * Test of fireAncestorWindowShown method, of class de.schlichtherle.truezip.swing.EnhancedPanel.
     */
    public void testFireAncestorWindowShown() {
        final CountingPanelListener l = new CountingPanelListener();
        instance.addPanelListener(l);
        instance.addPanelListener(l); // add again to receive same event twice!
        assertEquals(0, l.shown);

        PanelEvent event = null;
        instance.fireAncestorWindowShown(event);
        assertEquals(2, l.shown);
    }

    /**
     * Test of fireAncestorWindowHidden method, of class de.schlichtherle.truezip.swing.EnhancedPanel.
     */
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

            public void run() {
                frame.getContentPane().add(instance);
                frame.pack();
                frame.setLocationRelativeTo(null);
            }
        }

        class MakeFrameVisible extends FrameHolder implements Runnable {
            MakeFrameVisible(JFrame frame) {
                super(frame);
            }

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

            @SuppressWarnings("deprecation")
            public void run() {
                // Use multiple ways to make the frame visible.
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
        invokeAndWait(new SetupFrame(frame1));
        testEvents(new MakeFrameVisible(frame1), new MakeFrameInvisible(frame1), l);

        // Test in second frame.
        final JFrame frame2 = new JFrame();
        invokeAndWait(new SetupFrame(frame2));
        testEvents(new MakeFrameVisible(frame2), new MakeFrameInvisible(frame2), l);
    }

    private void testEvents(
            final Runnable makeVisible,
            final Runnable makeInvisible,
            final CountingPanelListener l)
    throws  Exception {
        l.shown = l.hidden = 0; // reset counters

        for (int i = 1; i <= 3; i++) {
            invokeAndWait(makeVisible);
            assertEquals(i, l.shown);
            assertEquals(i - 1, l.hidden);

            invokeAndWait(makeInvisible);
            assertEquals(i, l.shown);
            assertEquals(i, l.hidden);
        }
    }

    /**
     * Invokes the given {@code runnable} from the AWT Event Dispatch
     * Thread and waits for the AWT Event Queue to drain.
     *
     * @throws Error If this method is called from the AWT Event Dispatch Thread.
     */
    private static void invokeAndWait(final Runnable runnable)
    throws InterruptedException, InvocationTargetException {
        //EventQueue.invokeLater(runnable);
        EventQueue.invokeAndWait(runnable);
        waitEmptyQueue();
    }

    @SuppressWarnings("CallToThreadDumpStack")
    private static void waitEmptyQueue() throws AssertionError {
        //new QueueTool().waitEmpty();
        final EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        while (queue.peekEvent() != null) {
            try {
                EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                    }
                });
            } catch (InvocationTargetException cannotHappen) {
                throw new AssertionError(cannotHappen);
            } catch (InterruptedException continueAnyway) {
                continueAnyway.printStackTrace();
            }
        }
    }

    /**
     * Shows and hides the EnhancedPanel instance several times in a
     * JOptionPane and counts these events.
     */
    public void testEvents4JOptionPane() throws Exception {
        final CountingPanelListener l = new CountingPanelListener();
        instance.addPanelListener(l);

        final Runnable makeVisible = new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(null, instance);
            }
        };

        for (int i = 1; i <= 3; i++) {
            EventQueue.invokeLater(makeVisible);
            JDialogOperator dialogOp = new JDialogOperator(); // wait for JOptionPane
            waitEmptyQueue(); // wait until PanelEvent delivered
            assertEquals(i, l.shown);
            assertEquals(i - 1, l.hidden);

            JButtonOperator buttonOp = new JButtonOperator(dialogOp);
            buttonOp.push();
            waitEmptyQueue(); // wait until PanelEvent delivered
            assertEquals(i, l.shown);
            assertEquals(i, l.hidden);
        }
    }
}
