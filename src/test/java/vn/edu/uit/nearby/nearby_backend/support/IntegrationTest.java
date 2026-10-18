package vn.edu.uit.nearby.nearby_backend.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import vn.edu.uit.nearby.nearby_backend.TestcontainersConfiguration;

/**
 * Every integration test uses this exact configuration so they share one cached Spring
 * context and one set of containers.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, ProbeController.class })
public @interface IntegrationTest {

}
