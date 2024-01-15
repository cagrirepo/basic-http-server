package dersler.basic.http.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleHttpServer {

    private ServerSocket serverSocket;
    private ExecutorService pool;
    private Set<String> dataStore = Collections.synchronizedSet(new HashSet<>());
    private boolean running = true;

    public SimpleHttpServer(int port, int poolSize) throws IOException {
        serverSocket = new ServerSocket(port);
        pool = Executors.newFixedThreadPool(poolSize);
    }

    public void start() {
        System.out.println("Server started...");
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                pool.execute(new ClientHandler(clientSocket, dataStore));
            } catch (IOException ex) {
                Logger.getLogger(SimpleHttpServer.class.getName()).log(Level.SEVERE, "Socket closed.");
            }
        }
    }

    public void stop() throws IOException {
        running = false;
        serverSocket.close();
        pool.shutdownNow();
    }

    private static class ClientHandler implements Runnable {

        private final Socket clientSocket;
        private final Set<String> dataStore;

        ClientHandler(Socket socket, Set<String> dataStore) {
            this.clientSocket = socket;
            this.dataStore = dataStore;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String line;
                StringBuilder request = new StringBuilder();
                String method = "";
                int contentLength = 0;
                while (true) {
                    line = in.readLine();
                    if (line == null || line.isEmpty()) {
                        break;
                    }
                    request.append(line).append("\n");
                    if (line.startsWith("GET") || line.startsWith("POST") || line.startsWith("PUT") || line.startsWith("OPTIONS")) {
                        method = line.split(" ")[0];
                    }

                    // Extract content length for POST requests
                    if (line.startsWith("Content-Length: ")) {
                        contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
                    }
                }

                String rbody = "";
                if (method.startsWith("POST") || method.startsWith("PUT") && contentLength > 0) {
                    char[] body = new char[contentLength];
                    in.read(body, 0, contentLength);
                    rbody = new String(body);
                }

                logRequest(request.toString() + "\n" + rbody);

                switch (method) {
                    case "GET":
                        if (request.toString().contains("responsive")) {
                            handleResponsiveGet(out);
                        } else {
                            handleGet(out);
                        }

                        break;
                    case "POST":
                        handlePost(out, dataStore, rbody); // Here you can extract actual data from the request
                        break;
                    case "PUT":
                        handlePut(out, dataStore, rbody); // Here you can extract actual data from the request
                        break;
                    case "OPTIONS":
                        handleOptions(out);
                        break;
                    default:
                        handleNotFound(out);
                }

                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 'Toyota', 'Ford', 'Mercedes', 'BMW', 'Audi', 'Honda', 'Nissan', 'Volkswagen', 'Hyundai', 'Chevrolet'
        private String createCarList() {
            StringBuilder builder = new StringBuilder();
            for (String element : dataStore) {
                builder.append("'").append(element).append("',");
            }
            return builder.toString();
        }

        private String createCarListHtml() {
            StringBuilder builder = new StringBuilder();
            for (String element : dataStore) {
                builder.append("<ul>").append(element).append("</ul>");
            }
            return builder.toString();
        }

        private void handleGet(PrintWriter out) {
            String htmlResponse = "<!DOCTYPE html>"
                    + "<html lang='en'>"
                    + "<head>"
                    + "<meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                    + "<title>Car Brands</title>"
                    + "<style>"
                    + "table { width: 100%; border-collapse: collapse; }"
                    + "th, td { border: 1px solid black; padding: 8px; text-align: left; }"
                    + "th { background-color: #f2f2f2; }"
                    + "</style>"
                    + "</head>"
                    + "<body>"
                    + "<h1>Car Brands</h1>"
                    + "<table id='carTable'>"
                    + "<tr><th>Brand</th></tr>"
                    + // Table header
                    "</table>"
                    + // Form for POST request
                    "<form action='/' method='post'>"
                    + "<input type='text' name='brand' placeholder='Enter Car Brand'>"
                    + "<button type='submit'>Submit</button>"
                    + "</form>"
                    + "<script>"
                    + "var carBrands = [" + createCarList() + "];"
                    + "var table = document.getElementById('carTable');"
                    + "carBrands.forEach(function(brand) {"
                    + "  var row = table.insertRow();"
                    + "  var cell1 = row.insertCell(0);"
                    + "  cell1.innerHTML = brand;"
                    + "});"
                    + "</script>"
                    + "</body>"
                    + "</html>";

            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println();
            out.println(htmlResponse);
        }

        private void handleResponsiveGet(PrintWriter out) {
            String htmlResponse = "<!DOCTYPE html>\n"
                    + "<html lang=\"en\">\n"
                    + "<head>\n"
                    + "    <meta charset=\"UTF-8\">\n"
                    + "    <title>Add Brand</title>\n"
                    + "</head>\n"
                    + "<body>\n"
                    + "    <h2>Add a New Brand</h2>\n"
                    + "    <input type=\"text\" id=\"brandName\" placeholder=\"Enter brand name\">\n"
                    + "    <button onclick=\"addBrand()\">Add Brand</button>\n"
                    + "\n"
                    + "    <h3>Brand List:</h3>\n"
                    + createCarListHtml()
                    + "    <ul id=\"brandList\"></ul>\n"
                    + "\n"
                    + "    <script>function addBrand() {\n"
                    + "    var brandName = document.getElementById('brandName').value;\n"
                    + " if (brandName === 'toyota') { return; }"
                    + "    \n"
                    + "    // Check if the textbox is not empty\n"
                    + "    if (brandName) {\n"
                    + "        // Create the XMLHttpRequest object\n"
                    + "        var xhr = new XMLHttpRequest();\n"
                    + "\n"
                    + "        // Specify the method and URL\n"
                    + "        xhr.open(\"POST\", \"\", true);\n"
                    + "        xhr.setRequestHeader(\"Content-Type\", \"application/x-www-form-urlencoded\");\n"
                    + "\n"
                    + "        // Define what happens on successful data submission\n"
                    + "        xhr.onload = function() {\n"
                    + "            if (xhr.status == 200) {\n"
                    + "                // Add the new brand to the list\n"
                    + "                updateBrandList(brandName);\n"
                    + "                // Clear the input field\n"
                    + "                document.getElementById('brandName').value = '';\n"
                    + "            } else {\n"
                    + "                console.error(\"Error in request: \", xhr.responseText);\n"
                    + "            }\n"
                    + "        };\n"
                    + "\n"
                    + "        // Send the request with data\n"
                    + "        xhr.send(encodeURIComponent(brandName));\n"
                    + "    } else {\n"
                    + "        alert(\"Please enter a brand name.\");\n"
                    + "    }\n"
                    + "}\n"
                    + "\n"
                    + "function updateBrandList(brand) {\n"
                    + "    var brandList = document.getElementById('brandList');\n"
                    + "    var newBrand = document.createElement('li');\n"
                    + "    newBrand.textContent = brand;\n"
                    + "    brandList.appendChild(newBrand);\n"
                    + "}</script>\n"
                    + "</body>\n"
                    + "</html>";

            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println();
            out.println(htmlResponse);
        }

        private void handlePost(PrintWriter out, Set<String> dataStore, String data) {
            if (data != null && data.split("=").length == 2) {
                data = data.split("=")[1];
            }

            dataStore.add(data);
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println();
            out.println("<h1>POST Request Processed</h1>");
            out.println("<p>Data added: " + data + "</p>");
        }

        private void handlePut(PrintWriter out, Set<String> dataStore, String data) {
            if (dataStore.add(data)) {
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/html");
                out.println();
                out.println("<h1>PUT Request Processed</h1>");
                out.println("<p>Data added: " + data + "</p>");
            } else {
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/html");
                out.println();
                out.println("<h1>PUT Request Processed</h1>");
                out.println("<p>Data already exists: " + data + "</p>");
            }
        }

        private void handleOptions(PrintWriter out) {
            out.println("HTTP/1.1 204 No Content");
            out.println("Allow: GET, POST, PUT, OPTIONS");
            out.println();
        }

        private void handleNotFound(PrintWriter out) {
            out.println("HTTP/1.1 404 Not Found");
            out.println("Content-Type: text/html");
            out.println();
            out.println("<h1>404 Not Found</h1>");
        }

        private void logRequest(String request) {
            if (request == null || request.isBlank()) {

            }
            System.out.println("[" + new Date() + "] Received request:");
            System.out.println(request);
        }
    }

    public static void main(String[] args) throws IOException {
        int port = 80;
        int poolSize = 10;
        SimpleHttpServer server = new SimpleHttpServer(port, poolSize);
        server.start();
    }
}
