package uk.ncl.giacomobergami.SumoOsmosisBridger.network_generators.from_traffic_data;

import uk.ncl.giacomobergami.utils.data.YAML;
import uk.ncl.giacomobergami.utils.pipeline_confs.TrafficConfiguration;
import uk.ncl.giacomobergami.utils.structures.ImmutablePair;
import uk.ncl.giacomobergami.utils.structures.MutablePair;

import java.io.File;
import java.util.*;

public class TimeTicker {

    private final double begin;
    private final double end;
    private final double step;
    public static List<ImmutablePair<Double, Double>> traffic_simulation_ticks;
    private static final File converter_file = new File("clean_example/converter.yaml");
    protected static final Optional<TrafficConfiguration> time_conf = YAML.parse(TrafficConfiguration.class, converter_file);

    public TimeTicker(double begin,
                      double step,
                      double end) {
        traffic_simulation_ticks = new ArrayList<>((int)Math.ceil((end-begin)/step));
        if(time_conf.get().getIsBatch()) {
            begin = time_conf.get().getBatchStart();
            end = time_conf.get().getBatchEnd();
        }
        this.begin = Math.ceil(begin / step) * step;
        this.end = end;
        this.step = step;
        double current = this.begin;
        /*TreeMap<Double, Set<Set<String>>> subnets_in_time;
        subnets_in_time = gson.fromJson(reader1, sccType);
        HashMap<Set<Set<String>>, TreeSet<Double>> elements = new HashMap<>();
        subnets_in_time.forEach((x,y)-> elements.computeIfAbsent(y, sets -> new TreeSet<>()).add(x));
        subnets_in_time.clear();*/
        /*while (current + step < end) {
            current = (double) Math.round((current) * 1000) / 1000;
            double next = (double) Math.round((current + step) * 1000) / 1000;
            traffic_simulation_ticks.add(new ImmutablePair<>(current, next));
            current += step;
        }*/
    }

    public static void addToTSTicks(Object[] tem) {
        var temp = 5;
        for(int i = 0; i < tem.length -1; i++) {
            double current = (double) tem[i];
            double next = (double) tem[i+1];
            traffic_simulation_ticks.add(new ImmutablePair<>(current, next));
        }
    }

    public List<ImmutablePair<Double, Double>> getChron() {
        return traffic_simulation_ticks;
    }

    private List<Integer> scatteredIntervals(double low, double high) {
        if (low > high)
            return scatteredIntervals(high, low);
        List<Integer> ls = new ArrayList<>();
        boolean findMin = true;

        for (int i = 0, traffic_simulation_ticksSize = traffic_simulation_ticks.size(); i < traffic_simulation_ticksSize; i++) {
            ImmutablePair<Double, Double> pair = traffic_simulation_ticks.get(i);
            if (findMin) {
                if (pair.getLeft() <= low) {
                    if (pair.getRight() > low) {
                        ls.add(i);
                        findMin = false;
                    }
                }
            } else {
                if (pair.getLeft() >= high)
                    return ls;
                if (high < pair.getLeft())
                    return ls;
                else if (high <= pair.getRight()) {
                    ls.add(i);
                    return ls;
                } else
                    ls.add(i);
            }
        }
        return ls;
    }

    public boolean fitsAll(double low, double high) {
        return scatteredIntervals(low, high).size() == traffic_simulation_ticks.size();
    }

    public ImmutablePair<Double, Double> reconstructIntervals(double low, double high) {
        var ls = scatteredIntervals(low, high);
        if (ls.isEmpty())
            return null;
        else
            //return new ImmutablePair<>(low, high);
            return new ImmutablePair<>(traffic_simulation_ticks.get(ls.get(0)).getLeft(), traffic_simulation_ticks.get(ls.get(ls.size()-1)).getRight());
    }

    public static List<MutablePair<Double, Double>> mergeSUMOIntervals(List<ImmutablePair<Double, Double>> ls) {
        if (ls.size() <= 1) {
            var result = new ArrayList<MutablePair<Double, Double>>();
            result.add(new MutablePair<>(ls.get(0).getLeft(), ls.get(0).getRight()));
            return result;
        }
        else {
            List<MutablePair<Double, Double>> results  = new ArrayList<>();
            int i = 0;
            while (i<ls.size()) {
                var current = new MutablePair<>(ls.get(i).getLeft(), ls.get(i).getRight());
                i++;
                while ((i<ls.size()) && (ls.get(i).getLeft().equals(current.getRight()))) {
                    current.setValue(ls.get(i++).getRight());
                }
                results.add(current);
            }
            return results;
        }
    }

    public static List<MutablePair<Double, Double>> mergeDfTIntervals(List<ImmutablePair<Double, Double>> ls) {
        if (ls.isEmpty()) {
            return new ArrayList<>();
        } else if (ls.size() == 1) {
            var result = new ArrayList<MutablePair<Double, Double>>();
            var pair = ls.get(0);
            if (pair != null) {
                result.add(new MutablePair<>(pair.getLeft(), pair.getRight()));
            }
            return result;
        } else {
            List<MutablePair<Double, Double>> results = new ArrayList<>();
            int i = 0;
            while (i < ls.size()) {
                var currentPair = ls.get(i);
                if (currentPair != null) {
                    var current = new MutablePair<>(currentPair.getLeft(), currentPair.getRight());
                    i++;
                    while ((i < ls.size()) && (ls.get(i) != null) && (ls.get(i).getLeft().equals(current.getRight()))) {
                        current.setValue(ls.get(i).getRight());
                        i++;
                    }
                    results.add(current);
                } else {
                    i++;
                }
            }
            return results;
        }
    }

    public double getBegin() {
        return begin;
    }
    public double getEnd() {
        return end;
    }
    public double getStep() {
        return step;
    }
}
