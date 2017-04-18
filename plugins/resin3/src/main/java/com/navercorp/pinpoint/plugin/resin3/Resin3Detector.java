package com.navercorp.pinpoint.plugin.resin3;

import com.navercorp.pinpoint.bootstrap.plugin.ApplicationTypeDetector;
import com.navercorp.pinpoint.bootstrap.resolver.ConditionProvider;
import com.navercorp.pinpoint.common.trace.ServiceType;

/**
 * @author hamlet-lee
 */

public class Resin3Detector implements ApplicationTypeDetector {

    private static final String REQUIRED_MAIN_CLASS = "com.caucho.server.resin.Resin";

    private static final String REQUIRED_SYSTEM_PROPERTY = "resin.home";

    private static final String REQUIRED_CLASS = "com.caucho.server.resin.Resin";

    public ServiceType getApplicationType() {
        return Resin3Constants.RESIN3;
    }

    public boolean detect(ConditionProvider provider) {
        return provider.checkMainClass(REQUIRED_MAIN_CLASS) &&
                provider.checkSystemProperty(REQUIRED_SYSTEM_PROPERTY) &&
                provider.checkForClass(REQUIRED_CLASS);
    }
}

