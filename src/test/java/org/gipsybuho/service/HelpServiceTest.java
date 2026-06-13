package org.gipsybuho.service;

import org.gipsybuho.model.HelpEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HelpServiceTest {

    private final HelpService service = HelpService.getInstance();

    @Test
    void indexCargaArticulos() {
        List<HelpEntry> all = service.getAll();
        assertFalse(all.isEmpty(), "El índice debe contener artículos");
        assertTrue(all.size() >= 81, "Deben cargarse al menos 81 artículos");
    }

    @Test
    void searchPorTituloEncuentraResultado() {
        List<HelpEntry> results = service.search("cliente");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> e.id().equals("CLI-PS-1")));
    }

    @Test
    void searchPorTagEncuentraResultado() {
        List<HelpEntry> results = service.search("NIF");
        assertFalse(results.isEmpty(), "Búsqueda por tag 'NIF' debe devolver resultados");
    }

    @Test
    void searchQueryVaciaDevuelveTodo() {
        List<HelpEntry> all = service.getAll();
        assertEquals(all.size(), service.search("").size());
    }

    @Test
    void searchQueryNulaDevuelveTodo() {
        List<HelpEntry> all = service.getAll();
        assertEquals(all.size(), service.search(null).size());
    }

    @Test
    void getByModuleDevuelveEnOrdenDePrioridad() {
        List<HelpEntry> clientes = service.getByModule("clientes");
        assertFalse(clientes.isEmpty());
        for (int i = 0; i < clientes.size() - 1; i++) {
            assertTrue(clientes.get(i).priority() <= clientes.get(i + 1).priority(),
                "Los artículos deben estar ordenados por prioridad ascendente");
        }
    }

    @Test
    void getByModuleDesconocidoDevuelveListaVacia() {
        assertTrue(service.getByModule("modulo_inexistente").isEmpty());
    }

    @Test
    void getEntryPorIdExistente() {
        assertTrue(service.getEntry("GEN-PS-1").isPresent());
        assertEquals("GEN-PS-1", service.getEntry("GEN-PS-1").get().id());
    }

    @Test
    void getEntryPorIdInexistenteDevuelveEmpty() {
        assertTrue(service.getEntry("NONEXISTENT-999").isEmpty());
    }

    @Test
    void defaultArticleExistenteParaFallbackDeForArticle() {
        assertTrue(service.getEntry("GEN-PS-1").isPresent(),
            "GEN-PS-1 debe existir: es el artículo de fallback de HelpView.forArticle()");
    }

    @Test
    void searchEsInsensibleAMayusculas() {
        List<HelpEntry> lower = service.search("factura");
        List<HelpEntry> upper = service.search("FACTURA");
        assertEquals(lower.size(), upper.size());
    }
}
