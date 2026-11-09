package com.ourvision.utill;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.ourvision.dto.ApiResponse;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class ElectionExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ElectionExceptionHandler.class);

	// 400 - Bad Request: invalid input or business validation
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
	}

	// 400 - Malformed JSON or invalid request body
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvalidRequestBody(HttpMessageNotReadableException ex) {

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Invalid or malformed request body."));
	}

	// 400 - Validation errors, for example @Valid fields
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {

		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.joining("; "));

		if (message.isBlank()) {
			message = "Request validation failed.";
		}

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message));
	}

	// 400 - Missing required query parameter
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(),
				"Required parameter is missing: " + ex.getParameterName()));
	}

	// 400 - Wrong path/query parameter type
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
				ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Invalid value for parameter: " + ex.getName()));
	}

	// 500 - A required path variable is missing from the controller mapping
	@ExceptionHandler(MissingPathVariableException.class)
	public ResponseEntity<ApiResponse<Void>> handleMissingPathVariable(MissingPathVariableException ex) {

		log.error("Missing path variable in controller mapping", ex);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
				ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "A server configuration error occurred."));
	}

	// 404 - Election or candidate not found
	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException ex) {

		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
	}

	// 409 - Election state conflict, duplicate vote, etc.
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiResponse<Void>> handleConflict(IllegalStateException ex) {

		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiResponse.error(HttpStatus.CONFLICT.value(), ex.getMessage()));
	}

	// 409 - Database unique constraint / foreign key conflicts
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {

		log.warn("Election data integrity conflict", ex);

		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiResponse.error(HttpStatus.CONFLICT.value(), "The request conflicts with existing data."));
	}

	// 500 - Any unexpected server-side exception
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpectedError(Exception ex) {

		log.error("Unexpected error while processing election API request", ex);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
				ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected server error occurred."));
	}
}
