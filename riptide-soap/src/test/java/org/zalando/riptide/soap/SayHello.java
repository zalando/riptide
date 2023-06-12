package org.zalando.riptide.soap;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement(name = "sayHello", namespace = "http://soap.riptide.zalando.org/")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SayHello {

    private String name;

}
