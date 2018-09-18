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
                if(input.startsWith("/register")) {
                    writer.write(operateRegister(input) + "\n");
                    writer.flush();
                } else if(input.startsWith("/login")) {
                    writer.write(operateLogin(input) + "\n");
                    writer.flush();
                } else if(input.startsWith("/add")) {
                    writer.write(operateAdd(input) + "\n");
                    writer.flush();
                } else if(input.startsWith("/getdates")) {
                    ResultSet dates = getDates(input);
                    while(dates.next()) {
                        writer.write(String.format("/date%d~%d~%s\n", dates.getInt("minute_from"),
                                dates.getInt("minute_to"), dates.getString("info")));
                        writer.flush();
                    }
                    writer.write("/end\n");
                    writer.flush();
                } else if(input.startsWith("/delete")) {
                    writer.write(operateDelete(input) + "\n");
                    writer.flush();
                }
            } catch(SocketException e) {
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
        String[] args = line.replace("/add", "")
                            .replace("\n", "")
                            .split("~");
        int id = Integer.parseInt(args[0]);
        int year = Integer.parseInt(args[1]);
        int month = Integer.parseInt(args[2]);
        int day = Integer.parseInt(args[3]);
        int minuteFrom = Integer.parseInt(args[4]);
        int minuteTo = Integer.parseInt(args[5]);

        if(minuteFrom < 0 || minuteTo < 0) {
            return "/incorrecttime";
        }

        Main.selectThisDateInsideOthers.setInt(1, id);
        Main.selectThisDateInsideOthers.setInt(2, year);
        Main.selectThisDateInsideOthers.setInt(3, month);
        Main.selectThisDateInsideOthers.setInt(4, day);
        Main.selectThisDateInsideOthers.setInt(5, minuteFrom);
        Main.selectThisDateInsideOthers.setInt(6, minuteFrom);
        if(Main.selectThisDateInsideOthers.executeQuery().next()) {
            return "/dateexists";
        }

        Main.selectThisDateInsideOthers.setInt(5, minuteTo);
        Main.selectThisDateInsideOthers.setInt(6, minuteTo);
        if(Main.selectThisDateInsideOthers.executeQuery().next()) {
            return "/dateexists";
        }

        Main.selectDatesInsideThisDate.setInt(1, id);
        Main.selectDatesInsideThisDate.setInt(2, year);
        Main.selectDatesInsideThisDate.setInt(3, month);
        Main.selectDatesInsideThisDate.setInt(4, day);
        Main.selectDatesInsideThisDate.setInt(5, minuteFrom);
        Main.selectDatesInsideThisDate.setInt(6, minuteTo);
        Main.selectDatesInsideThisDate.setInt(7, minuteFrom);
        Main.selectDatesInsideThisDate.setInt(8, minuteTo);
        if(Main.selectDatesInsideThisDate.executeQuery().next()) {
            return "/dateexists";
        }

        Main.insertDate.setInt(1, id);
        Main.insertDate.setInt(2, year);
        Main.insertDate.setInt(3, month);
        Main.insertDate.setInt(4, day);
        Main.insertDate.setInt(5, minuteFrom);
        Main.insertDate.setInt(6, minuteTo);
        Main.insertDate.setString(7, args[6]);
        Main.insertDate.executeUpdate();
        return "/success";
    }

    private ResultSet getDates(String line) throws SQLException {
        String[] args = line.replace("/getdates", "")
                            .replace("\n", "")
                            .split("~");
        int id = Integer.parseInt(args[0]);
        int year = Integer.parseInt(args[1]);
        int month = Integer.parseInt(args[2]);
        int day = Integer.parseInt(args[3]);

        Main.selectDateFromID.setInt(1, id);
        Main.selectDateFromID.setInt(2, year);
        Main.selectDateFromID.setInt(3, month);
        Main.selectDateFromID.setInt(4, day);

        return Main.selectDateFromID.executeQuery();
    }

    private String operateDelete(String line) throws SQLException {
        String[] args = line.replace("/delete", "")
                            .replace("\n", "")
                            .split("~");
        int id = Integer.parseInt(args[0]);
        int year = Integer.parseInt(args[1]);
        int month = Integer.parseInt(args[2]);
        int day = Integer.parseInt(args[3]);
        int minute = Integer.parseInt(args[4]);

        Main.deleteDate.setInt(1, id);
        Main.deleteDate.setInt(2, year);
        Main.deleteDate.setInt(3, month);
        Main.deleteDate.setInt(4, day);
        Main.deleteDate.setInt(5, minute);
        Main.deleteDate.setInt(6, minute);

        int changed = Main.deleteDate.executeUpdate();
        if(changed > 0) {
            return "/changed";
        } else {
            return "/notchanged";
        }
    }
}
