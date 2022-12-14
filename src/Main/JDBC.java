package Main;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.*;
import static Main.Appt.*;

/** Defines the JDBC interface. */
public abstract class JDBC {

    private static final String protocol = "jdbc";
    private static final String vendor = ":mysql:";
    private static final String address = "//127.0.0.1:3306/";
    private static final String dbase = "client_schedule";
    private static final String fullUrl = protocol + vendor + address + dbase;
    private static final String driver = "com.mysql.cj.jdbc.Driver";
    private static final String uname = "sqlUser";
    private static final String pword = "Passw0rd!";
    public static Connection conn;

    /** Attempts to open a JDBC connection to the MySQL database. */
    public static void openConn() {
        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(fullUrl, uname, pword);
            System.out.println("JDBC connection success: " + conn);
        }
        catch(Exception e) {
            System.out.println("JDBC open connection error: " + e.getMessage());
        }
    }

    /** Attempts to close a JDBC connection to the MySQL database. */
    public static void closeConn() {
        try {
            conn.close();
            System.out.println("JDBC connection closed.");
        }
        catch(Exception e) {
            System.out.println("JDBC close connection error: " + e.getMessage());
        }
    }

    /** Creates new Appt objects by querying the database and then populates the allAppts table with them.
     *
     * @throws SQLException
     */
    public static void populateApptsTable() throws SQLException {
        String stmt = "select * from client_schedule.appointments";
        PreparedStatement ps = JDBC.conn.prepareStatement(stmt);
        ResultSet rs = ps.executeQuery();
        clearAllAppts();
        while (rs.next()) {
            int apptID = rs.getInt("appointment_id");
            String title = rs.getString("title");
            String desc = rs.getString("description");
            String type = rs.getString("type");
            String loc = rs.getString("location");
            LocalDateTime start = convertFromUTC(rs.getTimestamp("start").toLocalDateTime());
            LocalDateTime end = convertFromUTC(rs.getTimestamp("end").toLocalDateTime());
            String user = User.convertToName(rs.getInt("user_id"));
            int userFKID = rs.getInt("user_id");
            String cust = Cust.convertToName(rs.getInt("customer_id"));
            int custFKID = rs.getInt("customer_id");
            String contact = Contact.convertToName(rs.getInt("contact_id"));
            int contactFKID = rs.getInt("contact_id");
            Appt newAppt = new Appt(apptID, title, desc, type, loc, start, end, user, userFKID, cust, custFKID, contact, contactFKID);
            addAppt(newAppt);
        }
    }

    /** Inserts a new appointment in the database.
     *
     * @param title
     * @param desc
     * @param loc
     * @param type
     * @param start
     * @param end
     * @param userID
     * @param custID
     * @param contactID
     * @return id generated by MySQL database (using "select last_insert_id()" statement), otherwise 0.
     * @throws SQLException
     */
    public static int insAppt(String title, String desc, String loc, String type, LocalDateTime start, LocalDateTime end, int userID, int custID, int contactID) throws SQLException {
        String stmt = "insert into appointments (title, description, location, type, start, end, user_id, customer_id, contact_id) values( ?, ?, ?, ?, ?, ?, ?, ?, ? )";
        PreparedStatement ps = JDBC.conn.prepareStatement(stmt);
        ps.setString(1, title);
        ps.setString(2, desc);
        ps.setString(3, loc);
        ps.setString(4, type);
        ps.setTimestamp(5, Timestamp.valueOf(convertToUTC(start)));
        ps.setTimestamp(6, Timestamp.valueOf(convertToUTC(end)));
        ps.setInt(7, userID);
        ps.setInt(8, custID);
        ps.setInt(9, contactID);
        ps.executeUpdate();

        stmt = "select last_insert_id()";
        ps = JDBC.conn.prepareStatement(stmt);
        ResultSet getID = ps.executeQuery();
        while (getID.next()) {
            int id = getID.getInt("last_insert_id()");
            return id;
        }
        return 0;
    }

    /** Updates an existing appointment in the database.
     *
     * @param title
     * @param desc
     * @param loc
     * @param type
     * @param start
     * @param end
     * @param userID
     * @param custID
     * @param contactID
     * @param apptID
     * @throws SQLException
     */
    public static void updtAppt (String title, String desc, String loc, String type, LocalDateTime start, LocalDateTime end, int userID, int custID, int contactID, int apptID) throws SQLException {
        String stmt = "update appointments set title = ?, description = ?, location = ?, type = ?, start = ?, end = ?, user_id = ?, customer_id = ?, contact_id = ? where appointment_id = ?";
        PreparedStatement ps = JDBC.conn.prepareStatement(stmt);
        ps.setString(1, title);
        ps.setString(2, desc);
        ps.setString(3, loc);
        ps.setString(4, type);
        ps.setTimestamp(5, Timestamp.valueOf(convertToUTC(start)));
        ps.setTimestamp(6, Timestamp.valueOf(convertToUTC(end)));
        ps.setInt(7, userID);
        ps.setInt(8, custID);
        ps.setInt(9, contactID);
        ps.setInt(10, apptID);
        ps.executeUpdate();
    }

    /** Deletes an existing appointment from the database.
     *
     * @param apptID
     * @throws SQLException
     */
    public static void delAppt (int apptID) throws SQLException {
        String stmt = "delete from appointments where appointment_id = ?";
        PreparedStatement ps = JDBC.conn.prepareStatement(stmt);
        ps.setInt(1, apptID);
        ps.executeUpdate();
    }

    /** Creates new Cust objects by querying the database and then populates the allCusts table with them.
     *
     * @throws SQLException
     */
    public static void populateCustsTable() throws SQLException {
        String stmt = "select * from client_schedule.customers";
        PreparedStatement ps = JDBC.conn.prepareStatement(stmt);
        ResultSet rs = ps.executeQuery();
        Cust.clearAllCusts();
        while (rs.next()) {
            int cust = rs.getInt("customer_id");
            String custName = rs.getString("customer_name");
            String phone = rs.getString("phone");
            String addr = rs.getString("address");
            String zip = rs.getString("postal_code");
            int divID = rs.getInt("division_id");
            int countryID = FirstLD.convertToCountryID(divID);
            String territory = FirstLD.convertToName(divID);
            String country = Country.convertToName(countryID);
            Cust newCust = new Cust(cust, custName, phone, addr, zip, divID, countryID, territory, country);
            Cust.addCust(newCust);
        }
    }

    /** Inserts a new customer in the database.
     *
     * @param custName
     * @param phone
     * @param addr
     * @param zip
     * @param divID
     * @return id generated by MySQL database (using "select last_insert_id()" statement), otherwise 0.
     * @throws SQLException
     */
    public static int insCust(String custName, String phone, String addr, String zip, int divID) throws SQLException {
        String stmt = "insert into customers (Customer_Name, Phone, Address, Postal_Code, Division_ID) values( ?, ?, ?, ?, ? )";
        PreparedStatement ps = JDBC.conn.prepareStatement(stmt);
        ps.setString(1, custName);
        ps.setString(2, phone);
        ps.setString(3, addr);
        ps.setString(4, zip);
        ps.setInt(5, divID);
        ps.executeUpdate();

        stmt = "select last_insert_id()";
        ps = JDBC.conn.prepareStatement(stmt);
        ResultSet getID = ps.executeQuery();
        while (getID.next()) {
            int id = getID.getInt("last_insert_id()");
            return id;
        }
        return 0;
    }

    /** Updates an existing customer in the database.
     *
     * @param custName
     * @param phone
     * @param addr
     * @param zip
     * @param divID
     * @param cust
     * @throws SQLException
     */
    public static void updtCust(String custName, String phone, String addr, String zip, int divID, int cust) throws SQLException {
        String stmt = "update customers set Customer_Name = ?, Phone = ?, Address = ?, Postal_Code = ?, Division_ID = ? where customer_id = ?";
        PreparedStatement ps = JDBC.conn.prepareStatement(stmt);
        ps.setString(1, custName);
        ps.setString(2, phone);
        ps.setString(3, addr);
        ps.setString(4, zip);
        ps.setInt(5, divID);
        ps.setInt(6, cust);
        ps.executeUpdate();
    }

    /** Deletes an existing customer from the database.
     *
     * @param cust
     * @throws SQLException
     */
    public static void delCust (int cust) throws SQLException {
        String stmt = "delete from customers where customer_id = ?";
        PreparedStatement ps = JDBC.conn.prepareStatement(stmt);
        ps.setInt(1, cust);
        ps.executeUpdate();
    }

    /** Creates new User objects by querying the database and then populates the allUser table with them.
     *
     * @throws SQLException
     */
    public static void populateUsersTable() throws SQLException {
        String stmt = "select * from client_schedule.users";
        PreparedStatement ps = JDBC.conn.prepareStatement(stmt);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int userID = rs.getInt("user_id");
            String userName = rs.getString("user_name");
            String password = rs.getString("password");
            User newUser = new User(userID, userName, password);
            User.add(newUser);
        }
    }

    /** Creates new Contact objects by querying the database and then populates the allContacts table with them.
     *
     * @throws SQLException
     */
    public static void populateContactsTable() throws SQLException {
        String stmt = "select * from client_schedule.contacts";
        PreparedStatement ps = JDBC.conn.prepareStatement(stmt);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int contactID = rs.getInt("contact_id");
            String contactName = rs.getString("contact_name");
            Contact newContact = new Contact(contactID, contactName);
            Contact.addContact(newContact);
        }
    }

    /** Creates new Country objects by querying the database and then populates the allCountries table with them.
     *
     * @throws SQLException
     */
    public static void populateCountriesTable() throws SQLException {
        String stmt = "select * from client_schedule.countries";
        PreparedStatement ps = JDBC.conn.prepareStatement(stmt);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int countryID = rs.getInt("country_id");
            String countryName = rs.getString("country");
            Country newCountry = new Country(countryID, countryName);
            Country.addCountry(newCountry);
        }
    }

    /** Creates new FirstLD objects by querying the database and then populates the allFirstLDs table with them.
     *
     * @throws SQLException
     */
    public static void populateFirstLDsTable() throws SQLException {
        String stmt = "select * from client_schedule.first_level_divisions";
        PreparedStatement ps = JDBC.conn.prepareStatement(stmt);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int firstLDID = rs.getInt("division_id");
            String firstLDName = rs.getString("division");
            int firstLDCountry = rs.getInt("country_id");
            FirstLD newFirstLD = new FirstLD(firstLDID, firstLDName, firstLDCountry);
            FirstLD.addFirstLD(newFirstLD);
        }
    }

    /** Converts a LocalDateTime to UTC for insertion in the database.
     *
     * @param ldt
     * @return
     */
    private static LocalDateTime convertToUTC (LocalDateTime ldt) {
        LocalDateTime utcLDT = ldt.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
        return utcLDT;
    }

    /** Converts a UTC DateTime to LocalDateTime for insertion into Appts table.
     *
     * @param ldt
     * @return
     */
    private static LocalDateTime convertFromUTC (LocalDateTime ldt) {
        LocalDateTime localLDT = ldt.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        return localLDT;
    }

    /** Converts a LocalDateTime to EST for use in determining if a time is outside of business hours.
     *
     * @param ldt
     * @return
     */
    private static LocalDateTime convertToEST (LocalDateTime ldt) {
        LocalDateTime estLDT = ldt.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("America/New_York")).toLocalDateTime();
        return estLDT;
    }

    /** Converts an EST DateTime into LocalDateTime for use in the Appts table.
     *
     * @param ldt
     * @return
     */
    private static LocalDateTime convertFromEST (LocalDateTime ldt) {
        LocalDateTime localLDT = ldt.atZone(ZoneId.of("America/New_York")).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        return localLDT;
    }
}
