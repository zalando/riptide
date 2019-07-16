package org.zalando.riptide.autoconfigure;

import com.google.gag.annotation.remark.Hack;

/**
 * Constants referring to beans that need to be looked up by types during Riptide clients constructions.
 * <p>This is an artificial arrangement of 16 unicode characters, with its sole purpose
 * being to never match user-declared values.
 */
@Hack("To allow search of the parameters that need to be replaced with Bean references")
class ValueConstants {
    private ValueConstants() {}

    static final String METER_REGISTRY_REF = "\n\t\t\n\t\t\n\uE000\uE001\uE001\n\t\t\t\t\n";
    static final String TRACER_REF = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";
    static final String LOGBOOK_REF = "\n\t\t\n\t\t\n\uE000\uE001\uE003\n\t\t\t\t\n";
}
