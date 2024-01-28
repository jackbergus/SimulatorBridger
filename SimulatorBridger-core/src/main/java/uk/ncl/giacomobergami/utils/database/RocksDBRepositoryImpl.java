package uk.ncl.giacomobergami.utils.database;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class RocksDBRepositoryImpl implements KeyValueRepository<String, String> {

    private final static String NAME = "first-db";
    File dbDir;
    RocksDB db;

    void initialize() {
        RocksDB.loadLibrary();
        final Options options = new Options();
        options.setCreateIfMissing(true);
        dbDir = new File("/tmp/rocks-db", NAME);
        try {
            try {
                Files.createDirectories(dbDir.getParentFile().toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Files.createDirectories(dbDir.getAbsoluteFile().toPath());
            db = RocksDB.open(options, dbDir.getAbsolutePath());
        } catch(IOException | RocksDBException ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public synchronized void save(String key, String value) {
        try {
            db.put(key.getBytes(), value.getBytes());
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String find(String key) {
        String result = null;
        try {
            byte[] bytes = db.get(key.getBytes());
            if(bytes == null) return null;
            result = new String(bytes);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public void delete(String key) {
        try {
            db.delete(key.getBytes());
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }
}