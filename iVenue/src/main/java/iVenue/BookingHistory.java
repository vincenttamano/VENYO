package iVenue;

import java.util.Queue;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class BookingHistory {

    private static final Queue<Booking> finishedQueue = new LinkedList<>();
    private static final Queue<Booking> deletedQueue = new LinkedList<>();
    private static final MongoCollection<Document> collection = MongoDb.getDatabase().getCollection("booking_history");

    static {
        loadHistory();
    }

    private static void loadHistory() {
        synchronized (finishedQueue) {
            for (Document doc : collection.find(new Document("type", "finished"))) {
                // Parse paymentStatus from DB in a case-insensitive manner
                String payStr = doc.getString("paymentStatus");
                PaymentStatus paymentStatus = PaymentStatus.UNPAID;
                if (payStr != null) {
                    boolean matched = false;
                    for (PaymentStatus ps : PaymentStatus.values()) {
                        if (ps.name().equalsIgnoreCase(payStr)) {
                            paymentStatus = ps;
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        // try capitalized form as fallback
                        try {
                            paymentStatus = PaymentStatus
                                    .valueOf(payStr.substring(0, 1).toUpperCase() + payStr.substring(1).toLowerCase());
                        } catch (Exception ignored) {
                        }
                    }
                }

                // Normalize bookingStatus string from DB
                String statusStr = doc.getString("bookingStatus");
                BookingStatus bookingStatus = BookingStatus.PENDING;
                if (statusStr != null) {
                    boolean matched = false;
                    for (BookingStatus bs : BookingStatus.values()) {
                        if (bs.name().equalsIgnoreCase(statusStr)) {
                            bookingStatus = bs;
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        try {
                            bookingStatus = BookingStatus.valueOf(statusStr.toUpperCase());
                        } catch (Exception ignored) {
                        }
                    }
                }

                Booking b = new Booking(
                        doc.getInteger("bookingId"),
                        null,
                        null,
                        paymentStatus,
                        bookingStatus,
                        doc.getString("purpose"),
                        doc.getString("username"));
                finishedQueue.offer(b);
            }
        }

        synchronized (deletedQueue) {
            for (Document doc : collection.find(new Document("type", "cancelled"))) {
                String payStr = doc.getString("paymentStatus");
                PaymentStatus paymentStatus = PaymentStatus.UNPAID;
                if (payStr != null) {
                    boolean matched = false;
                    for (PaymentStatus ps : PaymentStatus.values()) {
                        if (ps.name().equalsIgnoreCase(payStr)) {
                            paymentStatus = ps;
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        try {
                            paymentStatus = PaymentStatus
                                    .valueOf(payStr.substring(0, 1).toUpperCase() + payStr.substring(1).toLowerCase());
                        } catch (Exception ignored) {
                        }
                    }
                }

                String statusStr = doc.getString("bookingStatus");
                BookingStatus bookingStatus = BookingStatus.PENDING;
                if (statusStr != null) {
                    boolean matched = false;
                    for (BookingStatus bs : BookingStatus.values()) {
                        if (bs.name().equalsIgnoreCase(statusStr)) {
                            bookingStatus = bs;
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        try {
                            bookingStatus = BookingStatus.valueOf(statusStr.toUpperCase());
                        } catch (Exception ignored) {
                        }
                    }
                }

                Booking b = new Booking(
                        doc.getInteger("bookingId"),
                        null,
                        null,
                        paymentStatus,
                        bookingStatus,
                        doc.getString("purpose"),
                        doc.getString("username"));
                deletedQueue.offer(b);
            }
        }
    }

    private static void writeHistory(Booking booking, String type) {
        if (booking == null)
            return;
        Document doc = new Document("bookingId", booking.getBookingId())
            .append("type", type)
            .append("paymentStatus", booking.getPaymentStatus() == null ? null : booking.getPaymentStatus().name())
            .append("bookingStatus", booking.getBookingStatus() == null ? null : booking.getBookingStatus().name())
            .append("purpose", booking.getPurpose())
            .append("username", booking.getUsername())
            .append("timestamp", new java.util.Date());
        collection.insertOne(doc);
    }

    // --- Finished bookings ---

    public static synchronized void addFinished(Booking booking) {
        if (booking == null)
            return;
        finishedQueue.offer(booking);
        writeHistory(booking, "finished");
    }

    public static synchronized Booking peekFinished() {
        return finishedQueue.peek();
    }

    public static synchronized Booking pollFinished() {
        return finishedQueue.poll();
    }

    public static synchronized List<Booking> listFinished() {
        return new ArrayList<>(finishedQueue);
    }

    public static synchronized int finishedCount() {
        return finishedQueue.size();
    }

    public static synchronized void clearFinished() {
        finishedQueue.clear();
        collection.deleteMany(new Document("type", "finished"));
    }

    // --- Deleted bookings ---

    public static synchronized void addDeleted(Booking booking) {
        if (booking == null)
            return;
        deletedQueue.offer(booking);
        writeHistory(booking, "cancelled");
    }

    public static synchronized Booking peekDeleted() {
        return deletedQueue.peek();
    }

    public static synchronized Booking pollDeleted() {
        return deletedQueue.poll();
    }

    public static synchronized List<Booking> listDeleted() {
        return new ArrayList<>(deletedQueue);
    }

    public static synchronized int deletedCount() {
        return deletedQueue.size();
    }

    public static synchronized void clearDeleted() {
        deletedQueue.clear();
        collection.deleteMany(new Document("type", "cancelled"));
    }

    public static synchronized boolean moveDeletedToFinished() {
        Booking b = deletedQueue.poll();
        if (b == null)
            return false;
        finishedQueue.offer(b);
        collection.deleteOne(new Document("bookingId", b.getBookingId()).append("type", "cancelled"));
        writeHistory(b, "finished");
        return true;
    }

    // Convenience lookup by booking id
    public static synchronized Booking findFinishedById(int bookingId) {
        for (Booking b : finishedQueue) {
            if (b.getBookingId() == bookingId)
                return b;
        }
        return null;
    }

    public static synchronized Booking findDeletedById(int bookingId) {
        for (Booking b : deletedQueue) {
            if (b.getBookingId() == bookingId)
                return b;
        }
        return null;
    }

    public static synchronized boolean removeFinishedById(int bookingId) {
        java.util.Iterator<Booking> it = finishedQueue.iterator();
        while (it.hasNext()) {
            Booking b = it.next();
            if (b.getBookingId() == bookingId) {
                it.remove();
                collection.deleteOne(new Document("bookingId", bookingId).append("type", "finished"));
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean removeDeletedById(int bookingId) {
        java.util.Iterator<Booking> it = deletedQueue.iterator();
        while (it.hasNext()) {
            Booking b = it.next();
            if (b.getBookingId() == bookingId) {
                it.remove();
                collection.deleteOne(new Document("bookingId", bookingId).append("type", "cancelled"));
                return true;
            }
        }
        return false;
    }

    // --- Additional helpers ---
    // List finished (paid) bookings by username
    public static synchronized List<Booking> listFinishedByUsername(String username) {
        List<Booking> result = new ArrayList<>();
        for (Booking b : finishedQueue) {
            if (b.getUsername() != null && b.getUsername().equals(username))
                result.add(b);
        }
        return result;
    }

    // List unpaid bookings (present in `bookings` collection with
    // PaymentStatus.Pending) for a userId
    public static synchronized List<Booking> listUnpaidFromBookings(int userId) {
        List<Booking> result = new ArrayList<>();
        com.mongodb.client.MongoCollection<Document> coll = MongoDb.getDatabase().getCollection("bookings");
        for (Document doc : coll
                .find(new Document("userId", userId).append("paymentStatus", PaymentStatus.UNPAID.name()))) {
            Integer id = doc.getInteger("bookingId");
            String purpose = doc.getString("purpose");
            String uname = null;
            if (doc.containsKey("bookedBy") && doc.get("bookedBy") instanceof Document bb) {
                uname = bb.getString("username");
            }
            String statusStr = doc.getString("bookingStatus");
            BookingStatus bstat = BookingStatus.PENDING;
            if (statusStr != null) {
                try {
                    bstat = BookingStatus.valueOf(statusStr);
                } catch (Exception e) {
                    // keep default
                }
            }
            Booking b = new Booking(id != null ? id : 0, null, null, PaymentStatus.UNPAID, bstat, purpose, uname);
            result.add(b);
        }
        return result;
    }

    // List bookings that contain any "down payment" indicator for a user
    // (This looks for keys that might indicate partial/down payments if present in
    // DB.)
    public static synchronized List<Booking> listDownPaymentsFromBookings(int userId) {
        List<Booking> result = new ArrayList<>();
        com.mongodb.client.MongoCollection<Document> coll = MongoDb.getDatabase().getCollection("bookings");
        for (Document doc : coll.find(new Document("userId", userId))) {
            if (doc.containsKey("downPayment") || doc.containsKey("downPaymentBy") || doc.containsKey("partialPaid")) {
                Integer id = doc.getInteger("bookingId");
                String purpose = doc.getString("purpose");
                String uname = null;
                if (doc.containsKey("bookedBy") && doc.get("bookedBy") instanceof Document bb) {
                    uname = bb.getString("username");
                }
                String statusStr = doc.getString("bookingStatus");
                BookingStatus bstat = BookingStatus.PENDING;
                if (statusStr != null) {
                    try {
                        bstat = BookingStatus.valueOf(statusStr);
                    } catch (Exception e) {
                        // keep default
                    }
                }
                Booking b = new Booking(id != null ? id : 0, null, null, PaymentStatus.DOWNPAID, bstat, purpose, uname);
                result.add(b);
            }
        }
        return result;
    }

}