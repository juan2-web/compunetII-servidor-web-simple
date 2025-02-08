

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public final class WebServer {
    public static void main(String argv[]) throws Exception {
        // numero de puerto
        int puerto = 6789;
        // establecer el socket de escucha
        ServerSocket socketDeEscucha = new ServerSocket(puerto);
        // agregar manejo de hilos
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        // procesar las solicitudes http en un ciclo infinito
        while (true) {
            //escuchar solicitudes de conexion tcp
            Socket socketDeConexion = socketDeEscucha.accept();
            //objeto para procesar el mensaje de solicitud http
            SolicitudHttp solicitud = new SolicitudHttp(socketDeConexion);
            // al parecer no es necesario crear manualmente un hilo 
            // cuando se usa threadpool executor
            //crear un nuevo hilo para la solicitud
            //Thread hilo = new Thread(solicitud);
            //ya no tenemos que inicializar el hilo al usar executor (threadpool)
            //hilo.start();
            executor.execute(solicitud);
        }
    }
}

final class SolicitudHttp implements Runnable {
    // por la especificacion del protocolo http, 
    // cada linea se debe terminar con un carriage return (CR)
    // y un line feed
    final static String CRLF = "\r\n";
    // referencia al socket de conexion
    Socket socket;
    // constructor
    public SolicitudHttp(Socket socket) throws Exception {
        this.socket = socket;
    }
    // implementar el metodo run de la interface runnable
    // esto es para poder pasar una instancia de la clase
    // solicitud http al constructor de hilos
    public void run(){
        try {
            proceseSolicitud();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    private void proceseSolicitud() throws Exception{
        // referencia al stream de salida del socket
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
        // referencia y filtros (InputStreamReader y BufferedReader)para el stream de entrada.
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        //linea de solicitud http del mensaje
        String lineaDeSolicitud = br.readLine();
        //muestra la linea de solicitud
        System.out.println();
        System.out.println(lineaDeSolicitud);
        // extraer el nombre del archivo
        StringTokenizer partesLinea = new StringTokenizer(lineaDeSolicitud);
        // suponemos que el metodo es GET
        partesLinea.nextToken();
        String nombreArchivo = partesLinea.nextToken();
        // anexamos un punto para que el archivo tenga que estar en el directorio actual
        // (cuando el navegador envia el archivo, tiene un slash, por eso sirve solo sumarle el punto)
        nombreArchivo = "."+nombreArchivo;
        // abrir el archivo
        FileInputStream fis = null;
        boolean existeArchivo = true; 
        try {
            fis = new FileInputStream(nombreArchivo);
        } catch (FileNotFoundException e) {
            existeArchivo = false;
        }
        // construimos el mensaje de respuesta
        String lineaDeEstado = null;
        String lineaDeTipoContenido = null;
        String cuerpoMensaje = null;

        if (existeArchivo) {
            lineaDeEstado = "HTTP/1.1 200 OK";
            // Cuando el archivo existe, se debe determinar el tipo de archivo MIME y enviar el especificador de tipo MIME apropiado.
            lineaDeTipoContenido =  "Content-type: " + contentType(nombreArchivo) + CRLF;
        } else {
            lineaDeEstado = "HTTP/1.1 404 Not Found";
            lineaDeTipoContenido = "Content-Type: text/html" + CRLF;
            cuerpoMensaje = "<HTML>" + 
                "<HEAD><TITLE>404 Not Found</TITLE></HEAD>" +
                "<BODY><b>404</b> Not Found</BODY></HTML>";
        }
        // recoge y muestra las líneas de header.
        String lineaDelHeader = null;
        while ((lineaDelHeader = br.readLine()).length() != 0) {
            System.out.println(lineaDelHeader);
        }
        os.writeBytes(lineaDeEstado + CRLF);
        os.writeBytes(lineaDeTipoContenido);
        //linea en blanco para indicar el final de las lineas del header
        os.writeBytes(CRLF);
        // ENVIAR EL CUERPO DEL MENSAJE
        if (existeArchivo) {
            enviarBytes(fis, os);
            fis.close();
        } else {
            os.writeBytes(cuerpoMensaje);
        }
        // cierra los streams y el socket
        
        br.close();
        os.close();
        socket.close();
    }

    private static void enviarBytes(FileInputStream fis, OutputStream os) throws Exception{
        // buffer de 1KB para los bytes que van hacia el socket
        byte[] buffer = new byte[1024];
        int bytes = 0;
        // copia el archivo en el outputstream del socket
        // cuando se alcanza el final del archivo se retorna -1
        while ((bytes = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytes);
        }
    }

    private static String contentType(String nombreArchivo){
        if (nombreArchivo.endsWith(".htm") || nombreArchivo.endsWith(".html")) {
            return "text/html";        
        }
        if (nombreArchivo.endsWith(".gif")) {
            return "image/gif";        
        }
        if (nombreArchivo.endsWith(".jpeg") || nombreArchivo.endsWith(".jpg")) {
            return "image/jpeg";        
        }
        return "application/octet-stream";
    }

}
