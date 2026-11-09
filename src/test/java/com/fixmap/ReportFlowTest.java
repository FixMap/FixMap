package com.fixmap;

import com.google.gson.*;
import com.fixmap.dao.UserReportDao;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReportFlowTest {
    static Tomcat server;
    static String origin, jdbc;
    static Path directory;
    HttpClient client;
    String csrf;
    static byte[] png;
    record File(String name, byte[] data) {}
    @BeforeAll static void start() throws Exception {
        directory=Files.createTempDirectory(Path.of("target"),"flow-test-").toAbsolutePath();
        jdbc="jdbc:h2:mem:userflow;MODE=Oracle;DB_CLOSE_DELAY=-1;DEFAULT_NULL_ORDERING=HIGH";
        var properties=new Properties();properties.setProperty("db.url",jdbc);properties.setProperty("db.user","sa");properties.setProperty("db.password","");properties.setProperty("upload.dir",directory.resolve("uploads").toString());
        try(var out=Files.newOutputStream(Path.of("target/test-classes/db.properties"))){properties.store(out,"integration test only");}
        try(Connection c=DriverManager.getConnection(jdbc,"sa","");Statement s=c.createStatement()){
            var ddl=java.util.regex.Pattern.compile("(?is)CREATE (?:TABLE|INDEX)\\s+.*?;").matcher(Files.readString(Path.of("db/schema.sql")));
            while(ddl.find())s.execute(ddl.group().replaceFirst(";\\s*$",""));
            String seed=Files.readString(Path.of("db/user-page.sql")).replaceAll("(?m)^--.*$","");
            for(String sql:seed.split(";"))if(!sql.isBlank())s.execute(sql);
            // The additive setup must be safe to re-run on an existing database.
            for(String sql:seed.split(";"))if(!sql.isBlank())s.execute(sql);
        }
        server=Server.start(0,directory.resolve("tomcat"));origin="http://127.0.0.1:"+server.getConnector().getLocalPort();
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",out);png=out.toByteArray();
    }
    @AfterAll static void stop() throws Exception { if(server!=null){server.stop();server.destroy();}Files.deleteIfExists(Path.of("target/test-classes/db.properties")); }
    @BeforeEach void reset() throws Exception {
        try(Connection c=DriverManager.getConnection(jdbc,"sa","");Statement s=c.createStatement()){s.executeUpdate("DELETE FROM report_file");s.executeUpdate("DELETE FROM report");}
        Path uploads=directory.resolve("uploads");if(Files.isDirectory(uploads))try(var paths=Files.list(uploads)){for(Path path:paths.toList())Files.delete(path);}
        client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        csrf=json(get("/api/meta")).get("csrfToken").getAsString();
    }
    @Test void jspAndUploadFlow() throws Exception {
        var root=get("/");assertEquals(200,root.statusCode());assertTrue(root.body().contains("어디가 불편한가요?"));assertFalse(root.body().contains("<%@"));
        assertEquals(200,get("/reports").statusCode());
        var response=post(fields("형광등 <script>alert(1)</script>","MAIN",2),List.of(new File("고장 사진.png",png)),csrf);
        assertEquals(201,response.statusCode(),response.body());long id=json(response).get("id").getAsLong();
        assertEquals(200,get("/reports/"+id).statusCode());
        var detail=json(get("/api/reports/"+id));var report=detail.getAsJsonObject("report");
        assertEquals("접수",report.get("status").getAsString());assertEquals(.4,report.get("x").getAsDouble());assertTrue(report.get("title").getAsString().contains("<script>"));
        assertTrue(get("/api/reports/"+id).body().contains("\\u003cscript\\u003e"));
        long fileId=detail.getAsJsonArray("attachments").get(0).getAsJsonObject().get("id").getAsLong();
        var download=client.send(HttpRequest.newBuilder(URI.create(origin+"/attachments/"+fileId+"?download=1")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200,download.statusCode());assertArrayEquals(png,download.body());assertTrue(download.headers().firstValue("Content-Disposition").orElseThrow().startsWith("attachment;"));
        assertEquals("nosniff",download.headers().firstValue("X-Content-Type-Options").orElseThrow());
        assertEquals(1,JsonParser.parseString(get("/api/pins?building=MAIN&floor=2").body()).getAsJsonArray().size());
        assertNotNull(new UserReportDao().find(id));
        long floorId=report.get("floorId").getAsLong();
        var commonPins=JsonParser.parseString(get("/api/reports/map?floor="+floorId).body()).getAsJsonArray();
        assertEquals(id,commonPins.get(0).getAsJsonObject().get("id").getAsLong());
        assertEquals("RECEIVED",commonPins.get(0).getAsJsonObject().get("status").getAsString());
        assertEquals(200,get("/api/buildings").statusCode());
    }
    @Test void validatesCsrfCoordinatesAndFilesWithoutPartialWrites() throws Exception {
        assertEquals(403,post(fields("권한", "MAIN",1),List.of(),"wrong").statusCode());
        var input=fields("좌표", "MAIN",1);input.put("x","NaN");assertEquals(400,post(input,List.of(),csrf).statusCode());
        assertEquals(400,post(fields("층", "DONGHAENG",6),List.of(),csrf).statusCode());
        input=fields("종류", "MAIN",1);input.put("category","<script>");assertEquals(400,post(input,List.of(),csrf).statusCode());
        input=fields(" ", "MAIN",1);assertEquals(400,post(input,List.of(),csrf).statusCode());
        assertEquals(400,post(fields("위장", "MAIN",1),List.of(new File("fake.png","<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8))),csrf).statusCode());
        assertEquals(400,post(fields("확장자", "MAIN",1),List.of(new File("fake.jpg",png)),csrf).statusCode());
        var four=List.of(new File("1.png",png),new File("2.png",png),new File("3.png",png),new File("4.png",png));
        assertEquals(400,post(fields("네 장", "MAIN",1),four,csrf).statusCode());
        assertEquals(413,post(fields("초과", "MAIN",1),List.of(new File("large.png",new byte[5*1024*1024+1])),csrf).statusCode());
        assertEquals(0,json(get("/api/reports")).get("total").getAsInt());
        try(Connection c=DriverManager.getConnection(jdbc,"sa","");Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM report_file")){r.next();assertEquals(0,r.getInt(1));}
    }
    @Test void combinesSearchFiltersSortAndPagination() throws Exception {
        for(int i=0;i<14;i++) assertEquals(201,post(fields("형광등 "+i,"MAIN",2),List.of(),csrf).statusCode());
        assertEquals(201,post(fields("다른 건물","GEUMBONG",1),List.of(),csrf).statusCode());
        assertEquals(201,post(fields("문자 %_! 검색","MAIN",1),List.of(),csrf).statusCode());
        String today=LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).toString();
        var combined=json(get("/api/reports?q="+encode("형광등")+"&building=MAIN&floor=2&status="+encode("접수")+"&category="+encode("전기")+"&from="+today+"&to="+today+"&sort=oldest&page=2"));
        assertEquals(14,combined.get("total").getAsInt());assertEquals(2,combined.getAsJsonArray("items").size());assertEquals("형광등 12",combined.getAsJsonArray("items").get(0).getAsJsonObject().get("title").getAsString());
        assertEquals(15,json(get("/api/reports?q="+encode("본관"))).get("total").getAsInt());
        assertEquals(14,json(get("/api/reports?q="+encode("본관 2층"))).get("total").getAsInt());
        assertEquals(1,json(get("/api/reports?q="+encode("%_!"))).get("total").getAsInt());
        assertEquals(0,json(get("/api/reports?status="+encode("완료"))).get("total").getAsInt());
        assertEquals(400,get("/api/reports?from=2026-12-01&to=2026-01-01").statusCode());
        assertEquals(400,get("/api/reports?sort=bad").statusCode());
        assertEquals(200,get("/api/reports?q="+encode("' OR 1=1 --")).statusCode());
        assertEquals(0,json(get("/api/reports?q="+encode("' OR 1=1 --"))).get("total").getAsInt());
        try(Connection c=DriverManager.getConnection(jdbc,"sa","");Statement s=c.createStatement()){
            s.executeUpdate("UPDATE report SET status='DONE' WHERE title='형광등 0'");
            s.executeUpdate("UPDATE report SET status='HOLD' WHERE title='형광등 1'");
        }
        var statusPage=json(get("/api/reports?sort=status&page=2")).getAsJsonArray("items");
        assertEquals("보류",statusPage.get(statusPage.size()-2).getAsJsonObject().get("status").getAsString());
        assertEquals("완료",statusPage.get(statusPage.size()-1).getAsJsonObject().get("status").getAsString());
    }
    @Test void unsupportedPagesAndReplyMutationsStayUnavailable() throws Exception {
        var admin=get("/admin");assertEquals(200,admin.statusCode());assertTrue(admin.body().contains("FixMap 관리자"));assertTrue(admin.headers().firstValue("Content-Security-Policy").isEmpty());
        assertEquals(404,get("/login").statusCode());
        assertEquals(404,get("/reports/9999999").statusCode());assertEquals(404,get("/attachments/9999999").statusCode());
        var request=HttpRequest.newBuilder(URI.create(origin+"/api/reports/1/replies")).header("X-CSRF-Token",csrf).POST(HttpRequest.BodyPublishers.noBody()).build();
        assertEquals(404,client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode());
    }
    @Test void databaseFailureRollsBackReportAndRemovesWrittenPhoto() throws Exception {
        try(Connection c=DriverManager.getConnection(jdbc,"sa","");Statement s=c.createStatement()){
            s.execute("ALTER TABLE report_file ADD CONSTRAINT test_reject_file CHECK(original_name<>'reject.png')");
            try{
                assertEquals(500,post(fields("롤백", "MAIN",1),List.of(new File("reject.png",png)),csrf).statusCode());
                assertEquals(0,json(get("/api/reports")).get("total").getAsInt());
                try(var paths=Files.list(directory.resolve("uploads"))){assertEquals(0,paths.count());}
            } finally{s.execute("ALTER TABLE report_file DROP CONSTRAINT test_reject_file");}
        }
    }
    private static LinkedHashMap<String,String> fields(String title,String building,int floor){
        var values=new LinkedHashMap<String,String>();values.put("title",title);values.put("content","상세 내용입니다.");values.put("category","전기");values.put("building",building);values.put("floor",String.valueOf(floor));values.put("x","0.4");values.put("y","0.6");return values;
    }
    private HttpResponse<String> post(Map<String,String> fields,List<File> files,String token)throws Exception{
        String boundary="FixMapTestBoundary";var body=new ByteArrayOutputStream();
        for(var entry:fields.entrySet())body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+entry.getKey()+"\"\r\n\r\n"+entry.getValue()+"\r\n").getBytes(StandardCharsets.UTF_8));
        for(File file:files){body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"files\"; filename=\""+file.name()+"\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));body.write(file.data());body.write("\r\n".getBytes(StandardCharsets.UTF_8));}
        body.write(("--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
        return client.send(HttpRequest.newBuilder(URI.create(origin+"/api/reports")).header("Content-Type","multipart/form-data; boundary="+boundary).header("X-CSRF-Token",token).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create(origin+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    private static JsonObject json(HttpResponse<String> response){assertEquals(response.statusCode()<400,true,response.body());return JsonParser.parseString(response.body()).getAsJsonObject();}
    private static String encode(String text){return URLEncoder.encode(text,StandardCharsets.UTF_8);}
}
