package org.zalando.riptide;

// TODO find better name
final class AccountRepresentation {
    
    private final String id;
    private final String name;

    private AccountRepresentation(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
