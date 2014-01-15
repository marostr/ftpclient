/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ftpclient;

/**
 *
 * @author sos
 */


import ftpclient.console.CommandParser;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.plugin.logging.Log;


/**
 * FTPClient is a simple package that implements a Java FTP client.
 * With FTPClient, you can connect to an FTP server and upload multiple files.
  <p>
 * Copyright Paul Mutton,
 *           <a href="http://www.jibble.org/">http://www.jibble.org/</a>
 * 
 */
public class FTPClient {
    
    private final Log log;
    
    
    /**
     * Create an instance of FTPClient.
     */
    public FTPClient() {
        this.log = null;
    }
    
    
    /**
     * Connects to the default port of an FTP server and logs in as
     * anonymous/anonymous.
     */
    public synchronized void connect(String host) throws IOException {
        connect(host, 21);
    }
    
    
    /**
     * Connects to an FTP server and logs in as anonymous/anonymous.
     */
    public synchronized void connect(String host, int port) throws IOException {
        connect(host, port, "anonymous", "anonymous");
    }
    
    
    /**
     * Connects to an FTP server and logs in with the supplied username
     * and password.
     */
    public synchronized void connect(String host, int port, String user, String pass) throws IOException {
        if (socket != null) {
            throw new IOException("FTPClient is already connected. Disconnect first.");
        }
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        
        String response = readLine();
        if (!response.startsWith("220 ")) {
            throw new IOException("FTPClient received an unknown response when connecting to the FTP server: " + response);
        }
        
        sendLine("USER " + user);
        
        response = readLine();
        if (!response.startsWith("331 ")) {
            throw new IOException("FTPClient received an unknown response after sending the user: " + response);
        }
        
        sendLine("PASS " + pass);
        
        response = readLine();
        if (!response.startsWith("230 ")) {
            throw new IOException("FTPClient was unable to log in with the supplied password: " + response);
        }
        
        System.out.println("FTPClient connected to " + host + " at port " + port);
        // Now logged in.
    }
    
    public boolean connected() {
    return !(socket == null);
    }
    
    /**
     * Disconnects from the FTP server.
     */
    public synchronized void disconnect() throws IOException {
        try {
            sendLine("QUIT");
        }
        finally {
            socket = null;
            System.out.println("Disconnected by peer.");
        }
    }
    
    
    /**
     * Returns the working directory of the FTP server it is connected to.
     */
    public synchronized String pwd() throws IOException {
        sendLine("PWD");
        String dir = null;
        String response = readLine();
        if (response.startsWith("257 ")) {
            int firstQuote = response.indexOf('\"');
            int secondQuote = response.indexOf('\"', firstQuote + 1);
            if (secondQuote > 0) {
                dir = response.substring(firstQuote + 1, secondQuote);
            }
        }
        System.out.println("Current directory: " + dir);
        return dir;
    }
    
    public synchronized boolean exists(String file) throws IOException {
        sendLine("SIZE " + file);
        String response = readLine();
        return (!response.startsWith("550 "));
    }
    
    /**
     * Changes permissions on  remote file
     */   
    public synchronized boolean chmod(String perms, String file) throws IOException {
        sendLine("SITE CHMOD " + perms + " " + file);
        String response = readLine();
        System.out.println("chmod response: " + response);
        return (response.startsWith("200 "));
    }


    /**
     * Changes the working directory (like cd). Returns true if successful.
     */   
    public synchronized boolean cd(String dir) throws IOException {
        sendLine("CWD " + dir);
        String response = readLine();
        System.out.println("cwd response: " + response);
        return (response.startsWith("250 "));
    }
    
    public void stor(String filepath) throws IOException {
        stor(new File(filepath));
    }
    /**
     * Sends a file to be stored on the FTP server.
     * Returns true if the file transfer was successful.
     * The file is sent in passive mode to avoid NAT or firewall problems
     * at the client end.
     */
    public synchronized boolean stor(File file) throws IOException {
        if (file.isDirectory()) {
            throw new IOException("FTPClient cannot upload a directory.");
        }
        
        String filename = file.getName();

        return stor(new FileInputStream(file), filename);
    }
    
    
    /**
     * Sends a file to be stored on the FTP server.
     * Returns true if the file transfer was successful.
     * The file is sent in passive mode to avoid NAT or firewall problems
     * at the client end.
     */
    public synchronized boolean stor(InputStream inputStream, String filename) throws IOException {

        BufferedInputStream input = new BufferedInputStream(inputStream);
        
        Socket dataSocket = pasv();
        sendLine("STOR " + filename);
        
        String response = readLine();
        if (!response.startsWith("150 ")) {
            throw new IOException("FTPClient was not allowed to send the file: " + response);
        }
        
        BufferedOutputStream output = new BufferedOutputStream(dataSocket.getOutputStream());
        byte[] buffer = new byte[4096];
        int bytesRead = 0;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        output.flush();
        output.close();
        input.close();
        
        response = readLine();
        System.out.println("stor response: " + response);
        return response.startsWith("226 ");
    }

