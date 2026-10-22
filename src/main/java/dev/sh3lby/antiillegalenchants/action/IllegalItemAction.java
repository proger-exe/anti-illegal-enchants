package dev.sh3lby.antiillegalenchants.action;

public enum IllegalItemAction {
    DELETE,
    STRIP,
    REPLACE,
    LOG_ONLY;

    public static IllegalItemAction fromString(String value) {
        if (value == null) {
            return DELETE;
        }
        try {
            return IllegalItemAction.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return DELETE;
        }
    }
}
