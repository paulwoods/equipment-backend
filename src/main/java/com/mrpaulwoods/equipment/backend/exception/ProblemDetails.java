package com.mrpaulwoods.equipment.backend.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public final class ProblemDetails {

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatusCode status, String detail, String title, String uri) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(uri));
        return problem;
    }
}
