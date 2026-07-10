# Instrucciones para agentes � Faltas

Antes de modificar el backend de Faltas, revisar:

1. `docs/faltas/README.md`
2. `docs/faltas/MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md`
3. `docs/faltas/MATRIZ_PROCESO_FALTAS_CIERRE_COMPLETA_2026-06-23.md`
4. `docs/faltas/DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md`
5. `backend/api-faltas-core/docs/spec-as-source/99-pendientes-siguientes-slices.md`
6. `backend/api-faltas-core/docs/spec-as-source/02-estados-bloques-eventos.md`

La única spec-as-source canónica del proyecto está en `backend/api-faltas-core/docs/spec-as-source/`.

Reglas:
- no inventar eventos;
- no inventar bloques;
- no inventar estados centrales;
- no usar strings libres para dominio;
- no reintroducir `D3_DOCUMENTAL`;
- no usar `PAGCON`;
- no usar `APELAC`;
- no usar `ACTCER`;
- no usar `DRVEXT`;
- no implementar MariaDB/JDBC sin reconciliar el delta del modelo.
