package uk.ncl.giacomobergami.components.routing_algorithm;

import uk.ncl.giacomobergami.components.network_type.networkTyping;
import uk.ncl.giacomobergami.components.network_type.wifi;
import uk.ncl.giacomobergami.utils.design_patterns.ReflectiveFactoryMethod;

public class RoutingAlgorithmGeneratorFactory {
    public static routing_algorithms generateFacade(String clazzPath) {
        return ReflectiveFactoryMethod
                .getInstance(routing_algorithms.class)
                .generateFacade(clazzPath, SPMB::new);
    }
}
