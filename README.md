<img width="368" height="225" alt="image" src="https://github.com/user-attachments/assets/da0ffd51-a086-4580-9c60-20ddf1039fa9" /># **VENYO**

A simple and efficient venue booking system designed to manage venue availability, schedule bookings, and organize customer reservations. This project streamlines the process of viewing venue details, selecting time slots, and managing both paid and free venue bookings.

---

##  **Features**

- Manage multiple venues including amenities  
- Time slot selection (AM / PM)  
- Customer booking management  
- Supports **paid** and **free** venue types  
- View availability and prevent double-booking  
- Search & filter bookings  
- Uses **MongoDB** for persistence  
- Unique booking IDs with indexing  

---

##  **Tech Stack**

- **Java** – Application logic  
- **MongoDB** – Database  
- **Maven** – Dependency management  

---

##  **Setup**

### **1. Clone the Repository**

Clone the repository from the **main branch**, inside the `Venyo_ver2` folder.

```bash
https://github.com/vincenttamano/VENYO.git
```
## 2. Install Necessary Drivers
- MongoDB Java Driver

Maven Repository (sync driver):
https://mvnrepository.com/artifact/org.mongodb/mongodb-driver-sync

MongoDB Java Documentation:
https://www.mongodb.com/docs/drivers/java/

- MongoDB Compass

Download:
https://www.mongodb.com/try/download/compass

- Java (JDK 17+)

OpenJDK download:
https://jdk.java.net/17/

Oracle JDK:
https://www.oracle.com/java/technologies/downloads/

- Maven

Download:
https://maven.apache.org/download.cgi


## 3.Install Dependencies
- Create .xml file
- Paste dependencies under properties
```bash
<dependencies>
    <!-- MongoDB Java Driver -->
    <dependency>
        <groupId>org.mongodb</groupId>
        <artifactId>mongodb-driver-sync</artifactId>
        <version>5.1.0</version>
    </dependency>

    <!-- JSON library: to handle JSON parsing and creation -->
    <dependency>
        <groupId>org.json</groupId>
        <artifactId>json</artifactId>
        <version>20231013</version>
    </dependency>

    <!-- SLF4J Simple: logging framework for simple console logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>2.0.12</version>
    </dependency>

    <!-- JUnit Jupiter API: for writing unit tests -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-api</artifactId>
        <version>5.10.2</version>
        <scope>test</scope>
    </dependency>

    <!-- JUnit Jupiter Engine: runtime engine for executing tests -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter-engine</artifactId>
        <version>5.10.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```
##  **Connecting MongoDb to Java**
## 1. Connect to Cluster through MongoDb Compass
- Connection String
```bash
mongodb+srv://vincentjohntamano_db_user:CZAngelsBaby1234567891011121314151617181920@cluster1.e8ynseg.mongodb.net/
```

- Create new connection
  Paste the connection string in the url, and use name the connection iVenue
-<img width="368" height="225" alt="image" src="https://github.com/user-attachments/assets/c1e39016-e7fa-42ad-93e1-68b9c9033bbe" />

## 2. Connect Java to MongoDb
- Create new java class
  paste this code to create as a connection to the database
```bash
umport com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDb {
    private static final String CONNECTION_STRING = "mongodb+srv://vincentjohntamano_db_user:CZAngelsBaby1234567891011121314151617181920@cluster1.e8ynseg.mongodb.net/";
    private static final String DATABASE_NAME = "iVenue";
    static MongoClient mongoClient;
    private static MongoDatabase database;

    public static MongoDatabase getDatabase() {
        if (mongoClient == null) {
            mongoClient = MongoClients.create(CONNECTION_STRING);
            database = mongoClient.getDatabase(DATABASE_NAME);
        }
        return database;
    }
}

 ```
## 3.Creating a New Class
-Imports to access the database from MongoDb 
```bash
import com.mongodb.client.MongoCollection; // Represents a collection in MongoDB, used to perform CRUD operations
import com.mongodb.client.MongoDatabase;   // Represents the database itself, used to access collections
import org.bson.Document;                  // Represents a BSON (JSON-like) document, used for storing and manipulating data
```

-Connecting

  

