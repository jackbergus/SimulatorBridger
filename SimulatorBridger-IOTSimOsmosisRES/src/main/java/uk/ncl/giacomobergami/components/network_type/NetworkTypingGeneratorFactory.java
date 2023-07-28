package uk.ncl.giacomobergami.components.network_type;


import uk.ncl.giacomobergami.utils.design_patterns.ReflectiveFactoryMethod;
import java.util.function.Supplier;

public class NetworkTypingGeneratorFactory {
    public static networkTyping generateFacade(String clazzPath) {
        return ReflectiveFactoryMethod
                .getInstance(networkTyping.class)
                .generateFacade(clazzPath,  wifi::new);
    }
}
