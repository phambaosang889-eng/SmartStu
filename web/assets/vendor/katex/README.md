# KaTeX 0.19.1

Official package: https://registry.npmjs.org/katex/-/katex-0.19.1.tgz
Documentation: https://katex.org/docs/api
MIT license: see LICENSE in this directory.

Only the browser distribution (JavaScript, CSS, fonts) is shipped here. No Node.js
runtime is required. Assets are served locally so AI answers do not depend on a CDN.
The tutor renderer disables trusted HTML/URL commands and bounds macro expansion
and formula sizing. Prose is rendered with DOM text nodes, never innerHTML.
