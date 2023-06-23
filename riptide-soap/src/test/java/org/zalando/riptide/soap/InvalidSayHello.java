package org.zalando.riptide.soap;

import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlType
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvalidSayHello {

    private Object name;

}
