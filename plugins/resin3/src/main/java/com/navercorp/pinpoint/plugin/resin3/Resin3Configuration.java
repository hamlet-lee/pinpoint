package com.navercorp.pinpoint.plugin.resin3;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;

/**
 * @author hamlet-lee
 */
public class Resin3Configuration {
    private final boolean resin3HidePinpointHeader;
    private final boolean profile;

    public Resin3Configuration(ProfilerConfig config) {
        this.resin3HidePinpointHeader = config.readBoolean("profiler.resin3.hidepinpointheader", true);
        this.profile = config.readBoolean("profiler.resin3", true);
    }

    public boolean isProfile(){
        return profile;
    }
    public boolean isResin3HidePinpointHeader() {
        return resin3HidePinpointHeader;
    }
}
