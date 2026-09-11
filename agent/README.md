# proxy-agent

> **Deprecated.** The agent is superseded by the proxy engine embedded in the
> RouteWeaver mod and is kept for reference only. It is not maintained and the
> mod does not use it.

Local Go agent for the Minecraft proxy mod.

## Run

```powershell
go run ./cmd/proxy-agent
```

## API

- `GET /health`
- `GET /routes/active`
- `POST /routes/active`

The current implementation stores the selected route in memory and exposes it over HTTP. Real Shadowsocks/VLESS transport wiring is the next step.
