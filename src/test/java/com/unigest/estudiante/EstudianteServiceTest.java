package com.unigest.estudiante;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.unigest.programa.Programa;
import com.unigest.programa.ProgramaRepository;
import com.unigest.usuario.Rol;
import com.unigest.usuario.RolRepository;
import com.unigest.usuario.Usuario;
import com.unigest.usuario.UsuarioRepository;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceTest {
    @Mock EstudianteRepository estudiantes;
    @Mock UsuarioRepository usuarios;
    @Mock RolRepository roles;
    @Mock ProgramaRepository programas;
    @Mock PasswordEncoder encoder;
    @InjectMocks EstudianteService service;

    private Programa programa;
    private Rol rol;

    @BeforeEach
    void preparar() throws Exception {
        programa = new ProgramaParaPrueba().crear();
        rol = new RolParaPrueba().crear();
    }

    private EstudianteRequest solicitud() {
        return new EstudianteRequest("Ana", "Pérez", "Ana@Ejemplo.COM", "123456789",
                "est-001", 1L, LocalDate.of(2026, 8, 1));
    }

    @Test
    void crearNormalizaDatosYNoRetornaClave() {
        when(programas.findById(1L)).thenReturn(Optional.of(programa));
        when(roles.findByCodigo("ESTUDIANTE")).thenReturn(Optional.of(rol));
        when(encoder.encode(anyString())).thenReturn("$2a$10$abc123abc123abc123abc123abc123abc123abc123abc123abc123");
        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(estudiantes.save(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));

        EstudianteResponse response = service.crear(solicitud());
        assertEquals("ana@ejemplo.com", response.correo());
        assertEquals("EST-001", response.codigo());
        assertEquals("123456789", response.documento());
        assertEquals("Ingeniería de Sistemas", response.programa());
        assertEquals(LocalDate.of(2026, 8, 1), response.fechaIngreso());
        verify(encoder).encode(anyString());
    }

    @Test
    void evitaCorreosDuplicados() {
        when(usuarios.existsByCorreoIgnoreCase("ana@ejemplo.com")).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class, () -> service.crear(solicitud()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(usuarios, never()).save(any());
    }

    @Test
    void evitaDocumentoDuplicado() {
        when(estudiantes.existsByDocumento("123456789")).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class, () -> service.crear(solicitud()));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void noAdmiteProgramaInexistente() {
        var error = assertThrows(ResponseStatusException.class, () -> service.crear(solicitud()));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
    }

    @Test
    void desactivacionConservaUsuarioEnBase() {
        Usuario usuario = new Usuario("Ana", "Pérez", "ana@ejemplo.com", "hash", rol);
        Estudiante estudiante = new Estudiante(usuario, "EST-001", "123456789", programa, LocalDate.now());
        when(estudiantes.findById(10L)).thenReturn(Optional.of(estudiante));
        service.desactivar(10L);
        assertFalse(usuario.isActivo());
        verify(estudiantes, never()).delete(any());
        verify(usuarios, never()).delete(any());
    }

    @Test
    void estudianteDesactivadoNoPuedeConsultarse() {
        Usuario usuario = new Usuario("Ana", "Pérez", "ana@ejemplo.com", "hash", rol);
        usuario.desactivar();
        when(estudiantes.findById(10L)).thenReturn(Optional.of(new Estudiante(
                usuario, "EST-001", "123456789", programa, LocalDate.now())));
        var error = assertThrows(ResponseStatusException.class, () -> service.consultar(10L));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void buscarNormalizaTextoYAplicaPaginacion() {
        when(estudiantes.buscarActivos(eq("ana"), eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));
        var pagina = service.listar(" ANA ", 1L, 0, 10);
        assertEquals(0, pagina.getTotalElements());
        verify(estudiantes).buscarActivos(eq("ana"), eq(1L), any(PageRequest.class));
    }

    private static void asignar(Object target, String atributo, Object valor) throws Exception {
        Field f = target.getClass().getDeclaredField(atributo);
        f.setAccessible(true);
        f.set(target, valor);
    }
    static class ProgramaParaPrueba {
        Programa crear() throws Exception {
            var p = constructorPrograma();
            asignar(p, "id", 1L);
            asignar(p, "nombre", "Ingeniería de Sistemas");
            asignar(p, "activo", true);
            return p;
        }
        private Programa constructorPrograma() throws Exception {
            var c = Programa.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        }
    }
    static class RolParaPrueba {
        Rol crear() throws Exception {
            var c = Rol.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        }
    }
}
