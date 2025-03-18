package org.cloudbus.cloudsim.power.models;

import uk.ncl.giacomobergami.components.network_type.networkTyping;
import uk.ncl.giacomobergami.components.network_type.wifi;
import uk.ncl.giacomobergami.utils.design_patterns.ReflectiveFactoryMethod;
import java.util.function.Supplier;


public class PowerModelGeneratorFactory {
    public static PowerModel generateFacade(String clazzPath) {
        return ReflectiveFactoryMethod
                .getInstance(PowerModel.class)
                .generateFacade(clazzPath, (Supplier<PowerModel>) PowerModelSpecPowerHpProLiantMl110G3PentiumD930::new);
    }
}
