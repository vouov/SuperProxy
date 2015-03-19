package com.vouov.superproxy.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author yuminglong@gmail.com
 * @date 2015/3/19
 */
public class PrivateServerThread extends Thread {

    private Socket publicSocket;
    private Socket privateSocket;
    public PrivateServerThread() {
        SuperProxyServer.threadQueue.offer(this);
    }

    public void setPublicSocket(Socket publicSocket) {
        this.publicSocket = publicSocket;
    }

    public Socket getPrivateSocket() {
        return privateSocket;
    }

    @Override
    public void run() {
        try {
            privateSocket = SuperProxyServer.privateServerSocket.accept();
            //新监听内部连接请求
            new PrivateServerThread().start();
            //当监听到内网请求时,保持连接,如果断了需要重新连接, 把内网连接等待数据发送
            //等待传输数据
            synchronized (this) {
                wait();
                final InputStream publicIs = publicSocket.getInputStream();
                OutputStream publicOs = publicSocket.getOutputStream();

                InputStream privateIs = privateSocket.getInputStream();
                final OutputStream privateOs = privateSocket.getOutputStream();
                //publicIs -> privateOs
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        byte[] buffer = new byte[1024];
                        int len;
                        try {
                            while ((len = publicIs.read(buffer)) != -1) {
                                privateOs.write(buffer, 0, len);
                                privateOs.flush();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                //privateIs -> publicOs
                byte[] buffer = new byte[1024];
                int len;
                while ((len = privateIs.read(buffer)) != -1) {
                    publicOs.write(buffer, 0, len);
                    publicOs.flush();
                }
                privateSocket.close();
                publicSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
