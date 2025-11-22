package Venyo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.LinkedList;

public class Venue {
    private String name;
    private String description;
    private boolean isFree;
    private double price;
    private int capacity;
    private boolean availability;
    private String location;
    public static LinkedList<Venue> venueList = new LinkedList<>();

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFree(boolean free) {
        isFree = free;
    }

    public void setPrice(double price) {
        this.price = price;
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

    public void setLocation(String location) {
        this.location = location;
    }

    public static LinkedList<Venue> getVenueList() {
        return venueList;
    }

    public static void setVenueList(LinkedList<Venue> venueList) {
        Venue.venueList = venueList;
    }

    // ✅ --- GETTERS ---
    public String getName() {
        return name;
    }

    public boolean isFree() {
        return isFree;
    }

    public double getPrice() {
        return price;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public boolean isAvailable() {
        return availability;
    }

    public Venue(String name, String description, boolean isFree, double price,
                 int capacity, boolean availability, String location) {
        this.name = name;
        this.description = description;
        this.isFree = isFree;
        this.price = price;
        this.capacity = capacity;
        this.availability = availability;
        this.location = location;
    }

    public static void loadVenue() {
        venueList.clear();
        MongoDatabase db = DatabaseConnection.getDatabase("venues");
        MongoCollection<Document> collection = db.getCollection("venue");

        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                double price = 0.0;
                Object priceObj = doc.get("price");
                if (priceObj instanceof Number) {
                    price = ((Number) priceObj).doubleValue();
                }

                Venue venue = new Venue(
                        doc.getString("name"),
                        doc.getString("description"),
                        doc.getBoolean("isFree", false),
                        price,
                        doc.getInteger("capacity", 0),
                        doc.getBoolean("availability", true),
                        doc.getString("location")
                );
                venueList.add(venue);
            }
        }
    }


    public static void displayVenues() {

        System.out.println("\n--- Available Venues ---");
        boolean anyAvailable = false;
        for (int i = 0; i < venueList.size(); i++) {
            Venue v = venueList.get(i);
            if (v.isAvailable()) {
                System.out.println((i + 1) + ". " + v.name + " (" + v.location + ") - " +
                        (v.isFree ? "Free" : "₱" + v.price));
                anyAvailable = true;
            }

        }
        System.out.println("\n--- Booked Venues ---");
        for (int i = 0; i < venueList.size(); i++) {
            Venue v = venueList.get(i);
            if (!v.isAvailable()) {
                System.out.println((i + 1) + ". " + v.name + " (" + v.location + ") - " +
                        (v.isFree ? "Free" : "₱" + v.price));
                anyAvailable = false;
            }
        }


        if (!anyAvailable) {
            System.out.println("No venues available right now.");
        }
    }

    public void setAvailable(boolean available) {
        this.availability = available;

        MongoDatabase db = DatabaseConnection.getDatabase("venues");
        MongoCollection<Document> collection = db.getCollection("venue");
        collection.updateOne(
                new Document("name", this.name),
                new Document("$set", new Document("availability", available))
        );
    }

    public static Venue findByName(String name) {
        for (Venue v : venueList) {
            if (v.getName().equalsIgnoreCase(name)) return v;
        }
        return null;
    }
}
