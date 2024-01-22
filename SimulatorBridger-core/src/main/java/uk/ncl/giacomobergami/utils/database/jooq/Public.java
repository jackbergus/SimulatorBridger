/*
 * This file is generated by jOOQ.
 */
package uk.ncl.giacomobergami.utils.database.jooq;


import java.util.Arrays;
import java.util.List;

import org.jooq.Catalog;
import org.jooq.Sequence;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

import uk.ncl.giacomobergami.utils.database.jooq.tables.Accuratebatteryinfo;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Applist;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Bandwidthshareinfo;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Connectionpersimtime;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Datacenterenergyconsumption;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Historyentry;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Hostpowerconsumption;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Neighbourschange;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Osmoticappsstats;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Overallappresults;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Powerutilisationhistory;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Rsuinformation;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Switchpowerconsumption;
import uk.ncl.giacomobergami.utils.database.jooq.tables.TimedScc;
import uk.ncl.giacomobergami.utils.database.jooq.tables.Vehinformation;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Public extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public</code>
     */
    public static final Public PUBLIC = new Public();

    /**
     * The table <code>public.accuratebatteryinfo</code>.
     */
    public final Accuratebatteryinfo ACCURATEBATTERYINFO = Accuratebatteryinfo.ACCURATEBATTERYINFO;

    /**
     * The table <code>public.applist</code>.
     */
    public final Applist APPLIST = Applist.APPLIST;

    /**
     * The table <code>public.bandwidthshareinfo</code>.
     */
    public final Bandwidthshareinfo BANDWIDTHSHAREINFO = Bandwidthshareinfo.BANDWIDTHSHAREINFO;

    /**
     * The table <code>public.connectionpersimtime</code>.
     */
    public final Connectionpersimtime CONNECTIONPERSIMTIME = Connectionpersimtime.CONNECTIONPERSIMTIME;

    /**
     * The table <code>public.datacenterenergyconsumption</code>.
     */
    public final Datacenterenergyconsumption DATACENTERENERGYCONSUMPTION = Datacenterenergyconsumption.DATACENTERENERGYCONSUMPTION;

    /**
     * The table <code>public.historyentry</code>.
     */
    public final Historyentry HISTORYENTRY = Historyentry.HISTORYENTRY;

    /**
     * The table <code>public.hostpowerconsumption</code>.
     */
    public final Hostpowerconsumption HOSTPOWERCONSUMPTION = Hostpowerconsumption.HOSTPOWERCONSUMPTION;

    /**
     * The table <code>public.neighbourschange</code>.
     */
    public final Neighbourschange NEIGHBOURSCHANGE = Neighbourschange.NEIGHBOURSCHANGE;

    /**
     * The table <code>public.osmoticappsstats</code>.
     */
    public final Osmoticappsstats OSMOTICAPPSSTATS = Osmoticappsstats.OSMOTICAPPSSTATS;

    /**
     * The table <code>public.overallappresults</code>.
     */
    public final Overallappresults OVERALLAPPRESULTS = Overallappresults.OVERALLAPPRESULTS;

    /**
     * The table <code>public.powerutilisationhistory</code>.
     */
    public final Powerutilisationhistory POWERUTILISATIONHISTORY = Powerutilisationhistory.POWERUTILISATIONHISTORY;

    /**
     * The table <code>public.rsuinformation</code>.
     */
    public final Rsuinformation RSUINFORMATION = Rsuinformation.RSUINFORMATION;

    /**
     * The table <code>public.switchpowerconsumption</code>.
     */
    public final Switchpowerconsumption SWITCHPOWERCONSUMPTION = Switchpowerconsumption.SWITCHPOWERCONSUMPTION;

    /**
     * The table <code>public.timed_scc</code>.
     */
    public final TimedScc TIMED_SCC = TimedScc.TIMED_SCC;

    /**
     * The table <code>public.vehinformation</code>.
     */
    public final Vehinformation VEHINFORMATION = Vehinformation.VEHINFORMATION;

    /**
     * No further instances allowed
     */
    private Public() {
        super("public", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Sequence<?>> getSequences() {
        return Arrays.asList(
            Sequences.DYNAMICINFORMATION_DI_ENTRY_ID_SEQ
        );
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            Accuratebatteryinfo.ACCURATEBATTERYINFO,
            Applist.APPLIST,
            Bandwidthshareinfo.BANDWIDTHSHAREINFO,
            Connectionpersimtime.CONNECTIONPERSIMTIME,
            Datacenterenergyconsumption.DATACENTERENERGYCONSUMPTION,
            Historyentry.HISTORYENTRY,
            Hostpowerconsumption.HOSTPOWERCONSUMPTION,
            Neighbourschange.NEIGHBOURSCHANGE,
            Osmoticappsstats.OSMOTICAPPSSTATS,
            Overallappresults.OVERALLAPPRESULTS,
            Powerutilisationhistory.POWERUTILISATIONHISTORY,
            Rsuinformation.RSUINFORMATION,
            Switchpowerconsumption.SWITCHPOWERCONSUMPTION,
            TimedScc.TIMED_SCC,
            Vehinformation.VEHINFORMATION
        );
    }
}
