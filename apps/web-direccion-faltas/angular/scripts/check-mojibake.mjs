/**
 * check-mojibake.mjs
 *
 * Recorre src/app buscando patrones tipicos de mojibake (UTF-8 leido como Latin-1).
 * Falla con exit code 1 si encuentra alguno.
 *
 * Uso: node scripts/check-mojibake.mjs
 */

import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, extname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { dirname } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_DIR = join(__dirname, '..', 'src', 'app');

// Patrones que indican mojibake: UTF-8 de caracteres latinos interpretados como Latin-1
const PATTERNS = [
  // UTF-8 de acentos latinos double-encoded: Ã³ en lugar de ó, Ã¡ en lugar de á, etc.
  { pattern: /\xc3[\xa0-\xbf]/u, desc: 'Atilde + combining (e.g. o con acento doble-encoded)' },
  // Â seguido de Latin-1 combining
  { pattern: /\xc2[\xa0-\xbf]/u, desc: 'Acirc + combining (doble-encoded)' },
  // Caracter de reemplazo Unicode
  { pattern: /\ufffd/, desc: 'U+FFFD (caracter de reemplazo)' },
];

const EXTENSIONS = new Set(['.ts', '.html', '.scss']);

let errors = 0;

function walk(dir) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    const stat = statSync(full);
    if (stat.isDirectory()) {
      walk(full);
    } else if (EXTENSIONS.has(extname(entry))) {
      checkFile(full);
    }
  }
}

function checkFile(filePath) {
  const content = readFileSync(filePath, 'utf8');
  const lines = content.split('\n');
  lines.forEach((line, i) => {
    for (const { pattern, desc } of PATTERNS) {
      if (pattern.test(line)) {
        console.error('[mojibake] ' + filePath + ':' + (i + 1) + ' --- ' + desc);
        console.error('  ' + line.trim().slice(0, 120));
        errors++;
      }
    }
  });
}

walk(SRC_DIR);

if (errors > 0) {
  console.error('\nERROR: ' + errors + ' linea(s) con posible mojibake. Verificar encoding del archivo.');
  process.exit(1);
} else {
  console.log('OK: Sin mojibake detectado.');
}