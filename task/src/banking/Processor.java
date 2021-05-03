package banking;

import org.sqlite.SQLiteDataSource;

import java.sql.*;

import static banking.Card.isCheckSumDigitValid;

/**
 * This program serves as a verification and processor box.
 * It stores card information such as PIN and
 * card number and can indicate whether a card
 * exists.
 *
 * @author Rodrigo Rogel-Perez
 * @version 2.0
 */
public class Processor {

    /**
     * Indicates whether all inputs are valid
     *
     * @param cardNumber Card number
     * @param pin PIN
     * @return True if inputs are valid, false otherwise
     */
    private static boolean areInputsValid(String cardNumber, String pin) {
        /* We know the login will fail if the card number does not meet
            the following criteria:
            1) Card number is not 16 characters long
            2) The MII not '4'
            3) The pin cannot be converted to an integer
         */
        if (cardNumber.length() != 16) {
            return false;
        } else if (cardNumber.charAt(0) != '4') {
            return false;
        } else if (!isCheckSumDigitValid(cardNumber)) {
            return false;
        } else if(!isInteger(pin)) {
            return false;
        }

        return true;
    }

    /**
     * Indicates whether the parameter is an integer
     *
     * @param input User-entered string
     * @return Boolean value
     */
    public static boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * Inserts card information such as card number and PIN into db table
     *
     * @param dataSource SQLite data source
     * @param card Instance of Card object
     * @return true if card info was stored in table, false otherwise
     */
    static boolean insertCardInfoToTable(SQLiteDataSource dataSource, Card card) {
        try (Connection con = dataSource.getConnection()) {
            String insertSQL = "INSERT INTO card (number, pin) "
                    + "VALUES (?, ?);";

            try (PreparedStatement insertCard = con.prepareStatement(insertSQL)) {
                insertCard.setString(1, card.getCardNumber());
                insertCard.setString(2, card.getPIN());
                insertCard.executeUpdate();

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
     * Executes a 'SELECT' query and returns a 'Card' object if card number exists in table
     *
     * @param dataSource SQLite data source
     * @param cardNumber Credit card number
     * @param pin Credit card PIN
     * @return 'Card' object if entry is found, null otherwise
     */
    static Card selectCardFromTable(SQLiteDataSource dataSource, String cardNumber, String pin) {

        if (!areInputsValid(cardNumber, pin)) {
            return null;
        }

        try (Connection con = dataSource.getConnection()) {
            String selectSQL = "SELECT * "
                    + "FROM card "
                    + "WHERE number = ? AND pin = ?;";
            try (PreparedStatement selectCard = con.prepareStatement(selectSQL)) {

                selectCard.setString(1, cardNumber);
                selectCard.setString(2, pin);

                try (ResultSet rst = selectCard.executeQuery()) {
                    if (rst.next()) {
                        double balance = rst.getDouble("balance");
                        return new Card(cardNumber, pin, balance);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Executes a 'SELECT' query and returns a 'Card' object if card number exists in table
     *
     * @param dataSource SQLite data source
     * @param cardNumber Card number
     * @return Card object if entry is found, null otherwise
     */
    static Card selectCardFromTable(SQLiteDataSource dataSource, String cardNumber) {

        try (Connection con = dataSource.getConnection()) {
            String selectSQL = "SELECT * "
                    + "FROM card "
                    + "WHERE number = ?;";
            try (PreparedStatement selectCard = con.prepareStatement(selectSQL)) {

                selectCard.setString(1, cardNumber);

                try (ResultSet rst = selectCard.executeQuery()) {
                    if (rst.next()) {
                        String pin = rst.getString("pin");
                        double balance = rst.getDouble("balance");
                        return new Card(cardNumber, pin, balance);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates the balance of the specified card number
     *
     * @param cardNumber Card number
     * @param dataSource SQLite data source
     * @return True if update is successful, false otherwise
     */
    static boolean updateBalanceInTable(SQLiteDataSource dataSource, String cardNumber, double balance) {
        try (Connection con = dataSource.getConnection()) {
            String updateSQL = "UPDATE card "
                    + "SET balance = ? "
                    + "WHERE number = ?;";

            try (PreparedStatement updateCard = con.prepareStatement(updateSQL)) {
                updateCard.setInt(1, (int) balance);
                updateCard.setString(2, cardNumber);
                updateCard.executeUpdate();

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
     * Transfers the specified amount from one account balance to another in a db table
     *
     * @param fromNumber Credit card number from which amount will be withdrawn
     * @param toNumber Credit card number upon which amount will be deposited
     * @param amount Amount to be transferred
     * @param dataSource SQLite data source
     * @return True if updates executed successfully, false otherwise
     */
    static boolean transferBalanceInTable(SQLiteDataSource dataSource, String fromNumber, String toNumber, int amount) {
        String updateCard1SQL = "UPDATE card SET balance = balance - ? WHERE number = ?";
        String updateCard2SQL = "UPDATE card SET balance = balance + ? WHERE number = ?";

        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement updateCard1 = con.prepareStatement(updateCard1SQL);
                 PreparedStatement updateCard2 = con.prepareStatement(updateCard2SQL)) {

                updateCard1.setInt(1, amount);
                updateCard1.setString(2, fromNumber);
                updateCard1.executeUpdate();

                updateCard2.setInt(1, amount);
                updateCard2.setString(2, toNumber);
                updateCard2.executeUpdate();

                con.commit();

                return true;

            } catch (SQLException e) {
                if (con != null) {
                    try {
                        System.err.print("Transaction is being rolled back");
                        con.rollback();
                    } catch (SQLException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Deletes the entry that corresponds to the specified card number
     *
     * @param cardNumber Card number
     * @param dataSource SQLite data source
     * @return True if row was deleted successfully in table, false otherwise
     */
    static boolean deleteAccountInTable(SQLiteDataSource dataSource, String cardNumber) {
        try (Connection con = dataSource.getConnection()) {
            String deleteCardSQL = "DELETE FROM card WHERE number = ?";

            try (PreparedStatement deleteCard = con.prepareStatement(deleteCardSQL)) {
                deleteCard.setString(1, cardNumber);
                deleteCard.executeUpdate();

                return true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
