package com.unigest.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** La documentación de la API es pública, pero no abre las rutas privadas. */
@SpringBootTest
class DocumentacionApiTest {

    @Autowired private WebApplicationContext contexto;
    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    private MockHttpServletResponse pedir(String ruta) throws Exception {
        return mockMvc.perform(get(ruta)).andReturn().getResponse();
    }

    @Test
    void laEspecificacionOpenApiEsPublicaYDescribeLaApi() throws Exception {
        MockHttpServletResponse respuesta = pedir("/v3/api-docs");
        assertEquals(200, respuesta.getStatus());
        String json = respuesta.getContentAsString();
        assertTrue(json.contains("UniGest API"));
        assertTrue(json.contains("/api/v1/estudiantes"));
        assertTrue(json.contains("/api/v1/mi-espacio/estudiante/boletin"));
        assertTrue(json.contains("3. Estudiantes"), "los grupos de rutas deben tener nombre en español");
        assertFalse(json.contains("-controller"), "no deben quedar nombres técnicos de controlador");
    }

    @Test
    void swaggerUiEsPublico() throws Exception {
        int estado = pedir("/swagger-ui/index.html").getStatus();
        assertEquals(200, estado);
    }

    @Test
    void verLaDocumentacionNoDaAccesoALosDatos() throws Exception {
        assertEquals(401, pedir("/api/v1/estudiantes").getStatus());
    }
}
