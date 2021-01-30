/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.annotations.processing;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;

import static javax.tools.Diagnostic.Kind.*;

/**
 * Common super class for {@link ServiceSpecificationProcessor} and
 * {@link ServiceImplementationProcessor}.
 *
 * @author Christian Schlichtherle
 */
public abstract class ServiceAnnnotationProcessor extends AbstractProcessor {

    boolean isDebugEnabled() {
        return false;
    }

    final void debug(CharSequence msg, Element e) {
        if (isDebugEnabled()) getMessager().printMessage(NOTE, msg, e);
    }

    final void warning(CharSequence message, Element e) {
        getMessager().printMessage(WARNING, message, e);
    }

    final boolean error(final CharSequence message, final Element e) {
        getMessager().printMessage(ERROR, message, e);
        return false;
    }

    final Messager getMessager() {
        return processingEnv.getMessager();
    }
}
