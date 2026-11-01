# GmailGenie AI dashboard

React 19 + Vite front end for the GmailGenie API. It posts the pasted email to the Spring Boot service and renders the generated reply.

```bash
npm ci
npm run dev
```

The API base URL defaults to the deployed Render service. Override it with `VITE_API_URL`, for example:

```bash
VITE_API_URL=http://localhost:8080/api/email npm run dev
```

`npm run build` produces a static bundle in `dist/`. See the [root README](../README.md) for the full setup.
