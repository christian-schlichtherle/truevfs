/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides annotation processors for
 * {@linkplain net.java.truecommons3.annotations.processing.ServiceSpecificationProcessor service specifications}
 * and
 * {@linkplain net.java.truecommons3.annotations.processing.ServiceImplementationProcessor service implementations}.
 * These processors are configured in
 * {@code META-INF/services/javax.annotation.processing.Processor}, so you
 * normally don't need to set the annotation processor path when invoking
 * {@code javac}.
 * <p>
 * Unless noted otherwise, this is a {@code null}-free API:
 * No parameter and no return value of public methods in public classes is
 * allowed to be {@code null}.
 * Likewise, no public field in public classes is allowed to be {@code null},
 * although such fields should not exist in the first place.
 *
 * @since  TrueCommons 2.1
 * @author Christian Schlichtherle
 */
package net.java.truecommons3.annotations.processing;
