/*
 * This file is generated by jOOQ.
 */
package uk.ncl.giacomobergami.utils.database.jooq.tables;


import java.util.Collection;

import org.jooq.Condition;
import org.jooq.Field;
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
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import uk.ncl.giacomobergami.utils.database.jooq.Public;
import uk.ncl.giacomobergami.utils.database.jooq.tables.records.VehinformationImportRecord;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class VehinformationImport extends TableImpl<VehinformationImportRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.vehinformation_import</code>
     */
    public static final VehinformationImport VEHINFORMATION_IMPORT = new VehinformationImport();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<VehinformationImportRecord> getRecordType() {
        return VehinformationImportRecord.class;
    }

    /**
     * The column <code>public.vehinformation_import.angle</code>.
     */
    public final TableField<VehinformationImportRecord, Double> ANGLE = createField(DSL.name("angle"), SQLDataType.DOUBLE.nullable(false), this, "");

    /**
     * The column <code>public.vehinformation_import.vehicle_id</code>.
     */
    public final TableField<VehinformationImportRecord, String> VEHICLE_ID = createField(DSL.name("vehicle_id"), SQLDataType.VARCHAR(50).nullable(false), this, "");

    /**
     * The column <code>public.vehinformation_import.lane</code>.
     */
    public final TableField<VehinformationImportRecord, String> LANE = createField(DSL.name("lane"), SQLDataType.VARCHAR(50).nullable(false), this, "");

    /**
     * The column <code>public.vehinformation_import.pos</code>.
     */
    public final TableField<VehinformationImportRecord, Double> POS = createField(DSL.name("pos"), SQLDataType.DOUBLE.nullable(false), this, "");

    /**
     * The column <code>public.vehinformation_import.simtime</code>.
     */
    public final TableField<VehinformationImportRecord, Double> SIMTIME = createField(DSL.name("simtime"), SQLDataType.DOUBLE.nullable(false), this, "");

    /**
     * The column <code>public.vehinformation_import.slope</code>.
     */
    public final TableField<VehinformationImportRecord, Double> SLOPE = createField(DSL.name("slope"), SQLDataType.DOUBLE.nullable(false), this, "");

    /**
     * The column <code>public.vehinformation_import.speed</code>.
     */
    public final TableField<VehinformationImportRecord, Double> SPEED = createField(DSL.name("speed"), SQLDataType.DOUBLE.nullable(false), this, "");

    /**
     * The column <code>public.vehinformation_import.vehicle_type</code>.
     */
    public final TableField<VehinformationImportRecord, String> VEHICLE_TYPE = createField(DSL.name("vehicle_type"), SQLDataType.VARCHAR(50).nullable(false), this, "");

    /**
     * The column <code>public.vehinformation_import.x</code>.
     */
    public final TableField<VehinformationImportRecord, Double> X = createField(DSL.name("x"), SQLDataType.DOUBLE.nullable(false), this, "");

    /**
     * The column <code>public.vehinformation_import.y</code>.
     */
    public final TableField<VehinformationImportRecord, Double> Y = createField(DSL.name("y"), SQLDataType.DOUBLE.nullable(false), this, "");

    private VehinformationImport(Name alias, Table<VehinformationImportRecord> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private VehinformationImport(Name alias, Table<VehinformationImportRecord> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>public.vehinformation_import</code> table
     * reference
     */
    public VehinformationImport(String alias) {
        this(DSL.name(alias), VEHINFORMATION_IMPORT);
    }

    /**
     * Create an aliased <code>public.vehinformation_import</code> table
     * reference
     */
    public VehinformationImport(Name alias) {
        this(alias, VEHINFORMATION_IMPORT);
    }

    /**
     * Create a <code>public.vehinformation_import</code> table reference
     */
    public VehinformationImport() {
        this(DSL.name("vehinformation_import"), null);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public VehinformationImport as(String alias) {
        return new VehinformationImport(DSL.name(alias), this);
    }

    @Override
    public VehinformationImport as(Name alias) {
        return new VehinformationImport(alias, this);
    }

    @Override
    public VehinformationImport as(Table<?> alias) {
        return new VehinformationImport(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public VehinformationImport rename(String name) {
        return new VehinformationImport(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public VehinformationImport rename(Name name) {
        return new VehinformationImport(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public VehinformationImport rename(Table<?> name) {
        return new VehinformationImport(name.getQualifiedName(), null);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public VehinformationImport where(Condition condition) {
        return new VehinformationImport(getQualifiedName(), aliased() ? this : null, null, condition);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public VehinformationImport where(Collection<? extends Condition> conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public VehinformationImport where(Condition... conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public VehinformationImport where(Field<Boolean> condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public VehinformationImport where(SQL condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public VehinformationImport where(@Stringly.SQL String condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public VehinformationImport where(@Stringly.SQL String condition, Object... binds) {
        return where(DSL.condition(condition, binds));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public VehinformationImport where(@Stringly.SQL String condition, QueryPart... parts) {
        return where(DSL.condition(condition, parts));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public VehinformationImport whereExists(Select<?> select) {
        return where(DSL.exists(select));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public VehinformationImport whereNotExists(Select<?> select) {
        return where(DSL.notExists(select));
    }
}