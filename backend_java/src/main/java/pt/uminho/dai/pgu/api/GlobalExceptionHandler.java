package pt.uminho.dai.pgu.api;

import pt.uminho.dai.pgu.business.acessos_configuracao.*;
import pt.uminho.dai.pgu.business.operacao_tempo_real.*;
import pt.uminho.dai.pgu.business.analitica_historico.*;
import pt.uminho.dai.pgu.data.*;
import pt.uminho.dai.pgu.data.acessos_configuracao.*;
import pt.uminho.dai.pgu.data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.analitica_historico.*;
import pt.uminho.dai.pgu.api.acessos_configuracao.*;
import pt.uminho.dai.pgu.api.operacao_tempo_real.*;
import pt.uminho.dai.pgu.api.analitica_historico.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler global de exceções para todos os controllers da API.
 *
 * Centraliza o tratamento de erros de validação (@Valid) para evitar
 * duplicação de código caso sejam adicionados mais controllers no futuro.
 * Retorna HTTP 400 com lista detalhada de campos inválidos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
 Map<String, Object> response = new LinkedHashMap<>();
 response.put("status", "erro");
 response.put("mensagem", "Dados de entrada inválidos. Verifique os campos assinalados.");

 Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
 .collect(Collectors.toMap(
 FieldError::getField,
 FieldError::getDefaultMessage,
 (existing, replacement) -> existing // manter o primeiro erro por campo
 ));

 response.put("erros", fieldErrors);
 return ResponseEntity.badRequest().body(response);
 }
}