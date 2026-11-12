package it.lorenzoscarpa.dnd.common.error;

/** One entry of the "errors" list in ValidationProblemDetails (see docs/api.yaml). */
public record InvalidField(String field, String message) {
}
