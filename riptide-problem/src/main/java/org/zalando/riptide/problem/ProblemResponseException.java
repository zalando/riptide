package org.zalando.riptide.problem;

import lombok.Getter;
import org.springframework.http.ProblemDetail;

/**
 * Exception thrown when a response contains a {@link ProblemDetail}.
 * <p>
 * This exception wraps the {@link ProblemDetail} object, allowing clients to access
 * the problem details returned from a remote service or API.
 */
@Getter
public class ProblemResponseException extends RuntimeException {
    private final ProblemDetail problem;

    /**
     * Constructs a new {@code ProblemResponseException} with the specified {@link ProblemDetail}.
     *
     * @param problem the problem detail associated with the response
     */
    public ProblemResponseException(ProblemDetail problem) {
        super(problem.getTitle());
        this.problem = problem;
    }
}
