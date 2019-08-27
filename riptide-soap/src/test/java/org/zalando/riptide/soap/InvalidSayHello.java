package org.zalando.riptide.soap;

import lombok.*;

import javax.xml.bind.annotation.*;

@XmlType
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvalidSayHello {

    private Object name;

}
