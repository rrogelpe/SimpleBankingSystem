package banking;

import org.sqlite.SQLiteDataSource;
import java.util.Random;
import static banking.Processor.*;

/**
 * This program represents a credit card
 *
 * @author Rodrigo Rogel-Perez
 * @version 2.0
 */
public class Card {

    private String binNumber;
    private String pin;
    private String account;
    private static int count;
    private int checkDigit;
    private double balance;
    private String cardNumber;

    /**
     * Custom constructor
     *
     * @param cardNumber Credit card number
     * @param pin Credit card PIN
     * @param balance Credit card balance
     */
    Card (String cardNumber, String pin, double balance) {
        this.cardNumber = cardNumber;
        this.pin = pin;
        this.balance = balance;
    }

    /**
     * Default constructor
     */
    private Card(boolean isRandom) {
        pin = generatePIN();
        account = generateAccount(isRandom);
        binNumber = "400000";
        checkDigit = generateCheckSumDigit(binNumber + account);
        balance = 0d;
        cardNumber = binNumber + account + checkDigit;
    }

    /**
     * Generates random 4 digit PIN
     *
     * @return A String
     */
    private String generatePIN() {
        String pin = "";

        for (int i = 0; i < 4; i++) {
            pin += new Random().nextInt(10);
        }

        return pin;
    }

    /**
     * Generates unique customer account number
     *
     * @param isRandom Indicates whether number should be randomly or sequentially generated
     * @return A String
     */
    private String generateAccount(boolean isRandom) {
        String account = "";

        if (isRandom) {
            Random rand = new Random();
            account += (rand.nextInt(999999999));
        } else {
            account += count++;
        }

        int size = account.length();

        for (int i = 0; i < (9 - size); i++) {
            account = "0" + account;
        }

        return account;
    }

    /**
     * Generates the checksum digit to be added to the credit card number
     *
     * @param cardNumber Account Identifier concatenated to BIN
     * @return Integer value
     */
    private static int generateCheckSumDigit(String cardNumber) {
        int controlNum = 0;
        int val;

        for (int i = 0; i < cardNumber.length(); i++) {
            val = Integer.parseInt(String.valueOf(cardNumber.charAt(i))); // Convert character to integer
            if ((i + 1) % 2 != 0) {
                val *= 2; // Multiply integer by 2
                if (val > 9) {
                    val -= 9; // Subtract 9 if integer is above 9
                }
            }
            controlNum += val;
        }

        return ((controlNum % 10 == 0) ? 0 : 10 - (controlNum % 10));
    }

    /**
     * Indicates whether the credit card number is valid by
     * verifying the check digit sum
     *
     * @param cardNumber Credit card number
     * @return True if card number is valid, false otherwise
     */
    static boolean isCheckSumDigitValid(String cardNumber) {
        int checkSumDigit;
        char lastChar = cardNumber.charAt(cardNumber.length() - 1);

        try {
            checkSumDigit = Integer.parseInt(String.valueOf(lastChar));
        } catch (NumberFormatException e) {
            return false;
        }

        String incompleteCardNumber = cardNumber.substring(0, cardNumber.length() - 1);
        int validCheckSumDigit = generateCheckSumDigit(incompleteCardNumber);

        return checkSumDigit == validCheckSumDigit;
    }

    /**
     * Gets the PIN
     *
     * @return A String
     */
    String getPIN() {
        return pin;
    }

    /**
     * Gets the card number
     *
     * @return A String
     */
    String getCardNumber() {
        return (cardNumber);
    }

    /**
     * Gets the balance in user's account
     *
     * @return A double
     */
     double getBalance() {
        return balance;
    }

    /**
     * Gets string containing card information such
     * as card number and PIN
     *
     * @return A String
     */
    @Override
    public String toString() {
        return ("Your card has been created\n"
                + "Your card number:\n"
                + getCardNumber() + "\n"
                + "Your card PIN:\n"
                + getPIN());
    }

    /**
     * Displays credit card information such as card number and PIN
     *
     * @param card Instance of Card Object
     */
    static void displayCardInformation(Card card) {
        System.out.println(card.toString());
    }

    /**
     * Displays the balance in the user's account
     */
    void displayBalance() {

        System.out.println("Balance: " + (int) getBalance());
    }

    /**
     * Prompts user to enter login information and indicates
     * whether account exists.
     *
     * @return True if account exists, false otherwise
     */
    static Card findCard(SQLiteDataSource dataSource, String cardNumber, String pin) {
        return selectCardFromTable(dataSource, cardNumber, pin);
    }

    static Card findCard(SQLiteDataSource dataSource, String cardNumber) {
        return selectCardFromTable(dataSource, cardNumber);
    }

    /**
     * Generates new Card object and stores its data in a db table
     * @param dataSource SQLite data source
     */
    static boolean generateNewCard(SQLiteDataSource dataSource, boolean isRandom) {
        Card newCard;

        do {
            newCard = new Card(isRandom);
        } while (selectCardFromTable(dataSource, newCard.cardNumber) != null);

        if (insertCardInfoToTable(dataSource, newCard)) {
            displayCardInformation(newCard);
            return true;
        }

        return false;
    }

    /**
     * Adds specified amount to this object's balance amount
     *
     * @param income Amount to be added
     * @return True if amount was successfully added, false otherwise
     */
    boolean addIncome(int income, SQLiteDataSource dataSource) {
        int currBalance = (int) getBalance();
        setBalance(currBalance + income);

        if (updateBalanceInTable(dataSource, getCardNumber(), getBalance())) {
            return true;
        }

        setBalance(currBalance);
        return false;
    }

    /**
     * Sets a new balance
     *
     * @param balance New balance
     */
    private void setBalance(double balance) {
        this.balance = balance;
    }

    /**
     * Transfer specified amount from this Card object to another specified object of the same type
     *
     * @param amount Amount to be transferred
     * @param otherCard Card object to receive transfer
     * @param dataSource SQLite data source
     * @return True if transfer was successful, false otherwise
     */
    boolean transferBalanceTo(int amount, Card otherCard, SQLiteDataSource dataSource) {
        if (transferBalanceInTable(dataSource, getCardNumber(), otherCard.getCardNumber(), amount)) {
            setBalance(getBalance() - amount);
            otherCard.setBalance(getBalance() + amount);
            return true;
        }

        return false;
    }

    /**
     * Deletes the table row associated with the specified card number
     *
     * @param dataSource SQLite data source
     * @param cardNumber Card number
     * @return True if record was successfully deleted in table, false otherwise
     */
    static boolean deleteAccount(SQLiteDataSource dataSource, String cardNumber) {
        if (deleteAccountInTable(dataSource, cardNumber)) {
            return true;
        }

        return false;
    }
}