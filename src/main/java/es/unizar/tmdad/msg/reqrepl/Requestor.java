package es.unizar.tmdad.msg.reqrepl;


import java.util.Scanner;
import java.util.UUID;

import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;

import org.json.*;

/** 
 * @author Rubén Béjar <http://www.rubenbejar.com>
 */
public class Requestor {
	
	public enum CommandType {
		PREFIX,
		SUFFIX,		
		NONE
	}

	private final static String REQUEST_QUEUE_NAME = "request_queue";		
	
	private Connection connection;
	private QueueingConsumer consumer;
	private Channel channel;
	private String replyQueueName;	
	
	public Requestor() throws Exception {
		// Creamos una conexión al broker RabbitMQ en localhost
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		connection = factory.newConnection();
		// Con un solo canal
		channel = connection.createChannel();
        // Declaramos la cola para las peticiones
		channel.queueDeclare(REQUEST_QUEUE_NAME, false, false, false, null);
		// Creamos una cola "temporal" para las respuestas
		// (nombre generado automáticamente,
		// no durable y con autodelete (el broker RabbitMQ la
		// borrará cuando ya no se use)).		
		replyQueueName = channel.queueDeclare().getQueue(); 
		// Creamos un objeto consumer para coger respuestas (luego 
		// podremos leer mensajes usando p.ej. consumer.nextDelivery)
		consumer = new QueueingConsumer(channel);
	    channel.basicConsume(replyQueueName, true, consumer);
	}
	
	public String doRequest(CommandType commandChosen, String wordEntered) throws Exception {
		String commandAsString = "{ \"command\": \""+ commandChosen.toString()  +"\", \"word\": \""+wordEntered +"\" }";
		JSONObject commandAsJson = new JSONObject(commandAsString);

		String correlationId = UUID.randomUUID().toString();			
		
		BasicProperties props = new BasicProperties	.Builder()
				.correlationId(correlationId)
				.replyTo(replyQueueName)
				.contentEncoding("application/json") // Podemos indicar el content encoding
				.build();
			
		// Hacer la petición
		// Usamos el exchange por defecto, que enruta a la cola que le indicamos
		channel.basicPublish("", REQUEST_QUEUE_NAME, props, commandAsJson.toString().getBytes());

		System.out.println("Enviado mensaje comando con este cuerpo: " + commandAsJson.toString());
		System.out.println("Con este id de correlación en la cabecera: " + correlationId);
		System.out.println("Con esta dirección de respuesta: " + replyQueueName);
		
		// Esperar la respuesta
		String reply = null;
		while (true) {
			QueueingConsumer.Delivery delivery = consumer.nextDelivery();
			// Si el id de correlación no fuera el que esperamos no 			
			// saldríamos del bucle
			if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
				// El contenido de la respuesta es un String
				reply = new String(delivery.getBody(), "UTF-8");
				break;
			}
		}
		
		return reply; 
	}
	
	public void close() throws Exception {
		channel.close();
		connection.close();
	}

	public static void main(String[] argv) throws Exception {
		Requestor requestor = null;
		
		try {
			requestor = new Requestor();
			CommandType commandChosen = CommandType.NONE;		
			do {
				int numberEntered = -1;
				System.out.println("Elige comando (0 para PREFIJO, cualquier otro para SUFIJO) y pulsa <Enter>: ");
				try {
					Scanner sc = new Scanner(System.in);				
					numberEntered = sc.nextInt();
					if (numberEntered == 0) {
						commandChosen = CommandType.PREFIX;
					} else {
						commandChosen = CommandType.SUFFIX;
					}
				} catch (Exception e) {
					System.out.println("Tienes que escribir un número.");
				}
			} while (commandChosen == CommandType.NONE);
			
			// Cualquier palabra escrita nos vale, y por defecto blancos, espacios etc.
			// no cuentan
			String wordEntered = null;		
			System.out.println("Escribe una palabra para enviar y pulsa <Enter>: ");
			Scanner sc = new Scanner(System.in);
			wordEntered = sc.next();
			
			String reply = requestor.doRequest(commandChosen, wordEntered);
			System.out.println("Ha llegado una respuesta con el siguiente contenido: " + reply);
		} catch (Exception e) {
			e.printStackTrace();			
		} finally {
			try {
				requestor.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
	}
}

