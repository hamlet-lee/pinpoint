package com.navercorp.pinpoint.plugin.resin3;

import com.navercorp.pinpoint.common.trace.TraceMetadataProvider;
import com.navercorp.pinpoint.common.trace.TraceMetadataSetupContext;

/**
 * @author hamlet-lee
 */
public class Resin3TypeProvider  implements TraceMetadataProvider {
    public void setup(TraceMetadataSetupContext context) {
        context.addServiceType(Resin3Constants.RESIN3);
        context.addServiceType(Resin3Constants.RESIN3_METHOD);
    }
}
