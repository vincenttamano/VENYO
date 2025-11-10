package Venyo_ver2;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.awt.event.ActionListener;
import java.util.Date;
import java.util.LinkedList;
import java.util.Scanner;

public class Booking {
    private int bookingId;
    private Venue venue;
    private Date date;
    private static timeSlot timeSlot;
    private String paymentStatus;
    private String bookingStatus;
    private String purpose;
    private LinkedList<Amenity> amenities;
    private static LinkedList<Booking> bookings = new LinkedList<>();


    public Booking(int bookingId, Venue venue, Date date, timeSlot timeSlot, String paymentStatus, String bookingStatus, String purpose) {
        this.bookingId = bookingId; // passed in
        this.venue = venue;
        this.date = date;
        this.timeSlot = timeSlot;
        this.paymentStatus = paymentStatus;
        this.bookingStatus = bookingStatus;
        this.purpose = purpose;
        this.amenities = new LinkedList<>();
    }

    public int getBookingId() { return bookingId; }
    public Venue getVenue() { return venue; }
    public void setVenue(Venue venue) { this.venue = venue; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public timeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(timeSlot timeSlot) { this.timeSlot = timeSlot; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public LinkedList<Amenity> getAmenities() { return amenities; }



    public static void createBooking(Scanner input) {
        MongoDatabase database = MongoDb.getDatabase();
        MongoCollection<Document> collection = database.getCollection("bookings");

        System.out.println("Creating Booking");

        Venue.displayAvailableVenues();
        System.out.print("Select Venue ID: ");
        int venueId = Integer.parseInt(input.nextLine());
        Venue selectedVenue = Venue.getVenue(venueId);
        if (selectedVenue == null) {
            System.out.println("Invalid Venue ID. Booking cancelled.");
            return;
        }
        selectedVenue.setAvailability(false);

        collection.updateOne(
                new Document("venueId", selectedVenue.getVenueId()),
                new Document("$set", new Document("availability", false))
        );

        Amenity.displayAmenities();
        LinkedList<Amenity> selectedAmenities = new LinkedList<>();
        while (true) {
            System.out.print("Select Amenity ID (0 to stop): ");
            int amenityId = Integer.parseInt(input.nextLine());
            if (amenityId == 0) break;
            Amenity amenity = Amenity.getAmenity(amenityId);
            if (amenity != null) {
                selectedAmenities.add(amenity);
                System.out.println("Added: " + amenity.getName());
            } else {
                System.out.println("Amenity not found.");
            }
        }

        System.out.print("Purpose: ");
        String purpose = input.nextLine();

        // Time slot
        timeSlot slot = null;
        while (slot == null) {
            System.out.println("Select Time Slot:");
            System.out.println("1. AM");
            System.out.println("2. PM");
            System.out.print("Choice: ");
            String choice = input.nextLine();
            if (choice.equals("1")) slot = timeSlot.AM;
            else if (choice.equals("2")) slot = timeSlot.PM;
            else System.out.println("Invalid input.");
        }

        int maxId = 0;
        Document lastBooking = collection.find().sort(new Document("bookingId", -1)).first();
        if (lastBooking != null) maxId = lastBooking.getInteger("bookingId");

        Booking newBooking = new Booking(maxId + 1, selectedVenue, new Date(), slot, "Pending", "Booked", purpose);
        newBooking.getAmenities().addAll(selectedAmenities);

        LinkedList<String> amenityNames = new LinkedList<>();
        for (Amenity a : selectedAmenities) amenityNames.add(a.getName());

        Document bookingDoc = new Document("bookingId", newBooking.getBookingId())
                .append("venueName", selectedVenue.getName())
                .append("date", newBooking.getDate().toString())
                .append("timeSlot", slot.toString())
                .append("paymentStatus", newBooking.getPaymentStatus())
                .append("bookingStatus", newBooking.getBookingStatus())
                .append("purpose", purpose)
                .append("amenities", amenityNames);

        collection.insertOne(bookingDoc);

        System.out.println("Booking created successfully! ID: " + newBooking.getBookingId());
    }


    public static void displayBookings() {
        MongoDatabase database = MongoDb.getDatabase();
        MongoCollection<Document> collection = database.getCollection("bookings");

        if (collection.countDocuments() == 0) {
            System.out.println("No bookings found!");
        } else {
            for (Document doc : collection.find()) {
                System.out.println("Booking ID: " + doc.getInteger("bookingId"));
                System.out.println("Venue: " + doc.getString("venueName"));

                if (doc.containsKey("amenities")) {
                    System.out.print("Amenities: ");
                    for (Object a : doc.getList("amenities", Object.class)) {
                        System.out.print(a + " ");
                    }
                    System.out.println();
                } else {
                    System.out.println("Amenities: None");
                }

                System.out.println("Date: " + doc.getString("date"));
                System.out.println("Time Slot: " + doc.getString("timeSlot"));
                System.out.println("Payment Status: " + doc.getString("paymentStatus"));
                System.out.println("Booking Status: " + doc.getString("bookingStatus"));
                System.out.println("Purpose: " + doc.getString("purpose"));
                System.out.println("---------------------------");
            }
        }
    }


}
