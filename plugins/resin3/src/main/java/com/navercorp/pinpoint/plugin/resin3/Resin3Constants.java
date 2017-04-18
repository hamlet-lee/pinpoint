package com.navercorp.pinpoint.plugin.resin3;

/**
 * @author hamlet-lee
 */

import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.trace.ServiceTypeFactory;

import static com.navercorp.pinpoint.common.trace.ServiceTypeProperty.*;

public interface Resin3Constants {
    public static final String TYPE_NAME = "RESIN3";
    public static final ServiceType RESIN3 = ServiceTypeFactory.of(1900, "RESIN3", "RESIN3", RECORD_STATISTICS);
    public static final ServiceType RESIN3_METHOD = ServiceTypeFactory.of(1901, "RESIN3_METHOD");
    public static final String METADATA_TRACE = "trace";
    public static final String METADATA_ASYNC = "async";
    public static final String ATTRIBUTE_PINPOINT_TRACE = "PINPOINT_TRACE";
    public static final String URL_UNKNOWN = "URL_UNKNOWN";
    public static final String FIELD_URI = "_uri";
    public static final String FIELD_URILENGTH = "_uriLength";
}