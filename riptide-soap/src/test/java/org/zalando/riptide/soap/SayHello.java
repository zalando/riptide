package org.zalando.riptide.soap;

import lombok.*;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "sayHello", namespace = "http://soap.riptide.zalando.org/")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SayHello {

    private String name;

}
