import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Main {
    public static final PreparedStatement selectLoginFromAccounts;
    public static final PreparedStatement selectAccount;
    public static final PreparedStatement insertAccount;
    public static final PreparedStatement insertDate;
    public static final PreparedStatement selectThisDateInsideOthers;
    public static final PreparedStatement selectDateFromID;
    public static final PreparedStatement selectDatesInsideThisDate;
    public static final PreparedStatement deleteDate;
    public static final PreparedStatement updateDate;
    public static final PreparedStatement selectIdOfDate;

    static {
        PreparedStatement tempSelectLogin = null, tempSelectAccount = null, tempInsertAccount = null, tempInsertDate = null,
                          tempSelectThisDateInsideOthers = null, tempSelectDateFromID = null, tempSelectDatesInsideThisDate = null,
                          tempDeleteDate = null, tempUpdateDate = null, tempSelectIdOfDate = null;
        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/calendar?useSSL=false", "root", "root");
            tempSelectLogin = connection.prepareStatement("select * from accounts where login = ?");
            tempSelectAccount = connection.prepareStatement("select * from accounts where login = ? and password = ?");
            tempInsertAccount = connection.prepareStatement("insert into accounts values (null, ?, ?)");
            tempInsertDate = connection.prepareStatement("insert into dates values (null, ?, ?, ?, ?, ?, ?, ?)");
            tempSelectThisDateInsideOthers = connection.prepareStatement("select * from dates where id = ? and year = ? and " +
                                                                              "month = ? and day = ? and " +
                                                                              "(minute_from <= ? and minute_to >= ?)");
            tempSelectDateFromID = connection.prepareStatement("select * from dates where account_id = ? and year = ? and " +
                                                                    "month = ? and day = ?");
            tempSelectDatesInsideThisDate = connection.prepareStatement("select * from dates where id != ? and account_id = ? and year = ? and " +
                                                                            "month = ? and day = ? and (" +
                                                                            "(minute_from > ? and minute_from < ?) or" +
                                                                            "(minute_to > ? and minute_to < ?))");
            tempDeleteDate = connection.prepareStatement("delete from dates where account_id = ? and year = ? and " +
                                                              "month = ? and day = ? and minute_from <= ? and minute_to >= ?");
            tempUpdateDate = connection.prepareStatement("update dates set info = ?, minute_from = ?, minute_to = ? " +
                                                            "where account_id = ? and year = ? and month = ? and day = ? " +
                                                            "and minute_from = ? and minute_to = ?");
            tempSelectIdOfDate = connection.prepareStatement("select id from dates where account_id = ? and year = ? and " +
                                                                 "month = ? and day = ? and minute_from = ? and minute_to = ?" +
                                                                " and info = ?");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        selectLoginFromAccounts = tempSelectLogin;
        selectAccount = tempSelectAccount;
        insertAccount = tempInsertAccount;
        insertDate = tempInsertDate;
        selectThisDateInsideOthers = tempSelectThisDateInsideOthers;
        selectDateFromID = tempSelectDateFromID;
        selectDatesInsideThisDate = tempSelectDatesInsideThisDate;
        deleteDate = tempDeleteDate;
        updateDate = tempUpdateDate;
        selectIdOfDate = tempSelectIdOfDate;
    }

    public static void main(String[] args) throws SQLException, IOException {
        ServerSocket server = new ServerSocket(10000);
        new Thread(() -> {
            while(true) {
                try {
                    Socket socket = server.accept();
                    new UserIO(socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}