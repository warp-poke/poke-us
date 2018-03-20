# Poke US

### Status: WIP

Application starts with scheduling each FETCH_ALERTS_INTERVAL seconds data retrieving to get alerts from warp10.

The goal is to retrieve "to alert" items and if alerts match "to alert" then send notification using unimplemented channels like SMS, mailing, etc.

### Needed env variables

- `WARP_HOST`
- `WARP_PORT`
- `POKE_API_READ_TOKEN_ENDPOINT`
- `POKE_API_INTERNAL_AUTH_TOKEN`
- `FETCH_ALERTS_INTERVAL` second(s)
- `FETCH_READ_TOKEN_INTERVAL` minute(s)

### Run in dev mode

- sbt run
