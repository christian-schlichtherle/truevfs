/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.annotations.processing;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;

import static javax.lang.model.element.ElementKind.*;
import static javax.lang.model.element.Modifier.*;

import javax.lang.model.type.*;
import javax.lang.model.util.*;
import javax.tools.*;

import static javax.tools.Diagnostic.Kind.*;
import static javax.tools.StandardLocation.*;

import net.java.truecommons3.annotations.ServiceImplementation;
import net.java.truecommons3.annotations.ServiceSpecification;

/**
 * Processes the {@link ServiceImplementation} annotation.
 * If any of the processing options
 * {@code net.java.truecommons3.annotations.verbose} or
 * {@code net.java.truecommons3.annotations.processing.verbose}
 * is set to {@code true} (whereby case is ignored), then this processor emits
 * a note for every service implementation class it registers in a service
 * provider configuration file in {@code META-INF/services}.
 *
 * @author Christian Schlichtherle
 * @since TrueCommons 2.1
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes("*")
@SupportedOptions({
        "net.java.truecommons3.annotations.verbose",
        "net.java.truecommons3.annotations.processing.verbose"
})
public final class ServiceImplementationProcessor extends ServiceAnnnotationProcessor {

    private static final Comparator<TypeElement> TYPE_ELEMENT_COMPARATOR =
            new Comparator<TypeElement>() {
                @Override
                public int compare(TypeElement o1, TypeElement o2) {
                    return o1.getQualifiedName().toString().compareTo(o2.getQualifiedName().toString());
                }
            };

    private boolean verbose;

    @Override
    public void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        final Set<String> supported = getSupportedOptions();
        final Map<String, String> present = processingEnv.getOptions();
        verbose = false;
        for (String option : supported) {
            verbose |= Boolean.parseBoolean(present.get(option));
        }
    }

    @Override
    boolean isDebugEnabled() {
        return verbose;
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        final Registry registry = new Registry();
        for (final Element elem : roundEnv.getElementsAnnotatedWith(ServiceImplementation.class)) {
            if (elem instanceof TypeElement) {
                final TypeElement impl = (TypeElement) elem;
                if (valid(impl)) {
                    if (!processAnnotations(impl, registry)) {
                        if (!processTypeHierarchy(impl, registry)) {
                            error("Cannot find any specification.", impl);
                        }
                    }
                }
            } else {
                warning("Expected a type element here.", elem);
            }
        }
        registry.persist();
        return false; // critical!
    }

    private boolean valid(final TypeElement impl) {
        {
            final Set<Modifier> modifiers = impl.getModifiers();
            if (!modifiers.contains(PUBLIC)
                    || modifiers.contains(ABSTRACT)
                    || impl.getKind() != CLASS) {
                return error("Not a public and non-abstract class.", impl);
            }
            if (impl.getNestingKind().isNested()) {
                if (!modifiers.contains(STATIC)) {
                    return error("Impossible to instantiate without an instance of the enclosing class.", impl);
                }
            }
        }
        final Collection<ExecutableElement> constructors = new LinkedList<>();
        for (final Element elem : impl.getEnclosedElements()) {
            if (elem.getKind() == CONSTRUCTOR) {
                constructors.add((ExecutableElement) elem);
            }
        }
        return constructors.isEmpty() || valid(constructors) ||
                error("No public constructor with zero parameters available.", impl);
    }

    private boolean valid(final Collection<ExecutableElement> ctors) {
        for (final ExecutableElement ctor : ctors) {
            if (valid(ctor)) {
                return true;
            }
        }
        return false;
    }

    private boolean valid(final ExecutableElement ctor) {
        return ctor.getModifiers().contains(PUBLIC) && ctor.getParameters().isEmpty();
    }

    private boolean processAnnotations(
            final TypeElement impl,
            final Registry registry) {
        final DeclaredType implType = (DeclaredType) impl.asType();
        for (final AnnotationMirror mirror : processingEnv.getElementUtils().getAllAnnotationMirrors(impl)) {
            if (!ServiceImplementation.class.getName().equals(
                    ((TypeElement) mirror.getAnnotationType().asElement()).getQualifiedName().toString())) {
                continue;
            }
            final Map<? extends ExecutableElement, ? extends AnnotationValue>
                    values = mirror.getElementValues();
            for (final Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
                final ExecutableElement element = entry.getKey();
                if (!"value".equals(element.getSimpleName().toString())) {
                    continue;
                }

                class Visitor extends SimpleAnnotationValueVisitor6<Boolean, Void> {

                    Visitor() {
                        super(false);
                    }

                    @Override
                    public Boolean visitType(final TypeMirror type, final Void p) {
                        if (processingEnv.getTypeUtils().isAssignable(implType, type)) {
                            registry.add(impl, (TypeElement) ((DeclaredType) type).asElement());
                        } else {
                            error("Unassignable to " + type + ".", impl);
                        }
                        return Boolean.TRUE;
                    }

                    @Override
                    public Boolean visitArray(final List<? extends AnnotationValue> values, final Void p) {
                        boolean found = false;
                        for (AnnotationValue value : values) {
                            found |= value.accept(this, p);
                        }
                        return found;
                    }
                }

                return entry.getValue().accept(new Visitor(), null);
            }
        }
        return false;
    }

    private boolean processTypeHierarchy(final TypeElement impl, final Registry registry) {

        class Visitor extends SimpleTypeVisitor6<Boolean, Void> {

            Visitor() {
                super(false);
            }

            @Override
            public Boolean visitDeclared(final DeclaredType type, final Void p) {
                boolean found = false;
                final TypeElement elem = (TypeElement) type.asElement();
                if (null != elem.getAnnotation(ServiceSpecification.class)) {
                    found = true;
                    registry.add(impl, elem);
                }
                for (final TypeMirror m : elem.getInterfaces()) {
                    found |= m.accept(this, p);
                }
                return elem.getSuperclass().accept(this, p) || found;
            }
        }

        return impl.asType().accept(new Visitor(), null);
    }

    private final class Registry {

        final Elements elements = processingEnv.getElementUtils();
        final Map<TypeElement, Collection<TypeElement>> services = new HashMap<>();

        void add(final TypeElement impl, final TypeElement spec) {
            Collection<TypeElement> coll = services.get(spec);
            if (null == coll) {
                coll = new TreeSet<>(TYPE_ELEMENT_COMPARATOR);
            }
            coll.add(impl);
            services.put(spec, coll);
        }

        void persist() {
            final Filer filer = processingEnv.getFiler();
            final Messager messager = getMessager();
            for (final Entry<TypeElement, Collection<TypeElement>> entry : services.entrySet()) {
                final TypeElement spec = entry.getKey();
                final Collection<TypeElement> coll = entry.getValue();
                if (coll.isEmpty()) {
                    continue;
                }
                final String path = "META-INF/services/" + name(spec);
                try {
                    final FileObject fo = filer.createResource(CLASS_OUTPUT, "", path);
                    try (Writer w = fo.openWriter()) {
                        for (final TypeElement impl : coll) {
                            w.append(name(impl)).append("\n");
                            debug(String.format("Registered at: %s", path), impl);
                        }
                    }
                } catch (final IOException ex) {
                    messager.printMessage(ERROR, String.format("Failed to register %d service implementation class(es) at: %s: %s", coll.size(), path, ex.getMessage()));
                }
            }
        }

        CharSequence name(TypeElement elem) {
            return elements.getBinaryName(elem);
        }
    }
}
