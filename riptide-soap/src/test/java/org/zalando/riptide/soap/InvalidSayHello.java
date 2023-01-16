package org.zalando.riptide.soap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.XmlType;

@XmlType
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvalidSayHello {

    private Object name;

}
