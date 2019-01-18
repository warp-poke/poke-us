# Poke US

### Status: WIP

Application starts with scheduling each FETCH_ALERTS_INTERVAL seconds data retrieving to get alerts from warp10.

The goal is to retrieve "to alert" items and if alerts match "to alert" then send notification using unimplemented channels like SMS, mailing, etc.

### Needed env variables

- `WARP_HOST`
- `WARP_PORT`
- `POKE_API_BASE_URL`
- `POKE_API_INTERNAL_AUTH_TOKEN`
- `POKE_API_READ_TOKEN_ENDPOINT`
- `POKE_API_WRITE_TOKEN_ENDPOINT`
- `FETCH_ALERTS_INTERVAL` second(s)
- `FETCH_READ_TOKEN_INTERVAL` minute(s)
- `FETCH_WRITE_TOKEN_INTERVAL` minute(s)
- `MAX_RETRIES_SEND_FOR_HOOK` integer
- `SERIES_TO_WATCH` string array

### Run in dev mode

- sbt run
