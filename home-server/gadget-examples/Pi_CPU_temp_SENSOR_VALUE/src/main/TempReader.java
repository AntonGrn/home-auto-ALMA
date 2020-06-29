package main;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A native SENSOR_VALUE ALMA gadget.
 * 
 * Monitors the CPU temperature of a Raspberry Pi:
 * - Monitor: Remote Pi (reachable by IP) OR local Pi (HomeServer hub).
 * - Note: for monitoring CPU temperature of local Pi (HomeServer hub),
 * consider instead using ALMA plugin 'local_pi_cpu_temp', which doesn't
 * require setting up an additional Linux daemon other than the HomeServer service.
 * *
 * SETUP:
 * 1. Run Pi_CPU_temp_SENSOR_VALUE as Linux daemon service on remote/hub Raspberry Pi.
 * 2. Add json record for gadget in 'gadgets.json' (HomeServer), under "alma".
 * e.g:
 * {
 * "gadget_id": 4,
 * "alias": "System Pi CPU temp(C)",
 * "type": "SENSOR_VALUE",
 * "poll_delay_seconds": 30,
 * "enabled": true,
 * "IP_address": 192.168.120,
 * "TCP_port": 8084,
 * "request_spec": null
 * }
 **
 * Note: If you don't want to update the JRE of Raspberry Pi
 * (which normally support up to Java SE 8 = 52 ):
 * Use prior compiling Java language level:
 * File -> Project Structure -> Project:
 * Project Language Level: 8 -> Apply
 */

public class TempReader {

    public static void main(String[] args) {
        try {
            System.out.println("Gadget is alive");
            listenForPollRequests();
        } catch (IOException e) {
            System.out.println("Gadget is terminating");
        }
    }

    private static void listenForPollRequests() throws IOException {
        BufferedReader input = null;
        BufferedWriter output = null;
        ServerSocket gadgetSocket = new ServerSocket(8084);
        while (true) {
            String request = null;
            // Receive TCP Socket connection request from Home Server
            try (Socket homeServerConnection = gadgetSocket.accept()) {
                // Obtain input and output streams
                input = new BufferedReader(new InputStreamReader(homeServerConnection.getInputStream()));
                output = new BufferedWriter(new OutputStreamWriter(homeServerConnection.getOutputStream()));

                while ((request = input.readLine()) != null) {
                    String[] almaRequest = request.split(":");
                    if (almaRequest[0].equals("15")) {
                        // if request: poll gadget state
                        try {
                            String pollResponse = String.format("%s%s%n", "16:", String.valueOf(read_CPU_temp()));
                            output.write(pollResponse);
                            output.flush();
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            } finally {
                if (input != null) {
                    input.close();
                }
                if (output != null) {
                    output.close();
                }
            }
        }
    }

    private static int read_CPU_temp() throws Exception {
        String fileName = "/sys/class/thermal/thermal_zone0/temp";
        BufferedReader bufferedReader = null;
        String line = null;
        int tempC = 0;

        try (FileReader fileReader = new FileReader(fileName)) {
            bufferedReader = new BufferedReader(fileReader);

            while ((line = bufferedReader.readLine()) != null) {
                tempC = (Integer.parseInt(line) / 1000);
            }

            return tempC;
        } catch (FileNotFoundException ex) {
            throw new Exception("Unable to open file '" + fileName + "'");
        } catch (IOException ex) {
            throw new Exception("Error reading file '" + fileName + "'");
        } catch (NumberFormatException e) {
            throw new Exception("Invalid number format of value: " + line);
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
    }
}
