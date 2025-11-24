package Venyo_ver2;

import java.util.Scanner;
 

public class Customer extends User implements Payment {
     private String firstName;
    private String lastName;
    private String contactNumber;
    private String email;
    private String userType;

    public Customer(String username, String password, int UserID, String firstName, String lastName, String contactNumber, String email, String userType){
        super(username, password, UserID);
        this.firstName = firstName;
        this.lastName = lastName;
        this.contactNumber = contactNumber;
        this.email = email;
        this.userType = userType;
    }

    // getters and setters
    public String getFirstName(){
        return firstName;
    }

    public String getLastName(){
        return lastName;
    }

    public String getContactNumber(){
        return contactNumber;
    }

    public String getEmail(){
        return email;
    }

    public String getUserType(){
        return userType;
    }

    public void setFirstName(String firstName){
        this.firstName = firstName;
    }

    public void setLastName(String lastName){
        this.lastName = lastName;
    }

    public void setContactNumber(String contactNumber){
        this.contactNumber = contactNumber;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setUserType(String userType){
        this.userType = userType;
    }

    //Display the customer menu and dispatch to booking operations.
    public void userMenu(){
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- Customer Menu ---");
            System.out.println("1. Create Booking");
            System.out.println("2. Cancel Booking");
            System.out.println("3. View My Bookings");
            System.out.println("4. Check Booking Status");
            System.out.println("5. Pay Booking");
            System.out.println("6. Back / Logout");
            System.out.print("Choose: ");
            String c = sc.nextLine().trim();
            switch (c) {
                case "1": createBooking(); break;
                case "2": cancelBooking(); break;
                case "3": viewBookingDetails(); break;
                case "4": checkStatus(); break;
                case "5": payBooking(); break;
                case "6": return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    //Create a booking by selecting a venue and optional amenities.
    //The booking is stored as pending and amenities are stored by id.
    public void createBooking(){
        Scanner sc = new Scanner(System.in);
        Venue.displayAvailableVenues();
        System.out.print("Choose venue ID: ");
        int vid = Integer.parseInt(sc.nextLine().trim());
        Venue chosen = Venue.getVenue(vid);
        if (chosen == null) { System.out.println("Invalid venue."); return; }

        Amenity.displayAmenities();
        java.util.List<Integer> amenityIds = new java.util.ArrayList<>();
        while (true) {
            System.out.print("Enter Amenity ID to add (0 to stop): ");
            int aid = Integer.parseInt(sc.nextLine().trim());
            if (aid == 0) break;
            Amenity am = Amenity.getAmenity(aid);
            if (am != null) {
                amenityIds.add(aid);
                System.out.println("Added: " + am.getName());
            } else {
                System.out.println("Amenity not found.");
            }
        }

        System.out.print("Purpose: ");
        String purpose = sc.nextLine().trim();

        com.mongodb.client.MongoCollection<org.bson.Document> collection = MongoDb.getDatabase().getCollection("bookings");
        int maxId = 0;
        org.bson.Document last = collection.find().sort(new org.bson.Document("bookingId", -1)).first();
        if (last != null) maxId = last.getInteger("bookingId");

        int userId = this.getUserId();
        // fetch registered customer details from users collection
        org.bson.Document userDoc = MongoDb.getDatabase().getCollection("users").find(new org.bson.Document("userId", userId)).first();

        org.bson.Document bookedBy = new org.bson.Document("userId", userId)
            .append("username", this.getUsername())
            .append("firstName", userDoc != null ? userDoc.getString("firstName") : this.getFirstName())
            .append("lastName", userDoc != null ? userDoc.getString("lastName") : this.getLastName())
            .append("contactNumber", userDoc != null ? userDoc.getString("contactNumber") : this.getContactNumber())
            .append("email", userDoc != null ? userDoc.getString("email") : this.getEmail());

        org.bson.Document doc = new org.bson.Document("bookingId", maxId + 1)
            .append("venueId", chosen.getVenueId())
            .append("venueName", chosen.getName())
            .append("userId", userId)
            .append("bookedBy", bookedBy)
            .append("date", new java.util.Date().toString())
            .append("paymentStatus", "Pending")
            .append("bookingStatus", "Pending")
            .append("purpose", purpose)
            .append("amenities", amenityIds)
            .append("price", chosen.getPrice())
            .append("isFree", chosen.isFree());

        collection.insertOne(doc);

        // mark venue unavailable
        MongoDb.getDatabase().getCollection("venues").updateOne(new org.bson.Document("venueId", chosen.getVenueId()), new org.bson.Document("$set", new org.bson.Document("availability", false)));

        System.out.println("Booking created with ID: " + (maxId + 1));
    }

    //Cancel an existing booking owned by this customer.
    public void cancelBooking(){
        Scanner sc = new Scanner(System.in);
        System.out.print("Booking ID to cancel: ");
        int id = Integer.parseInt(sc.nextLine().trim());
        com.mongodb.client.MongoCollection<org.bson.Document> collection = MongoDb.getDatabase().getCollection("bookings");
        org.bson.Document doc = collection.find(new org.bson.Document("bookingId", id).append("userId", this.getUserId())).first();
        if (doc == null) { System.out.println("Booking not found or not your booking."); return; }

        collection.updateOne(new org.bson.Document("bookingId", id), new org.bson.Document("$set", new org.bson.Document("bookingStatus", "cancelled")));
        // free the venue
        int venueId = doc.getInteger("venueId");
        MongoDb.getDatabase().getCollection("venues").updateOne(new org.bson.Document("venueId", venueId), new org.bson.Document("$set", new org.bson.Document("availability", true)));
        System.out.println("Booking cancelled.");
    }

    //Display bookings that belong to this customer.
    public void viewBookingDetails(){
        com.mongodb.client.MongoCollection<org.bson.Document> collection = MongoDb.getDatabase().getCollection("bookings");
        boolean found = false;
        for (org.bson.Document doc : collection.find(new org.bson.Document("userId", this.getUserId()))) {
            found = true;
            System.out.println("Booking ID: " + doc.getInteger("bookingId"));
            System.out.println("Venue: " + doc.getString("venueName"));
            System.out.println("Purpose: " + doc.getString("purpose"));
            System.out.println("Status: " + doc.getString("bookingStatus"));
            System.out.println("Price: " + doc.getDouble("price"));
            System.out.println("Amenities: " + doc.get("amenities"));
            System.out.println("---------------------------");
        }
        if (!found) System.out.println("You have no bookings.");
    }

    //Check the status of a booking owned by this customer.
    public void checkStatus(){
        Scanner sc = new Scanner(System.in);
        System.out.print("Booking ID to check status: ");
        int id = Integer.parseInt(sc.nextLine().trim());
        com.mongodb.client.MongoCollection<org.bson.Document> collection = MongoDb.getDatabase().getCollection("bookings");
        org.bson.Document doc = collection.find(new org.bson.Document("bookingId", id).append("userId", this.getUserId())).first();
        if (doc == null) { System.out.println("Booking not found or not your booking."); return; }
        System.out.println("Status: " + doc.getString("bookingStatus"));
    }

    //Pay for a pending booking. Delegates calculation to `calculatePayment`.
    public void payBooking(){
        Scanner sc = new Scanner(System.in);
        System.out.print("Booking ID to pay: ");
        int id = Integer.parseInt(sc.nextLine().trim());
        com.mongodb.client.MongoCollection<org.bson.Document> collection = MongoDb.getDatabase().getCollection("bookings");
        org.bson.Document doc = collection.find(new org.bson.Document("bookingId", id).append("userId", this.getUserId())).first();
        if (doc == null) { System.out.println("Booking not found or not your booking."); return; }
        if ("booked".equalsIgnoreCase(doc.getString("bookingStatus"))) { System.out.println("Already booked/paid."); return; }

        // calculate total
        double total = 0;
        Integer venueId = doc.getInteger("venueId");
        if (venueId != null) {
            Venue v = Venue.getVenue(venueId);
            if (v != null) total += v.getPrice();
        }
        if (doc.containsKey("amenities")) {
            for (Object aidObj : doc.getList("amenities", Object.class)) {
                try {
                    int aid = Integer.parseInt(aidObj.toString());
                    Amenity a = Amenity.getAmenity(aid);
                    if (a != null) total += a.getQuantity(); // if quantity used as price placeholder, else 0
                } catch (Exception e) { }
            }
        }

        System.out.println("Total amount to pay: " + total);
        System.out.print("Enter any input to simulate payment: ");
        sc.nextLine();

        collection.updateOne(new org.bson.Document("bookingId", id), new org.bson.Document("$set", new org.bson.Document("paymentStatus", "Paid").append("bookingStatus", "Booked").append("total", total)));
        System.out.println("Payment accepted. Booking confirmed.");
    }

    //Calculate the total payable amount for the provided booking by
    //summing the venue price and selected amenities.
    @Override
    public double calculatePayment(int bookingId) {
        com.mongodb.client.MongoCollection<org.bson.Document> collection = MongoDb.getDatabase().getCollection("bookings");
        org.bson.Document doc = collection.find(new org.bson.Document("bookingId", bookingId)).first();
        if (doc == null) return 0;
        double total = 0;
        Integer venueId = doc.getInteger("venueId");
        if (venueId != null) {
            Venue v = Venue.getVenue(venueId);
            if (v != null) total += v.getPrice();
        }
        if (doc.containsKey("amenities")) {
            for (Object aidObj : doc.getList("amenities", Object.class)) {
                try {
                    int aid = Integer.parseInt(aidObj.toString());
                    Amenity a = Amenity.getAmenity(aid);
                    if (a != null) total += a.getQuantity();
                } catch (Exception e) { }
            }
        }
        return total;
    }
}