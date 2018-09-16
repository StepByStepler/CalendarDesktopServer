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
    public static final PreparedStatement selectDateFromTime;
    public static final PreparedStatement selectDateFromID;
    static {
        PreparedStatement tempSelectLogin = null, tempSelectAccount = null, tempInsertAccount = null, tempInsertDate = null,
                          tempSelectDateFromTime = null, tempSelectDateFromID = null;
        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/calendar?useSSL=false", "root", "root");
            tempSelectLogin = connection.prepareStatement("select * from accounts where login = ?");
            tempSelectAccount = connection.prepareStatement("select * from accounts where login = ? and password = ?");
            tempInsertAccount = connection.prepareStatement("insert into accounts values (null, ?, ?)");
            tempInsertDate = connection.prepareStatement("insert into dates values (null, ?, ?, ?, ?, ?)");
            tempSelectDateFromTime = connection.prepareStatement("select * from dates where day = ? and " +
                                                                  "(minute_from <= ? and minute_to >= ?)");
            tempSelectDateFromID = connection.prepareStatement("select * from dates where id = ?");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        selectLoginFromAccounts = tempSelectLogin;
        selectAccount = tempSelectAccount;
        insertAccount = tempInsertAccount;
        insertDate = tempInsertDate;
        selectDateFromTime = tempSelectDateFromTime;
        selectDateFromID = tempSelectDateFromID;
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