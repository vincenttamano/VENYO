package Venyo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.LinkedList;

public class Amenity {
    private String name;
    private String description;
    private int quantity;

    public static LinkedList<Amenity> amenityList = new LinkedList<>();

    public Amenity(String name, String description, int quantity) {
        this.name = name;
        this.description = description;
        this.quantity = quantity;
    }

    public static void loadAmenities() {
        amenityList.clear();
        MongoDatabase db = DatabaseConnection.getDatabase("amenities");
        MongoCollection<Document> collection = db.getCollection("amenity");

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Amenity a = new Amenity(
                        doc.getString("name"),
                        doc.getString("description"),
                        doc.getInteger("quantity")
                );
                amenityList.add(a);
            }
        }
    }

    public static void displayAmenities() {
        System.out.println("\n--- Available Amenities ---");
        for (int i = 0; i < amenityList.size(); i++) {
            Amenity a = amenityList.get(i);
            System.out.println((i + 1) + ". " + a.name + " (x" + a.quantity + ")");
        }
    }

    public String getName() {
        return name;
    }

    public static Amenity findByName(String name) {
        for (Amenity a : amenityList) {
            if (a.getName().equalsIgnoreCase(name)) return a;
        }
        return null;
    }


}
