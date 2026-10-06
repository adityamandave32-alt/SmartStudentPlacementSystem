import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class PlacementServer {
    static final int PORT = 8080;
    static final Path DATA = Paths.get("../data/students.csv");

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/", exchange -> serveFile(exchange, "../html/index.html"));
        server.createContext("/add", PlacementServer::addStudent);
        server.createContext("/students", PlacementServer::students);
        server.createContext("/predict", PlacementServer::predict);

        server.setExecutor(null);
        System.out.println("Java server running at http://localhost:" + PORT);
        server.start();
    }

    static void serveFile(HttpExchange ex, String file) throws IOException {
        Path path = Paths.get(file).normalize();
        if (!Files.exists(path)) {
            send(ex, 404, "File not found");
            return;
        }
        byte[] bytes = Files.readAllBytes(path);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    static Map<String,String> form(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String,String> map = new HashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) continue;
            String[] p = pair.split("=", 2);
            String key = URLDecoder.decode(p[0], StandardCharsets.UTF_8);
            String value = p.length > 1 ? URLDecoder.decode(p[1], StandardCharsets.UTF_8) : "";
            map.put(key, value);
        }
        return map;
    }

    static void addStudent(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equalsIgnoreCase("POST")) {
            send(ex, 405, "POST required");
            return;
        }

        Map<String,String> f = form(ex);
        String name = f.getOrDefault("name", "").trim();
        String email = f.getOrDefault("email", "").trim();
        String cgpa = f.getOrDefault("cgpa", "").trim();
        String skills = f.getOrDefault("skills", "").trim();
        String internship = f.getOrDefault("internship", "No");

        if (name.isEmpty() || email.isEmpty() || cgpa.isEmpty()) {
            send(ex, 400, "Name, email and CGPA are required.");
            return;
        }

        Files.createDirectories(DATA.getParent());
        if (!Files.exists(DATA)) {
            Files.writeString(DATA, "Name,Email,CGPA,Skills,Internship\n");
        }

        String row = csv(name) + "," + csv(email) + "," + csv(cgpa) + ","
                + csv(skills) + "," + csv(internship) + "\n";
        Files.writeString(DATA, row, StandardOpenOption.APPEND);

        send(ex, 200, "<h2>Student added successfully.</h2><a href='/'>Back</a>");
    }

    static void students(HttpExchange ex) throws IOException {
        if (!Files.exists(DATA)) {
            send(ex, 200, "<h2>No students found.</h2><a href='/'>Back</a>");
            return;
        }

        List<String> lines = Files.readAllLines(DATA);
        StringBuilder html = new StringBuilder();
        html.append("<html><body><h1>Student Records</h1>");
        html.append("<table border='1' cellpadding='8'><tr>");
        html.append("<th>Name</th><th>Email</th><th>CGPA</th><th>Skills</th><th>Internship</th></tr>");

        for (int i = 1; i < lines.size(); i++) {
            String[] p = lines.get(i).split(",", -1);
            if (p.length >= 5) {
                html.append("<tr>");
                for (int j = 0; j < 5; j++) html.append("<td>").append(escape(p[j])).append("</td>");
                html.append("</tr>");
            }
        }
        html.append("</table><br><a href='/'>Back</a></body></html>");
        send(ex, 200, html.toString());
    }

    static void predict(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equalsIgnoreCase("POST")) {
            send(ex, 405, "POST required");
            return;
        }

        Map<String,String> f = form(ex);
        String cgpa = f.getOrDefault("cgpa", "0");
        String skills = f.getOrDefault("skills", "");
        String internship = f.getOrDefault("internship", "No");

        String json = "{\"cgpa\":\"" + jsonEscape(cgpa) + "\","
                + "\"skills\":\"" + jsonEscape(skills) + "\","
                + "\"internship\":\"" + jsonEscape(internship) + "\"}";

        URL url = new URL("http://localhost:5000/predict");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        try (OutputStream os = con.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int status = con.getResponseCode();
        InputStream input = status >= 400 ? con.getErrorStream() : con.getInputStream();
        String result = new String(input.readAllBytes(), StandardCharsets.UTF_8);

        con.disconnect();

        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = result.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    static String csv(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    static String escape(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;");
    }

    static String jsonEscape(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }

    static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
}
