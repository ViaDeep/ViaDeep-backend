package pe.viadeep.auth.infrastructure.persistence;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import pe.viadeep.auth.application.PageResult;
import pe.viadeep.auth.application.UserFilter;
import pe.viadeep.auth.application.UserQueryRepository;
import pe.viadeep.auth.domain.User;

@Repository
class UserQueryRepositoryJpa implements UserQueryRepository {

    private static final char LIKE_ESCAPE = '\\';

    private final SpringDataUserRepository jpa;

    UserQueryRepositoryJpa(SpringDataUserRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public PageResult<User> search(UserFilter filter, int page, int size) {
        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        Page<UserJpaEntity> result = jpa.findAll(specification(filter), request);
        return new PageResult<>(result.map(UserJpaEntity::toDomain).getContent(), page, size, result.getTotalElements());
    }

    private static Specification<UserJpaEntity> specification(UserFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (filter.role() != null) {
                conditions.add(cb.equal(root.get("role"), filter.role()));
            }
            if (filter.enabled() != null) {
                conditions.add(cb.equal(root.get("enabled"), filter.enabled()));
            }
            if (filter.text() != null && !filter.text().isBlank()) {
                String pattern = "%" + escapeLike(filter.text().strip().toLowerCase(Locale.ROOT)) + "%";
                conditions.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("email")), pattern, LIKE_ESCAPE)));
            }
            return cb.and(conditions.toArray(Predicate[]::new));
        };
    }

    /** Busca el texto tal cual: % y _ no actúan como comodines. */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
