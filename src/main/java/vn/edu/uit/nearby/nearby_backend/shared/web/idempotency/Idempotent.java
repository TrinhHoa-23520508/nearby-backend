package vn.edu.uit.nearby.nearby_backend.shared.web.idempotency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires an {@code Idempotency-Key} header. A key is accepted once per user and
 * endpoint within {@code app.idempotency.ttl}; a repeat is rejected with
 * {@code 409 DUPLICATE_REQUEST}.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

}
