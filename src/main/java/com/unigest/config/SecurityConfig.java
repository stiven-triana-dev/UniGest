package com.unigest.config;

import com.unigest.usuario.UsuarioAutenticacionService;
import com.unigest.usuario.UsuarioRepository;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService usuarioAutenticacionService(
            UsuarioRepository repo) {
        return new UsuarioAutenticacionService(repo);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(auth -> auth

                .dispatcherTypeMatchers(
                        DispatcherType.ERROR
                ).permitAll()

                .requestMatchers(HttpMethod.GET,
                        "/",
                        "/api/v1/estado",
                        "/api/v1/programas",
                        "/api/v1/asignaturas",
                        "/api/v1/auth/csrf",
                        "/api/v1/demo",
                        "/panel/**",
                        "/portal/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**"
                ).permitAll()

                .requestMatchers(HttpMethod.POST,
                        "/api/v1/auth/login"
                ).permitAll()

                .requestMatchers(HttpMethod.GET,
                        "/api/v1/auth/me"
                ).authenticated()

                .requestMatchers("/api/v1/auth/cambiar-clave", "/api/v1/auth/estado-clave")
                .authenticated()

                .requestMatchers("/api/v1/admin/accesos/**").hasRole("ADMIN")

                .requestMatchers("/api/v1/mi-espacio/**").hasAnyRole("DOCENTE", "ESTUDIANTE")

                .requestMatchers(
                        "/api/v1/estudiantes",
                        "/api/v1/estudiantes/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                        "/api/v1/docentes",
                        "/api/v1/docentes/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                        "/api/v1/grupos",
                        "/api/v1/grupos/**"
                ).hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET,
                        "/api/v1/periodos"
                ).hasRole("ADMIN")

                .requestMatchers(
                        "/api/v1/matriculas",
                        "/api/v1/matriculas/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                        "/api/v1/evaluaciones",
                        "/api/v1/evaluaciones/**",
                        "/api/v1/calificaciones",
                        "/api/v1/calificaciones/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                        "/api/v1/reportes",
                        "/api/v1/reportes/**"
                ).hasRole("ADMIN")

                .anyRequest().denyAll()
        );

        http.sessionManagement(session -> session
                .sessionCreationPolicy(
                        SessionCreationPolicy.IF_REQUIRED
                )
        );

        http.formLogin(form -> form
                .loginPage("/panel/index.html")
                .loginProcessingUrl("/api/v1/auth/login")

                .successHandler((req, res, auth) -> {
                    res.setStatus(HttpStatus.OK.value());
                    res.setContentType(
                            "application/json;charset=UTF-8"
                    );
                    res.getWriter().write(
                            "{\"autenticado\":true}"
                    );
                })

                .failureHandler((req, res, ex) ->
                        res.sendError(
                                HttpStatus.UNAUTHORIZED.value(),
                                "Credenciales incorrectas"
                        )
                )
                .permitAll()
        );

        http.httpBasic(basic -> basic.disable());

        http.exceptionHandling(ex ->
                ex.authenticationEntryPoint(
                        new HttpStatusEntryPoint(
                                HttpStatus.UNAUTHORIZED
                        )
                )
        );

        http.logout(logout -> logout
                .logoutUrl("/api/v1/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) ->
                        res.setStatus(
                                HttpStatus.NO_CONTENT.value()
                        )
                )
        );

        return http.build();
    }
}
