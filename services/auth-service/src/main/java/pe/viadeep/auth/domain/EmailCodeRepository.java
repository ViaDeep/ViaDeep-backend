package pe.viadeep.auth.domain;

import java.util.Optional;

public interface EmailCodeRepository {

    EmailCode save(EmailCode code);

    /** El último código enviado a ese correo para ese fin; los anteriores ya no sirven. */
    Optional<EmailCode> findLatest(String email, EmailCodePurpose purpose);

    void deleteAll(String email, EmailCodePurpose purpose);
}
