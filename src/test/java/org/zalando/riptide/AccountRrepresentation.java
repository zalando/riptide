package org.zalando.riptide;

// TODO find better name
final class AccountRrepresentation {
    
    private final String id;
    private final String name;

    private AccountRrepresentation(String id, String name) {
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
