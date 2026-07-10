# 03 — .gitignore y .gitattributes

## .gitignore

### Reglas nuevas agregadas

- `/CONTEXTO_CONTINUIDAD_BOD_FALTAS_POST_B1_PRE_MARIADB/`
- `/recuperacion-110-*/`
- `/auditoria-copias-110.csv`
- `/*.corrupto-backup-*`
- `/_tmp_*/`, `/_tmp_*.patch`
- `/CONTEXTO_*.zip`
- `/git-baseline-*.txt`, `/MOJIBAKE-*.txt`
- `**/__pycache__/`, `*.py[cod]`
- `/mvn-*.log`, `/backend/api-faltas-core/log-*.txt`
- `/util_pablo*.ps1`, `/validar*.ps1`
- Backups corruptos, scripts Python temporales, carpeta DIAGNOSTICO

### Correccion aplicada en esta sesion

Se elimino la regla:
`
/backend/api-faltas-core/docs/spec-as-source/handoff/
`
Motivo: los handoffs canonicos DEBEN poder versionarse.
Esa regla bloqueaba el versionado del directorio handoff.

## .gitattributes

Politica correcta verificada:

- `* text=auto`
- Java, XML, YML, YAML, properties, MD, SQL, JSON, TXT, CSV: `eol=lf`
- PS1: `eol=crlf`
- `*.patch -text` (evidencia inmutable, no renormalizar)

Sin BOM. Politica coherente y completa.