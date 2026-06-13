package org.gipsybuho.service;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.gipsybuho.db.DatabaseManager;

public final class PreferenceService {

    private static final String KEY_BEGINNER   = "modo_principiante";
    private static final String KEY_FIRST_RUN  = "first_run_completed";

    private static PreferenceService instance;
    private final BooleanProperty beginnerMode = new SimpleBooleanProperty(false);

    private PreferenceService() {
        beginnerMode.set("true".equals(DatabaseManager.getConfig(KEY_BEGINNER)));
        beginnerMode.addListener((obs, ov, nv) ->
            DatabaseManager.setConfig(KEY_BEGINNER, nv.toString()));
    }

    public static synchronized PreferenceService getInstance() {
        if (instance == null) instance = new PreferenceService();
        return instance;
    }

    public BooleanProperty beginnerModeProperty() { return beginnerMode; }
    public boolean isBeginnerMode()               { return beginnerMode.get(); }
    public void    setBeginnerMode(boolean value) { beginnerMode.set(value); }

    public boolean isFirstRun() {
        return !"true".equals(DatabaseManager.getConfig(KEY_FIRST_RUN));
    }

    public void markFirstRunCompleted() {
        DatabaseManager.setConfig(KEY_FIRST_RUN, "true");
    }
}
