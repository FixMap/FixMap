package com.fixmap.controller;

import com.google.gson.Gson;
import com.fixmap.dao.UserReportDao;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@WebServlet(urlPatterns={"/reports","/reports/*","/api/meta","/api/pins","/api/reports","/api/reports/*","/attachments/*"})
@MultipartConfig(maxFileSize=5242880,maxRequestSize=16777216,fileSizeThreshold=0)
public final class UserReportController extends HttpServlet {
    private static final Gson JSON = new Gson();
    private static final long MAX_FILE = 5L * 1024 * 1024;
    private final UserReportDao reports = new UserReportDao();
    private UserReportDao store() { return reports; }
    @Override protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        try { super.service(req,res); }
        catch (NumberFormatException e) { error(res,400,"좌표, 층, 신고 번호와 페이지 번호를 확인해 주세요."); }
        catch (IllegalArgumentException e) { error(res,400,e.getMessage()); }
        catch (IllegalStateException e) { log("사용자 페이지 설정 오류",e);error(res,500,"서버 설정을 확인해 주세요. 관리자에게 문의해 주세요."); }
        catch (ServletException e) { error(res,400,"요청 형식을 확인해 주세요. 첨부사진은 JPG·PNG 파일만 가능합니다."); }
    }
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException {
        String path=path(req);
        try {
            if (path.equals("/api/meta")) {
                HttpSession session=req.getSession();
                synchronized (session) { if(session.getAttribute("csrf")==null)session.setAttribute("csrf",UUID.randomUUID().toString()); }
                json(res,Map.of("buildings",store().buildings(),"categories",store().categories(),"statuses",UserReportDao.STATUSES,"csrfToken",session.getAttribute("csrf")));return;
            }
            if (path.equals("/api/reports")) { json(res,store().list(query(req)));return; }
            if (path.equals("/api/pins")) { json(res,store().pins(param(req,"building","CAMPUS"),Integer.parseInt(param(req,"floor","0"))));return; }
            if (path.matches("/api/reports/[1-9][0-9]*")) {
                long id=id(path);UserReportDao.Item report=store().find(id);
                if(report==null){error(res,404,"신고를 찾을 수 없습니다.");return;}
                json(res,Map.of("report",report,"attachments",store().attachments(id)));return;
            }
            if (path.matches("/attachments/[1-9][0-9]*")) {
                UserReportDao.Download file=store().download(id(path));
                if(file==null){error(res,404,"첨부사진을 찾을 수 없습니다.");return;}
                res.setContentType(file.type());res.setContentLength(file.bytes().length);
                String disposition="1".equals(req.getParameter("download"))?"attachment":"inline";
                res.setHeader("Content-Disposition",disposition+"; filename=\"photo\"; filename*=UTF-8''"+URLEncoder.encode(file.name(),StandardCharsets.UTF_8).replace("+","%20"));
                res.getOutputStream().write(file.bytes());return;
            }
            String view;
            if(path.equals("/")||path.isEmpty()) view="map";
            else if(path.equals("/reports")) view="list";
            else if(path.matches("/reports/[1-9][0-9]*")) {
                if(store().find(id(path))==null){error(res,404,"신고를 찾을 수 없습니다.");return;}
                req.setAttribute("reportId",id(path));view="detail";
            } else { error(res,404,"페이지를 찾을 수 없습니다.");return; }
            req.setAttribute("page",view);
            req.getRequestDispatcher("/WEB-INF/views/"+view+".jsp").forward(req,res);
        } catch (java.sql.SQLException|IOException e) { getServletContext().log("신고 조회 실패",e);error(res,500,"데이터를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."); }
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res) throws IOException,ServletException {
        if(!path(req).equals("/api/reports")){error(res,404,"요청 경로를 찾을 수 없습니다.");return;}
        if(req.getContentType()==null||!req.getContentType().toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
            error(res,415,"신고는 첨부 업로드 양식으로 전송해 주세요.");return;
        }
        try {
            Collection<Part> parts;
            try { parts=req.getParts(); }
            catch(IllegalStateException e){error(res,413,"첨부사진은 파일당 5MB, 최대 3장까지 등록할 수 있습니다.");return;}
            var files=new ArrayList<UserReportDao.Upload>();
            for(Part part:parts) {
                if(part.getSubmittedFileName()==null)continue;
                if(part.getSubmittedFileName().isEmpty()&&part.getSize()==0)continue;
                if(!part.getName().equals("files"))throw new IllegalArgumentException("첨부 필드를 확인해 주세요.");
                if(files.size()>=3)throw new IllegalArgumentException("첨부사진은 최대 3장까지 등록할 수 있습니다.");
                files.add(upload(part));
            }
            String title=required(req,"title"),content=required(req,"content"),category=required(req,"category"),building=required(req,"building");
            int floor=Integer.parseInt(required(req,"floor"));double x=Double.parseDouble(required(req,"x")),y=Double.parseDouble(required(req,"y"));
            long reportId=store().create(title,content,category,building,floor,x,y,files);
            res.setStatus(201);res.setHeader("Location",req.getContextPath()+"/reports/"+reportId);json(res,Map.of("id",reportId));
        } catch(java.sql.SQLException|IOException e){getServletContext().log("신고 저장 실패",e);error(res,500,"저장하지 못했습니다. 잠시 후 다시 시도해 주세요.");}
        finally { try { for(Part part:req.getParts())part.delete(); } catch(Exception ignored) { /* Container also cleans multipart temporary files. */ } }
    }
    private static UserReportDao.Upload upload(Part part) throws IOException {
        if(part.getSize()==0||part.getSize()>MAX_FILE)throw new IllegalArgumentException("첨부사진은 0바이트보다 크고 5MB 이하여야 합니다.");
        String name=part.getSubmittedFileName().replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]", "");
        if(name.isBlank()||name.length()>150)throw new IllegalArgumentException("사진 파일명은 150자 이내로 입력해 주세요.");
        String lower=name.toLowerCase(Locale.ROOT);
        boolean png=lower.endsWith(".png"),jpeg=lower.endsWith(".jpg")||lower.endsWith(".jpeg");
        if(!png&&!jpeg)throw new IllegalArgumentException("JPG·PNG 사진만 첨부할 수 있습니다.");
        byte[] bytes;try(InputStream in=part.getInputStream()){bytes=in.readNBytes((int)MAX_FILE+1);}
        if(bytes.length>MAX_FILE)throw new IllegalArgumentException("파일당 최대 5MB까지 첨부할 수 있습니다.");
        try(ImageInputStream image=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
            var readers=ImageIO.getImageReaders(image);if(!readers.hasNext())throw new IllegalArgumentException("유효한 이미지 파일이 아닙니다.");
            var reader=readers.next();
            try {
                reader.setInput(image,true,true);String format=reader.getFormatName();
                if((png&&!format.equalsIgnoreCase("png"))||(jpeg&&!format.equalsIgnoreCase("jpeg")))throw new IllegalArgumentException("확장자와 사진 형식이 일치하지 않습니다.");
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<1||height<1||(long)width*height>25000000)throw new IllegalArgumentException("사진 해상도는 2,500만 화소 이하여야 합니다.");
                if(reader.read(0)==null)throw new IllegalArgumentException("사진을 읽을 수 없습니다.");
            } finally {reader.dispose();}
        } catch(IOException e){throw new IllegalArgumentException("손상된 사진은 첨부할 수 없습니다.");}
        return new UserReportDao.Upload(name,png?"image/png":"image/jpeg",bytes);
    }
    private static UserReportDao.Query query(HttpServletRequest req){
        String floor=param(req,"floor","");
        return new UserReportDao.Query(param(req,"q",""),param(req,"status",""),param(req,"building",""),floor.isEmpty()?null:Integer.valueOf(floor),param(req,"category",""),date(req,"from"),date(req,"to"),param(req,"sort","newest"),Integer.parseInt(param(req,"page","1")));
    }
    private static LocalDate date(HttpServletRequest r,String key){String value=param(r,key,"");try{return value.isEmpty()?null:LocalDate.parse(value);}catch(java.time.DateTimeException e){throw new IllegalArgumentException("날짜를 YYYY-MM-DD 형식으로 입력해 주세요.");}}
    private static String required(HttpServletRequest r,String key){String value=param(r,key,"");if(value.isEmpty())throw new IllegalArgumentException("필수 항목과 지도 위치를 확인해 주세요.");return value;}
    private static String param(HttpServletRequest r,String key,String fallback){String value=r.getParameter(key);return value==null?fallback:value.trim();}
    private static String path(HttpServletRequest r){return r.getRequestURI().substring(r.getContextPath().length());}
    private static long id(String path){return Long.parseLong(path.substring(path.lastIndexOf('/')+1));}
    private static void json(HttpServletResponse r,Object value)throws IOException{r.setContentType("application/json");JSON.toJson(value,r.getWriter());}
    private static void error(HttpServletResponse r,int status,String message)throws IOException{if(!r.isCommitted()){r.setStatus(status);json(r,Map.of("error",message==null?"입력값을 확인해 주세요.":message));}}
}
