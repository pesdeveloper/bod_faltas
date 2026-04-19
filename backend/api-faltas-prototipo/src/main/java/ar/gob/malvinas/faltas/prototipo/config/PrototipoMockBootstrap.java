package ar.gob.malvinas.faltas.prototipo.config;

import ar.gob.malvinas.faltas.prototipo.store.MockDataFactory;
import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class PrototipoMockBootstrap implements ApplicationRunner {

    private final PrototipoStore store;
    private final MockDataFactory mockDataFactory;

    public PrototipoMockBootstrap(PrototipoStore store, MockDataFactory mockDataFactory) {
        this.store = store;
        this.mockDataFactory = mockDataFactory;
    }

    @Override
    public void run(ApplicationArguments args) {
        mockDataFactory.loadInitialData(store);
    }
}
