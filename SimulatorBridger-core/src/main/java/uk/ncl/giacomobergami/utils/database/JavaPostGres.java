package uk.ncl.giacomobergami.utils.database;

import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
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
            stmt = conn.prepareStatement("DELETE FROM " + tableName + ";");
            int rs = stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static DSLContext getDSLContext (Connection conn) {
        DSLContext context =  DSL.using(conn, SQLDialect.POSTGRES);
        return context;
    }

   /*    PreparedStatement stmt;
            try {
        stmt = conn.prepareStatement("SELECT * FROM accounts");
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }

    ResultSet rs;
            try {
        rs = stmt.executeQuery();
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }

            while (true) {
        try {
            if (!rs.next()) break;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            System.out.printf("id:%d username %s password:%s%n", rs.getLong("user_id"), rs.getString("username"), rs.getString("password"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }*/
}
