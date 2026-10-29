package com.example.demodatn.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;

/** Also masks database exception text, which can contain a rejected identity number. */
public class IdentityLogMask extends CompositeConverter<ILoggingEvent> {
    @Override public String transform(ILoggingEvent event,String text) {
        return text.replaceAll("(?<![0-9])[0-9]{8}([0-9]{4})(?![0-9])","***$1");
    }
}
