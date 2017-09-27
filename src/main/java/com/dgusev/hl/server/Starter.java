package com.dgusev.hl.server;

import com.dgusev.hl.server.model.Location;
import com.dgusev.hl.server.model.User;
import com.dgusev.hl.server.model.Visit;
import com.dgusev.hl.server.model.file.*;
import com.dgusev.hl.server.service.TravelService;
import com.dgusev.hl.server.threads.WorkerThreadFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.Epoll0EventLoopGroup;
import io.netty.channel.epoll.Epoll0ServerSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Date;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Created by dgusev on 19.08.2017.
 */
@Component
public class Starter implements CommandLineRunner {

    @Value("${data.initial.file}")
    private String initFile;

    @Value("${data.options.file}")
    private String optionsFile;

    @Value("${server.port}")
    private Integer serverPort;


    @Autowired
    private TravelService travelService;

    @Autowired
    private RequestHandler requestHandler;

    @Override
    public void run(String... args) throws Exception {
        long serverTime = new Scanner(new File(optionsFile)).nextLong();
        travelService.init(serverTime);

        new Thread(() -> {
            ServerBootstrap serverBootstrap = new ServerBootstrap()
                    .group(new Epoll0EventLoopGroup(true), new Epoll0EventLoopGroup(false,3, new WorkerThreadFactory()))
                    .channel(Epoll0ServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(requestHandler);
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 500)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_LINGER, -1)
                    .childOption(ChannelOption.TCP_NODELAY, true);
            try {
                serverBootstrap.bind(new InetSocketAddress(serverPort)).sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        long t1 = System.currentTimeMillis();
        ObjectMapper mapper = new ObjectMapper();
        ZipFile zipFile = new ZipFile(initFile);
        TreeMap<Integer, ZipEntry> usersFileTreeMap = new TreeMap<>();
        TreeMap<Integer, ZipEntry> locationsFileTreeMap = new TreeMap<>();
        TreeMap<Integer, ZipEntry> visitsFileTreeMap = new TreeMap<>();
        Collections.list(zipFile.entries()).forEach(zipEntry -> {
            try {
                if (zipEntry.getName().startsWith("users_")) {
                    String number = zipEntry.getName().substring(6);
                    usersFileTreeMap.put(Integer.valueOf(number.substring(0, number.length() - 5)), zipEntry);
                } else if (zipEntry.getName().startsWith("locations_")) {
                    String number = zipEntry.getName().substring(10);
                    locationsFileTreeMap.put(Integer.valueOf(number.substring(0, number.length() - 5)), zipEntry);
                } else if (zipEntry.getName().startsWith("visits_")) {
                    String number = zipEntry.getName().substring(7);
                    visitsFileTreeMap.put(Integer.valueOf(number.substring(0, number.length() - 5)), zipEntry);
                } else {
//                    throw new IllegalArgumentException();
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        });
        AtomicInteger userCount = new AtomicInteger();
        usersFileTreeMap.forEach((n,z) -> {
            try {
                String file = readFile(zipFile.getInputStream(z));
                UsersFile usersFile = mapper.readValue(file, UsersFile.class);
                userCount.addAndGet(usersFile.getUsers().size());
                usersFile.getUsers().parallelStream().map(this::map).forEach(travelService::createUser);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        System.gc();
        System.out.println("Users: " + userCount.get());
        AtomicInteger locationCount = new AtomicInteger();
        locationsFileTreeMap.forEach((n,z) -> {
            try {
                String file = readFile(zipFile.getInputStream(z));
                LocationsFile locationsFile = mapper.readValue(file, LocationsFile.class);
                locationCount.addAndGet(locationsFile.getLocations().size());
                locationsFile.getLocations().parallelStream().map(this::map).forEach(travelService::createLocation);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        System.gc();
        System.out.println("Locations: " + locationCount.get());
        visitsFileTreeMap.forEach((n,z) -> {
            try {
                String file = readFile(zipFile.getInputStream(z));
                VisitsFile visitsFile = mapper.readValue(file, VisitsFile.class);
                visitsFile.getVisits().stream().map(this::map).forEach(travelService::createVisit);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        System.gc();
        long t2 = System.currentTimeMillis();
        System.out.println("Load time: " + (t2 - t1));
        long t3 = System.currentTimeMillis();
        System.out.println("Start warm-up");
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        for (int i = 0; i< 4; i++) {
            executorService.submit(()->{
                RestTemplate restTemplate = new RestTemplate();
                while (!Thread.currentThread().isInterrupted()) {
                    request(restTemplate,"http://localhost:" + serverPort+"/users/1");
                    request(restTemplate,"http://localhost:" + serverPort+"/users/bad");
                    request(restTemplate,"http://localhost:" + serverPort+"/locations/1");
                    request(restTemplate,"http://localhost:" + serverPort+"/locations/bad");
                    request(restTemplate,"http://localhost:" + serverPort+"/visits/1");
                    request(restTemplate,"http://localhost:" + serverPort+"/visits/bad");
                    request(restTemplate,"http://localhost:" + serverPort+"/users/1/visits");
                    request(restTemplate,"http://localhost:" + serverPort+"/users/9999999/visits");
                    request(restTemplate,"http://localhost:" + serverPort+"/locations/1/avg");
                    request(restTemplate,"http://localhost:" + serverPort+"/locations/9999999/avg");
                }
                return null;
            });
        }
        Thread.sleep(400000);
        executorService.shutdownNow();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        long t4 = System.currentTimeMillis();
        System.gc();
        System.out.println("Warm-up time: " + (t4 - t3));
        System.out.println(new Date().getTime());

    }

    private void request(RestTemplate restTemplate, String path) {
        try {
            restTemplate.getForEntity(path, String.class);
        } catch (Exception ex) {

        }
    }


    public User map(UserJson userJson) {
        User user = new User();
        user.id = userJson.getId();
        user.email = userJson.getEmail();
        user.birthDate = userJson.getBirthDate();
        user.gender = userJson.getGender();
        user.firstName = userJson.getFirstName();
        user.lastName = userJson.getLastName();
        return user;
    }

    public Location map(LocationJson locationJson) {
        Location location = new Location();
        location.id = locationJson.getId();
        location.country = locationJson.getCountry();
        location.city = locationJson.getCity();
        location.place = locationJson.getPlace();
        location.distance = locationJson.getDistance();
        return location;
    }

    public Visit map(VisitJson visitJson) {
        Visit visit = new Visit();
        visit.id = visitJson.getId();
        visit.visitedAt = visitJson.getVisitedAt();
        visit.location = visitJson.getLocation();
        visit.user = visitJson.getUser();
        visit.mark = visitJson.getMark().byteValue();
        return visit;
    }

    public String readFile(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }
}
