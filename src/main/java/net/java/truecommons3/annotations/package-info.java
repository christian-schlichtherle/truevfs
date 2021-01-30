/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides annotations for
 * {@linkplain net.java.truecommons3.annotations.ServiceSpecification service specifications}
 * and
 * {@linkplain net.java.truecommons3.annotations.ServiceImplementation service implementations}.
 * Using these annotations saves you from the tedious and error-prone process
 * of manually editing service provider configuration files in
 * {@code META-INF/services}
 * and enables some design-time error checking for your service specifications
 * and implementations in your IDE.
 *
 * <h3>The {@code @ServiceImplementation} Annotation</h3>
 * <p>
 * Suppose you wanted to implement a service provider for location by the
 * {@link java.util.ServiceLoader} class.
 * Using the {@code @ServiceImplementation} annotation, your implementation
 * could then look similar to this:
 * <pre><code>
 * package com.company.project;
 *
 * import java.nio.charset.spi.CharsetProvider;
 * import net.java.truecommons3.services.annotations.ServiceImplementation;
 *
 * &#64;ServiceImplementation(CharsetProvider.class)
 * public class Ibm437CharsetProvider extends CharsetProvider {
 *     ...
 * }
 * </code></pre>
 * <p>
 * The
 * {@linkplain net.java.truecommons3.annotations.processing.ServiceImplementationProcessor processor}
 * associated with the {@code @ServiceImplementation} annotation will then
 * generate the service provider configuration file
 * {@code META-INF/services/java.nio.charset.spi.CharsetProvider}
 * and place the service provider class name
 * {@code com.company.project.Ibm437CharsetProvider} into it.
 * <p>
 * The annotation processor performs some static code analysis in order to
 * detect any obvious errors and emits appropriate error messages,
 * e.g. if the implementation class is non-public or abstract
 * or if there is no public constructor with zero parameters available.
 * <p>
 * If your IDE performs annotation processing, then any error messages should
 * get highlighted in the editor at design-time.
 * Furthermore, if your IDE supports refactoring, then changing the class name
 * of the implementation automatically updates the entry in the service
 * provider configuration file.
 *
 * <h3>The {@code @ServiceSpecification} Annotation</h3>
 * <p>
 * Suppose you wanted to design your own specification class or interface.
 * Using the {@code @ServiceSpecification} annotation, your specification
 * could then look similar to this:
 * <pre><code>
 * package com.company.project.spec;
 *
 * import net.java.truecommons3.services.annotations.ServiceSpecification;
 *
 * &#64;ServiceSpecification
 * public interface UltimateServiceSpecification {
 *     ...
 * }
 * </code></pre>
 * <p>
 * The
 * {@linkplain net.java.truecommons3.annotations.processing.ServiceSpecificationProcessor processor}
 * associated with the {@code @ServiceSpecification} annotation will then
 * perform some static code analysis to detect any obvious errors and emit
 * appropriate error messages, e.g. if the specification class or interface is
 * non-public or final or if there is no public or protected constructor with
 * zero parameters available.
 * <p>
 * An implementation of your specification could then look like this:
 * <pre><code>
 * package com.company.project.impl;
 *
 * import com.company.project.spec.UltimateServiceSpecification;
 * import net.java.truecommons3.services.annotations.ServiceSpecification;
 *
 * &#64;ServiceImplementation
 * public class UltimateServiceImplementation
 * implements UltimateServiceSpecification {
 *     ...
 * }
 * </code></pre>
 * <p>
 * Note that the {@code @ServiceImplementation} annotation does not specify any
 * implemented classes or interfaces.
 * The annotation processor associated with the {@code @ServiceImplementation}
 * annotation will then scan the type hierarchy of the annotated class for any
 * superclass or interface which is annotated with the
 * {@code @ServiceSpecification} annotation and generate the service provider
 * configuration files according to its findings.
 * If no specification class or interface is found then an appropriate error
 * message gets emitted.
 * <p>
 * Unless noted otherwise, this is a {@code null}-free API:
 * No parameter and no return value of public methods in public classes is
 * allowed to be {@code null}.
 * Likewise, no public field in public classes is allowed to be {@code null},
 * although such fields should not exist in the first place.
 *
 * @see    java.util.ServiceLoader
 * @see    <a href="http://docs.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Service%20Provider">JAR File Specification for Java SE 6, Section "Service Provider"</a>
 * @since  TrueCommons 2.1
 * @author Christian Schlichtherle
 */
package net.java.truecommons3.annotations;