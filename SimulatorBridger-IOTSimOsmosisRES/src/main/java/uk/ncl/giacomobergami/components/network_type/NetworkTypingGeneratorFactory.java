package uk.ncl.giacomobergami.components.network_type;


import uk.ncl.giacomobergami.utils.design_patterns.ReflectiveFactoryMethod;
import java.util.function.Supplier;

public class NetworkTypingGeneratorFactory {


    public static networkTyping generateFacade(String clazzPath) {

        int type = switch (clazzPath) {
            case "uk.ncl.giacomobergami.components.network_type.networkTyping.cellular_3G":
                yield 1;
            case "uk.ncl.giacomobergami.components.network_type.networkTyping.cellular_4G":
                yield 2;
            case "uk.ncl.giacomobergami.components.network_type.networkTyping.cellular_5G":
                yield 3;
            default:
                yield 0;
        };

        return switch (type) {
            case 1 -> ReflectiveFactoryMethod
                    .getInstance(networkTyping.class)
                    .generateFacade(clazzPath, cellular_3G::new);
            case 2 -> ReflectiveFactoryMethod
                    .getInstance(networkTyping.class)
                    .generateFacade(clazzPath, cellular_4G::new);
            case 3 -> ReflectiveFactoryMethod
                    .getInstance(networkTyping.class)
                    .generateFacade(clazzPath, cellular_5G::new);
            default -> ReflectiveFactoryMethod
                    .getInstance(networkTyping.class)
                    .generateFacade(clazzPath, wifi::new);
        };
    }
}
