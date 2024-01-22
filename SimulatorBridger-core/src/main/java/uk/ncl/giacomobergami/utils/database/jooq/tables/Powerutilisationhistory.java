/*
 * This file is generated by jOOQ.
 */
package uk.ncl.giacomobergami.utils.database.jooq.tables;


import java.util.Collection;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.PlainSQL;
import org.jooq.QueryPart;
import org.jooq.SQL;
import org.jooq.Schema;
import org.jooq.Select;
import org.jooq.Stringly;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import uk.ncl.giacomobergami.utils.database.jooq.Keys;
import uk.ncl.giacomobergami.utils.database.jooq.Public;
import uk.ncl.giacomobergami.utils.database.jooq.tables.records.PowerutilisationhistoryRecord;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Powerutilisationhistory extends TableImpl<PowerutilisationhistoryRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.powerutilisationhistory</code>
     */
    public static final Powerutilisationhistory POWERUTILISATIONHISTORY = new Powerutilisationhistory();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<PowerutilisationhistoryRecord> getRecordType() {
        return PowerutilisationhistoryRecord.class;
    }

    /**
     * The column <code>public.powerutilisationhistory.unique_entry_id</code>.
     */
    public final TableField<PowerutilisationhistoryRecord, Integer> UNIQUE_ENTRY_ID = createField(DSL.name("unique_entry_id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>public.powerutilisationhistory.dcname</code>.
     */
    public final TableField<PowerutilisationhistoryRecord, String> DCNAME = createField(DSL.name("dcname"), SQLDataType.VARCHAR(50).nullable(false), this, "");

    /**
     * The column <code>public.powerutilisationhistory.puh_name</code>.
     */
    public final TableField<PowerutilisationhistoryRecord, String> PUH_NAME = createField(DSL.name("puh_name"), SQLDataType.VARCHAR(50).nullable(false), this, "");

    /**
     * The column <code>public.powerutilisationhistory.starttime</code>.
     */
    public final TableField<PowerutilisationhistoryRecord, Double> STARTTIME = createField(DSL.name("starttime"), SQLDataType.DOUBLE.nullable(false), this, "");

    /**
     * The column <code>public.powerutilisationhistory.usedmips</code>.
     */
    public final TableField<PowerutilisationhistoryRecord, Double> USEDMIPS = createField(DSL.name("usedmips"), SQLDataType.DOUBLE.nullable(false), this, "");

    private Powerutilisationhistory(Name alias, Table<PowerutilisationhistoryRecord> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private Powerutilisationhistory(Name alias, Table<PowerutilisationhistoryRecord> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>public.powerutilisationhistory</code> table
     * reference
     */
    public Powerutilisationhistory(String alias) {
        this(DSL.name(alias), POWERUTILISATIONHISTORY);
    }

    /**
     * Create an aliased <code>public.powerutilisationhistory</code> table
     * reference
     */
    public Powerutilisationhistory(Name alias) {
        this(alias, POWERUTILISATIONHISTORY);
    }

    /**
     * Create a <code>public.powerutilisationhistory</code> table reference
     */
    public Powerutilisationhistory() {
        this(DSL.name("powerutilisationhistory"), null);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public Identity<PowerutilisationhistoryRecord, Integer> getIdentity() {
        return (Identity<PowerutilisationhistoryRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<PowerutilisationhistoryRecord> getPrimaryKey() {
        return Keys.POWERUTILISATIONHISTORY_PKEY;
    }

    @Override
    public Powerutilisationhistory as(String alias) {
        return new Powerutilisationhistory(DSL.name(alias), this);
    }

    @Override
    public Powerutilisationhistory as(Name alias) {
        return new Powerutilisationhistory(alias, this);
    }

    @Override
    public Powerutilisationhistory as(Table<?> alias) {
        return new Powerutilisationhistory(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Powerutilisationhistory rename(String name) {
        return new Powerutilisationhistory(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Powerutilisationhistory rename(Name name) {
        return new Powerutilisationhistory(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Powerutilisationhistory rename(Table<?> name) {
        return new Powerutilisationhistory(name.getQualifiedName(), null);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public Powerutilisationhistory where(Condition condition) {
        return new Powerutilisationhistory(getQualifiedName(), aliased() ? this : null, null, condition);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public Powerutilisationhistory where(Collection<? extends Condition> conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public Powerutilisationhistory where(Condition... conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public Powerutilisationhistory where(Field<Boolean> condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public Powerutilisationhistory where(SQL condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public Powerutilisationhistory where(@Stringly.SQL String condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public Powerutilisationhistory where(@Stringly.SQL String condition, Object... binds) {
        return where(DSL.condition(condition, binds));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public Powerutilisationhistory where(@Stringly.SQL String condition, QueryPart... parts) {
        return where(DSL.condition(condition, parts));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public Powerutilisationhistory whereExists(Select<?> select) {
        return where(DSL.exists(select));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public Powerutilisationhistory whereNotExists(Select<?> select) {
        return where(DSL.notExists(select));
    }
}
