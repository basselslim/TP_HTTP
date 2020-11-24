///A Simple Web Server (WebServer.java)

package http.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Example program from Chapter 1 Programming Spiders, Bots and Aggregators in
 * Java Copyright 2001 by Jeff Heaton
 *
 * WebServer is a very simple web-server. Any request is responded with a very
 * simple web-page.
 *
 * @author Jeff Heaton
 * @version 1.0
 */
public class WebServer {

	/**
	* WebServer constructor.
	*/
	protected void start() {
		ServerSocket s;

		System.out.println("Webserver starting up on port 3000");
		System.out.println("(press ctrl-c to exit)");
		try {
			// create the main server socket
			s = new ServerSocket(3000);
		} catch (Exception e) {
			System.out.println("Error: " + e);
			return;
		}

		System.out.println("Waiting for connection");
		for (;;) {
			try {
			// wait for a connection
			Socket remote = s.accept();
			// remote is now the connected socket
			System.out.println("Connection, sending data.");
			BufferedReader in = new BufferedReader(new InputStreamReader(remote.getInputStream()));
			BufferedOutputStream out = new BufferedOutputStream(remote.getOutputStream());

			// read the data sent. We basically ignore it,
			// stop reading once a blank line is hit. This
			// blank line signals the end of the client HTTP
			// headers.
			String str = ".";
			String request = in.readLine();
			while (str != null && !str.equals("")) {
				str = in.readLine();
			}
			System.out.println(request);

			// Send the response
			String[] words = request.split(" ");
			String requestType = words[0];
			System.out.println(requestType);
			String fileName = words[1].substring(1, words[1].length());
			if(requestType.equals("GET")) {
				httpGET(out, fileName);
			}
			// Send the HTML page
			out.write(("<H1>Welcome to the Ultra Mini-WebServer</H2>").getBytes());
			out.flush();
			remote.close();
			} catch (Exception e) {
				System.out.println("Error: " + e);
			}
		}
	}

	//HTTP GET method
	protected void httpGET(BufferedOutputStream out, String filename){
		System.out.println("GET " + filename);
		filename = "doc/" + filename;
		try{
			File file = new File(filename);
			if(file.exists() && file.isFile()){
				out.write(makeHeader("200 OK", filename, file.length()).getBytes());
			}else{
				file = new File("doc/file_not_found.html");
				out.write(makeHeader("404 Not Found", "doc/file_not_found.html", file.length()).getBytes());
			}

			BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(file));
			// Envoi du corps : le fichier (page HTML, image, vid�o...)
			byte[] buffer = new byte[256];
			int nbRead;
			while((nbRead = fileIn.read(buffer)) != -1) {
				out.write(buffer);
			}
			fileIn.close();

			out.flush();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	//HTTP PUT method

	protected String makeHeader(String status) {
		String header = "HTTP/1.0 " + status + "\r\n";
		header += "Server: Bot\r\n";
		header += "\r\n";
		System.out.println("ANSWER HEADER :");
		System.out.println(header);
		return header;
	}

	protected String makeHeader(String status, String filename, long length) {
		String header = "HTTP/1.1 " + status + "\r\n";
		if(filename.endsWith(".html") || filename.endsWith(".htm"))
			header += "Content-Type: text/html\r\n";
		else if(filename.endsWith(".mp4"))
			header += "Content-Type: video/mp4\r\n";
		else if(filename.endsWith(".png"))
			header += "Content-Type: image/png\r\n";
		else if(filename.endsWith(".jpeg") || filename.endsWith(".jpeg"))
			header += "Content-Type: image/jpg\r\n";
		else if(filename.endsWith(".mp3"))
			header += "Content-Type: audio/mp3\r\n";
		else if(filename.endsWith(".avi"))
			header += "Content-Type: video/x-msvideo\r\n";
		else if(filename.endsWith(".css"))
			header += "Content-Type: text/css\r\n";
		else if(filename.endsWith(".pdf"))
			header += "Content-Type: application/pdf\r\n";
		else if(filename.endsWith(".odt"))
			header += "Content-Type: application/vnd.oasis.opendocument.text\r\n";
		header += "Content-Length: " + length + "\r\n";
		header += "Server: Bot\r\n";
		header += "\r\n";
		System.out.println("ANSWER HEADER :");
		System.out.println(header);
		return header;
	}

	/**
	* Start the application.
	*
	* @param args
	*            Command line parameters are not used.
	*/
	public static void main(String args[]) {
		WebServer ws = new WebServer();
		ws.start();
	}
}
