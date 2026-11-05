package com.unigest.config;

import com.unigest.docente.DocenteController;
import com.unigest.docente.DocenteService;
import com.unigest.usuario.UsuarioRepository;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocenteController.class)
@Import(SecurityConfig.class)
class DocenteAutorizacionTest {

    private static final String RUTA = "/api/v1/docentes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocenteService docenteService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void visitanteNoPuedeConsultarDocentes() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(docenteService);
    }

    @Test
    void docenteNoPuedeAdministrarDocentes() throws Exception {
        mockMvc.perform(get(RUTA)
                .with(user("docente@unigest.local").roles("DOCENTE")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(docenteService);
    }

    @Test
    void administradorPuedeConsultarDocentes() throws Exception {
        when(docenteService.listar()).thenReturn(List.of());

        mockMvc.perform(get(RUTA)
                .with(user("admin@unigest.local").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(docenteService).listar();
    }

    @Test
    void administradorSinCsrfNoPuedeRegistrar() throws Exception {
        mockMvc.perform(post(RUTA)
                .with(user("admin@unigest.local").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(docenteService);
    }
}