    public synchronized void ls() throws IOException {
        ls("");
    }
    
    public synchronized boolean ls(String filename) throws IOException {
        Socket dataSocket = pasv();
        
        InputStream input = dataSocket.getInputStream();  
        sendLine("LIST" + " " + filename);
        
        readLine();
        System.out.println("ls command output: "+ getAsString(input));
        input.close();
        String response = readLine();
        System.out.println("ls command response: "+ response);
        return response.startsWith("226");
    }
    
    public synchronized boolean rm(String filename) throws IOException {
        sendLine("DELE " + filename);
        
        String response = readLine();
        System.out.println("rm command response:" + response);
        
        return response.startsWith("250");
    }
    
    public synchronized boolean rmd(String directory) throws IOException {
        sendLine("RMD " + directory);
        
        String response = readLine();
        System.out.println("rmd command response: " + response);
        
        return response.startsWith("250");
    }
    
    public synchronized boolean noop() throws IOException {
        sendLine("NOOP");  
        String response = readLine();
        System.out.println("noop command response: " + response);
        
        return response.startsWith("200");
    }
    
    public void abort() throws IOException {
        sendLine("ABOR");
        System.out.println("ABOR command sent");
    }
    
    

    /**
     * Enter binary mode for sending binary files.
     */
    public synchronized boolean bin() throws IOException {
        sendLine("TYPE I");
        String response = readLine();
        return (response.startsWith("200 "));
    }
    
    
    /**
     * Enter ASCII mode for sending text files. This is usually the default
     * mode. Make sure you use binary mode if you are sending images or
     * other binary data, as ASCII mode is likely to corrupt them.
     */
    public synchronized boolean ascii() throws IOException {
        sendLine("TYPE A");
        String response = readLine();
        return (response.startsWith("200 "));
    }
    
    private synchronized Socket pasv() throws IOException {
        sendLine("PASV");
        String response = readLine();
        if (!response.startsWith("227 ")) {
            throw new IOException(" could not request passive mode: " + response);
        }
        
        String ip = null;
        int port = -1;
        int opening = response.indexOf('(');
        int closing = response.indexOf(')', opening + 1);
        if (closing > 0) {
            String dataLink = response.substring(opening + 1, closing);
            StringTokenizer tokenizer = new StringTokenizer(dataLink, ",");
            try {
                ip = tokenizer.nextToken() + "." + tokenizer.nextToken() + "." + tokenizer.nextToken() + "." + tokenizer.nextToken();
                port = Integer.parseInt(tokenizer.nextToken()) * 256 + Integer.parseInt(tokenizer.nextToken());
            }
            catch (Exception e) {
                throw new IOException("FTPClient received bad data link information: " + response);
            }
        }
        return new Socket(ip, port);
    }
    
    /**
     * Sends a raw command to the FTP server.
     */
    private void sendLine(String line) throws IOException {
        if (socket == null) {
            throw new IOException("FTPClient is not connected.");
        }
        try {
            writer.write(line + "\r\n");
            writer.flush();
            if (DEBUG) {
                System.out.println("> " + line);
            }
        }
        catch (IOException e) {
            socket = null;
            throw e;
        }
    }
    
    private String readLine() throws IOException {
        String line = reader.readLine();
        if (DEBUG) {
            System.out.println("< " + line);
        }
        return line;
    }
    
    private String getAsString(InputStream is) {
        int c=0;
        char lineBuffer[]=new char[128], buf[]=lineBuffer;
        int room= buf.length, offset=0;
        try {
            loop:
            while (true) {
                switch (c = is.read() ) {
                case -1: 
                    break loop;
                default: 
                    if (--room < 0) {
                        buf = new char[offset + 128];
                        room = buf.length - offset - 1;
                        System.arraycopy(lineBuffer, 0,buf, 0, offset);
                        lineBuffer = buf;
                     }
                    buf[offset++] = (char) c;
                    break;
                }
            }
        } catch(IOException e) {
        System.err.println("Error in getAsStrig." + e.getMessage());
        //ioe.printStackTrace();
        }
        if ((c == -1) && (offset == 0)) {
            return null;
        }
            return String.copyValueOf(buf, 0, offset);
    }

    
    private Socket socket = null;
    private BufferedReader reader = null;
    private BufferedWriter writer = null;
    
    private static boolean DEBUG = false;
    
   /*
    public static void main( String [] args) throws Exception {
        try {
            FTPClient client = new FTPClient();
            client.connect("ftp.stosowana.pl", 21, "ftp_www", "tromb0cyt");
            client.cd("www");
            CommandParser.parseCommand(client, "ls");
            CommandParser.parseCommand(client, "ls upload");
            client.disconnect();
        } catch (Exception ex) {
            Logger.getLogger(FTPClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    */
}