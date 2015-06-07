package org.zalando.riptide;

final class Account {
    
    private final String id;
    private final String revision;
    private final String name;

    Account(String id, String revision, String name) {
        this.id = id;
        this.revision = revision;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getRevision() {
        return revision;
    }

    public String getName() {
        return name;
    }

}
