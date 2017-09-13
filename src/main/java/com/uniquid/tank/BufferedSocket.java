package com.uniquid.tank;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketImpl;

public class BufferedSocket extends Socket {

    private BufferedInputStream bis;
    private BufferedOutputStream bos;

    public BufferedSocket() { super(); }
    public BufferedSocket(String host, int port) throws IOException { super(host, port); }
    public BufferedSocket(SocketImpl base) throws IOException { super(base); }

    @Override
    public BufferedInputStream getInputStream() throws IOException {
        if(this.bis == null)
            this.bis = new BufferedInputStream(super.getInputStream());

        return this.bis;
    }

    @Override
    public BufferedOutputStream getOutputStream() throws IOException {
        if(this.bos == null)
            this.bos = new BufferedOutputStream(super.getOutputStream());

        return this.bos;
    }

}
