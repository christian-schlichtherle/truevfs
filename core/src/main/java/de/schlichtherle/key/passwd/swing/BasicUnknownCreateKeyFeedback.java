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

/**
 * Provides feedback by beeping using the default toolkit.
 * when prompting for a key to create or overwrite a protected resource
 * for the first time.
 * <p>
 * If you would like to play a nice sound for feedback, you need to override
 * the {@link #startSound} method.
 * <p>
 * <b>Warning:</b> Playing a {@code java.applet.AudioClip} on J2SE
 * 1.4.2_12 causes a client application not to terminate until System.exit(0)
 * is called explicitly - hence this feature is currently not implemented in
 * this class!
 * This issue is fixed in JSE 1.5.0_07 (and probably earlier versions).
 *
 * @author Christian Schlichtherle
 * @since TrueZIP 6.4
 * @version $Id$
 */
public class BasicUnknownCreateKeyFeedback extends BasicUnknownKeyFeedback {
}
