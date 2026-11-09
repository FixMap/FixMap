package com.fixmap;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.*;
import java.nio.file.*;

public final class Server {
    public static Tomcat start(int port, Path base) throws Exception {
        Files.createDirectories(base);
        Tomcat tomcat=new Tomcat();tomcat.setBaseDir(base.toAbsolutePath().toString());tomcat.setPort(port);
        tomcat.getConnector().setProperty("address","127.0.0.1");
        Context context=tomcat.addWebapp("",Path.of("src/main/webapp").toAbsolutePath().toString());
        context.setParentClassLoader(Server.class.getClassLoader());
        StandardRoot resources=new StandardRoot(context);
        resources.addPreResources(new DirResourceSet(resources,"/WEB-INF/classes",Path.of("target/classes").toAbsolutePath().toString(),"/"));
        context.setResources(resources);
        tomcat.start();
        if (tomcat.getConnector().getLocalPort() < 0 || !context.getState().isAvailable()) {
            tomcat.stop(); tomcat.destroy();
            throw new IllegalStateException("서버를 시작하지 못했습니다. 사용 중인 포트인지 확인하고 PORT를 변경해 주세요.");
        }
        return tomcat;
    }
    public static void main(String[] args)throws Exception{
        int port=Integer.parseInt(System.getenv().getOrDefault("PORT","8090"));
        Tomcat server=start(port,Path.of("target/tomcat"));
        Runtime.getRuntime().addShutdownHook(new Thread(()->{try{server.stop();server.destroy();}catch(Exception ignored){}}));
        System.out.println("FixMap: http://localhost:"+server.getConnector().getLocalPort());
        server.getServer().await();
    }
}
