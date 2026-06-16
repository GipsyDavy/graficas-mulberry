package org.gipsybuho.service;

import org.gipsybuho.db.DatabaseManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public final class LanguageManager {

    private static final String KEY_IDIOMA    = "idioma";
    private static final String DEFAULT_LANG  = "es";

    private static LanguageManager instance;
    private final ResourceBundle bundle;
    private final String idiomaActual;

    private LanguageManager() {
        String stored = DatabaseManager.getConfig(KEY_IDIOMA);
        idiomaActual  = (stored != null && !stored.isBlank()) ? stored : DEFAULT_LANG;
        bundle        = loadBundle(Locale.forLanguageTag(idiomaActual));
    }

    public static synchronized LanguageManager getInstance() {
        if (instance == null) instance = new LanguageManager();
        return instance;
    }

    /** Shorthand para import estático: import static ...LanguageManager.t */
    public static String t(String key) {
        try {
            return getInstance().bundle.getString(key);
        } catch (MissingResourceException e) {
            return "[" + key + "]";
        }
    }

    public String getIdioma() {
        return idiomaActual;
    }

    /** Persiste el código de idioma. El cambio aplica al reiniciar la aplicación. */
    public void setIdioma(String codigo) {
        DatabaseManager.setConfig(KEY_IDIOMA, codigo);
    }

    private static ResourceBundle loadBundle(Locale locale) {
        for (Locale l : List.of(locale, Locale.forLanguageTag(DEFAULT_LANG))) {
            String path = "org/gipsybuho/i18n/messages_" + l.getLanguage() + ".properties";
            try (InputStream is = LanguageManager.class.getClassLoader().getResourceAsStream(path)) {
                if (is != null) {
                    return new PropertyResourceBundle(new InputStreamReader(is, StandardCharsets.UTF_8));
                }
            } catch (IOException ignored) {}
        }
        throw new IllegalStateException("Bundle de idioma no encontrado para: " + locale);
    }
}
