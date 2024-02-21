package run.halo.moments.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * NotFoundException.
 *
 * @author LIlGG
 */
public class NotFoundException extends ResponseStatusException {
    public NotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
