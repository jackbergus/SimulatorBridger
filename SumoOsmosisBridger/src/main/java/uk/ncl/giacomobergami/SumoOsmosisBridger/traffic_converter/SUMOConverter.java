package uk.ncl.giacomobergami.SumoOsmosisBridger.traffic_converter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import uk.ncl.giacomobergami.components.iot.IoTEntityGenerator;
import uk.ncl.giacomobergami.components.network_type.NetworkTypingGeneratorFactory;
import uk.ncl.giacomobergami.traffic_converter.abstracted.TrafficConverter;
import uk.ncl.giacomobergami.traffic_orchestrator.rsu_network.netgen.NetworkGenerator;
import uk.ncl.giacomobergami.traffic_orchestrator.rsu_network.netgen.NetworkGeneratorFactory;
import uk.ncl.giacomobergami.traffic_orchestrator.rsu_network.rsu.RSUUpdater;
import uk.ncl.giacomobergami.traffic_orchestrator.rsu_network.rsu.RSUUpdaterFactory;
import uk.ncl.giacomobergami.utils.data.GZip;
import uk.ncl.giacomobergami.utils.data.XPathUtil;
import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;
import uk.ncl.giacomobergami.utils.shared_data.edge.TimedEdge;
import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;
import uk.ncl.giacomobergami.utils.structures.StraightforwardAdjacencyList;

