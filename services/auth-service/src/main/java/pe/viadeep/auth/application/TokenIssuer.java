package pe.viadeep.auth.application;

import pe.viadeep.auth.domain.User;

public interface TokenIssuer {

    IssuedToken issue(User user);

    record IssuedToken(String value, long expiresInSeconds) {
    }
}
