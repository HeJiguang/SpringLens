package io.springlens.starter.probe;

import io.springlens.model.diagnostic.ProbeCapturePhases;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation-based probe declaration.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LensWatch {

    String id();

    String description() default "";

    String phase() default ProbeCapturePhases.AFTER_RETURN;

    String target() default "#result";
}
