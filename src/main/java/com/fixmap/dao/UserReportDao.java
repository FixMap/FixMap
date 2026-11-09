package com.fixmap.dao;

import com.fixmap.dto.Report;
import com.fixmap.util.AppConfig;
import com.fixmap.util.DBUtil;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** 유저 페이지 조회와 사진 저장. main의 Oracle 테이블·연결·신고 등록을 재사용한다. */
public final class UserReportDao {
    public record Item(long id, String title, String content, String category, String building,
            String buildingName, int floor, double x, double y, String status, String createdAt,
            long floorId, String mapImageUrl, String placeName) {}
    public record Upload(String name, String type, byte[] bytes) {}
    public record Attachment(long id, String name, String type, long size) {}
    public record Download(String name, String type, byte[] bytes) {}
    public record Page(List<Item> items, long total, int page, int pageSize) {}
    public record Level(long id, int level, String name, String mapImageUrl) {}
    public record MapBuilding(String id, String name, List<Level> levels) {}
    public static final List<String> STATUSES = List.of("접수", "완료", "보류");
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final String JOINS = """
         FROM report r JOIN building_floor f ON f.id=r.floor_id
         JOIN building b ON b.id=f.building_id JOIN category c ON c.id=r.category_id
         LEFT JOIN place p ON p.id=r.place_id
        """;
    private static final String SELECT = """
        SELECT r.id,r.title,r.content,c.name AS category,b.code AS building,b.name AS building_name,
               f.floor_level,r.pos_x,r.pos_y,r.status,r.created_at,f.id AS floor_id,f.map_image_url,p.name AS place_name
        """;
    public record Query(String keyword, String status, String building, Integer floor, String category,
            LocalDate from, LocalDate to, String sort, int page) {
        public Query {
            if (keyword.length()>100 || building.length()>20 || category.length()>30 || page<1 || page>100000)
                throw new IllegalArgumentException("검색어나 페이지 번호를 확인해 주세요.");
            if (!status.isEmpty()&&!STATUSES.contains(status)) throw new IllegalArgumentException("처리 상태를 확인해 주세요.");
            if (floor!=null&&(floor<-99||floor>99)) throw new IllegalArgumentException("층을 확인해 주세요.");
            if (!List.of("newest","oldest","status").contains(sort)) throw new IllegalArgumentException("정렬 기준을 확인해 주세요.");
            if (from!=null&&to!=null&&from.isAfter(to)) throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
    }
    public List<MapBuilding> buildings() throws SQLException {
        return new BuildingDao().findAllWithFloors().stream().filter(b->!b.getFloors().isEmpty())
            .map(b->new MapBuilding(b.getCode(),b.getName(),b.getFloors().stream()
                .map(f->new Level(f.getId(),f.getFloorLevel(),f.getName(),f.getMapImageUrl())).toList())).toList();
    }
    public List<String> categories() throws SQLException {
        try(Connection c=DBUtil.getConnection();PreparedStatement p=c.prepareStatement("SELECT name FROM category ORDER BY sort_order,id");ResultSet r=p.executeQuery()){
            var values=new ArrayList<String>();while(r.next())values.add(r.getString(1));return values;
        }
    }
    public long create(String title,String content,String category,String building,int floor,double x,double y,List<Upload> files) throws SQLException,IOException {
        if(title.isBlank()||title.length()>100||content.isBlank()||content.length()>4000||!Double.isFinite(x)||!Double.isFinite(y)||x<0||x>1||y<0||y>1)
            throw new IllegalArgumentException("제목(100자), 내용(4,000자), 지도 위치를 확인해 주세요.");
        Path root=uploadRoot();Files.createDirectories(root);
        var written=new ArrayList<Path>();
        try(Connection c=DBUtil.getConnection()){
            c.setAutoCommit(false);
            try {
                long floorId=floorId(c,building,floor);
                long categoryId;
                try(PreparedStatement p=c.prepareStatement("SELECT id FROM category WHERE name=?")){
                    p.setString(1,category);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("카테고리를 확인해 주세요.");categoryId=r.getLong(1);}
                }
                var report=new Report();report.setTitle(title);report.setContent(content);report.setFloorId(floorId);report.setCategoryId(categoryId);report.setPosX(x);report.setPosY(y);
                long id=new ReportDao().insert(c,report);
                try(PreparedStatement p=c.prepareStatement("INSERT INTO report_file(report_id,original_name,stored_path,content_type,file_size) VALUES(?,?,?,?,?)")){
                    for(Upload file:files){
                        String stored=UUID.randomUUID()+(file.type().equals("image/png")?".png":".jpg");Path path=root.resolve(stored);
                        try(OutputStream out=Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)){written.add(path);out.write(file.bytes());}
                        p.setLong(1,id);p.setString(2,file.name());p.setString(3,stored);p.setString(4,file.type());p.setLong(5,file.bytes().length);p.executeUpdate();
                    }
                }
                c.commit();return id;
            } catch(SQLException|IOException|RuntimeException e){
                try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}
                for(Path path:written)try{Files.deleteIfExists(path);}catch(IOException cleanup){e.addSuppressed(cleanup);}
                throw e;
            }
        }
    }
    public Page list(Query q) throws SQLException {
        var sql=new StringBuilder(JOINS+" WHERE 1=1");var args=new ArrayList<Object>();
        for(String term:q.keyword().split("\\s+")){
            if(term.isEmpty())continue;
            String pattern="%"+term.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
            sql.append(" AND (LOWER(r.title) LIKE ? ESCAPE '!' OR LOWER(r.content) LIKE ? ESCAPE '!' OR LOWER(b.name) LIKE ? ESCAPE '!' OR LOWER(b.code) LIKE ? ESCAPE '!' OR LOWER(f.name) LIKE ? ESCAPE '!' OR LOWER(p.name) LIKE ? ESCAPE '!')");
            for(int i=0;i<6;i++)args.add(pattern);
        }
        if(!q.status().isEmpty()){sql.append(" AND r.status=?");args.add(statusCode(q.status()));}
        if(!q.building().isEmpty()){sql.append(" AND b.code=?");args.add(q.building());}
        if(q.floor()!=null){sql.append(" AND f.floor_level=?");args.add(q.floor());}
        if(!q.category().isEmpty()){sql.append(" AND c.name=?");args.add(q.category());}
        if(q.from()!=null){sql.append(" AND r.created_at>=?");args.add(Timestamp.valueOf(q.from().atStartOfDay()));}
        if(q.to()!=null){sql.append(" AND r.created_at<?");args.add(Timestamp.valueOf(q.to().plusDays(1).atStartOfDay()));}
        try(Connection c=DBUtil.getConnection()){
            long total;try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*)"+sql)){bind(p,args);try(ResultSet r=p.executeQuery()){r.next();total=r.getLong(1);}}
            String order=switch(q.sort()){
                case "oldest"->"r.created_at ASC,r.id ASC";
                case "status"->"CASE r.status WHEN 'RECEIVED' THEN 0 WHEN 'HOLD' THEN 1 ELSE 2 END,r.created_at DESC,r.id DESC";
                default->"r.created_at DESC,r.id DESC";
            };
            var items=new ArrayList<Item>();try(PreparedStatement p=c.prepareStatement(SELECT+sql+" ORDER BY "+order+" OFFSET ? ROWS FETCH NEXT 12 ROWS ONLY")){
                var pageArgs=new ArrayList<>(args);pageArgs.add((q.page()-1)*12);bind(p,pageArgs);try(ResultSet r=p.executeQuery()){while(r.next())items.add(item(r));}
            }
            return new Page(items,total,q.page(),12);
        }
    }
    public List<Item> pins(String building,int floor) throws SQLException {
        try(Connection c=DBUtil.getConnection()){
            long floorId=floorId(c,building,floor);
            try(PreparedStatement p=c.prepareStatement(SELECT+JOINS+" WHERE r.floor_id=? ORDER BY r.id")){
                p.setLong(1,floorId);var items=new ArrayList<Item>();try(ResultSet r=p.executeQuery()){while(r.next())items.add(item(r));}return items;
            }
        }
    }
    public Item find(long id) throws SQLException {
        try(Connection c=DBUtil.getConnection();PreparedStatement p=c.prepareStatement(SELECT+JOINS+" WHERE r.id=?")){
            p.setLong(1,id);try(ResultSet r=p.executeQuery()){return r.next()?item(r):null;}
        }
    }
    public List<Attachment> attachments(long id) throws SQLException {
        try(Connection c=DBUtil.getConnection();PreparedStatement p=c.prepareStatement("SELECT id,original_name,content_type,file_size FROM report_file WHERE report_id=? ORDER BY id")){
            p.setLong(1,id);var items=new ArrayList<Attachment>();try(ResultSet r=p.executeQuery()){while(r.next())items.add(new Attachment(r.getLong(1),r.getString(2),r.getString(3),r.getLong(4)));}return items;
        }
    }
    public Download download(long id) throws SQLException,IOException {
        try(Connection c=DBUtil.getConnection();PreparedStatement p=c.prepareStatement("SELECT original_name,content_type,stored_path FROM report_file WHERE id=?")){
            p.setLong(1,id);try(ResultSet r=p.executeQuery()){
                if(!r.next())return null;
                String type=r.getString(2);if(!List.of("image/png","image/jpeg").contains(type))return null;
                Path root=uploadRoot(),path=root.resolve(r.getString(3)).normalize();
                if(!path.startsWith(root)||!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))return null;
                try(InputStream in=Files.newInputStream(path)){
                    byte[] bytes=in.readNBytes(5*1024*1024+1);if(bytes.length>5*1024*1024)return null;return new Download(r.getString(1),type,bytes);
                }
            }
        }
    }
    private static Path uploadRoot(){return Path.of(AppConfig.get("upload.dir")).toAbsolutePath().normalize();}
    private static long floorId(Connection c,String building,int floor) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT f.id FROM building_floor f JOIN building b ON b.id=f.building_id WHERE b.code=? AND f.floor_level=?")){
            p.setString(1,building);p.setInt(2,floor);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("지도에서 올바른 건물과 층을 선택해 주세요.");return r.getLong(1);}
        }
    }
    private static void bind(PreparedStatement p,List<?> values)throws SQLException{for(int i=0;i<values.size();i++)p.setObject(i+1,values.get(i));}
    private static String statusCode(String label){return switch(label){case "완료"->"DONE";case "보류"->"HOLD";default->"RECEIVED";};}
    private static Item item(ResultSet r)throws SQLException{
        String status=switch(r.getString("status")){case "DONE"->"완료";case "HOLD"->"보류";default->"접수";};
        return new Item(r.getLong("id"),r.getString("title"),r.getString("content"),r.getString("category"),r.getString("building"),r.getString("building_name"),r.getInt("floor_level"),r.getDouble("pos_x"),r.getDouble("pos_y"),status,r.getTimestamp("created_at").toLocalDateTime().atZone(SEOUL).toOffsetDateTime().toString(),r.getLong("floor_id"),r.getString("map_image_url"),r.getString("place_name"));
    }
}
