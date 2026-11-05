package com.unigest.portal;

import com.unigest.config.SecurityConfig;
import com.unigest.usuario.CuentaAccesoService;
import com.unigest.usuario.UsuarioRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MiEspacioController.class)
@Import(SecurityConfig.class)
class MiEspacioAutorizacionTest {
    @Autowired MockMvc mvc;
    @MockitoBean MiEspacioService servicio;
    @MockitoBean CuentaAccesoService cuentaAccesoService;
    @MockitoBean UsuarioRepository usuarioRepository;

    @Test void visitanteNoPuedeEntrar() throws Exception {
        mvc.perform(get("/api/v1/mi-espacio/docente/grupos"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test void estudianteNoPuedeConsultarDocencia() throws Exception {
        mvc.perform(get("/api/v1/mi-espacio/docente/grupos")
            .with(user("alumno@test.local").roles("ESTUDIANTE")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test void docenteNoPuedeConsultarEspacioEstudiantil() throws Exception {
        mvc.perform(get("/api/v1/mi-espacio/estudiante/periodos")
            .with(user("docente@test.local").roles("DOCENTE")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test void docenteConClaveProvisionalNoPuedeConsultarNotas() throws Exception {
        mvc.perform(get("/api/v1/mi-espacio/docente/grupos")
            .with(user("docente@test.local").roles("DOCENTE")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test void docenteConClaveCambiadaPuedeConsultarSoloSusGrupos() throws Exception {
        when(cuentaAccesoService.claveDefinitiva("docente@test.local")).thenReturn(true);
        when(servicio.misGrupos("docente@test.local")).thenReturn(List.of());
        mvc.perform(get("/api/v1/mi-espacio/docente/grupos")
            .with(user("docente@test.local").roles("DOCENTE")))
            .andExpect(status().isOk());
        verify(servicio).misGrupos("docente@test.local");
    }

    @Test void administradorNoUtilizaPortalPersonal() throws Exception {
        mvc.perform(get("/api/v1/mi-espacio/estudiante/periodos")
            .with(user("admin@test.local").roles("ADMIN")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }
}
