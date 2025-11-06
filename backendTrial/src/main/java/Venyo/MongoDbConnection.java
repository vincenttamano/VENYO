package Venyo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDbConnection {
    private static final String URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "venue";
    private static MongoDatabase database;

    public static MongoDatabase getDatabase() {
        if (database == null) {
            MongoClient client = MongoClients.create(URI);
            database = client.getDatabase(DB_NAME);
            System.out.println("✅ Connected to MongoDB!");
        }
        return database;
    }
}
