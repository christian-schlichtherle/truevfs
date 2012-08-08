/*
 * Copyright (c) 2012 Schlichtherle IT Services.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Schlichtherle IT Services - initial API and implementation and/or initial documentation
 */
package net.java.truevfs.comp.jmx;

/**
 * Implements the controller role of the MVC pattern for JMX beans.
 * 
 * @author Christian Schlichtherle
 */
public interface JmxController {

    /**
     * A hook which gets called after the instantiation of the implementation
     * class in order to enable this controller to setup its view.
     */
    void init();
}
