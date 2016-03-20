package com.navercorp.pinpoint.plugin.metamx.netty.httpclient;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;

/**
 * @author hamlet-lee
 */
public class MetamxNettyHttpClientPluginConfig {

    private boolean profile = true;

    public MetamxNettyHttpClientPluginConfig(ProfilerConfig src) {
        this.profile = src.readBoolean("profiler.metamx.netty.httpclient", true);
    }

    public boolean isProfile() {
        return profile;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MetamxNettyHttpClientPluginConfig{");
        sb.append("profile=").append(profile);
        sb.append('}');
        return sb.toString();
    }
}
