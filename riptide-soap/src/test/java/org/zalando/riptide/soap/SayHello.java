package org.zalando.riptide.soap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "sayHello", namespace = "http://soap.riptide.zalando.org/")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SayHello {

    private String name;

}
