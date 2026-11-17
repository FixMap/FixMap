# User frontend checks

Run the UI with a backend matching the two GET contracts in `main` commit `7d34c2e`. The test makes no writes and checks that unconnected APIs are never requested.

```powershell
# Browser tooling only; do not commit the installed directory.
npm install --prefix "$env:TEMP/fixmap-ui-tools" --no-save puppeteer-core
$env:FIXMAP_BROWSER_TOOLS = "$env:TEMP/fixmap-ui-tools"
$env:FIXMAP_URL = 'http://localhost:8080/FixMap'
node src/test/ui/main-api.mjs
```

Chrome's Windows default location is used, or set `CHROME_PATH`. Test data should include the main branch's sample buildings/floors and at least one second-floor pin.

The check uses the running GET endpoints normally. Error and unsafe-title cases intercept only the pin response. It asserts the `/FixMap` asset paths, `floor` ID parameter, `posX`/`posY` coordinates, responsive controls, local previews, disabled submission, zoom and error/retry. No report creation or upload is tested or claimed.

`develop` currently uses a different pin parameter (`level`) and omits `mapImageUrl`. Those differences need backend coordination before integration; the frontend deliberately does not invent a fallback endpoint or use the unapproved additional APIs.
