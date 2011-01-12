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
package de.schlichtherle.truezip.key.passwd.swing;

import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyManagers;
import java.awt.EventQueue;
import java.net.URI;
import java.util.Random;
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
public class PromptingKeyManagerTest extends TestCase {
    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }

    public PromptingKeyManagerTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() {
        KeyManagers.setManager(new PromptingKeyManager());
    }

    @Override
    protected void tearDown() {
        KeyManager.resetAndRemoveKeyProviders();
        KeyManagers.setManager(null);
    }

    /**
     * Concurrently start some threads which open some dialogs to prompt the
     * user for the keys during the typical life cycle of a protected resource
     * and its associated key.
     * Ensure that only one window is showing at any time.
     */
    public void testMultithreadedKeyMgmtLifeCycle() {
        // TODO: This is a workaround: Without it, the very first key dialog
        // would get closed on any input by its remote control.
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, "Hello World!");
            }
        });
        new JButtonOperator(new JDialogOperator()).push();

        testMultithreadedKeyMgmtLifeCycle(4);
    }

    private void testMultithreadedKeyMgmtLifeCycle(final int nThreads) {
        final URI RESOURCE_PREFIX = URI.create("resource/");
        final Random rnd = new Random();

        // Init required threads for each resource.
        final RemoteControlThread[] rcThreads
                = new RemoteControlThread[nThreads];
        final KeyMgmtLifeCycleThread[] kmlcThreads
                = new KeyMgmtLifeCycleThread[nThreads];
        for (int i = 0; i < nThreads; i++) {
            final URI resource = RESOURCE_PREFIX.resolve("" + i);

            // Init, but don't yet start Remote Control Thread.
            final RemoteControl rc = (i % 2 == 0)
                    ? new AesRemoteControl(resource)
                    : new    RemoteControl(resource);
            final RemoteControlThread rcThread
                    = new RemoteControlThread(rc);
            rcThreads[i] = rcThread;

            // Init, but don't yet start Key Management Life Cycle Thread.
            final KeyMgmtLifeCycle kmlc = (i % 2 == 0)
                    ? new AesKeyMgmtLifeCycle(resource)
                    : new    KeyMgmtLifeCycle(resource);
            final KeyMgmtLifeCycleThread rlcThread
                    = new KeyMgmtLifeCycleThread(kmlc);
            kmlcThreads[i] = rlcThread;
        }

        // Start Key Management Life Cycle Threads in arbitrary order.
        for (int i = 0; i < nThreads; i++) {
            final int j = i + rnd.nextInt(nThreads - i);
            final KeyMgmtLifeCycleThread thread = kmlcThreads[j];
            kmlcThreads[j] = kmlcThreads[i];
            kmlcThreads[i] = thread;

            thread.start();
            /*try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }*/
        }

        /*try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        }*/

        // Start Remote Control Threads in order.
        for (int i = 0; i < nThreads; i++) {
            final RemoteControlThread thread = rcThreads[i];

            thread.start();
            /*try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }*/
        }

        // Wait for each thread to finish and check that key prompting hasn't
        // been cancelled by the user and that no exceptions happened.
        boolean kmlcThreadStillAlive = false;
        boolean threadDiedWithException = false;
        for (int i = 0; i < nThreads; ) {
            // Wait for Remote Control Thread to be finished..
            final RemoteControlThread rcThread = rcThreads[i];
            try {
                rcThread.join();
            } catch (InterruptedException ignored) {
                continue; // repeat
            }
            if (rcThread.getThrowable() != null)
                threadDiedWithException = true;

            // Wait for Key Management Life Cycle Thread to be finished.
            final KeyMgmtLifeCycleThread kmlcThread = kmlcThreads[i];
            try {
                kmlcThread.join(20000);
            } catch (InterruptedException ignored) {
                continue; // repeat
            }
            if (kmlcThread.isAlive())
                kmlcThreadStillAlive = true;
            else if (kmlcThread.getThrowable() != null)
                threadDiedWithException = true;

            i++;
        }

        if (threadDiedWithException)
            fail("Some threads terminated with an exception!");
        if (kmlcThreadStillAlive)
            fail("The life cycles of some resources and their associated keys haven't finished!");
    }
}
