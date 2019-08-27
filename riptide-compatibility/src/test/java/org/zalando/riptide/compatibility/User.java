package org.zalando.riptide.compatibility;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class User {
    private String name;
    private String birthday;
}
