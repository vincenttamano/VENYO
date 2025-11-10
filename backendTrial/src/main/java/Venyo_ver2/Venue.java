package Venyo_ver2;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class Venue {
    private static int counter = 1; // static counter for auto-increment
    private int venueId;
    private String name;
    private String description;
    private int capacity;
    private boolean availability;
    private String location;

    public Venue(String name, String description, int capacity, boolean availability, String location) {
        this.venueId = counter++;
        this.name = name;
        this.description = description;
        this.capacity = capacity;
        this.availability = availability;
        this.location = location;
    }

    public int getVenueId() {
        return venueId;
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

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isAvailability() {
        return availability;
    }

    public void setAvailability(boolean availability) {
        this.availability = availability;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public static void displayAvailableVenues() {
        MongoDatabase database = MongoDb.getDatabase();
        MongoCollection<Document> collection = database.getCollection("venues");

        System.out.println("Available Venues:");
        for (Document doc : collection.find(new Document("availability", true))) {
            System.out.println("ID: " + doc.getInteger("venueId"));
            System.out.println("Name: " + doc.getString("name"));
            System.out.println("Description: " + doc.getString("description"));
            System.out.println("Capacity: " + doc.getInteger("capacity"));
            System.out.println("Location: " + doc.getString("location"));
            System.out.println("---------------------------");
        }
    }


    public static void displayAllVenues() {
        MongoDatabase database = MongoDb.getDatabase();
        MongoCollection<Document> collection = database.getCollection("venues");

        System.out.println("All Venues:");
        System.out.println("Available Venues:");
        for (Document doc : collection.find(new Document("availability", true))) {
            System.out.println("ID: " + doc.getInteger("venueId"));
            System.out.println("Name: " + doc.getString("name"));
            System.out.println("Description: " + doc.getString("description"));
            System.out.println("Capacity: " + doc.getInteger("capacity"));
            String availability = doc.getBoolean("availability") ? "Available" : "Booked";
            System.out.println("Availability: " + availability);
            System.out.println("Location: " + doc.getString("location"));
            System.out.println("---------------------------");
        }
        System.out.println("Booked Venues");
        for (Document doc : collection.find(new Document("availability", false))) {
            System.out.println("ID: " + doc.getInteger("venueId"));
            System.out.println("Name: " + doc.getString("name"));
            System.out.println("Description: " + doc.getString("description"));
            System.out.println("Capacity: " + doc.getInteger("capacity"));
            String availability = doc.getBoolean("availability") ? "Available" : "Booked";
            System.out.println("Availability: " + availability);
            System.out.println("Location: " + doc.getString("location"));
            System.out.println("---------------------------");
        }
    }

    public static Venue getVenue(int venueId) {
        MongoDatabase database = MongoDb.getDatabase();
        MongoCollection<Document> collection = database.getCollection("venues");
        Document doc = collection.find(new Document("venueId", venueId)).first();
        if (doc != null) {
            return new Venue(
                    doc.getString("name"),
                    doc.getString("description"),
                    doc.getInteger("capacity"),
                    doc.getBoolean("availability"),
                    doc.getString("location")
            );
        }
        System.out.println("Venue not found!");
        return null;
    }
}