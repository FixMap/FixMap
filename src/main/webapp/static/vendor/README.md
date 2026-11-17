# User-page libraries

| Library | Version | Files |
| --- | --- | --- |
| Bootstrap | 5.3.9 | CSS and JS bundle including Popper |
| htmx | 2.0.11 | htmx.min.js |
| Alpine.js CSP | 3.17.5 | csp.min.js |

Pinned distribution files from jsDelivr's npm packages are served locally. Their licenses are included; Alpine's license comes from its matching source tag. Bootstrap and htmx SHA-384 values were checked against the official installation documentation when downloaded.

The common user header loads the scripts with defer. Bootstrap CSS sits in a lower cascade layer. Alpine uses the CSP-friendly build and htmx disables eval and script-tag execution. No htmx request is configured.

JSTL comes from the existing develop Maven configuration; this frontend PR does not change dependencies or security configuration. The frontend calls only the two GET endpoints verified in main. Report submission, category lookup, detail lookup and file transfer are not connected.
