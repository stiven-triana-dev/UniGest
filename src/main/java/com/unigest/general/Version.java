package com.unigest.general;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;

public final class Version {
    private Version() {
    }

    public static String de(ObjectProvider<BuildProperties> compilacion) {
        BuildProperties datos = compilacion.getIfAvailable();
        return datos != null && datos.getVersion() != null ? datos.getVersion() : "desarrollo";
    }
}
