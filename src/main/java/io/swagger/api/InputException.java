package io.swagger.api;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Custom class as I needed to output a HTTP 400 BAD REQUEST code.
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InputException extends RuntimeException {
    public InputException(String exception) {
        super(exception);
    }
}
