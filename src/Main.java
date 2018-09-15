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
    static {
        PreparedStatement tempSelectLogin = null, tempSelectAccount = null;
        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/calendar?useSSL=false", "root", "root");
            tempSelectLogin = connection.prepareStatement("select * from accounts where login = ?");
            tempSelectAccount = connection.prepareStatement("select * from accounts where login = ? and password = ?");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        selectLoginFromAccounts = tempSelectLogin;
        selectAccount = tempSelectAccount;
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