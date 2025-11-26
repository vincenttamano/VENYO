package iVenue;

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
                case "1": Booking.createBooking(this); break;
                case "2": Booking.cancelBooking(this); break;
                case "3": Booking.viewBookingDetails(this); break;
                case "4": Booking.checkStatus(this); break;
                case "5": Booking.payBooking(this); break;
                case "6": return;
                default: System.out.println("Invalid choice.");
            }
        }
    }
    
    // registration helper that collects customer details and stores in MongoDB
    public static void registerCustomer(Scanner input) {
        System.out.println("--- Customer Registration ---");
        String username = nameInput(input, "Username: ");
        String password = passwordInput(input);
        String firstName = nameInput(input, "First Name: ");
        String lastName = nameInput(input, "Last Name: ");
        String contact = contactInput(input);
        String email = emailInput(input);

        Customer c = UserStore.registerCustomer(username, password, firstName, lastName, contact, email);

        if (c != null) {
            System.out.println("Registration successful for: " + c.getUsername());
        } else {
            System.out.println("Registration failed. Please try again.");
        }
    }
    
    //catch statement for Username, First name and Last name
    private static String nameInput(Scanner input, String message) {
        String value;
        do {
            System.out.print(message);
            value = input.nextLine().trim();
            if (value.isEmpty()) {
                System.out.println("This field cannot be empty. Please try again.");
            }
        } while (value.isEmpty());
        return value;
    }

    //catch statement for password
    private static String passwordInput(Scanner input) {
        String pass;
        do {
            System.out.print("Password (minimum of 6 characters): ");
            pass = input.nextLine().trim();

            if (pass.length() < 6) {
                System.out.println("Password must be at least 6 characters long.");
            }
        } while (pass.length() < 6);

        return pass;
    }
    
    //catch statement for contact number
    private static String contactInput(Scanner input) {
        String contact;
        do {
            System.out.print("Contact number: ");
            contact = input.nextLine().trim();

            if (!contact.matches("\\d+")) {
                System.out.println("Contact number must contain digits only.");
                continue;
            }
            if (contact.length() < 7) {
                System.out.println("Contact number must be at least 7 digits.");
                continue;
            }
            if (contact.isEmpty()) {
                System.out.println("This field cannot be empty. Please try again.");
            }
            break;

        } while (true);

        return contact;
    }
    
    //catch statement for email
    private static String emailInput(Scanner input) {
        String email;
        do {
            System.out.print("Email: ");
            email = input.nextLine().trim();

            if (!email.contains("@") || !email.contains(".")) {
                System.out.println("Invalid email format. Please enter a valid email.");
                continue;
            }
            if (email.isEmpty()) {
                System.out.println("This field cannot be empty. Please try again.");
            }
            break;

        } while (true);

        return email;
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