# User frontend integration checks

Use a fresh disposable backend with the sample data from db/data.sql. This check creates a report with five photo uploads and leaves the record and files in place. Never point it at the team's shared or production database.

Install browser tooling outside the repository, start the backend with a separate test database/upload directory, and run from the project root:

```powershell
npm install --prefix "$env:TEMP/fixmap-ui-tools" --no-save puppeteer-core
$env:FIXMAP_BROWSER_TOOLS = "$env:TEMP/fixmap-ui-tools"
$env:FIXMAP_URL = 'http://localhost:8080/FixMap'
$env:FIXMAP_ALLOW_WRITES = '1'
node src/test/ui/develop-api.mjs
```

Chrome's Windows default location is used, or set CHROME_PATH. The sample second-floor pins and report #2's reply are used by the check. All normal operations use the running APIs. Targeted failures, delayed responses, PROGRESS, and unsafe text are intercepted to check frontend handling.

Coverage includes floor-specific pins, place tooltips, grouped report cards, blank-map click rejection, zoom, responsive controls, multipart creation, and public detail/replies. Set `FIXMAP_ADMIN_KEY` to the disposable instance's admin key to also check the administrator floor plan, saved coordinates, and empty-state spacing.

The 2026-11-23 verification used actual develop controllers/services/repositories/JPA, Tomcat 10.1 and a disposable H2 Oracle-compatibility database. This is not Oracle/Eclipse integration verification. The preview launcher/database overrides stay outside this PR.
