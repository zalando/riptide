package org.zalando.riptide;

public class ProblemException extends RuntimeException {

    private final Problem problem;

    public ProblemException(Problem problem) {
        this.problem = problem;
    }

    public Problem getProblem() {
        return problem;
    }

}
