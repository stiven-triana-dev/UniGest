package com.unigest.docente;

import com.unigest.usuario.Rol;
import com.unigest.usuario.RolRepository;
import com.unigest.usuario.Usuario;
import com.unigest.usuario.UsuarioRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocenteService {

    private final DocenteRepository docentes;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder passwordEncoder;

// Demo pública: protege las cuentas de prueba. Opcional para las pruebas unitarias.
    private com.unigest.demo.DemoModo demo;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setDemo(com.unigest.demo.DemoModo demo) { this.demo = demo; }

    private void protegerDemo(String correo) { if (demo != null) demo.protegerCuenta(correo); }

    public DocenteService(DocenteRepository docentes,
            UsuarioRepository usuarios, RolRepository roles,
            PasswordEncoder passwordEncoder) {
        this.docentes = docentes;
        this.usuarios = usuarios;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<DocenteResponse> listar() {
        return docentes.findAll(Sort.by("id").ascending())
                .stream()
                .filter(d -> d.getUsuario().isActivo())
                .map(this::convertir)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocenteResponse consultar(Long id) {
        return convertir(buscarActivo(id));
    }

    @Transactional
    public DocenteResponse crear(DocenteRequest solicitud) {
        String correo = normalizarCorreo(solicitud.correo());
        String codigo = normalizarCodigo(solicitud.codigo());
        String documento = limpiar(solicitud.documento());

        validarUnicos(correo, codigo, documento, null);

        Rol rol = roles.findByCodigo("DOCENTE")
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el rol DOCENTE"));

        // Cuenta preparada sin contraseña de acceso conocida.
        String clave = passwordEncoder.encode(
                UUID.randomUUID().toString() + UUID.randomUUID());

        Usuario usuario = usuarios.save(new Usuario(
                limpiar(solicitud.nombres()),
                limpiar(solicitud.apellidos()),
                correo, clave, rol));

        LocalDate fecha = solicitud.fechaVinculacion() == null
                ? LocalDate.now()
                : solicitud.fechaVinculacion();

        Docente docente = docentes.save(
                new Docente(usuario, codigo, documento, fecha));

        return convertir(docente);
    }

    @Transactional
    public DocenteResponse actualizar(Long id, DocenteRequest solicitud) {
        Docente docente = buscarActivo(id);

        String correo = normalizarCorreo(solicitud.correo());
        String codigo = normalizarCodigo(solicitud.codigo());
        String documento = limpiar(solicitud.documento());

        validarUnicos(correo, codigo, documento, docente);
        if (!correo.equalsIgnoreCase(docente.getUsuario().getCorreo())) protegerDemo(docente.getUsuario().getCorreo());

        docente.getUsuario().actualizarDatos(
                limpiar(solicitud.nombres()),
                limpiar(solicitud.apellidos()),
                correo);

        docente.actualizarDatos(codigo, documento);

        // La fecha de vinculación se conserva.
        return convertir(docente);
    }

    @Transactional
    public void desactivar(Long id) {
        var usuario = buscarActivo(id).getUsuario();
        protegerDemo(usuario.getCorreo());
        usuario.desactivar();
    }

    private Docente buscarActivo(Long id) {
        return docentes.findById(id)
                .filter(d -> d.getUsuario().isActivo())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Docente no encontrado"));
    }

    private void validarUnicos(String correo, String codigo,
            String documento, Docente actual) {

        Long id = actual == null ? null : actual.getId();

        boolean correoExiste = id == null
                ? usuarios.existsByCorreoIgnoreCase(correo)
                : usuarios.existsByCorreoIgnoreCaseAndIdNot(correo, id);

        boolean codigoExiste =
                (actual == null || !actual.getCodigo().equals(codigo))
                && docentes.existsByCodigo(codigo);

        boolean documentoExiste =
                (actual == null || !actual.getDocumento().equals(documento))
                && docentes.existsByDocumento(documento);

        if (correoExiste || codigoExiste || documentoExiste) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Correo, código o documento ya registrado");
        }
    }

    private DocenteResponse convertir(Docente docente) {
        Usuario usuario = docente.getUsuario();

        return new DocenteResponse(
                docente.getId(),
                docente.getCodigo(),
                docente.getDocumento(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                docente.getFechaVinculacion());
    }

    private static String limpiar(String texto) {
        return texto.trim().replaceAll("\\s+", " ");
    }

    private static String normalizarCorreo(String correo) {
        return limpiar(correo).toLowerCase(Locale.ROOT);
    }

    private static String normalizarCodigo(String codigo) {
        return limpiar(codigo).toUpperCase(Locale.ROOT);
    }
}
