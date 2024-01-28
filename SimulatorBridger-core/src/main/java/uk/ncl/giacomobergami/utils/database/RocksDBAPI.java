package uk.ncl.giacomobergami.utils.database;


public class RocksDBAPI {

    private final KeyValueRepository<String, String> rocksDB;

    public RocksDBAPI(KeyValueRepository<String, String> rocksDB) {
        this.rocksDB = rocksDB;
    }

    public void save(String key, String value) {
        rocksDB.save(key, value);
    }


    public void find(String key) {
        String result = rocksDB.find(key);
    }

    public void delete(String key) {
        rocksDB.delete(key);
    }
}
