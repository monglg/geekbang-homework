package org.geektimes.web.mvc.header.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PaddingParam {

    String value() default "";

    Class<?>[] type() default {};

}
