# Deployment

One VM running Docker Compose (Architecture Design §2): Nginx → Spring Boot → PostgreSQL/PostGIS, Redis, MinIO, plus Prometheus + Grafana.

## First deploy

```sh
git clone <repo> /opt/nearby && cd /opt/nearby/deploy
cp .env.example .env          # fill in secrets, domains and APP_IMAGE
docker compose up -d
```

Point two DNS records at the VM: `API_DOMAIN` (REST, WebSocket `/ws`, Grafana `/grafana/`) and `STORAGE_DOMAIN` (MinIO, used for presigned image URLs).

## TLS (Let's Encrypt)

```sh
docker compose run --rm --entrypoint certbot certbot certonly --webroot -w /var/www/certbot \
  -d "$API_DOMAIN" -d "$STORAGE_DOMAIN" --email you@example.com --agree-tos
```

Certbot names the certificate after the first `-d` domain. Either issue one certificate per domain (run the command twice) or point both `ssl_certificate` paths in the HTTPS template at the shared one.

```sh
mv nginx/templates/https.conf.template.example nginx/templates/https.conf.template
docker compose restart nginx
docker compose --profile tls up -d certbot   # auto-renewal loop
```

## CI/CD

Only CI is set up for now (see [docs/setup-ci.md](../docs/setup-ci.md)). There is no automated deployment yet. Until there is, build the image on the VM (`docker compose up -d --build`) or push it to a registry by hand.

## Backups

The nightly `pg_dump` goes to `deploy/backups/`. Sync that folder off-site.

```cron
0 2 * * * cd /opt/nearby/deploy && sh scripts/backup.sh >> backups/backup.log 2>&1
```

Restore with `docker compose exec -T postgres pg_restore -U nearby -d nearby --clean /backups/<file>.dump`.

## Fallback

If the cloud VM is unavailable during the defense, run the same stack on a laptop with `docker compose up -d`.
