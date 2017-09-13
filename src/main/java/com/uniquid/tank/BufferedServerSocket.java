package com.uniquid.tank;

import java.io.IOException;
import java.net.*;

public class BufferedServerSocket extends ServerSocket {

    public BufferedServerSocket() throws IOException {
    }

    public BufferedServerSocket(int port) throws IOException {
        super(port);
    }

    public BufferedServerSocket(int port, int backlog) throws IOException {
        super(port, backlog);
    }

    public BufferedServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        super(port, backlog, bindAddr);
    }

    @Override
    public Socket accept() throws IOException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        if (!isBound())
            throw new SocketException("Socket is not bound yet");
        final Socket s = new BufferedSocket((SocketImpl) null);
        implAccept(s);
        return s;
    }
}
