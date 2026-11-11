package com.unigest.estudiante;

import com.unigest.programa.Programa;
import com.unigest.programa.ProgramaRepository;
import com.unigest.usuario.*;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EstudianteService {
    private final EstudianteRepository estudiantes;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final ProgramaRepository programas;
    private final PasswordEncoder passwordEncoder;

// Demo pública: protege las cuentas de prueba. Opcional para las pruebas unitarias.
    private com.unigest.demo.DemoModo demo;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setDemo(com.unigest.demo.DemoModo demo) { this.demo = demo; }

    private void protegerDemo(String correo) { if (demo != null) demo.protegerCuenta(correo); }

    public EstudianteService(EstudianteRepository estudiantes, UsuarioRepository usuarios,
            RolRepository roles, ProgramaRepository programas, PasswordEncoder passwordEncoder) {
        this.estudiantes = estudiantes;
        this.usuarios = usuarios;
        this.roles = roles;
        this.programas = programas;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<EstudianteResponse> listar(String busqueda, Long programaId, int pagina, int tamano) {
        var texto = busqueda == null ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
        return estudiantes.buscarActivos(texto, programaId,
                PageRequest.of(pagina, tamano, Sort.by("id").ascending())).map(this::convertir);
    }

    @Transactional(readOnly = true)
    public EstudianteResponse consultar(Long id) {
        return convertir(buscarActivo(id));
    }

    @Transactional
    public EstudianteResponse crear(EstudianteRequest solicitud) {
        String correo = normalizarCorreo(solicitud.correo());
        String codigo = normalizarCodigo(solicitud.codigo());
        String documento = normalizarDocumento(solicitud.documento());
        validarUnicos(correo, codigo, documento, null);
        Programa programa = buscarPrograma(solicitud.programaId());
        Rol rol = roles.findByCodigo("ESTUDIANTE")
                .orElseThrow(() -> new IllegalStateException("Falta el rol ESTUDIANTE en la migración inicial"));

        // Credencial aleatoria inaccesible: el login estudiantil NO está implementado.
        // No se generan claves conocidas ni se incluyen en la respuesta.
        String claveInutilizable = passwordEncoder.encode(UUID.randomUUID().toString() + UUID.randomUUID());
        Usuario usuario = usuarios.save(new Usuario(
                limpiar(solicitud.nombres()), limpiar(solicitud.apellidos()), correo,
                claveInutilizable, rol));
        Estudiante estudiante = estudiantes.save(new Estudiante(usuario, codigo, documento, programa,
                solicitud.fechaIngreso() == null ? LocalDate.now() : solicitud.fechaIngreso()));
        return convertir(estudiante);
    }

    @Transactional
    public EstudianteResponse actualizar(Long id, EstudianteRequest solicitud) {
        Estudiante estudiante = buscarActivo(id);
        String correo = normalizarCorreo(solicitud.correo());
        String codigo = normalizarCodigo(solicitud.codigo());
        String documento = normalizarDocumento(solicitud.documento());
        validarUnicos(correo, codigo, documento, id);
        if (!correo.equalsIgnoreCase(estudiante.getUsuario().getCorreo())) protegerDemo(estudiante.getUsuario().getCorreo());
        Programa programa = buscarPrograma(solicitud.programaId());
        estudiante.getUsuario().actualizarDatos(limpiar(solicitud.nombres()), limpiar(solicitud.apellidos()), correo);
        estudiante.actualizarDatos(codigo, documento, programa);
        // La fecha de ingreso es histórica; no se cambia por edición básica.
        return convertir(estudiante);
    }

    @Transactional
    public void desactivar(Long id) {
        var usuario = buscarActivo(id).getUsuario();
        protegerDemo(usuario.getCorreo());
        usuario.desactivar();
    }

    private Estudiante buscarActivo(Long id) {
        return estudiantes.findById(id)
                .filter(e -> e.getUsuario().isActivo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));
    }

    private Programa buscarPrograma(Long id) {
        return programas.findById(id).filter(Programa::isActivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "El programa académico no existe o está inactivo"));
    }

    private void validarUnicos(String correo, String codigo, String documento, Long id) {
        boolean yaCorreo = id == null ? usuarios.existsByCorreoIgnoreCase(correo)
                : usuarios.existsByCorreoIgnoreCaseAndIdNot(correo, id);
        boolean yaCodigo = id == null ? estudiantes.existsByCodigo(codigo)
                : estudiantes.existsByCodigoAndIdNot(codigo, id);
        boolean yaDocumento = id == null ? estudiantes.existsByDocumento(documento)
                : estudiantes.existsByDocumentoAndIdNot(documento, id);
        if (yaCorreo || yaCodigo || yaDocumento) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Correo, código o documento ya registrado");
        }
    }

    private EstudianteResponse convertir(Estudiante e) {
        var u = e.getUsuario();
        var p = e.getPrograma();
        return new EstudianteResponse(e.getId(), e.getCodigo(), e.getDocumento(), u.getNombres(),
                u.getApellidos(), u.getCorreo(), p.getId(), p.getNombre(), e.getFechaIngreso());
    }
    private static String limpiar(String s) { return s.trim().replaceAll("\\s+", " "); }
    private static String normalizarCorreo(String s) { return limpiar(s).toLowerCase(Locale.ROOT); }
    private static String normalizarCodigo(String s) { return limpiar(s).toUpperCase(Locale.ROOT); }
    private static String normalizarDocumento(String s) { return limpiar(s); }
}
