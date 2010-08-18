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

package de.schlichtherle.key.passwd.swing;

import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Provides feedback by beeping using the default toolkit, disabling the
 * default button in the root pane for three seconds and concurrently
 * hurling the containing window for 1.5 seconds.
 * <p>
 * This class is inspired by chapter #38 "Earthquake Dialog" of the book
 * "Swing Hacks" by Joshua Marinacci & Chris Adamson, published by O'Reilly
 * in 2005.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.4
 */
public class HurlingWindowFeedback extends BasicInvalidKeyFeedback {

    private static final double PI = Math.PI;
    private static final double TWO_PI  = 2.0 * PI;
    
    public static final int DISTANCE = 25;
    public static final int CYCLE    = 150;
    public static final int DURATION = 1500;
    public static final int FPS      = 75;

    private final double distance;
    private final double cycle;
    private final int    duration;
    private final int    fps;
    
    public HurlingWindowFeedback() {
        this(DISTANCE, CYCLE, DURATION, FPS);
    }

    /**
     * Constructs a new <code>HurlingWindowFeedback</code>.
     * 
     * @param distance The maximum distance for quaking the window.
     * @param cycle Milliseconds required for one cycle.
     * @param duration Millisecons of duration of quake.
     * @param fps Frames per second for animation.
     */
    protected HurlingWindowFeedback(
            final int distance,
            final int cycle,
            final int duration,
            final int fps) {
        this.distance = distance;
        this.cycle    = cycle;
        this.duration = duration;
        this.fps      = fps;

        if (duration > getDelay())
            setDelay(duration);
    }

    @Override
    protected void startAnimation() {
        super.startAnimation(); // temporarily disable default button
        
        final Window window = SwingUtilities.getWindowAncestor(getPanel());
        if (window == null)
            return;

        final Point origin = window.getLocation();
        final long startTime = System.currentTimeMillis();
        final Timer timer = new Timer(1000 / fps, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Calculate elapsed time.
                final long elapsed = System.currentTimeMillis() - startTime;

                if (elapsed < duration && window.isShowing()) {
                    final double amplitude
                            = Math.sin(PI * elapsed / duration) * distance;
                    final double angle = TWO_PI * elapsed / cycle;
                    final int quakingX
                            = (int) (Math.cos(angle) * amplitude + origin.x);
                    final int quakingY
                            = (int) (Math.sin(angle) * amplitude + origin.y);

                    window.setLocation(quakingX, quakingY);
                    window.repaint();
                } else {
                    ((Timer) e.getSource()).stop();
                    window.setLocation(origin);
                    window.repaint();
                }
            }
        });
        timer.start();
    }
}
