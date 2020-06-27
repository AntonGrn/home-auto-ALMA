package main.database;

import main.JSON.JSON_reader;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Responsible for logging client traffic to database table: Client_Traffic
 */

public class DB_TrafficLogging {

    // DB authentication
    private String ip;
    private String port;
    private String database;
    private String account;
    private String password;

    //DB operations
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private static DB_TrafficLogging instance = null;

    // Singleton
    public static DB_TrafficLogging getInstance() {
        if (instance == null) {
            instance = new DB_TrafficLogging();
            instance.setDbSpecs();
        }
        return instance;
    }

    private DB_TrafficLogging() {}

    private void setDbSpecs() {
        try {
            // Read database specs from JSON file.
            String[] dbSpecs = JSON_reader.loadSpecsTrafficLogsDB();
            ip = dbSpecs[0];
            port = dbSpecs[1];
            database = dbSpecs[2];
            account = dbSpecs[3];
            password = dbSpecs[4];
        } catch (Exception e) {
            System.out.println("Unable to load traffic logs DB specs from JSON file.");
        }
    }

    private void connect() {
        connection = null;

        String url = "jdbc:mysql://" + ip + ":" + port + "/" + database + "?useSSL=false&user=" + account + "&password=" + password + "&serverTimezone=UTC";
        try {
            connection = DriverManager.getConnection(url);
            preparedStatement = null;
        } catch (SQLException ex) {
            System.out.println("DB_Clients connection error");
            System.out.println(ex.getMessage());
            closeConnection();
        }
    }

    private void closeConnection() {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                System.out.println("Error on closing DB_Clients ResultSet");
            }
        }

        if (preparedStatement != null) {
            try {
                preparedStatement.close();
                preparedStatement = null;
            } catch (SQLException e) {
                System.out.println("Error on closing DB_Clients PreparedStatement");
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.out.println("Error on closing DB_Clients connection");
            }
        }
    }


    public void clientConnectionLoginResult(String clientName, String clientType, String clientIP) throws Exception {
        // 1. Check if the client has contacted the service before (based on IP and clientName)
        // 2. If client is recognized: Update its record
        // 3. If client is not recognized: Create new record
        connect();
        try {
            preparedStatement = connection.prepareStatement("SELECT nbrOfConnections FROM ClientTraffic WHERE clientName = ? AND clientIP = ?;");
            preparedStatement.setString(1, clientName);
            preparedStatement.setString(2, clientIP);
            resultSet = preparedStatement.executeQuery();

            int nbrOfConnections = -1;
            int clientRecognized = 0;
            while (resultSet.next()) {
                clientRecognized++;
                nbrOfConnections = resultSet.getInt("nbrOfConnections");
            }

            if (clientRecognized == 1) {
                // Update currently existing record
                preparedStatement = connection.prepareStatement("UPDATE ClientTraffic SET nbrOfConnections = ?, lastConnStart = ? WHERE clientName = ? AND clientIP = ?;");
                preparedStatement.setInt(1, ++nbrOfConnections);
                preparedStatement.setString(2, getTimeStamp());
                preparedStatement.setString(3, clientName);
                preparedStatement.setString(4, clientIP);
                if (preparedStatement.executeUpdate() != 1) {
                    throw new Exception("Unable to update client log. Code 1");
                }

            } else {
                // Create new record
                preparedStatement = connection.prepareStatement("INSERT INTO ClientTraffic (clientName, clientType, clientIP, nbrOfConnections, lastConnStart, lastConnClose, firstConn) VALUES (?,?,?,?,?,'-',?);");
                preparedStatement.setString(1, clientName);
                preparedStatement.setString(2, clientType);
                preparedStatement.setString(3, clientIP);
                preparedStatement.setInt(4, 1);
                preparedStatement.setString(5, getTimeStamp());
                preparedStatement.setString(6, getTimeStamp());
                preparedStatement.execute();
            }

        } catch (SQLException e) {
            throw new Exception("SQL exception in logging client traffic. Code 2");
        } finally {
            closeConnection();
        }
    }

    public void clientConnectionClose(String clientName, String clientIP) throws Exception {
        connect();
        try {
            preparedStatement = connection.prepareStatement("UPDATE ClientTraffic SET lastConnClose = ? WHERE clientName = ? AND clientIP = ?;");
            preparedStatement.setString(1, getTimeStamp());
            preparedStatement.setString(2, clientName);
            preparedStatement.setString(3, clientIP);
            if (preparedStatement.executeUpdate() != 1) {
                throw new Exception("Unable to update client log. Code 3");
            }
        } catch (SQLException e) {
            throw new Exception("SQL Exception in logging client traffic. Code 4");
        } finally {
            closeConnection();
        }
    }

    private String getTimeStamp() {
        return new SimpleDateFormat("YYYY-MM-dd [HH:mm:ss]").format(new Date());
    }

}
