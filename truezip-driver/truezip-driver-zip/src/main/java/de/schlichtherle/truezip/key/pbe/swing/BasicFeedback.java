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
package de.schlichtherle.truezip.key.pbe.swing;

import java.awt.Toolkit;
import javax.swing.JPanel;

/**
 * Provides run by beeping using the default toolkit.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class BasicFeedback implements Feedback {

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in this class simply beeps using the default toolkit.
     */
    @Override
    public void run(JPanel panel) {
        Toolkit.getDefaultToolkit().beep();
    }
}
