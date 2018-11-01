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
    public static final PreparedStatement deleteDate;
    public static final PreparedStatement updateDate;
    public static final PreparedStatement selectIdOfDate;
    public static final PreparedStatement selectAllUserDates;

    static {
        PreparedStatement tempSelectLogin = null, tempSelectAccount = null, tempInsertAccount = null, tempInsertDate = null,
                          tempDeleteDate = null, tempUpdateDate = null, tempSelectIdOfDate = null, tempSelectAllUserDates = null;
        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/calendar?useSSL=false", "root", "root");
            tempSelectLogin = connection.prepareStatement("select * from accounts where login = ?");
            tempSelectAccount = connection.prepareStatement("select * from accounts where login = ? and password = ?");
            tempInsertAccount = connection.prepareStatement("insert into accounts values (null, ?, ?, ?)");
            tempInsertDate = connection.prepareStatement("insert into dates values (null, ?, ?, ?, ?)");
            tempDeleteDate = connection.prepareStatement("delete from dates where account_id = ? and date_from = ? " +
                                                              "and date_to = ?");
            tempUpdateDate = connection.prepareStatement("update dates set date_from = ?, date_to = ?, info = ? " +
                                                            "where account_id = ? and date_from = ? and date_to = ? and info = ?");
            tempSelectIdOfDate = connection.prepareStatement("select id from dates where account_id = ? and " +
                                                                "date_from = ? and date_to = ? and info = ?");
            tempSelectAllUserDates = connection.prepareStatement("select * from dates where account_id = ?");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        selectLoginFromAccounts = tempSelectLogin;
        selectAccount = tempSelectAccount;
        insertAccount = tempInsertAccount;
        insertDate = tempInsertDate;
        deleteDate = tempDeleteDate;
        updateDate = tempUpdateDate;
        selectIdOfDate = tempSelectIdOfDate;
        selectAllUserDates = tempSelectAllUserDates;
    }

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(10000);
        new Thread(() -> {
            while(true) {
                try {
                    Socket socket = server.accept();
                    new UserIO(socket).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}