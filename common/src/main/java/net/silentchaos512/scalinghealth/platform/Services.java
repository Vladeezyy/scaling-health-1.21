package net.silentchaos512.scalinghealth.platform;

import java.util.ServiceLoader;

/** Finds the loader's {@link IPlatform} (META-INF/services in the neoforge and fabric jars). */
public final class Services {
    public static final IPlatform PLATFORM = ServiceLoader.load(IPlatform.class, Services.class.getClassLoader())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No Scaling Health platform implementation"));

    private Services() {}
}
