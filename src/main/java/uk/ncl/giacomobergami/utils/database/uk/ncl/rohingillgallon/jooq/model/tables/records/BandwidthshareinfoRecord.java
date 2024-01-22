/*
 * This file is generated by jOOQ.
 */
package uk.ncl.rohingillgallon.jooq.model.tables.records;


import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;

import uk.ncl.rohingillgallon.jooq.model.tables.Bandwidthshareinfo;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class BandwidthshareinfoRecord extends UpdatableRecordImpl<BandwidthshareinfoRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.bandwidthshareinfo.unique_entry_id</code>.
     */
    public void setUniqueEntryId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.bandwidthshareinfo.unique_entry_id</code>.
     */
    public Integer getUniqueEntryId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>public.bandwidthshareinfo.bandwidthshare</code>.
     */
    public void setBandwidthshare(Double value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.bandwidthshareinfo.bandwidthshare</code>.
     */
    public Double getBandwidthshare() {
        return (Double) get(1);
    }

    /**
     * Setter for <code>public.bandwidthshareinfo.channelid</code>.
     */
    public void setChannelid(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.bandwidthshareinfo.channelid</code>.
     */
    public String getChannelid() {
        return (String) get(2);
    }

    /**
     * Setter for <code>public.bandwidthshareinfo.edgename</code>.
     */
    public void setEdgename(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.bandwidthshareinfo.edgename</code>.
     */
    public String getEdgename() {
        return (String) get(3);
    }

    /**
     * Setter for <code>public.bandwidthshareinfo.melname</code>.
     */
    public void setMelname(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>public.bandwidthshareinfo.melname</code>.
     */
    public String getMelname() {
        return (String) get(4);
    }

    /**
     * Setter for <code>public.bandwidthshareinfo.timestamp</code>.
     */
    public void setTimestamp(Double value) {
        set(5, value);
    }

    /**
     * Getter for <code>public.bandwidthshareinfo.timestamp</code>.
     */
    public Double getTimestamp() {
        return (Double) get(5);
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
     * Create a detached BandwidthshareinfoRecord
     */
    public BandwidthshareinfoRecord() {
        super(Bandwidthshareinfo.BANDWIDTHSHAREINFO);
    }

    /**
     * Create a detached, initialised BandwidthshareinfoRecord
     */
    public BandwidthshareinfoRecord(Integer uniqueEntryId, Double bandwidthshare, String channelid, String edgename, String melname, Double timestamp) {
        super(Bandwidthshareinfo.BANDWIDTHSHAREINFO);

        setUniqueEntryId(uniqueEntryId);
        setBandwidthshare(bandwidthshare);
        setChannelid(channelid);
        setEdgename(edgename);
        setMelname(melname);
        setTimestamp(timestamp);
        resetChangedOnNotNull();
    }
}
