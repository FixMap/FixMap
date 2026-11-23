# User frontend integration checks

Use a disposable backend matching develop (commit 432680a or its compatible contract) with db/data.sql test data. This check creates a report with five real photo uploads; it does not delete the created report/files. Never point it at the team's shared or production database.

Install browser tooling outside the repository, start the backend with a separate test database/upload directory, and run from the project root:

```powershell
npm install --prefix "$env:TEMP/fixmap-ui-tools" --no-save puppeteer-core
$env:FIXMAP_BROWSER_TOOLS = "$env:TEMP/fixmap-ui-tools"
$env:FIXMAP_URL = 'http://localhost:8080/FixMap'
$env:FIXMAP_ALLOW_WRITES = '1'
node src/test/ui/develop-api.mjs
```

Chrome's Windows default location is used, or set CHROME_PATH. The sample second-floor pins and report #2's reply are used by the check. All normal operations use the running APIs. Targeted failures, delayed responses, PROGRESS, and unsafe text are intercepted to check frontend handling.

Coverage: one pin request per level, floorId placement, normalized place rectangles, MainPageController's level model, responsive 1440/375/320px controls, zoom, stale responses, API retry, categories, upload count/type/size checks, duplicate submission prevention, preserved input/files on error, actual multipart creation, public detail/replies, returned file bytes and safe text. Administrator API calls and temporary SVG map requests are excluded.

The 2026-11-23 verification used actual develop controllers/services/repositories/JPA, Tomcat 10.1 and a disposable H2 Oracle-compatibility database. This is not Oracle/Eclipse integration verification. The preview launcher/database overrides stay outside this PR.
