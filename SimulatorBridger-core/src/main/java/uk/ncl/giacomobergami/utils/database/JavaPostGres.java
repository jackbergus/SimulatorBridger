package uk.ncl.giacomobergami.utils.database;

import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;

public class JavaPostGres {
    public static DataSource createDataSource() {
        final String url = "jdbc:postgresql://localhost:5432/SimulatorBridger-VehicleData?user=postgres&password=gnomes";
        final PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(url);
        return dataSource;
    }

    public static Connection ConnectToSource(DataSource dataSource) {
        Connection conn = null;
        {
            try{
                conn = dataSource.getConnection();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return conn;
    }

    public static void DisconnectFromSource(Connection conn) {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static PreparedStatement StartINSERTtoTable(Connection conn, String addition) throws SQLException {
        String noValues = "(?";
        if(StringUtils.countMatches(addition, ',') > 1) {
            for (int i = 0; i < StringUtils.countMatches(addition, ','); i++) {
                noValues += ",?";
            }
        }
        noValues += ')';
        PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO " + addition + " VALUES " + noValues);
        return insertStmt;
    }

    public static int INSERTInt(PreparedStatement insertStmt, int index, int x) {
        try {
            insertStmt.setInt(index, x);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return index;
    }

    public static int INSERTDouble(PreparedStatement insertStmt, int index, double x) {
        try {
            insertStmt.setDouble(index, x);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return index;
    }

    public static int INSERTString(PreparedStatement insertStmt, int index, String x) {
        try {
            insertStmt.setString(index, x);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return index;
    }

    public static int INSERTTimestamp(PreparedStatement insertStmt, int index, Timestamp x) {
        try {
            insertStmt.setTimestamp(index, x);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return index;
    }

    public static boolean EndINSERTtoTable(PreparedStatement insertStmt) {
        try {
            insertStmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static int TABLEsize(Connection conn, String tableName) {
        Statement stmt;
        try {
            stmt = conn.createStatement();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        ResultSet rs;
        try {
            rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName + ";");
            rs.next();
            return  rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void emptyTABLE(Connection conn, String tableName) {
        PreparedStatement stmt;
        try {
            stmt = conn.prepareStatement("TRUNCATE " + tableName + " RESTART IDENTITY;");
            int rs = stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void indexVEHINFORMATION(Connection conn) {
        System.out.print("Starting indexing of vehInformation SQL table...\n");
        PreparedStatement stmt;
        try {
            stmt = conn.prepareStatement("DROP INDEX CONCURRENTLY IF EXISTS mysearchIndex; CREATE INDEX mysearchIndex ON vehInformation(simtime, vehicle_id, x, y);");
            int rs = stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        System.out.print("vehInformation SQL table indexing complete\n");
    }

    public static void indexAMBULANCEINFORMATION(Connection conn) {
        System.out.print("Starting indexing of ambulanceInformation SQL table...\n");
        PreparedStatement stmt;
        try {
            stmt = conn.prepareStatement("DROP INDEX CONCURRENTLY IF EXISTS ambulanceIndex; CREATE INDEX ambulanceIndex ON ambulanceInformation(simtime, vehicle_id, x, y);");
            int rs = stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        System.out.print("ambulanceInformation SQL table indexing complete\n");
    }

    public static void indexLINKSDATA(Connection conn) throws SQLException {
        System.out.print("Starting indexing of Links Data Information SQL table...\n");
        PreparedStatement stmt;

            stmt = conn.prepareStatement("DROP INDEX CONCURRENTLY IF EXISTS linksINDEX; CREATE INDEX linksINDEX ON sourceToDestLinks (topology_id, link_id, from_id, to_id);");
            int rs = stmt.executeUpdate();

        System.out.print("Links Data Information SQL table indexing complete\n");
    }



    public static DSLContext getDSLContext (Connection conn) {
        return DSL.using(conn, SQLDialect.POSTGRES);
    }

    public static void copyCSVDATA(Connection conn, String file, String origin) {

        origin = origin + "_import";

        CopyManager copyManager = null;
        try {
            copyManager = new CopyManager((BaseConnection) conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        FileReader fileReader;
        try {
            fileReader = new FileReader(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            copyManager.copyIn("COPY " + origin + " FROM stdin DELIMITER ',' CSV header", fileReader);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void transferDATABetweenTables(Connection conn, String dest, String params, String origin){
        PreparedStatement stmt;
        origin = origin + "_import";
        try {
            stmt = conn.prepareStatement("INSERT INTO " + dest + " SELECT " + params+ " FROM " + origin + ";");
            int rs = stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void INSERTLinkData(Connection conn, String dest, String values) {
        PreparedStatement stmt;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + dest + " VALUES " + values);
            int rs = stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateLinkBW(Connection conn, double newBW, double from, double to) {
        PreparedStatement stmt;
        try {
            stmt = conn.prepareStatement("UPDATE sourceToDestLinks SET bw =" + newBW + " WHERE from_id =" + from + " AND to_id =" + to);
            int rs = stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateLinkChannels(Connection conn, int change, int from, int to, int linkID, int topologyID) {
        PreparedStatement stmt;
        try {
            stmt = conn.prepareStatement("UPDATE sourceToDestLinks SET nochannels = nochannels + " + change + " WHERE from_id = "
                    + from + " AND to_id = " + to + " AND link_id = " + linkID + " AND topology_id = " + topologyID);
            int rs = stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
