/*
 * This file is generated by jOOQ.
 */
package uk.ncl.rohingillgallon.jooq.model.tables.records;


import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;

import uk.ncl.rohingillgallon.jooq.model.tables.Switchpowerconsumption;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SwitchpowerconsumptionRecord extends UpdatableRecordImpl<SwitchpowerconsumptionRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.switchpowerconsumption.unique_entry_id</code>.
     */
    public void setUniqueEntryId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.switchpowerconsumption.unique_entry_id</code>.
     */
    public Integer getUniqueEntryId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>public.switchpowerconsumption.dcname</code>.
     */
    public void setDcname(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.switchpowerconsumption.dcname</code>.
     */
    public String getDcname() {
        return (String) get(1);
    }

    /**
     * Setter for <code>public.switchpowerconsumption.energy</code>.
     */
    public void setEnergy(Double value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.switchpowerconsumption.energy</code>.
     */
    public Double getEnergy() {
        return (Double) get(2);
    }

    /**
     * Setter for <code>public.switchpowerconsumption.spc_name</code>.
     */
    public void setSpcName(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>public.switchpowerconsumption.spc_name</code>.
     */
    public String getSpcName() {
        return (String) get(3);
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
     * Create a detached SwitchpowerconsumptionRecord
     */
    public SwitchpowerconsumptionRecord() {
        super(Switchpowerconsumption.SWITCHPOWERCONSUMPTION);
    }

    /**
     * Create a detached, initialised SwitchpowerconsumptionRecord
     */
    public SwitchpowerconsumptionRecord(Integer uniqueEntryId, String dcname, Double energy, String spcName) {
        super(Switchpowerconsumption.SWITCHPOWERCONSUMPTION);

        setUniqueEntryId(uniqueEntryId);
        setDcname(dcname);
        setEnergy(energy);
        setSpcName(spcName);
        resetChangedOnNotNull();
    }
}
