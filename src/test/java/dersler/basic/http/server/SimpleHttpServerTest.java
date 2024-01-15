package dersler.basic.http.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author cagri.eroglu
 */
public class SimpleHttpServerTest {

    private SimpleHttpServer testServer;
    private static String url;

    public SimpleHttpServerTest() {
    }

    @BeforeEach
    public void setUp() throws IOException {
        url = "http://localhost:1234";
        testServer = new SimpleHttpServer(1234, 1);
        Executors.newSingleThreadExecutor().execute(() -> {
            testServer.start();
        });
    }

    @Test
    public void runTest() throws IOException {
        int sendGetRequest = sendGetRequest();
        assertTrue(sendGetRequest == 200);
    }

    @AfterEach
    public void tearDown() {
        try {
            testServer.stop();
        } catch (IOException ex) {
        }

        System.out.println("Server closed.");
    }

    private static int sendGetRequest() throws IOException {
        URL obj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

        // Setting request method to GET
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        System.out.println("GET Request URL : " + url);
        System.out.println("Response Code : " + responseCode);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            // Print the response
            System.out.println(response.toString());
        }

        return responseCode;
    }
}
