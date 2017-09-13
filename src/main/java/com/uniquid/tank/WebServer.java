package com.uniquid.tank;

import java.io.InputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
    private static boolean isRunning = false;
    private static Thread serverThread;

    public void launchWebserver() {
        if(!isRunning)
        {
            isRunning = true;
            Thread webServerThread = new Thread(new WebServer());
            webServerThread.setName("Hexcast-WebServerThread");
            webServerThread.start();
            serverThread = webServerThread;
        }
        else  {
            logger.error("A web server is already running!");
        }
    }

    public void shutdownWebserver() {
        if(!isRunning)
            logger.error("There isn't a webserver to shutdown");
        else
        {
            logger.info("Attempting to shutdown webserver");
            isRunning = false;
        }
    }

    @Override
    public void run()
    {
        try
        {
            String cn = "hexcast.hivemedia.net.au",
                   ou = "Production",
                   o  = "Hive Media Productions",
                   c  = "AU";

            int defaultPort = Integer.parseInt("9200");

            BufferedServerSocket serverSocket = new BufferedServerSocket(defaultPort);
            logger.info("Web server started on " + serverSocket.toString());

            while(isRunning)
            {
                BufferedSocket clientSocket = (BufferedSocket) serverSocket.accept();

                if(SecurityTools.isSSLPacket(clientSocket.getInputStream()))
                {
                    logger.debug(clientSocket.toString() + " handled as SSL client");
                    Socket clientSecureSocket = SecurityTools.convertToSecureSocket(clientSocket, String.format("cn=%s,ou=%s,o=%s,c=%s", cn, ou, o, c));
                    
                    InputStream is = clientSecureSocket.getInputStream();
                    
                    int content;
            		StringBuffer stringBuffer = new StringBuffer();
            		while ((content = is.read()) != -1) {
            			
            			stringBuffer.append((char) content);
            			
            		}
            		
            		System.out.println(stringBuffer.toString());
            		
            		
                    //WebConnectionHandler.handleClient(clientSecureSocket);
                }
                else {
                    logger.debug(clientSocket.toString() + " handled as plain text client");
                    //WebConnectionHandler.handleClient(clientSocket);
                }
            }

            serverSocket.close();
        }
        catch(Exception ex)
        {
            logger.error("Error was thrown while running the web server", ex);
            System.exit(1);
        }
    }
    
    public static void main(String[] args) throws Exception {
    	WebServer w = new WebServer();
    	w.launchWebserver();
    }
}
