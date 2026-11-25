
FROM postgres:18-alpine

WORKDIR /bootstrap

COPY infra/db/aws/01-postgis.sql .
COPY infra/db/aws/02-service-roles-and-schemas.sql .
COPY infra/db/aws/03-set-service-passwords.psql .
COPY infra/db/aws/04-verify.sql .

COPY infra/db/aws/bootstrap/global-bundle.pem /bootstrap/global-bundle.pem

ENV PGSSLMODE=verify-full
ENV PGSSLROOTCERT=/bootstrap/global-bundle.pem

CMD ["sleep", "3600"]
