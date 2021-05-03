package banking;

import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import static banking.Card.*;
import static banking.Processor.*;

/**
 * This program simulates a simple banking system that
 * allows customers to create an account or log into
 * an existing one. If the customer creates an account,
 * a new card number is generated and associated to the
 * account.
 *
 * @author Rodrigo Rogel-Perez
 * @version 2.0
 */
public class Main {

    /**
     * Main entry to the program
     *
     * @param args Terminal passed-down arguments
     */
    public static void main(String[] args) {

        int input;

        String url = "jdbc:sqlite:" + args[1];

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        boolean hasValidConnection = checkDatabaseTables(dataSource);

        if (!hasValidConnection) {
            System.out.print("Connection to database or table was not successful!");
            System.exit(-1);
        }

        do {
            displayMainMenu();
            do {
                input = getValidUserInput();
            } while(!isWithinRange(input, getExitNumOption(), getNumMenuOptions()));

            if (input != getExitNumOption()) {System.out.print("\n");}

            switch (input) {
                case 1:
                    generateNewCard(dataSource, true);
                    break;
                case 2:
                    input = handleUserLogin(dataSource);
                    break;
            }
            System.out.print("\n");
        } while (input != getExitNumOption());

        displayExitMsg();
    }

    /**
     * Handles user login process by verifying login
     * information and displaying sub-menu
     */
    public static int handleUserLogin(SQLiteDataSource dataSource) {
        int input = getNumMenuOptions();

        String prompt = "Enter your card number:";
        String cardNumber = getUserInput(prompt);
        prompt = "Enter your PIN:";
        String pin = getUserInput(prompt);
        Card card = findCard(dataSource, cardNumber, pin);

        String amount;

        System.out.print("\n");

        if (card != null) {
            System.out.println("You have successfully logged in!");

            do {
                System.out.print("\n");
                displaySubMenu();

                do {
                    input = getValidUserInput();
                } while(!isWithinRange(input, getExitNumOption(), getNumSubMenuOptions()));

                if (input != getExitNumOption()) {System.out.print("\n");}

                switch (input) {
                    case 1:
                        card.displayBalance();
                        break;
                    case 2:
                        prompt = "Enter income:";
                        amount = getUserInput(prompt);
                        System.out.print("\n");

                        if (isInteger(amount)) {
                            if (card.addIncome(Integer.parseInt(amount), dataSource)) {
                                System.out.println("Income was added!");
                            }
                        } else{
                            System.out.println("Income must be an integer!");
                        }
                        break;
                    case 3:
                        prompt = "Enter card number:";
                        cardNumber = getUserInput(prompt);

                        boolean isCheckSumDigitValid = isCheckSumDigitValid(cardNumber);

                        if (isCheckSumDigitValid) {
                            Card otherCard = findCard(dataSource, cardNumber);

                            if (otherCard == null) {
                                System.out.println("Such a card does not exist.");
                            } else if (otherCard.getCardNumber() == card.getCardNumber()) {
                                System.out.println("You can't transfer money to the same account!");
                            } else{
                                prompt = "Enter how much money you want to transfer:";
                                amount = getUserInput(prompt);

                                if (isInteger(amount)) {
                                    if (Integer.parseInt(amount) > card.getBalance()) {
                                        System.out.println("Not enough money!");
                                    } else if (card.transferBalanceTo(Integer.parseInt(amount), otherCard, dataSource)) {
                                        System.out.println("Success!");
                                    }
                                } else{
                                    System.out.println("Income must be an integer!");
                                }
                            }
                        } else {
                            System.out.println("Probably you made a mistake in the card number. Please try again!");
                        }
                        break;
                    case 4:
                        deleteAccount(dataSource, card.getCardNumber());
                        card = null;
                        System.out.println("The account has been closed!");
                        break;
                    case 5:
                        System.out.println("You have successfully logged out!");
                        break;
                }
            } while (input != getDelNumOption() & input != getNumSubMenuOptions() & input != getExitNumOption());
        } else {
            System.out.println("Wrong card number or PIN!");
        }

        return input;
    }

    /**
     * Display main menu to user
     */
    public static void displayMainMenu() {
        System.out.print("1. Create an account\n" +
                "2. Log into account\n" +
                "0. Exit\n>");
    }

    /**
     * Display sub menu to user after successful log in attempt
     */
    public static void displaySubMenu() {
        System.out.print("1. Balance\n" +
                "2. Add income\n" +
                "3. Do transfer\n" +
                "4. Close account\n" +
                "5. Log out\n" +
                "0. Exit\n>");
    }

    /**
     * Prompts user to enter an integer
     *
     * @return Integer value
     */
    public static int getValidUserInput() {
        Scanner scan = new Scanner(System.in);
        String input = scan.next();

        while (!isInteger(input)) {
            System.out.print("Invalid input!\n>");
            input = scan.next();
        }

        return Integer.parseInt(input);
    }

    /**
     * Gets number of options in main menu
     *
     * @return Integer value
     */
    public static int getNumMenuOptions() {
        return 2;
    }

    /**
     * Gets the number of options in second menu
     *
     * @return Integer value
     */
    public static int getNumSubMenuOptions() {
        return 5;
    }

    /**
     * Gets the integer associated with the 'exit' command
     *
     * @return 0
     */
    public static int getExitNumOption() {
        return 0;
    }

    /**
     * Gets the integer associated with the option to delete the account
     *
     * @return 4
     */
    public static int getDelNumOption() {
        return 4;
    }

    /**
     * Indicates whether user input is valid
     *
     * @param input User input
     * @return Boolean
     */
    public static boolean isWithinRange(int input, int start, int end) {
        if (input < start | input > end) {
            System.out.print("Invalid input!\n>");
            return false;
        }
        return true;
    }

    /**
     * Display exit message
     */
    public static void displayExitMsg() {
        System.out.print("Bye!");
    }

    /**
     * Establishes connection to SQLite database and checks status of required tables
     *
     * @param dataSource SQLite data source
     * @return True if connection is successful, false otherwise
     */
    public static boolean checkDatabaseTables(SQLiteDataSource dataSource) {
        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {
                String createSQL = "CREATE TABLE IF NOT EXISTS card ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "number TEXT, "
                            + "pin TEXT, "
                            + "balance INTEGER DEFAULT 0);";
                statement.executeUpdate(createSQL);
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Takes in user input
     *
     * @param prompt Text to display
     * @return Next token as entered by user
     */
    static String getUserInput(String prompt) {
        System.out.print(prompt + "\n>");
        Scanner scanner = new Scanner(System.in);
        return scanner.next();
    }
}