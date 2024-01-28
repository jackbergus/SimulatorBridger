package uk.ncl.giacomobergami.traffic_converter;

import org.jooq.DSLContext;
import uk.ncl.giacomobergami.traffic_converter.abstracted.TrafficConverter;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.design_patterns.ReflectiveFactoryMethod;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;

import java.io.File;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public class TrafficConverterRunner {

    private static Class<?> clazz;
    private static Constructor<? extends TrafficConverter> object;

    public static TrafficConverter generateFacade(TrafficConfiguration conf) {
        return ReflectiveFactoryMethod
                .getInstance(TrafficConverter.class)
                .generateFacade(conf.clazzPath,
                                () -> { System.exit(1); return null; },
                                conf);
    }

    public static void convert(String configuration, Connection conn, DSLContext context) {
        Optional<TrafficConfiguration> conf = YAML.parse(TrafficConfiguration.class, new File(configuration));
        conf.ifPresent(x -> {
            TrafficConverter conv = generateFacade(x);
            try {
                conv.run(conn, context);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void main(String[] args, Connection conn, DSLContext context) {
        String configuration = "converter.yaml";
        if (args.length > 0) {
            configuration = args[0];
        }
        convert(configuration, conn, context);
    }
}
