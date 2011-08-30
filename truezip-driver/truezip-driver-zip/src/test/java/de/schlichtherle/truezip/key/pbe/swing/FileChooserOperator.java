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

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import org.netbeans.jemmy.operators.JFileChooserOperator;

/**
 * A file chooser operator which always uses the cross platform look and feel.
 *
 * @author Christian Schlichtherle
 * @author Michael Hall
 * @version $Id$
 */
final class FileChooserOperator extends JFileChooserOperator {

    FileChooserOperator() {
        LookAndFeel laf = UIManager.getLookAndFeel();
        if (laf.isNativeLookAndFeel()) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                super.updateUI();
                UIManager.setLookAndFeel(laf);
            } catch (Exception ex) {
                throw new AssertionError(ex);
            }
        }
    }
}
