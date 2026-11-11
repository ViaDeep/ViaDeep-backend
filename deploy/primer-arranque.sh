#!/usr/bin/env bash
# Prepara el .env del servidor la primera vez: pide el dominio, el administrador y el Gmail, y genera las claves.
# Uso, desde la raíz del repositorio del backend:  bash deploy/primer-arranque.sh
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -f .env ]; then
  echo "Ya existe .env; no se cambia. Bórralo si quieres crearlo de nuevo."
  exit 0
fi

read -rp "Dominio del servidor (ej. viadeep-tesis.eastus.cloudapp.azure.com): " domain
read -rp "Correo del administrador municipal: " admin_email
read -rsp "Contraseña del administrador (8 caracteres o más): " admin_password; echo
read -rp "Gmail del proyecto (Enter para no enviar correos): " mail_user
mail_password=""
if [ -n "$mail_user" ]; then
  read -rsp "Contraseña de aplicación de ese Gmail (16 letras): " mail_password; echo
fi
read -rp "Clave de CARTO para el mapa (Enter para omitir): " carto_key

jwt_key=$(openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 2>/dev/null | grep -v '^-----' | tr -d '\n')
internal_token=$(openssl rand -hex 32)

umask 077
cat > .env <<ENV
DOMAIN=${domain}
OPERATOR_EMAIL=${admin_email}
OPERATOR_PASSWORD=${admin_password}
OPERATOR_NAME=Administrador municipal
MAIL_HOST=$([ -n "$mail_user" ] && echo smtp.gmail.com)
MAIL_USERNAME=${mail_user}
MAIL_PASSWORD=${mail_password}
MAIL_FROM=$([ -n "$mail_user" ] && echo "ViaDeep <${mail_user}>")
VITE_CARTO_API_KEY=${carto_key}
VIADEEP_JWT_PRIVATE_KEY=${jwt_key}
NOTIFICATIONS_INTERNAL_TOKEN=${internal_token}
ENV
echo "Listo: .env creado (solo lo puede leer tu usuario)."
