package com.vouov.superproxy.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author yuminglong@gmail.com
 * @date 2015/3/19
 */
public class SuperProxyClient {
    public static final String PUBLIC_SERVER_IP;
    public static final int PUBLIC_SERVER_PORT;
    public static final int PUBLIC_SERVER_MONITOR_PORT;

    public static final String INNER_NET_SERVER_IP;
    public static final int INNER_NET_SERVER_PORT;

    static {
        PUBLIC_SERVER_IP = System.getProperty("PUBLIC_SERVER_IP", "127.0.0.1");
        PUBLIC_SERVER_PORT = Integer.parseInt(System.getProperty("PUBLIC_SERVER_PORT", "8080"));
        PUBLIC_SERVER_MONITOR_PORT = Integer.parseInt(System.getProperty("PUBLIC_SERVER_MONITOR_PORT", "8081"));

        INNER_NET_SERVER_IP = System.getProperty("INNER_NET_SERVER_IP", "127.0.0.1");
        INNER_NET_SERVER_PORT = Integer.parseInt(System.getProperty("INNER_NET_SERVER_PORT", "80"));
    }

    public static void monitorClient() throws Exception{
        try {
            Socket remoteMonitorServerSocket = new Socket(PUBLIC_SERVER_IP, PUBLIC_SERVER_MONITOR_PORT);
            InputStream rmssis = remoteMonitorServerSocket.getInputStream();
            while (true) {
                int threadSize = rmssis.read();
                System.out.println("Remote server handler socket size: "+threadSize);
                if(threadSize<2){
                    for(int i=0; i<5-threadSize; i++){
                        System.out.println("create new connect:"+i);
                        remoteServerClient();
                    }
                }
                Thread.sleep(500);
            }
        }finally {
            monitorClient();
        }
    }

    public static void remoteServerClient(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket remoteServerSocket = new Socket(PUBLIC_SERVER_IP, PUBLIC_SERVER_PORT);
                    Socket innerNetServerSocket = new Socket(INNER_NET_SERVER_IP, INNER_NET_SERVER_PORT);

                    final InputStream remoteIs = remoteServerSocket.getInputStream();
                    OutputStream remoteOs = remoteServerSocket.getOutputStream();

                    InputStream innerIs = innerNetServerSocket.getInputStream();
                    final OutputStream innerOs = innerNetServerSocket.getOutputStream();
                    //remoteIs -> innerOs
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            byte[] buffer = new byte[1024];
                            int len;
                            try {
                                while ((len = remoteIs.read(buffer)) != -1) {
                                    innerOs.write(buffer, 0, len);
                                    innerOs.flush();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    //innerIs -> remoteOs
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = innerIs.read(buffer)) != -1) {
                        remoteOs.write(buffer, 0, len);
                        remoteOs.flush();
                    }
                    innerNetServerSocket.close();
                    remoteServerSocket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    public static void main(String[] args) throws Exception{
        monitorClient();
    }
}
