package Venyo_ver2;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class Amenity {
    private static int counter = 1; // starts at 1
    private int amenityId;
    private String name;
    private String description;
    private int quantity;

    public Amenity(String name, String description, int quantity) {
        this.amenityId = counter++; // auto-increment ID
        this.name = name;
        this.description = description;
        this.quantity = quantity;
    }

    public int getAmenityId() {
        return amenityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public static void displayAmenities() {
        MongoDatabase database = MongoDb.getDatabase();
        MongoCollection<Document> collection = database.getCollection("amenities");

        for (Document doc : collection.find()) {
            System.out.println("ID: " + doc.getInteger("amenityId"));
            System.out.println("Name: " + doc.getString("name"));
            System.out.println("Description: " + doc.getString("description"));
            System.out.println("---------------------------");
        }
    }

    public static Amenity getAmenity(int amenityId) {
        MongoDatabase database = MongoDb.getDatabase();
        MongoCollection<Document> collection = database.getCollection("amenities");
        Document doc = collection.find(new Document("amenityId", amenityId)).first();
        if (doc != null) {
            return new Amenity(
                    doc.getString("name"),
                    doc.getString("description"),
                    doc.getInteger("quantity")
            );
        }
        System.out.println("Amenity not found!");
        return null;
    }


}
