package Venyo;

public class Venue {
    private String name;
    private String description;
    private boolean isFree;
    private double price;
    private int capacity;
    private boolean availability;
    private String location;
    private byte[] photo;


    public Venue(String name, String description, boolean isFree, double price, int capacity, boolean availability, String location, byte[] photo) {
        this.name = name;
        this.description = description;
        setFree(isFree) ;
        this.price = price;
        this.capacity = capacity;
        this.availability = availability;
        this.location = location;
        this.photo = photo;
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

    public boolean isFree() {
        return isFree;
    }

    public void setFree(boolean free) {
        if (price == 0) {
            isFree = free;
        }
        else  {
            isFree = false;
        }
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
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

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public void displayVenue() {
        System.out.println("Venue");
        System.out.println("Name: " + name);
        System.out.println("Description: " + description);
        System.out.println("Free: " + isFree);
        System.out.println("Price: " + price);
        System.out.println("Capacity: " + capacity);
        System.out.println("Availability: " + availability);
        System.out.println("Location: " + location);
        System.out.println("Photo: " + photo);
    }



    public void sortByType() {}

    public void sortByAvailability() {}

    public void sortByCapacity() {}

    public void searchVenue() {}
}
