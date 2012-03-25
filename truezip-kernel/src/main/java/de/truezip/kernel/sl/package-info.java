/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides singletons to locate various service providers on the class path.
 * Note that this module has absolutely no dependency on this package!
 * It's up to the modules in the access tier to inject the service locators
 * into this module if they want to.
 */
@edu.umd.cs.findbugs.annotations.DefaultAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class)
package de.truezip.kernel.sl;