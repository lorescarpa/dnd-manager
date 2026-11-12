package it.lorenzoscarpa.dnd.common.error;

import it.lorenzoscarpa.dnd.auth.EmailAlreadyUsedException;
import it.lorenzoscarpa.dnd.auth.PasswordTooLongException;
import it.lorenzoscarpa.dnd.user.UserNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Turns every exception into a Problem Details response (RFC 9457).
 *
 * <p>The parent class already handles Spring MVC's own exceptions (404 unknown path, 405, 415, ...);
 * here we customise the 400s and map the application exceptions. Spring Security errors raised
 * in filters reach this class too, through {@code SecurityProblemHandler}.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	// ---------- 400: request body ----------

	/** Bean Validation failures on the request body (@Size, @NotNull, ...). */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Stream<InvalidField> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new InvalidField(error.getField(), error.getDefaultMessage()));
		Stream<InvalidField> objectErrors = ex.getBindingResult().getGlobalErrors().stream()
				.map(error -> new InvalidField(error.getObjectName(), error.getDefaultMessage()));
		List<InvalidField> errors = Stream.concat(fieldErrors, objectErrors)
				.sorted(Comparator.comparing(InvalidField::field))
				.toList();
		return handleExceptionInternal(ex, validationProblem("Request validation failed", errors), headers, status,
				request);
	}

	/** Body that is not valid JSON, has a wrong type or enum value, or contains a field not in the contract. */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<InvalidField> errors = List.of();
		if (ex.getMostSpecificCause() instanceof JacksonException jackson && !jackson.getPath().isEmpty()) {
			String message = jackson instanceof UnrecognizedPropertyException ? "Unknown field" : "Invalid value";
			errors = List.of(new InvalidField(toFieldPath(jackson.getPath()), message));
		}
		return handleExceptionInternal(ex, validationProblem("Malformed request body", errors), headers, status,
				request);
	}

	@ExceptionHandler(PasswordTooLongException.class)
	ResponseEntity<ProblemDetail> handlePasswordTooLong(PasswordTooLongException ex) {
		return respond(validationProblem("Request validation failed",
				List.of(new InvalidField("password", ex.getMessage()))));
	}

	// ---------- 401 / 403 ----------

	/**
	 * Wrong credentials at login, or a missing/invalid/expired token (forwarded from the
	 * security filters). The detail never says which part was wrong.
	 */
	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
		String detail = ex instanceof BadCredentialsException
				? "Invalid email or password"
				: "Missing, invalid or expired access token";
		return respond(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail));
	}

	/** Token is valid but its user no longer exists: treat it as an invalid token. */
	@ExceptionHandler(UserNotFoundException.class)
	ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException ex) {
		return respond(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
				"Missing, invalid or expired access token"));
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
		return respond(ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
				"You are not allowed to perform this action"));
	}

	// ---------- 409 ----------

	@ExceptionHandler(EmailAlreadyUsedException.class)
	ResponseEntity<ProblemDetail> handleEmailAlreadyUsed(EmailAlreadyUsedException ex) {
		return respond(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage()));
	}

	// ---------- 500 ----------

	/** Last resort: log the cause, but never send internal details to the client. */
	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
		log.error("Unexpected error", ex);
		return respond(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error"));
	}

	// ---------- helpers ----------

	private static ProblemDetail validationProblem(String detail, List<InvalidField> errors) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setProperty("errors", errors); // extra member, serialized at the top level
		return problem;
	}

	private static ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
		return ResponseEntity.status(problem.getStatus()).body(problem);
	}

	/** Jackson path → "abilityScores.STR" or "inventory[0].name". */
	private static String toFieldPath(List<JacksonException.Reference> path) {
		StringBuilder field = new StringBuilder();
		for (JacksonException.Reference reference : path) {
			if (reference.getPropertyName() != null) {
				if (!field.isEmpty()) {
					field.append('.');
				}
				field.append(reference.getPropertyName());
			} else if (reference.getIndex() >= 0) {
				field.append('[').append(reference.getIndex()).append(']');
			}
		}
		return field.toString();
	}
}
