package Venyo_ver2;

import java.util.Scanner;

public interface AdminManagement<T> {
    void create(Scanner input);
    void update(Scanner input);
    void delete(Scanner input);
    void displayAll();
    }
