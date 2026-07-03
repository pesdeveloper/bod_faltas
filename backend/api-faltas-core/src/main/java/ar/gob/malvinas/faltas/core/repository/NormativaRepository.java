package ar.gob.malvinas.faltas.core.repository;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaNormativa;
import ar.gob.malvinas.faltas.core.domain.model.FalNormativaFaltas;
import java.util.List;
import java.util.Optional;
public interface NormativaRepository {
    FalNormativaFaltas guardarNormativa(FalNormativaFaltas normativa);
    Optional<FalNormativaFaltas> findNormativaById(Long id);
    Optional<FalNormativaFaltas> findNormativaByCodigoYVersion(String codigoNorma, int versionNorma);
    boolean existsNormativaByCodigoYVersion(String codigoNorma, int versionNorma);
    List<FalNormativaFaltas> findAllNormativasActivas();
    FalArticuloNormativaFaltas guardarArticulo(FalArticuloNormativaFaltas articulo);
    Optional<FalArticuloNormativaFaltas> findArticuloById(Long id);
    Optional<FalArticuloNormativaFaltas> findArticuloByNormativaYCodigo(Long normativaId, String codigoArticulo, int versionArticulo);
    boolean existsArticuloByNormativaYCodigo(Long normativaId, String codigoArticulo, int versionArticulo);
    List<FalArticuloNormativaFaltas> findArticulosByNormativa(Long normativaId);
    List<FalArticuloNormativaFaltas> findArticulosActivosByNormativa(Long normativaId);
    FalDependenciaNormativa guardarDependenciaNormativa(FalDependenciaNormativa rel);
    Optional<FalDependenciaNormativa> findDependenciaNormativa(Long idDep, int verDep, Long normativaId);
    boolean existsDependenciaNormativaActiva(Long idDep, int verDep, Long normativaId);
    List<FalDependenciaNormativa> findNormativasByDepVersion(Long idDep, int verDep);
    List<FalDependenciaNormativa> findNormativasActivasByDepVersion(Long idDep, int verDep);
}
