package it.lorenzoscarpa.dnd.common.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Brings the real security setup into @WebMvcTest slices, which do not load @Configuration classes
 * on their own. Lives in this package because SecurityProblemHandler is package-private.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import({ SecurityConfig.class, SecurityProblemHandler.class, JwtConfig.class })
public class SecurityTestConfig {
}
