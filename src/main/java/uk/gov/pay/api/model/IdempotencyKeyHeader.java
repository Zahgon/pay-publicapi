package uk.gov.pay.api.model;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Pattern;

/**
 * The {@code Idempotency-Key} header of a payment creation request, with its constraints.
 */
public record IdempotencyKeyHeader(
        @Length(min = 1, max = 255, message = "Header [Idempotency-Key] can have a size between 1 and 255")
        @Pattern(regexp = "^$|^[a-zA-Z0-9-]+$", message = "Header [Idempotency-Key] can only contain alphanumeric characters and hyphens")
        String idempotencyKey) {
}
