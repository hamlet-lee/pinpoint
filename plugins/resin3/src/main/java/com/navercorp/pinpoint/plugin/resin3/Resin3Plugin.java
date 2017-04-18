package com.navercorp.pinpoint.plugin.resin3;

import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;

/**
 * @author hamlet-lee
 */
public class Resin3Plugin  implements ProfilerPlugin, TransformTemplateAware {
    TransformTemplate transformTemplate;
    public void setup(ProfilerPluginSetupContext context) {
        Resin3Configuration config = new Resin3Configuration(context.getConfig());

    }

    public void setTransformTemplate(TransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }
}
