/*
 * This file is generated by jOOQ.
 */
package uk.ncl.giacomobergami.utils.database.jooq.tables.records;


import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;

import uk.ncl.giacomobergami.utils.database.jooq.tables.Applist;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ApplistRecord extends UpdatableRecordImpl<ApplistRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.applist.unique_entry_id</code>.
     */
    public void setUniqueEntryId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.applist.unique_entry_id</code>.
     */
    public Integer getUniqueEntryId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>public.applist.appid</code>.
     */
    public void setAppid(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.applist.appid</code>.
     */
    public Integer getAppid() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>public.applist.appname</code>.
     */
    public void setAppname(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.applist.appname</code>.
     */
    public String getAppname() {
        return (String) get(2);
    }

    /**
     * Setter for <code>public.applist.appstarttime</code>.
     */
    public void setAppstarttime(Double value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.applist.appstarttime</code>.
     */
    public Double getAppstarttime() {
        return (Double) get(3);
    }

    /**
     * Setter for <code>public.applist.clouddatacentername</code>.
     */
    public void setClouddatacentername(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>public.applist.clouddatacentername</code>.
     */
    public String getClouddatacentername() {
        return (String) get(4);
    }

    /**
     * Setter for <code>public.applist.clouddcid</code>.
     */
    public void setClouddcid(Integer value) {
        set(5, value);
    }

    /**
     * Getter for <code>public.applist.clouddcid</code>.
     */
    public Integer getClouddcid() {
        return (Integer) get(5);
    }

    /**
     * Setter for <code>public.applist.datarate</code>.
     */
    public void setDatarate(Double value) {
        set(6, value);
    }

    /**
     * Getter for <code>public.applist.datarate</code>.
     */
    public Double getDatarate() {
        return (Double) get(6);
    }

    /**
     * Setter for <code>public.applist.edgedatacentername</code>.
     */
    public void setEdgedatacentername(String value) {
        set(7, value);
    }

    /**
     * Getter for <code>public.applist.edgedatacentername</code>.
     */
    public String getEdgedatacentername() {
        return (String) get(7);
    }

    /**
     * Setter for <code>public.applist.edgedcid</code>.
     */
    public void setEdgedcid(Integer value) {
        set(8, value);
    }

    /**
     * Getter for <code>public.applist.edgedcid</code>.
     */
    public Integer getEdgedcid() {
        return (Integer) get(8);
    }

    /**
     * Setter for <code>public.applist.edgeletlist</code>.
     */
    public void setEdgeletlist(String value) {
        set(9, value);
    }

    /**
     * Getter for <code>public.applist.edgeletlist</code>.
     */
    public String getEdgeletlist() {
        return (String) get(9);
    }

    /**
     * Setter for <code>public.applist.endtime</code>.
     */
    public void setEndtime(Double value) {
        set(10, value);
    }

    /**
     * Getter for <code>public.applist.endtime</code>.
     */
    public Double getEndtime() {
        return (Double) get(10);
    }

    /**
     * Setter for <code>public.applist.iotdevicebatteryconsumption</code>.
     */
    public void setIotdevicebatteryconsumption(Double value) {
        set(11, value);
    }

    /**
     * Getter for <code>public.applist.iotdevicebatteryconsumption</code>.
     */
    public Double getIotdevicebatteryconsumption() {
        return (Double) get(11);
    }

    /**
     * Setter for <code>public.applist.iotdevicebatterystatus</code>.
     */
    public void setIotdevicebatterystatus(String value) {
        set(12, value);
    }

    /**
     * Getter for <code>public.applist.iotdevicebatterystatus</code>.
     */
    public String getIotdevicebatterystatus() {
        return (String) get(12);
    }

    /**
     * Setter for <code>public.applist.iotdeviceid</code>.
     */
    public void setIotdeviceid(Integer value) {
        set(13, value);
    }

    /**
     * Getter for <code>public.applist.iotdeviceid</code>.
     */
    public Integer getIotdeviceid() {
        return (Integer) get(13);
    }

    /**
     * Setter for <code>public.applist.iotdevicename</code>.
     */
    public void setIotdevicename(String value) {
        set(14, value);
    }

    /**
     * Getter for <code>public.applist.iotdevicename</code>.
     */
    public String getIotdevicename() {
        return (String) get(14);
    }

    /**
     * Setter for <code>public.applist.iotdeviceoutputsize</code>.
     */
    public void setIotdeviceoutputsize(Integer value) {
        set(15, value);
    }

    /**
     * Getter for <code>public.applist.iotdeviceoutputsize</code>.
     */
    public Integer getIotdeviceoutputsize() {
        return (Integer) get(15);
    }

    /**
     * Setter for <code>public.applist.isiotdevicedied</code>.
     */
    public void setIsiotdevicedied(String value) {
        set(16, value);
    }

    /**
     * Getter for <code>public.applist.isiotdevicedied</code>.
     */
    public String getIsiotdevicedied() {
        return (String) get(16);
    }

    /**
     * Setter for <code>public.applist.melid</code>.
     */
    public void setMelid(Integer value) {
        set(17, value);
    }

    /**
     * Getter for <code>public.applist.melid</code>.
     */
    public Integer getMelid() {
        return (Integer) get(17);
    }

    /**
     * Setter for <code>public.applist.melname</code>.
     */
    public void setMelname(String value) {
        set(18, value);
    }

    /**
     * Getter for <code>public.applist.melname</code>.
     */
    public String getMelname() {
        return (String) get(18);
    }

    /**
     * Setter for <code>public.applist.meloutputsize</code>.
     */
    public void setMeloutputsize(Integer value) {
        set(19, value);
    }

    /**
     * Getter for <code>public.applist.meloutputsize</code>.
     */
    public Integer getMeloutputsize() {
        return (Integer) get(19);
    }

    /**
     * Setter for <code>public.applist.osmesiscloudletsize</code>.
     */
    public void setOsmesiscloudletsize(Integer value) {
        set(20, value);
    }

    /**
     * Getter for <code>public.applist.osmesiscloudletsize</code>.
     */
    public Integer getOsmesiscloudletsize() {
        return (Integer) get(20);
    }

    /**
     * Setter for <code>public.applist.osmesisedgeletsize</code>.
     */
    public void setOsmesisedgeletsize(Integer value) {
        set(21, value);
    }

    /**
     * Getter for <code>public.applist.osmesisedgeletsize</code>.
     */
    public Integer getOsmesisedgeletsize() {
        return (Integer) get(21);
    }

    /**
     * Setter for <code>public.applist.startdatagenerationtime</code>.
     */
    public void setStartdatagenerationtime(Double value) {
        set(22, value);
    }

    /**
     * Getter for <code>public.applist.startdatagenerationtime</code>.
     */
    public Double getStartdatagenerationtime() {
        return (Double) get(22);
    }

    /**
     * Setter for <code>public.applist.stopdatagenerationtime</code>.
     */
    public void setStopdatagenerationtime(Double value) {
        set(23, value);
    }

    /**
     * Getter for <code>public.applist.stopdatagenerationtime</code>.
     */
    public Double getStopdatagenerationtime() {
        return (Double) get(23);
    }

    /**
     * Setter for <code>public.applist.vmcloudid</code>.
     */
    public void setVmcloudid(Integer value) {
        set(24, value);
    }

    /**
     * Getter for <code>public.applist.vmcloudid</code>.
     */
    public Integer getVmcloudid() {
        return (Integer) get(24);
    }

    /**
     * Setter for <code>public.applist.vmname</code>.
     */
    public void setVmname(String value) {
        set(25, value);
    }

    /**
     * Getter for <code>public.applist.vmname</code>.
     */
    public String getVmname() {
        return (String) get(25);
    }

    /**
     * Setter for <code>public.applist.workflowid</code>.
     */
    public void setWorkflowid(Integer value) {
        set(26, value);
    }

    /**
     * Getter for <code>public.applist.workflowid</code>.
     */
    public Integer getWorkflowid() {
        return (Integer) get(26);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ApplistRecord
     */
    public ApplistRecord() {
        super(Applist.APPLIST);
    }

    /**
     * Create a detached, initialised ApplistRecord
     */
    public ApplistRecord(Integer uniqueEntryId, Integer appid, String appname, Double appstarttime, String clouddatacentername, Integer clouddcid, Double datarate, String edgedatacentername, Integer edgedcid, String edgeletlist, Double endtime, Double iotdevicebatteryconsumption, String iotdevicebatterystatus, Integer iotdeviceid, String iotdevicename, Integer iotdeviceoutputsize, String isiotdevicedied, Integer melid, String melname, Integer meloutputsize, Integer osmesiscloudletsize, Integer osmesisedgeletsize, Double startdatagenerationtime, Double stopdatagenerationtime, Integer vmcloudid, String vmname, Integer workflowid) {
        super(Applist.APPLIST);

        setUniqueEntryId(uniqueEntryId);
        setAppid(appid);
        setAppname(appname);
        setAppstarttime(appstarttime);
        setClouddatacentername(clouddatacentername);
        setClouddcid(clouddcid);
        setDatarate(datarate);
        setEdgedatacentername(edgedatacentername);
        setEdgedcid(edgedcid);
        setEdgeletlist(edgeletlist);
        setEndtime(endtime);
        setIotdevicebatteryconsumption(iotdevicebatteryconsumption);
        setIotdevicebatterystatus(iotdevicebatterystatus);
        setIotdeviceid(iotdeviceid);
        setIotdevicename(iotdevicename);
        setIotdeviceoutputsize(iotdeviceoutputsize);
        setIsiotdevicedied(isiotdevicedied);
        setMelid(melid);
        setMelname(melname);
        setMeloutputsize(meloutputsize);
        setOsmesiscloudletsize(osmesiscloudletsize);
        setOsmesisedgeletsize(osmesisedgeletsize);
        setStartdatagenerationtime(startdatagenerationtime);
        setStopdatagenerationtime(stopdatagenerationtime);
        setVmcloudid(vmcloudid);
        setVmname(vmname);
        setWorkflowid(workflowid);
        resetChangedOnNotNull();
    }
}