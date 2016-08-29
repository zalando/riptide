package org.zalando.riptide.model;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 Zalando SE
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */

public final class Account {

    private final String id;
    private final String revision;
    private final String name;

    public Account(final String id, final String revision, final String name) {
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
