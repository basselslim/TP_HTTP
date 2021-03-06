///A Simple Web Server (WebServer.java)

package http.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * WebServer is a very simple web-server HTTP, it implements simple requests such as GET, HEAD, PUT, POST, and DELETE
 * @author Bassel Slim et Clément Parret
 */

public class WebServer {

	/**
	* Start the webserver on the port specified (here port is 3000).
    * The web-server waits for a socket connection, then it receives a request, it sends the response according to the
    * action asked, and it closed the socket connection.
    * The web-server manages different errors: if client tries to access to a not found directory, if he tries to use a
    * not-omplemented request...
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
				// Waiting for Socket acceptation 
				Socket remote = s.accept();
				// Connection accepted
				System.out.println("Connection accepted ");
				
				BufferedInputStream in = new BufferedInputStream(remote.getInputStream());
				BufferedOutputStream out = new BufferedOutputStream(remote.getOutputStream());

				// Header reading
				System.out.println("Waiting for data...");
				String header = new String();
				
				int current = '\0';
				int precedent = '\0';
				boolean newline = false;
				while((current = in.read()) != -1 && !(newline && precedent == '\r' && current == '\n')) {
					if(precedent == '\r' && current == '\n') {
						newline = true;
					} else if(!(precedent == '\n' && current == '\r')) {
						newline = false;
					}
					precedent = current;
					header += (char) current;
				}

				// If current is -1, Header doesn't end with an empty line --> Bad request
				if(current != -1 && !header.isEmpty()) {
					String[] text = header.split(" ");
					String requestType = text[0];
					String resourceName = text[1].substring(1, text[1].length()); //to get rid of the /
					// Index by default
					if(resourceName.isEmpty()) {
						httpGET(out, "/doc/index.html");
						// Is the file in the right directory
					} else if(resourceName.startsWith("doc")) {
						// Choosing the right request: GET, PUT...
						if(requestType.equals("GET")) {
							httpGET(out, resourceName);
						} else if(requestType.equals("PUT")) {
							httpPUT(in, out, resourceName);
						} else if(requestType.equals("POST")) {
							httpPOST(in, out, resourceName);
						} else if(requestType.equals("HEAD")) {
							httpHEAD(in, out, resourceName);
						} else if(requestType.equals("DELETE")) {
							httpDELETE(out, resourceName);
						} else {
							// If the request asked is not implemented in our code
							out.write(makeHeader("501 Request Not Implemented").getBytes());
							out.flush();
						}
					} else {
						// If we're not in the doc directory
						out.write(makeHeader("403 Bad Directory").getBytes());
						out.flush();
					}
				} else {
					out.write(makeHeader("400 Bad Request").getBytes());
					out.flush();
				}
				// Closing the socket and the connexion
				remote.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * HTTP GET request implementation.
	 * It opens and reads a file, then sends it to the client in a stream of bytes.
	 * The response contains the header and the body of the ressource.
	 * If the file is not found, the response contains the file file_not_found.html.
	 * @param out Output stream in binary, containing the response.
	 * @param filename Path of the file to get.
	 */
	protected void httpGET(BufferedOutputStream out, String filename){
		System.out.println("GET " + filename);
		try{
			File file = new File(filename);
			if(file.exists() && file.isFile()){
				out.write(makeHeader("200 OK", filename, file.length()).getBytes());
			}else{
				file = new File("doc/file_not_found.html");
				out.write(makeHeader("404 Not Found", "doc/file_not_found.html", file.length()).getBytes());
			}

			// Opening a buffered stream
			BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(file));
			// Reading and sending the stream of bytes
			byte[] buffer = new byte[256];
			int nbRead;
			while((nbRead = fileIn.read(buffer)) != -1) {
				out.write(buffer, 0, nbRead);
			}
			// Closing the stream
			fileIn.close();

			out.flush();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	/**
	 * HTTP HEAD request implementation.
	 * HEAD checks if a file exists and sends the header.
	 * If the file is found, status is "200 OK", else it is "404 Not Found".
	 * @param in Input stream in binary.
	 * @param out Output stream in binary, containing the response.
	 * @param filename Path of the file to get.
	 */
	protected void httpHEAD(BufferedInputStream in, BufferedOutputStream out, String filename) {
		System.out.println("HEAD " + filename);
		try{
			File file = new File(filename);
			if(file.exists() && file.isFile()){
				out.write(makeHeader("200 OK", filename, file.length()).getBytes());
			}else{
				file = new File("doc/file_not_found.html");
				out.write(makeHeader("404 Not Found", "doc/file_not_found.html", file.length()).getBytes());
			}

			out.flush();
		}catch(IOException e){
			e.printStackTrace();
			try {
				out.write(makeHeader("500 Internal Server Error").getBytes());
				out.flush();
			} catch (Exception e2) {};
		}
	}

	/**
	 * HTTP PUT request implementation.
	 * PUT request allows the client to write on an existing file or to create a new file. Only the header is sent.
	 * If the specified file already exists, file is overwritten. The status is "204 No Content".
	 * If the specified file does not exist, a new one is created. The status is "201 Created".
	 * @param in Input stream in binary.
	 * @param out Output stream in binary, containing the response.
	 * @param filename Path of the file to create or modify.
	 */
	protected void httpPUT(BufferedInputStream in, BufferedOutputStream out, String filename){
		System.out.println("PUT " + filename);
		try {
			File file = new File(filename);
			boolean existed = file.exists();

			// Erasing the current file if already created
			PrintWriter pw = new PrintWriter(file);
			pw.close();

			// Opening the buffered stream
			BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(file));

			// Reading the stream
			byte[] buffer = new byte[256];
			while(in.available() > 0) {
				int nbRead = in.read(buffer);
				fileOut.write(buffer, 0, nbRead);
			}
			fileOut.flush();

			// Closing the stream
			fileOut.close();

			// Sending the header
			if(existed) {
				// File well modified
				out.write(makeHeader("204 No Content").getBytes());
			} else {
				// New file created
				out.write(makeHeader("201 Created").getBytes());
			}
			// sending the stream of bytes
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				out.write(makeHeader("500 Internal Server Error").getBytes());
				out.flush();
			} catch (Exception e2) {};
		}
	}

	/**
	 * HTTP POST request implementation.
	 * POST request allows the client to write on an existing file or to create a new file. Only the header is sent.
	 * If the specified file already exists, it appends the body of the request to the file. The status is "200 OK".
	 * If the specified file does not exist, a new one is created. The status is "201 Created".
	 * @param in Input stream in binary.
	 * @param out Output stream in binary, containing the response.
	 * @param filename Path of the file to create or modify.
	 */
	protected void httpPOST(BufferedInputStream in, BufferedOutputStream out, String filename){
		System.out.println("POST " + filename);
		try {
			File file = new File(filename);
			boolean existed = file.exists();

			// Opening the buffered stream, and going directly at the end of the file
			BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(file, existed));

			// Getting the information and copying it in the file
			byte[] buffer = new byte[256];
			while(in.available() > 0) {
				int nbRead = in.read(buffer);
				fileOut.write(buffer, 0, nbRead);
			}
			fileOut.flush();

			// Closing the stream
			fileOut.close();

			// Sending the header
			if(existed) {
				// File well modified
				out.write(makeHeader("200 OK").getBytes());
			} else {
				// New file created
				out.write(makeHeader("201 Created").getBytes());
			}
			// Sending the stream of bytes
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				out.write(makeHeader("500 Internal Server Error").getBytes());
				out.flush();
			} catch (Exception e2) {};
		}
	}

	/**
	 * HTTP DELETE request implementation.
	 * DELETE request allows the client to delete an existing file. Only the header is sent.
	 * If the specified file already exists, it deletes the file. The status is "204 No Content".
	 * If the specified file does not exist, the status is "404 Not Found".
	 * @param out Output stream in binary, containing the response.
	 * @param filename Path of the file to delete.
	 */
	protected void httpDELETE(BufferedOutputStream out, String filename){
		System.out.println("DELETE " + filename);
		try {
			File file = new File(filename);
			// Deleting the file
			boolean deleted = false;
			boolean existed = false;
			if((existed = file.exists()) && file.isFile()) {
				deleted = file.delete();
			}

			// Sending the Header
			if(deleted) {
				// File well deleted
				out.write(makeHeader("204 No Content").getBytes());
			} else if (!existed) {
				// File not found
				out.write(makeHeader("404 Not Found").getBytes());
			}
			// Sending the stream bytes
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				out.write(makeHeader("500 Internal Server Error").getBytes());
				out.flush();
			} catch (Exception e2) {};
		}
	}

	/**
	 * Creates a header response for a request without body.
	 * Header contains the Return Code and the name of the server
	 * @param status Return Code.
	 * @return the header of the response
	 */
	protected String makeHeader(String status) {
		String header = "HTTP/1.1 " + status + "\r\n";
		header += "Server: MyServer\r\n";
		header += "\r\n";
		System.out.println("ANSWER HEADER :");
		System.out.println(header);
		return header;
	}

	/**
	 * Creates a header response for a request with a body.
	 * Header contains the Return Code and the name of the server
	 * @param status Return Code.
	 * @param filename Path of the file to return.
	 * @param length Size of the file in bytes.
	 * @return the header of the response.
	 */
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

		header += "Content-Length: " + length + "\r\n";
		header += "Server: MyServer\r\n";
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
