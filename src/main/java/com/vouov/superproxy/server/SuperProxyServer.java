package com.vouov.superproxy.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author yuminglong@gmail.com
 * @date 2015/3/19
 */
public class SuperProxyServer {
    public static BlockingQueue<PrivateServerThread> threadQueue = new LinkedBlockingQueue<PrivateServerThread>();
    public static ServerSocket privateMonitorServerSocket;
    public static ServerSocket publicServerSocket;
    public static ServerSocket privateServerSocket;

    private static void privateMonitorServer() {
        //监听外部连接请求
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket privateMonitorSocket = privateMonitorServerSocket.accept();
                    //新监听外部连接请求
                    privateMonitorServer();
                    //转发数据
                    while (true) {
                        int tqSize = threadQueue.size();
                        System.out.println("Remote server handler socket size: " + tqSize);
                        privateMonitorSocket.getOutputStream().write(tqSize);
                        privateMonitorSocket.getOutputStream().flush();
                        Thread.sleep(1000);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void publicServer() {
        //监听外部连接请求
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket publicSocket = publicServerSocket.accept();
                    //新监听外部连接请求
                    publicServer();

                    //转发数据
                    while (threadQueue.isEmpty()) {
                        System.out.println("没有可用的处理线程!");
                        Thread.sleep(1000);
                    }
                    PrivateServerThread thread = threadQueue.take();
                    while (true) {
                        if (threadQueue.isEmpty()) {
                            Thread.sleep(1000);
                            continue;
                        }
                        try {
                            thread.getPrivateSocket().sendUrgentData(1);
                            break;
                        } catch (Exception e) {
                            thread = threadQueue.take();
                        }
                    }
                    synchronized (thread) {
                        //有外部代理请求时,找到可用的内网连接,然后开始传送数据
                        thread.setPublicSocket(publicSocket);
                        thread.notify();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void main(String[] args) throws Exception {
        int publicPort = Integer.parseInt(System.getProperty("PUBLIC_PORT", "7001"));;
        int privatePort = Integer.parseInt(System.getProperty("PRIVATE_PORT", "8080"));;
        int privateMonitorPort = Integer.parseInt(System.getProperty("PRIVATE_MONITOR_PORT", "8081"));;
        publicServerSocket = new ServerSocket(publicPort);
        privateServerSocket = new ServerSocket(privatePort);
        privateMonitorServerSocket = new ServerSocket(privateMonitorPort);
        //监听内网连接请求
        new PrivateServerThread().start();
        publicServer();
        privateMonitorServer();
    }
}
