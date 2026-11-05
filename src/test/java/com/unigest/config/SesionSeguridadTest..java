package com.unigest.config;

import com.unigest.usuario.Rol;
import com.unigest.usuario.Usuario;
import com.unigest.usuario.UsuarioRepository;
import com.unigest.usuario.SesionController;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SesionController.class)
@Import(SecurityConfig.class)
class SesionSeguridadTest {

    private static final String CORREO = "admin@unigest.local";
    private static final String CLAVE = "ClaveFicticiaSoloPruebas123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private void prepararAdministrador() {
        Rol rol = mock(Rol.class);
        when(rol.getCodigo()).thenReturn("ADMIN");

        Usuario admin = new Usuario(
                "Administrador",
                "UniGest",
                CORREO,
                passwordEncoder.encode(CLAVE),
                rol
        );

        when(usuarioRepository.findByCorreoIgnoreCase(CORREO))
                .thenReturn(Optional.of(admin));
    }

    // 1. Login correcto y sesion persistente.
    @Test
    void loginCorrectoConservaSesion() throws Exception {
        prepararAdministrador();

        var resultado = mockMvc.perform(
                formLogin("/api/v1/auth/login")
                        .user(CORREO)
                        .password(CLAVE)
        )
        .andExpect(status().isOk())
        .andReturn();

        MockHttpSession sesion =
                (MockHttpSession) resultado.getRequest()
                        .getSession(false);

        assertNotNull(sesion);

        mockMvc.perform(
                get("/api/v1/auth/me").session(sesion)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correo").value(CORREO))
        .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
    }

    // 2. Contrasena incorrecta rechazada.
    @Test
    void loginConClaveIncorrectaEsRechazado() throws Exception {
        prepararAdministrador();

        mockMvc.perform(
                formLogin("/api/v1/auth/login")
                        .user(CORREO)
                        .password("ClaveIncorrecta123!")
        )
        .andExpect(status().isUnauthorized());
    }

    // 3. Login sin CSRF rechazado.
    @Test
    void loginSinCsrfEsRechazado() throws Exception {
        mockMvc.perform(
                post("/api/v1/auth/login")
                        .param("username", CORREO)
                        .param("password", CLAVE)
        )
        .andExpect(status().isForbidden());
    }

    // 4. Logout invalida la sesion HTTP.
    @Test
    void logoutInvalidaSesion() throws Exception {
        prepararAdministrador();

        var resultado = mockMvc.perform(
                formLogin("/api/v1/auth/login")
                        .user(CORREO)
                        .password(CLAVE)
        )
        .andExpect(status().isOk())
        .andReturn();

        MockHttpSession sesion =
                (MockHttpSession) resultado.getRequest()
                        .getSession(false);

        assertNotNull(sesion);

        mockMvc.perform(
                post("/api/v1/auth/logout")
                        .session(sesion)
                        .with(csrf())
        )
        .andExpect(status().isNoContent());

        assertTrue(sesion.isInvalid());
    }

    // Token CSRF disponible sin iniciar sesion.
    @Test
    void tokenCsrfEstaDisponible() throws Exception {
        mockMvc.perform(
                get("/api/v1/auth/csrf")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.headerName")
                .value("X-CSRF-TOKEN"));
    }
}