import javax.xml.parsers.*;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class SUMOConverter extends TrafficConverter {
    private final NetworkGenerator netGen;
    private final RSUUpdater rsuUpdater;
    private SUMOConfiguration concreteConf;
    private final DocumentBuilderFactory dbf;
    private DocumentBuilder db;
    List<Double> temporalOrdering;
    Document networkFile;
    StraightforwardAdjacencyList<String> connectionPath;
    HashMap<Double, List<TimedIoT>> timedIoTDevices;
    HashSet<TimedEdge> roadSideUnits;
    private static Logger logger = LogManager.getRootLogger();

    public SUMOConverter(TrafficConfiguration conf)  {
        super(conf);
        dbf = DocumentBuilderFactory.newInstance();
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            db = null;
        }
        concreteConf = YAML.parse(SUMOConfiguration.class, new File(conf.YAMLConverterConfiguration)).orElseThrow();
        temporalOrdering = new ArrayList<>();
        networkFile = null;
        timedIoTDevices = new HashMap<>();
        roadSideUnits = new HashSet<>();
        netGen = NetworkGeneratorFactory.generateFacade(concreteConf.generateRSUAdjacencyList);
        rsuUpdater = RSUUpdaterFactory.generateFacade(concreteConf.updateRSUFields,
                                                      concreteConf.default_rsu_communication_radius,
                                                      concreteConf.default_max_vehicle_communication);
        connectionPath = new StraightforwardAdjacencyList<>();
    }

    @Override
    protected boolean initReadSimulatorOutput() {
        connectionPath.clear();
        temporalOrdering.clear();
        timedIoTDevices.clear();
        networkFile = null;

        File file = new File(concreteConf.sumo_configuration_file_path);
        Document configurationFile = null;
        try {
            configurationFile = db.parse(file);
        } catch (SAXException | IOException e) {
            e.printStackTrace();
            return false;
        }
        File network_python = null;
        try {
            network_python = Paths.get(file.getParent(), XPathUtil.evaluate(configurationFile, "/configuration/input/net-file/@value"))
                    .toFile();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            return false;
        }
        if (!network_python.exists()) {
            logger.fatal("ERR: file " + network_python.getAbsolutePath() + " from " + file.getAbsolutePath() + " does not exists!");
            System.exit(1);
        } else if (network_python.getAbsolutePath().endsWith(".gz")) {
            String ap = network_python.getAbsolutePath();
            ap = ap.substring(0, ap.lastIndexOf('.'));
            try {
                GZip.decompressGzip(network_python.toPath(), new File(ap).toPath());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            network_python = new File(ap);
        }
        logger.trace("Loading the traffic light information...");
        try {
            networkFile = db.parse(network_python);
        } catch (SAXException | IOException e) {
            e.printStackTrace();
            return false;
        }

        File trajectory_python = new File(concreteConf.trace_file);
        if (!trajectory_python.exists()) {
            logger.error("ERROR: sumo has not built the trace file: " + trajectory_python.getAbsolutePath());
            return false;
        }

        logger.trace("Loading the vehicle information...");
        Document trace_document = null;
        try {
            trace_document = db.parse(trajectory_python);
        } catch (SAXException | IOException e) {
            e.printStackTrace();
            return false;
        }


        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser;
        try {
            saxParser = factory.newSAXParser();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
        try {
            saxParser.parse(trajectory_python, new SUMODataParser(temporalOrdering, timedIoTDevices));
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        temporalOrdering = SUMODataParser.getTemporalOrdering();
        timedIoTDevices = SUMODataParser.getTimedIoTDevices();

       /* NodeList timestamp_eval;
        try {
            timestamp_eval = XPathUtil.evaluateNodeList(trace_document, "/fcd-export/timestep");
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            return false;
        }

        for (int i = 0, N = timestamp_eval.getLength(); i<N; i++) {
            var curr = timestamp_eval.item(i);
            double currTime = Double.parseDouble(curr.getAttributes().getNamedItem("time").getTextContent());
            temporalOrdering.add(currTime);
            var ls = new ArrayList<TimedIoT>();
            timedIoTDevices.put(currTime, ls);
            var tag = timestamp_eval.item(i).getChildNodes();
            for (int j = 0, M = tag.getLength(); j < M; j++) {
                var veh = tag.item(j);
                if (veh.getNodeType() == Node.ELEMENT_NODE) {
                    assert (Objects.equals(veh.getNodeName(), "vehicle"));
                    var attrs = veh.getAttributes();
                    TimedIoT rec = new TimedIoT();
                    rec.angle = Double.parseDouble(attrs.getNamedItem("angle").getTextContent());
                    rec.x = Double.parseDouble(attrs.getNamedItem("x").getTextContent());
                    rec.y = Double.parseDouble(attrs.getNamedItem("y").getTextContent());
                    rec.speed = Double.parseDouble(attrs.getNamedItem("speed").getTextContent());
                    rec.pos = Double.parseDouble(attrs.getNamedItem("pos").getTextContent());
                    rec.slope = Double.parseDouble(attrs.getNamedItem("slope").getTextContent());
                    rec.id = (attrs.getNamedItem("id").getTextContent());
                    rec.type = (attrs.getNamedItem("type").getTextContent());
                    rec.lane = (attrs.getNamedItem("lane").getTextContent());
                    rec.simtime = currTime;
                    ls.add(rec);
                }
            }
        }*/

        NodeList traffic_lights = null;
        try {
            traffic_lights = XPathUtil.evaluateNodeList(networkFile, "/net/junction[@type='traffic_light']");
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            return false;
        }
        for (int i = 0, N = traffic_lights.getLength(); i<N; i++) {
            var curr = traffic_lights.item(i).getAttributes();
            var rsu = new TimedEdge(curr.getNamedItem("id").getTextContent(),
                    Double.parseDouble(curr.getNamedItem("x").getTextContent()),
                    Double.parseDouble(curr.getNamedItem("y").getTextContent()),
                    concreteConf.default_rsu_communication_radius,
                    concreteConf.default_max_vehicle_communication, 0);
            rsuUpdater.accept(rsu);
            roadSideUnits.add(rsu);
        }
        connectionPath.clear();
        var tmp = netGen.apply(roadSideUnits);
        tmp.forEach((k, v) -> {
            connectionPath.put(k.id, v.id);
        });
        return true;
    }

    @Override
    protected List<Double> getSimulationTimeUnits() {
        return temporalOrdering;
    }

    @Override
    protected Collection<TimedIoT> getTimedIoT(Double tick) {
        return timedIoTDevices.get(tick);
    }

    protected HashMap<Double, List<TimedIoT>> getAllTimedIoT() {
        return timedIoTDevices;
    }

    @Override
    protected StraightforwardAdjacencyList<String> getTimedEdgeNetwork(Double tick) {
        return connectionPath;
    }

    @Override
    protected HashSet<TimedEdge> getTimedEdgeNodes(Double tick) {
        return roadSideUnits.stream().map(x -> {
            var ls = x.copy();
            ls.setSimtime(tick);
            return ls;
        }).collect(Collectors.toCollection(HashSet<TimedEdge>::new));
    }

    @Override
    protected void endReadSimulatorOutput() {
        temporalOrdering.clear();
        timedIoTDevices.clear();
        networkFile = null;
        connectionPath.clear();
    }

    @Override
    public boolean runSimulator(long begin,
                             long end,
                             double step) {
        var conf1 = YAML.parse(IoTEntityGenerator.IoTGlobalConfiguration.class, new File("clean_example/3_extIOTSim_configuration/iot_generators.yaml")).orElseThrow();
        var conf2 = YAML.parse(SUMOConfiguration.class, new File("clean_example/sumo.yaml")).orElseThrow();

        var latency = conf1.networkType.equals("custom") ? conf1.latency: NetworkTypingGeneratorFactory.generateFacade(conf1.networkType).getNTLat();
        step = conf1.match ?  latency : step;

        var detectorsPath = conf2.getSumo_detectors_file_path();
        var vTypesPath = conf2.getSumo_vTypes_file_path();
        var cfgFile = conf2.getSumo_configuration_file_path();
        var pyPath = conf2.getPython_filepath();
        var lcm  = 1.59; // this is the lowest common multiple of the latency for 3G, 4G and 5G, or 0.212, 0.075 and 0.001
        var last = conf1.match ? lcm : step;
        var path = pyPath + ' ' + conf1.stepSizeEditorPath + ' ' + detectorsPath + ' ' + vTypesPath + ' ' + cfgFile + ' ' + last;

        try {
            Runtime.getRuntime().exec(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (new File(concreteConf.trace_file).exists()) {
            logger.info("Skipping the sumo running: the trace_file already exists");
            return true;
        }
        File fout = new File(concreteConf.logger_file);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fout);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(concreteConf.sumo_program, "-c", concreteConf.sumo_configuration_file_path, "--begin", Long.toString(begin), "--end", Long.toString(end), "--step-length", Double.toString(step), "--fcd-output", concreteConf.trace_file);
        try {
            Process process = processBuilder.start();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                bw.write(line);
                bw.newLine();
            }
            int exitCode = process.waitFor();
            bw.write("\nExited with error code : ");
            bw.write(exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
