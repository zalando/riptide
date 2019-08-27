package org.zalando.riptide.soap;

import lombok.*;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "sayHelloResponse", namespace = "http://soap.riptide.zalando.org/")
@NoArgsConstructor
@AllArgsConstructor
public class SayHelloResponse {

    @XmlElement(name = "return")
    private String _return;

    public String getReturn() {
        return _return;
    }

    public void setReturn(final String _return) {
        this._return = _return;
    }

}
