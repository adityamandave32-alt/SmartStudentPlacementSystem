import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class PlacementServer {

    static final int PORT = 8080;
    static final String DATA_FILE = "students.csv";

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(
                new InetSocketAddress(PORT), 0);

        server.createContext("/", PlacementServer::home);

        server.createContext("/addStudent",
                PlacementServer::addStudent);

        server.createContext("/students",
                PlacementServer::students);

        server.start();

        System.out.println("=================================");
        System.out.println("Smart Student Placement System");
        System.out.println("Server started successfully!");
        System.out.println("Open: http://localhost:8080");
        System.out.println("=================================");
    }

    // Home page
    static void home(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            send(exchange, "Invalid Request");
            return;
        }

        String html = Files.readString(
                Paths.get("index.html"),
                StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/html; charset=UTF-8");

        send(exchange, html);
    }

    // Add student
    static void addStudent(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, "Invalid Request");
            return;
        }

        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);

        String name = getValue(body, "name");
        String email = getValue(body, "email");
        String cgpa = getValue(body, "cgpa");
        String skills = getValue(body, "skills");
        String internship = getValue(body, "internship");

        String row =
                escape(name) + "," +
                escape(email) + "," +
                escape(cgpa) + "," +
                escape(skills) + "," +
                escape(internship) + System.lineSeparator();

        Files.writeString(
                Paths.get(DATA_FILE),
                row,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );

        send(exchange, "Student record added successfully!");
    }

    // View students
    static void students(HttpExchange exchange) throws IOException {

        String data = Files.readString(
                Paths.get(DATA_FILE),
                StandardCharsets.UTF_8);

        StringBuilder html = new StringBuilder();

        html.append("""
                <!DOCTYPE html>
                <html>
                <head>
                <title>Student Records</title>
                <style>
                body {
                    font-family: Arial;
                    background: #f2f6fc;
                    padding: 30px;
                }

                h1 {
                    text-align: center;
                    color: #1d4ed8;
                }

                table {
                    width: 100%;
                    border-collapse: collapse;
                    background: white;
                }

                th, td {
                    border: 1px solid #aaa;
                    padding: 10px;
                    text-align: left;
                }

                th {
                    background: #1d4ed8;
                    color: white;
                }
                </style>
                </head>
                <body>

                <h1>Student Records</h1>

                <table>
                <tr>
                <th>Name</th>
                <th>Email</th>
                <th>CGPA</th>
                <th>Skills</th>
                <th>Internship</th>
                </tr>
                """);

        String[] lines = data.split("\\R");

        for (int i = 1; i < lines.length; i++) {

            if (lines[i].trim().isEmpty()) {
                continue;
            }

            String[] values = lines[i].split(",", -1);

            html.append("<tr>");

            for (String value : values) {
                html.append("<td>")
                    .append(value)
                    .append("</td>");
            }

            html.append("</tr>");
        }

        html.append("""
                </table>
                </body>
                </html>
                """);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/html; charset=UTF-8");

        send(exchange, html.toString());
    }

    // Read JSON value
    static String getValue(String json, String key) {

        String search = "\"" + key + "\"";

        int start = json.indexOf(search);

        if (start == -1) {
            return "";
        }

        start = json.indexOf(":", start);

        if (start == -1) {
            return "";
        }

        start++;

        while (start < json.length()
                && (json.charAt(start) == ' '
                || json.charAt(start) == '"')) {
            start++;
        }

        int end = start;

        while (end < json.length()
                && json.charAt(end) != '"'
                && json.charAt(end) != ','
                && json.charAt(end) != '}') {
            end++;
        }

        return json.substring(start, end).trim();
    }

    // CSV escaping
    static String escape(String value) {

        if (value == null) {
            return "";
        }

        return value.replace(",", " ");
    }

    // Send response
    static void send(HttpExchange exchange,
                     String response) throws IOException {

        byte[] bytes =
                response.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(200, bytes.length);

        OutputStream output =
                exchange.getResponseBody();

        output.write(bytes);
        output.close();
    }
}