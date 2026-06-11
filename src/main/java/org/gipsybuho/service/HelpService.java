package org.gipsybuho.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gipsybuho.model.HelpEntry;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public final class HelpService {

    private static HelpService instance;
    private final List<HelpEntry> entries;

    private HelpService() {
        entries = loadIndex();
    }

    public static synchronized HelpService getInstance() {
        if (instance == null) instance = new HelpService();
        return instance;
    }

    /** Todos los artículos cargados desde index.json. */
    public List<HelpEntry> getAll() {
        return entries;
    }

    /**
     * Busca artículos cuyo título o tags contengan la query (insensible a mayúsculas).
     * Si query es nula o vacía devuelve todos los artículos.
     */
    public List<HelpEntry> search(String query) {
        if (query == null || query.isBlank()) return entries;
        String q = query.trim().toLowerCase();
        return entries.stream()
            .filter(e -> e.title().toLowerCase().contains(q)
                || e.tags().stream().anyMatch(t -> t.toLowerCase().contains(q)))
            .collect(Collectors.toList());
    }

    /** Artículos de un módulo concreto, ordenados por prioridad ascendente. */
    public List<HelpEntry> getByModule(String module) {
        return entries.stream()
            .filter(e -> module.equals(e.module()))
            .sorted(Comparator.comparingInt(HelpEntry::priority))
            .collect(Collectors.toList());
    }

    /** Artículo por ID exacto. */
    public Optional<HelpEntry> getEntry(String id) {
        return entries.stream().filter(e -> id.equals(e.id())).findFirst();
    }

    private List<HelpEntry> loadIndex() {
        try (InputStream is = getClass().getResourceAsStream("/org/gipsybuho/help/index.json")) {
            if (is == null) return List.of();
            JsonNode array = new ObjectMapper().readTree(is);
            List<HelpEntry> list = new ArrayList<>();
            for (JsonNode node : array) {
                List<String> tags = new ArrayList<>();
                node.get("tags").forEach(t -> tags.add(t.asText()));
                list.add(new HelpEntry(
                    node.get("id").asText(),
                    node.get("module").asText(),
                    node.get("category").asText(),
                    node.get("title").asText(),
                    List.copyOf(tags),
                    node.get("path").asText(),
                    node.get("priority").asInt()
                ));
            }
            return List.copyOf(list);
        } catch (Exception e) {
            return List.of();
        }
    }
}
