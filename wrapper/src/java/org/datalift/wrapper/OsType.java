package org.datalift.wrapper;

public enum OsType {
    MacOS("Mac OS"),
    Windows("Windows"),
    Other("");

    public static OsType CURRENT_OS = fromName(System.getProperty("os.name"));

    private final String prefix;

    OsType(String prefix) {
        this.prefix = prefix;
    }

    public static OsType fromName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name");
        }
        OsType found = Other;
        for (OsType e : OsType.values()) {
            if (name.startsWith(e.prefix)) {
                found = e;
                break;
            }
        }
        return found;
    }
}
