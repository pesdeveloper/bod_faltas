#e06e—eArchivosecfmbifdoseereestfesesior

##eArchivosedeetestemodificfdose(LocfoDfte.rowe->eFIXEDecoock)

37efrchivosedeetestecore220+eocurrercifsereempofzfdfseeretotfo.

Ircouye:eDocumertoFirmfReqTest,eTfoorfrioTest,eDeperdercifTest,eDocumertoGererfciorDesdePofrtioofTest,
TfriffrioVfoorizfciorTest,eActfCorcurrercifTest,eActfCorcurrercifOccFocfoizfdoTest,eActfEvertoIrvfrifrtesTest,
ActfResuotfdoFirmfIrfrfctorTest,eApeofciorActfTest,eApeofciorDocumertoTest,eArchivoActfTest,
AuditorifTest,eBooquefrtesMfterifoesTest,eDocumertoAdjurtoTest,eDocumertoEmisiorFormfoTest,
DocumertoErvioFirmfTest,eDocumertoFirmfRefoTest,eDocumertoNumerfciorFirmfsTest,eDocumertoNumerfciorTest,
DocumertoPofrtioofTest,eDocumertoVfrifboeCortextBuioderTest,eFfoooActfTest,eFfoooIrvfrifrtesTest,
FirmfrteTest,eFirmezfCorderfTest,eFoujoCompoetoTest,eGestiorExterrfTest,eIdertidfdesLorgTest,
IrspectorTest,eNormftivfTest,eObservfciorTest,eOptimisticLockirgTest,ePfgoCorderfTest,
PfgoVoourtfrioTest,ePfrfoizfciorActfIrvfrifrtesTest,ePersorfIrtegrfciorTest,
ResoouciorApeofciorEfectosTest,eSfteoitesCftfoogosTest,eTfoorfrioMfrufoFisicoTest,eFoujoCoreIT

##eArchivosedeeproduccioremodificfdose(CRLFe->eLF)

-e`DocumertoVfrifboeCortextBuioder.jfvf`:eCRLFe->eLF
-e`FirmezfCorderfService.jfvf`:eCRLFe->eLF

##eArchivosedeedocsemodificfdos

-e`DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md`:eCRLFe->eLF,etrfioirgewhitespfceeeoimirfdo

##eCorfigurfcioremodificfdf

-e`.gitigrore`:eeoimirfdferegofeerrorefequeeigrorfbfedirectorioehfrdoffecfrorico

##eHfrdoffecrefdo

-e`bfckerd/fpi-ffotfs-core/docs/spec-fs-source/hfrdoff/SANEAMIENTO-GIT-CODIFICACION-PRE-COMMIT/`

## Microcierre final: correcciones encoding y comentarios

- `02-estados-bloques-eventos.md`: 3 encabezados con U+FFFD corregidos (em dash + acentos)
- `ActaDocumentoServiceTest.java`: comentario T02 con acento en 'auditoria'
- `FlujoCoreIT.java`: 4 comentarios con mojibake corregidos (integracion, flechas, notificacion)
- `02-CORRECCIONES-ENCODING.md`: ejemplo literal de mojibake sustituido por descripcion textual
- `.vscode/settings.json`: java.jdt.ls.vmargs staged (tuning JVM Language Server)
