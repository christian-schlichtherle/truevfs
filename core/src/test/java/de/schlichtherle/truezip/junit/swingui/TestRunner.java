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

package de.schlichtherle.truezip.junit.swingui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Christian Schlichtherle
 */
public class TestRunner extends junit.swingui.TestRunner {

    public TestRunner() {
        final Properties defaults = new Properties();
        defaults.put("loading", "true");
        defaults.put("filterstack", "true");

        final Properties preferences = new Properties(defaults);
        try {
            try {
                preferences.load(
                        TestRunner.class.getResourceAsStream("/META-INF/junit.properties"));
            } catch (IOException ignored) {
                preferences.load(
                        new FileInputStream(
                            new File(
                                System.getProperty("user.home"),
                                "junit.properties")));
            }
        } catch (IOException failure) {
            // Use defaults only.
        }

        setPreferences(preferences);
    }

    public static void main(String[] args) {
        new TestRunner().start(args);
    }
}
