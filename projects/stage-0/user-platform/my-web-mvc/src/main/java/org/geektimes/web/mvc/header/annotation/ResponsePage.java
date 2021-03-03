package org.geektimes.web.mvc.header.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponsePage {

    String value() default "";
    
}
