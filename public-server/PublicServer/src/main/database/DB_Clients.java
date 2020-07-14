package main.database;

import main.JSON.JSON_reader;

import java.sql.*;

public class DB_Clients {

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

    // Singleton
    private static DB_Clients instance = null;

    public static DB_Clients getInstance() {
        if (instance == null) {
            instance = new DB_Clients();
            instance.setDbSpecs();
        }
        return instance;
    }

    private DB_Clients() {}

    private void setDbSpecs() {
        try {
            // Read database specs from JSON file.
            String[] dbSpecs = JSON_reader.loadSpecsClientDB();
            ip = dbSpecs[0];
            port = dbSpecs[1];
            database = dbSpecs[2];
            account = dbSpecs[3];
            password = dbSpecs[4];
        } catch (Exception e) {
            System.out.println("Unable to load client DB specs from JSON file.");
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

    public String[] androidLogin(String userName, String password, String newSessionKey) throws Exception {
        //catch or declare. Here: declare so we can pass exception messages back to client application
        connect();
        String[] items = new String[2];
        int results = 0;
        try {
            // Get systemID and admin-state
            preparedStatement = connection.prepareStatement("SELECT systemID, admin FROM Client_Android WHERE name = ? AND password = ?;");
            preparedStatement.setString(1, userName);
            preparedStatement.setString(2, password);
            resultSet = preparedStatement.executeQuery();

            //Note: If Query gives no result, the while(next) below won't launch.
            while (resultSet.next()) {
                results++;
                //We already have user name from the method parameters (so we don't need to acquire it from DB_Clients).
                String homeServerID = resultSet.getString("systemID"); // As String, while it is an integer in MySQL
                String admin = resultSet.getString("admin");

                items[0] = homeServerID;
                items[1] = admin;
            }

            if (results != 1) { //If there was to few matches, or for some reason, multiple matches.
                throw new Exception("Login failed. Connection is good");
            }
            // Update sessionKey to DB_Clients
            preparedStatement = connection.prepareStatement("UPDATE Client_Android SET sessionKey = ? WHERE name = ?;");
            preparedStatement.setString(1, newSessionKey);
            preparedStatement.setString(2, userName);
            results = preparedStatement.executeUpdate();
            if (results != 1) {
                throw new Exception("Server unable to update session key. Code 2");
            }
        } catch (SQLException e) {
            throw new Exception("Error on SQL query. Code 1");
        } catch (NullPointerException e) {
            throw new Exception("NullPointer Exception");
        } finally {
            closeConnection();
        }
        return items;
    }

    public String[] androidReconnect(String userName, String sessionKey) throws Exception{
        String[] items = new String[2];
        try{
            connect();
            int results = 0;

            // For protection. Make sure no noe tries wth standard null values
            if(sessionKey.equalsIgnoreCase("Key destroyed") || sessionKey.equalsIgnoreCase("Unassigned")) {
                throw new Exception("Reconnection failed. Connection is good");
            }
            // Get systemID and admin-state
            preparedStatement = connection.prepareStatement("SELECT systemID, admin FROM Client_Android WHERE name = ? AND sessionKey = ?;");
            preparedStatement.setString(1, userName);
            preparedStatement.setString(2, sessionKey);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                results++;
                String homeServerID = resultSet.getString("systemID");
                String admin = resultSet.getString("admin");

                items[0] = homeServerID;
                items[1] = admin;
            }
            if (results != 1) {
                throw new Exception("Reconnection failed. Connection is good");
            }
        } catch(SQLException e) {
            throw new Exception("Error on SQL query. Code2");
        }catch (NullPointerException e) {
            throw new Exception("NullPointer Exception");
        } finally {
            closeConnection();
        }
        return items;
    }

    public void destroySessionKey(String userName) throws Exception{
        try {
            connect();
            int results = 0;
            // Update sessionKey to DB_Clients
            preparedStatement = connection.prepareStatement("UPDATE Client_Android SET sessionKey = 'Key destroyed' WHERE name = ?;");
            preparedStatement.setString(1, userName);
            results = preparedStatement.executeUpdate();

            if (results != 1) {
                throw new Exception("Server unable to destroy session key.");
            }
        } catch(SQLException e) {
            throw new Exception("Error on SQL query. Code3");
        }catch (NullPointerException e) {
            throw new Exception("NullPointer Exception");
        } finally {
            closeConnection();
        }
    }

    public void homeServerLogin(int homeServerID, String password) throws Exception {
        boolean banned = true;
        try{
            connect();
            int results = 0;
            // Verify login data and get systemName
            preparedStatement = connection.prepareStatement("SELECT banned FROM Client_HomeServer WHERE systemID = ? AND password = ?;");
            preparedStatement.setInt(1, homeServerID);
            preparedStatement.setString(2, password);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                results++;
                banned =(resultSet.getInt("banned") == 1);
            }
            if (results != 1) {
                throw new Exception("Login failed. Connection is good");
            }
            if(banned) {
                throw new Exception("Home server is banned");
            }
        } catch(SQLException e) {
            throw new Exception("Error on SQL query. Code4");
        }catch (NullPointerException e) {
            throw new Exception("NullPointer Exception");
        } finally {
            closeConnection();
        }
    }
}