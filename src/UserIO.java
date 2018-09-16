import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class UserIO extends Thread {
    private BufferedReader reader;
    private BufferedWriter writer;
    private static Pattern accountPattern = Pattern.compile("^[\\w\\d]+$");

    public UserIO(Socket socket) throws IOException {
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        start();
    }

    @Override
    public void run() {
        while(true) {
            try {
                String input = reader.readLine();
                System.out.println("event: " + input);
                if(input.startsWith("/register")) {
                    writer.write(operateRegister(input) + "\n");
                    writer.flush();
                } else if(input.startsWith("/login")) {
                    writer.write(operateLogin(input) + "\n");
                    writer.flush();
                } else if(input.startsWith("/add")) {
                    System.out.println("in the add switch");
                    writer.write(operateAdd(input) + "\n");
                    writer.flush();
                } else if(input.startsWith("/getdates")) {
                    ResultSet dates = getDates(input);
                    while(dates.next()) {
                        writer.write(String.format("/date%d~%d~%d~%s\n", dates.getInt("day"), dates.getInt("minute_from"),
                                                                        dates.getInt("minute_to"), dates.getString("info")));
                        writer.flush();
                    }
                    writer.write("/end\n");
                    writer.flush();
                }
            } catch(SocketException e) {
                System.out.println("returning");
                return;
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean stringMatches(String s) {
        return accountPattern.matcher(s).find();
    }

    private String operateRegister(String line) throws SQLException {
        String[] args = line.replace("/register", "")
                            .replace("\n", "")
                            .split(" ");
        if(!stringMatches(args[0])) {
            return "/unsuccess:login";
        } else if(!stringMatches(args[1])) {
            return "/unsuccess:password";
        } else {
            Main.selectLoginFromAccounts.setString(1, args[0]);
            ResultSet result = Main.selectLoginFromAccounts.executeQuery();
            if(result.next()) {
                return "/unsuccess:loginExists";
            } else {
                Main.insertAccount.setString(1, args[0]);
                Main.insertAccount.setString(2, args[1]);
                Main.insertAccount.executeUpdate();
                return "/success";
            }
        }
    }

    private String operateLogin(String line) throws SQLException {
        String[] args = line.replace("/login", "")
                            .replace("\n", "")
                            .split(" ");
        Main.selectAccount.setString(1, args[0]);
        Main.selectAccount.setString(2, args[1]);
        ResultSet resultSet = Main.selectAccount.executeQuery();
        if(!resultSet.next()) {
            return "/unsuccess";
        } else {
            return "/success" + resultSet.getInt("id");
        }
    }

    private String operateAdd(String line) throws SQLException {
        System.out.println("in the add");
        String[] args = line.replace("/add", "")
                            .replace("\n", "")
                            .split("~");
        int id = Integer.parseInt(args[0]);
        int day = Integer.parseInt(args[1]);
        int minuteFrom = Integer.parseInt(args[2]);
        int minuteTo = Integer.parseInt(args[3]);
        System.out.println(id + " " + day + " " + minuteFrom + " " + minuteTo + " " + args[4]);

        Main.selectDateFromTime.setInt(1, day);
        Main.selectDateFromTime.setInt(2, minuteFrom);
        Main.selectDateFromTime.setInt(3, minuteFrom);
        if(Main.selectDateFromTime.executeQuery().next()) {
            return "/dateexists";
        }

        Main.selectDateFromTime.setInt(2, minuteTo);
        Main.selectDateFromTime.setInt(3, minuteTo);
        if(Main.selectDateFromTime.executeQuery().next()) {
            return "/dateexists";
        }

        Main.insertDate.setInt(1, id);
        Main.insertDate.setInt(2, day);
        Main.insertDate.setInt(3, minuteFrom);
        Main.insertDate.setInt(4, minuteTo);
        Main.insertDate.setString(5, args[4]);
        Main.insertDate.executeUpdate();
        return "/success";
    }

    private ResultSet getDates(String line) throws SQLException {
        int id = Integer.parseInt(line.replace("/getdates", ""));
        Main.selectDateFromID.setInt(1, id);
        return Main.selectDateFromID.executeQuery();
    }
}
