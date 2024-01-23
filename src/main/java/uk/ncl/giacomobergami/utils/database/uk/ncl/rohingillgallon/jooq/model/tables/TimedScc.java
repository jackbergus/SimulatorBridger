/*
 * This file is generated by jOOQ.
 */
package uk.ncl.rohingillgallon.jooq.model.tables;


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

import uk.ncl.rohingillgallon.jooq.model.Keys;
import uk.ncl.rohingillgallon.jooq.model.Public;
import uk.ncl.rohingillgallon.jooq.model.tables.records.TimedSccRecord;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class TimedScc extends TableImpl<TimedSccRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.timed_scc</code>
     */
    public static final TimedScc TIMED_SCC = new TimedScc();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<TimedSccRecord> getRecordType() {
        return TimedSccRecord.class;
    }

    /**
     * The column <code>public.timed_scc.unique_entry_id</code>.
     */
    public final TableField<TimedSccRecord, Integer> UNIQUE_ENTRY_ID = createField(DSL.name("unique_entry_id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>public.timed_scc.time_of_update</code>.
     */
    public final TableField<TimedSccRecord, Double> TIME_OF_UPDATE = createField(DSL.name("time_of_update"), SQLDataType.DOUBLE.nullable(false), this, "");

    /**
     * The column <code>public.timed_scc.networkneighbours1</code>.
     */
    public final TableField<TimedSccRecord, String> NETWORKNEIGHBOURS1 = createField(DSL.name("networkneighbours1"), SQLDataType.VARCHAR(50), this, "");

    /**
     * The column <code>public.timed_scc.networkneighbours2</code>.
     */
    public final TableField<TimedSccRecord, String> NETWORKNEIGHBOURS2 = createField(DSL.name("networkneighbours2"), SQLDataType.VARCHAR(50), this, "");

    /**
     * The column <code>public.timed_scc.networkneighbours3</code>.
     */
    public final TableField<TimedSccRecord, String> NETWORKNEIGHBOURS3 = createField(DSL.name("networkneighbours3"), SQLDataType.VARCHAR(50), this, "");

    /**
     * The column <code>public.timed_scc.networkneighbours4</code>.
     */
    public final TableField<TimedSccRecord, String> NETWORKNEIGHBOURS4 = createField(DSL.name("networkneighbours4"), SQLDataType.VARCHAR(50), this, "");

    private TimedScc(Name alias, Table<TimedSccRecord> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private TimedScc(Name alias, Table<TimedSccRecord> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>public.timed_scc</code> table reference
     */
    public TimedScc(String alias) {
        this(DSL.name(alias), TIMED_SCC);
    }

    /**
     * Create an aliased <code>public.timed_scc</code> table reference
     */
    public TimedScc(Name alias) {
        this(alias, TIMED_SCC);
    }

    /**
     * Create a <code>public.timed_scc</code> table reference
     */
    public TimedScc() {
        this(DSL.name("timed_scc"), null);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public Identity<TimedSccRecord, Integer> getIdentity() {
        return (Identity<TimedSccRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<TimedSccRecord> getPrimaryKey() {
        return Keys.TIMED_SCC_PKEY;
    }

    @Override
    public TimedScc as(String alias) {
        return new TimedScc(DSL.name(alias), this);
    }

    @Override
    public TimedScc as(Name alias) {
        return new TimedScc(alias, this);
    }

    @Override
    public TimedScc as(Table<?> alias) {
        return new TimedScc(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public TimedScc rename(String name) {
        return new TimedScc(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public TimedScc rename(Name name) {
        return new TimedScc(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public TimedScc rename(Table<?> name) {
        return new TimedScc(name.getQualifiedName(), null);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public TimedScc where(Condition condition) {
        return new TimedScc(getQualifiedName(), aliased() ? this : null, null, condition);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public TimedScc where(Collection<? extends Condition> conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public TimedScc where(Condition... conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public TimedScc where(Field<Boolean> condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public TimedScc where(SQL condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public TimedScc where(@Stringly.SQL String condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public TimedScc where(@Stringly.SQL String condition, Object... binds) {
        return where(DSL.condition(condition, binds));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public TimedScc where(@Stringly.SQL String condition, QueryPart... parts) {
        return where(DSL.condition(condition, parts));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public TimedScc whereExists(Select<?> select) {
        return where(DSL.exists(select));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public TimedScc whereNotExists(Select<?> select) {
        return where(DSL.notExists(select));
    }
}