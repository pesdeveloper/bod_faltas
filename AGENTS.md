# Instrucciones para agentes — Faltas

Antes de modificar el backend productivo de Faltas:

1. leer `backend/api-faltas-core/docs/spec-as-source/README.md`;
2. identificar allí los documentos canónicos aplicables al alcance;
3. consultar código y tests como evidencia de conformidad;
4. consultar `backend/api-faltas-core/docs/spec-as-source/00-governance/document-registry.md`, `backend/api-faltas-core/docs/spec-as-source/50-persistence/mariadb-logical-model.md` y `backend/api-faltas-core/docs/spec-as-source/50-persistence/ddl-decisions.md` cuando el alcance requiera auditar proceso, modelo MariaDB o decisiones históricas; usar la historia Git únicamente como evidencia adicional, nunca como autoridad.

La única spec-as-source canónica del proyecto está en `backend/api-faltas-core/docs/spec-as-source/`.

Los documentos externos, el código, los tests, los handoffs y la historia Git no pueden redefinir silenciosamente la spec. Si aparece una contradicción, detenerse, reportarla y no resolverla por inferencia.

Reglas:

- no inventar eventos;
- no inventar bloques;
- no inventar estados centrales;
- no usar strings libres para dominio cuando exista un tipo canónico;
- no confundir estado de dominio, estado documental, bandeja, acción o proyección;
- no reintroducir `D3_DOCUMENTAL`;
- no usar `PAGCON`;
- no usar `APELAC`;
- no usar `ACTCER`;
- no usar `DRVEXT`;
- no implementar MariaDB/JDBC fuera de un slice expresamente autorizado;
- no modificar reglas funcionales para adaptarlas a la persistencia;
- no eliminar documentación histórica sin extracción y trazabilidad previas.
