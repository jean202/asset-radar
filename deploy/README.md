# Production Deployment

## Deployment Decision

- Build target: GitHub Actions builds API and frontend Docker images and pushes them to GHCR.
- Runtime target: a Docker Compose host, default path `/opt/asset-radar`.
- Secret injection: runtime-only `.env.prod` on the production host. Application secrets are not stored in GitHub Actions, Docker images, or source control.
- GitHub Actions secrets: only deployment transport credentials and optional GHCR pull credentials.

## Required GitHub Settings

Repository secrets:

- `PROD_SSH_HOST`: production host.
- `PROD_SSH_USER`: SSH user with Docker permissions.
- `PROD_SSH_PRIVATE_KEY`: private key for the deploy user.
- `PROD_SSH_PORT`: optional, defaults to `22`.
- `GHCR_USERNAME`: optional, required only when GHCR packages are private.
- `GHCR_TOKEN`: optional, required only when GHCR packages are private.

Repository variable:

- `PROD_DEPLOY_PATH`: optional, defaults to `/opt/asset-radar`.

## First-Time Server Setup

```bash
ssh "$PROD_SSH_USER@$PROD_SSH_HOST" 'sudo mkdir -p /opt/asset-radar && sudo chown "$USER" /opt/asset-radar'
scp deploy/.env.prod.example "$PROD_SSH_USER@$PROD_SSH_HOST:/opt/asset-radar/.env.prod"
ssh "$PROD_SSH_USER@$PROD_SSH_HOST" 'vi /opt/asset-radar/.env.prod'
```

Set at minimum:

- `POSTGRES_PASSWORD`
- `SPRING_R2DBC_PASSWORD`
- external API keys that should run in production
- alert webhook URLs and enabled flags, if needed

The deploy workflow copies `deploy/docker-compose.prod.yml` into the deploy path and runs:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml pull
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --remove-orphans
```

## Running A Deployment

Use one of:

- push a version tag such as `v0.1.0`
- run `Deploy Production` manually from GitHub Actions

Manual runs can provide an explicit image tag. If omitted, the workflow uses the current ref name, or the short commit SHA for `main`.
