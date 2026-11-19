package com.unigest.config;

import com.unigest.estudiante.EstudianteController;
import com.unigest.estudiante.EstudianteService;
import com.unigest.usuario.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EstudianteController.class)
@Import(SecurityConfig.class)
class EstudianteAutorizacionTest {

    private static final String RUTA = "/api/v1/estudiantes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EstudianteService estudianteService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private MockHttpServletRequestBuilder conRol(
            MockHttpServletRequestBuilder peticion, String rol) {
        return peticion.with(user(rol.toLowerCase() + "@unigest.local").roles(rol));
    }

    private MockHttpServletRequestBuilder json(
            MockHttpServletRequestBuilder peticion) {
        return peticion.contentType(MediaType.APPLICATION_JSON).content("{}");
    }

    private void rechazado(MockHttpServletRequestBuilder peticion, int codigo)
            throws Exception {
        mockMvc.perform(peticion).andExpect(status().is(codigo));
        verifyNoInteractions(estudianteService);
    }

    @Test
    void administradorPuedeConsultarEstudiantes() throws Exception {
        when(estudianteService.listar("", null, 0, 10)).thenReturn(Page.empty());

        mockMvc.perform(conRol(get(RUTA), "ADMIN"))
                .andExpect(status().isOk());

        verify(estudianteService).listar("", null, 0, 10);
    }

    @Test
    void estudianteNoPuedeConsultarAdministracion() throws Exception {
        rechazado(conRol(get(RUTA), "ESTUDIANTE"), 403);
    }

    @Test
    void docenteNoPuedeConsultarAdministracion() throws Exception {
        rechazado(conRol(get(RUTA), "DOCENTE"), 403);
    }

    @Test
    void visitanteSinAutenticacionEsRechazado() throws Exception {
        rechazado(get(RUTA), 401);
    }

    @Test
    void administradorSinCsrfNoPuedeRegistrar() throws Exception {
        rechazado(json(conRol(post(RUTA), "ADMIN")), 403);
    }

    @Test
    void tokenCsrfInvalidoEsRechazado() throws Exception {
        rechazado(json(conRol(post(RUTA), "ADMIN")
                .with(csrf().useInvalidToken())), 403);
    }

    @Test
    void administradorConCsrfSuperaFiltroSeguridad() throws Exception {
        rechazado(conRol(post(RUTA), "ADMIN")
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON), 400);
    }

    @Test
    void docenteConCsrfNoPuedeRegistrar() throws Exception {
        rechazado(json(conRol(post(RUTA), "DOCENTE")
                .with(csrf())), 403);
    }

    @Test
    void estudianteConCsrfNoPuedeRegistrar() throws Exception {
        rechazado(json(conRol(post(RUTA), "ESTUDIANTE")
                .with(csrf())), 403);
    }

    @Test
    void visitanteConCsrfSigueSinAcceso() throws Exception {
        rechazado(json(post(RUTA).with(csrf())), 401);
    }

    @Test
    void administradorSinCsrfNoPuedeDesactivar() throws Exception {
        rechazado(conRol(delete(RUTA + "/999"), "ADMIN"), 403);
    }

    @Test
    void administradorConCsrfPuedeDesactivar() throws Exception {
        mockMvc.perform(conRol(delete(RUTA + "/999"), "ADMIN")
                .with(csrf().asHeader()))
                .andExpect(status().isNoContent());

        verify(estudianteService).desactivar(999L);
    }

    @Test
    void administradorSinCsrfNoPuedeEditar() throws Exception {
        rechazado(json(conRol(put(RUTA + "/999"), "ADMIN")), 403);
    }
}
