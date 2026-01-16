package org.zalando.riptide.problem;

import lombok.Getter;
import org.springframework.http.ProblemDetail;

@Getter
public class ProblemResponseException extends RuntimeException {
    private final ProblemDetail problem;

    public ProblemResponseException(ProblemDetail problem) {
        super(problem.getTitle());
        this.problem = problem;
    }
}
