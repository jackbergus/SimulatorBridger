package org.cloudbus.osmosis.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class OsmoticRoutingTable implements Serializable {
    Map<String, String> table;

    public OsmoticRoutingTable() {
        table = new HashMap<>();
    }

    public String getRule(String melName){
        if (table.containsKey(melName)){
            return table.get(melName);
        } else {
            return melName;
        }
    }

    public void addRule(String abstractMel, String instanceMel){
        table.put(abstractMel, instanceMel);
    }

    public void removeRule(String abstractMel, String instanceMel){
        if (table.containsKey(abstractMel)){
            if (instanceMel.equals(table.get(abstractMel))){
                table.remove(abstractMel);
            }
        }
    }

    public void removeRules(String abstractMel){
        if (table.containsKey(abstractMel)){
            table.remove(abstractMel);
        }
    }

    public void clear(){
        table.clear();
    }

}
