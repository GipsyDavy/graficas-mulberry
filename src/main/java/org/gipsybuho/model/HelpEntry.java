package org.gipsybuho.model;

import java.util.List;

public record HelpEntry(
    String id,
    String module,
    String category,
    String title,
    List<String> tags,
    String path,
    int priority
) {}
