# iVenue
A simple and efficient venue booking system designed to manage venue availability, schedule bookings, and organize customer reservations. This project streamlines the process of viewing venue details, selecting time slots, and managing both paid and free venue bookings.

---
## Notes
- Branching: Before starting any new feature or fixing bugs, create a new branch from main. Example:
```bash
git checkout -b feature/booking-module
```
- Push your branch and create a pull request when your work is ready. This keeps the main branch stable.
- Latest Version: Venyo_ver2 is the latest version of the code.
- README Updates: Rewrite the README file if any changes are made to the project.

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

Clone the repository

```bash
https://github.com/vincenttamano/VENYO.git
```
note: Venyo_ver2 is the latest code

## 2. Install Necessary Tools
- MongoDB Compass
  
Download:
https://www.mongodb.com/try/download/compass

- Maven
Download:
https://maven.apache.org/download.cgi

- Eclipse
Import the project: File → Import → Existing Maven Project.
Right-click project → Maven → Update Project to download dependencies.
Ensure MongoDB is running to connect via Java code.

- Vs Code
Install Extension Pack for Java (includes Maven support).
Open cloned repo folder in VS Code.
Maven dependencies are automatically downloaded, or run Maven: Update Project.
Ensure MongoDB is running to connect via Java code.

## 3.Install Dependencies
- Add the following to your pom.xml under <dependencies>:
```bash
  <dependencies>

        <!-- MongoDB Driver (Java 21 compatible) -->
        <dependency>
            <groupId>org.mongodb</groupId>
            <artifactId>mongodb-driver-sync</artifactId>
            <version>5.1.0</version>
        </dependency>

        <!-- JSON Support -->
        <dependency>
            <groupId>org.json</groupId>
            <artifactId>json</artifactId>
            <version>20231013</version>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>2.0.12</version>
        </dependency>

        <!-- JUnit Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>

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
```bash
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class 'Name of Your Class' {
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
- Imports to access the collections in Java from MongoDb 
```bash
import com.mongodb.client.MongoCollection; // Represents a collection in MongoDB, used to perform CRUD operations
import com.mongodb.client.MongoDatabase;   // Represents the database itself, used to access collections
import org.bson.Document;                  // Represents a BSON (JSON-like) document, used for storing and manipulating data
```

- Accessing data from a specific collection
```bash
MongoDatabase database = MongoDb.getDatabase();    
MongoCollection<Document> collection = database.getCollection("Use name of collection you want to access"); Collection are like tables inside a database
```


  

