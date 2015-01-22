/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.samples.raes;

import java.io.IOException;
import static java.lang.System.*;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;

/**
 * @author Christian Schlichtherle
 */
public abstract class Application {

    String message(String key, Object... args) {
        return String.format(
                ResourceBundle.getBundle(getClass().getName()).getString(key),
                args);
    }

    int run(final String[] args) throws IOException {
        try {
            runChecked(args);
        } catch (IllegalArgumentException | NoSuchElementException ex) {
            String message = ex.getLocalizedMessage();
            if (null == message) message = message("usage", getClass().getSimpleName());
            err.println(message);
            return 2;
        } catch (Exception ex) {
            ex.printStackTrace();
            return 1;
        }
        return 0;
    }

    abstract void runChecked(String[] args) throws IOException;
}
