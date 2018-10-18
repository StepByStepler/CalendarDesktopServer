import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

public class UserIO extends Thread {
    private BufferedReader reader;
    private BufferedWriter writer;
    private static Pattern accountPattern = Pattern.compile("^[\\w\\d]+$");
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    public UserIO(Socket socket) throws IOException {
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
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
//                    ResultSet dates = loadDates(input);
//                    while(dates.next()) {
//                        writer.write(String.format("/date%d~%d~%s\n", dates.getInt("minute_from"),
//                                dates.getInt("minute_to"), dates.getString("info")));
//                        writer.flush();
//                    }
//                    writer.write("/end\n");
                    loadDates(input);
                    writer.flush();
                } else if(input.startsWith("/delete")) {
                    writer.write(operateDelete(input) + "\n");
                    writer.flush();
                } else if(input.startsWith("/update")) {
                    writer.write(operateUpdate(input) + "\n");
                    writer.flush();
                }
            } catch(SocketException e) {
                return;
            } catch (IOException | SQLException | ParseException e) {
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
                Main.insertAccount.setString(2, hashPassword(args[1]));
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
        Main.selectAccount.setString(2, hashPassword(args[1]));
        ResultSet resultSet = Main.selectAccount.executeQuery();
        if(!resultSet.next()) {
            return "/unsuccess";
        } else {
            return "/success" + resultSet.getInt("id");
        }
    }

    private String operateAdd(String line) throws SQLException, ParseException {
        String[] args = line.replace("/add", "")
                            .replace("\n", "")
                            .split("~");
        int id = Integer.parseInt(args[0]);
        Calendar dateFrom = new Calendar.Builder().setInstant(dateFormat.parse(args[1])).build();
        Calendar dateTo = new Calendar.Builder().setInstant(dateFormat.parse(args[2])).build();

        System.out.println("dateFrom: " + dateFrom.getTime());
        System.out.println("dateTo: " + dateTo.getTime());
        String info = args[3];

        if(dateFrom.get(Calendar.DAY_OF_MONTH) != dateTo.get(Calendar.DAY_OF_MONTH)
                || dateFrom.get(Calendar.MONTH) != dateTo.get(Calendar.MONTH)
                || dateFrom.get(Calendar.YEAR) != dateTo.get(Calendar.YEAR)) {
            System.out.println("incorrect");
            return "/incorrecttime";
        }

        if(dateAlreadyExists(-1, id, dateFrom, dateTo)) {
            System.out.println("dateExists");
            return "/dateexists";
        }

        Timestamp date1 = new Timestamp(dateFrom.getTimeInMillis());
        Timestamp date2 = new Timestamp(dateTo.getTimeInMillis());

        Main.insertDate.setInt(1, id);
        Main.insertDate.setTimestamp(2, date1);
        Main.insertDate.setTimestamp(3, date2);
        Main.insertDate.setString(4, info);
        Main.insertDate.executeUpdate();
        return "/success";
    }

    private void loadDates(String line) throws SQLException, ParseException, IOException {
        String[] args = line.replace("/getdates", "")
                            .replace("\n", "")
                            .split("~");
        int id = Integer.parseInt(args[0]);
        Main.selectAllUserDates.setInt(1, id);
        ResultSet allDates = Main.selectAllUserDates.executeQuery();

        Date dateFrom = dateFormat.parse(args[1]);
        Date dateTo = dateFormat.parse(args[2]);

        while(allDates.next()) {
            Date currentFrom = new Date(allDates.getTimestamp("date_from").getTime());
            Date currentTo = new Date(allDates.getTimestamp("date_to").getTime());

            if(dateFrom.before(currentFrom) && dateTo.after(currentTo)) {
                writer.write(String.format("/date%s~%s~%s\n", dateFormat.format(currentFrom),
                        dateFormat.format(currentTo),
                        allDates.getString("info")));
                writer.flush();
            }
        }

        writer.write("/end\n");
        writer.flush();
    }

    private String operateDelete(String line) throws SQLException, ParseException {
        String[] args = line.replace("/delete", "")
                            .replace("\n", "")
                            .split("~");
        int id = Integer.parseInt(args[0]);
        Date dateFrom = dateFormat.parse(args[1]);
        Date dateTo = dateFormat.parse(args[2]);

        Timestamp timestampFrom = new Timestamp(dateFrom.getTime());
        Timestamp timestampTo = new Timestamp(dateTo.getTime());

        System.out.println("from: " + timestampFrom);
        System.out.println("to: " + timestampTo);

        Main.deleteDate.setInt(1, id);
        Main.deleteDate.setString(2, args[1]);
        Main.deleteDate.setString(3, args[2]);

        int changed = Main.deleteDate.executeUpdate();
        if(changed > 0) {
            return "/changed";
        } else {
            return "/notchanged";
        }
    }

    private String operateUpdate(String line) throws SQLException, ParseException {
        String[] args = line.replace("/update", "")
                            .replace("\n", "")
                            .split("~");
        int account_id = Integer.parseInt(args[0]);

        Calendar oldDateFrom = new Calendar.Builder().setInstant(dateFormat.parse(args[1])).build();
        Calendar oldDateTo = new Calendar.Builder().setInstant(dateFormat.parse(args[2])).build();

        Calendar newDateFrom = new Calendar.Builder().setInstant(dateFormat.parse(args[3])).build();
        Calendar newDateTo = new Calendar.Builder().setInstant(dateFormat.parse(args[4])).build();

        String oldText = args[5];
        String newText = args[6];

        Main.selectIdOfDate.setInt(1, account_id);
        Main.selectIdOfDate.setString(2, args[1]);
        Main.selectIdOfDate.setString(3, args[2]);
        Main.selectIdOfDate.setString(4, oldText);

        ResultSet resultId = Main.selectIdOfDate.executeQuery();
        resultId.next();
        int id = resultId.getInt("id");

        System.out.println("account id: " + account_id);
        System.out.println("id: " + id + "\n");
        System.out.println("oldFrom: " + oldDateFrom.getTime());
        System.out.println("oldTo: " + oldDateTo.getTime() + "\n");
        System.out.println("newFrom: " + newDateFrom.getTime());
        System.out.println("newTo: " + newDateTo.getTime() + "\n");


        if(dateAlreadyExists(id, account_id, newDateFrom, newDateTo)) {
            return "/dateexists";
        }

        Main.updateDate.setTimestamp(1, new Timestamp(newDateFrom.getTimeInMillis()
                                                                  + newDateFrom.getTimeZone().getRawOffset()));
        Main.updateDate.setTimestamp(2, new Timestamp(newDateTo.getTimeInMillis()
                                                                  + newDateTo.getTimeZone().getRawOffset()));
        Main.updateDate.setString(3, newText);
        Main.updateDate.setInt(4, account_id);
        Main.updateDate.setString(5, args[1]);
        Main.updateDate.setString(6, args[2]);
        Main.updateDate.setString(7, oldText);

        Main.updateDate.executeUpdate();

        return "/success";
    }

    private boolean dateAlreadyExists(int id, int account_id, Calendar newDateFrom, Calendar newDateTo) throws SQLException {
        newDateFrom.setTimeInMillis(newDateFrom.getTimeInMillis() - newDateFrom.getTimeZone().getRawOffset());
        newDateTo.setTimeInMillis(newDateTo.getTimeInMillis() - newDateTo.getTimeZone().getRawOffset());

        Main.selectAllUserDates.setInt(1, account_id);
        ResultSet allDates = Main.selectAllUserDates.executeQuery();
        while(allDates.next()) {
            if(allDates.getInt("id") != id) {
                Calendar dateFrom = new Calendar.Builder().setInstant(allDates.getTimestamp("date_from")).build();
                Calendar dateTo = new Calendar.Builder().setInstant(allDates.getTimestamp("date_to")).build();

                if ((dateFrom.before(newDateFrom) && dateTo.after(newDateFrom))
                        || (dateFrom.before(newDateTo) && dateTo.after(newDateTo))) {
                    return true;
                }

                if ((newDateFrom.before(dateFrom) && newDateTo.after(dateTo))
                        || newDateFrom.before(dateTo) && newDateTo.after(dateTo)) {
                    return true;
                }
            }
        }

        newDateFrom.setTimeInMillis(newDateFrom.getTimeInMillis() + newDateFrom.getTimeZone().getRawOffset());
        newDateTo.setTimeInMillis(newDateTo.getTimeInMillis() + newDateTo.getTimeZone().getRawOffset());

        return false;
    }

    private String hashPassword(String password) {
        MessageDigest messageDigest = null;
        byte[] digest = new byte[0];

        try {
            messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(password.getBytes());
            digest = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        BigInteger bigInt = new BigInteger(1, digest);
        StringBuilder md5Hex = new StringBuilder(bigInt.toString(16));

        while( md5Hex.length() < 32 ){
            md5Hex.insert(0, "0");
        }
        return md5Hex.toString();

    }
}
