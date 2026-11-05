package com.unigest.config;

import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> solicitudInvalida(MethodArgumentNotValidException e) {
        var campos = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField())
                .distinct().toList();
        return ResponseEntity.badRequest().body(Map.of("error", "Datos inválidos", "campos", campos));
    }
    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> formatoIncorrecto(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Solicitud o parámetros inválidos"));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflictoBaseDatos(DataIntegrityViolationException e) {
        // No exponer SQL, nombres de columnas ni detalles internos al navegador.
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Datos duplicados o referencias inválidas"));
    }
}
