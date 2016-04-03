package es.unizar.tmdad.msg.reqrepl;

import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;

import es.unizar.tmdad.msg.reqrepl.Requestor.CommandType;

import org.json.*;

public class Replier {

	private final static String REQUEST_QUEUE_NAME = "request_queue";		

	private static String addPrefix(String word) {
		return "PREFIJO"+word;
	}

	private static String addSuffix(String word) {
		return word+"SUFIJO";
	}

	public static void main(String[] argv) {
		Connection connection = null;
		try {
			// Creamos una conexión al broker RabbitMQ en localhost
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost("localhost");
			connection = factory.newConnection();
			// Con un solo canal
			Channel channel = connection.createChannel();
			// Declaramos la cola para las peticiones
			channel.queueDeclare(REQUEST_QUEUE_NAME, false, false, false, null);

			// Creamos un objeto consumer para coger peticiones (luego 
			// podremos leer mensajes usando p.ej. consumer.nextDelivery)
			QueueingConsumer consumer = new QueueingConsumer(channel);
			channel.basicConsume(REQUEST_QUEUE_NAME, true, consumer);

			System.out.println("Esperando peticiones...");

			while (true) {			
				QueueingConsumer.Delivery delivery = consumer.nextDelivery();

				// Cabecera del mensaje de petición
				BasicProperties props = delivery.getProperties();

				// Ponemos como id de correlación de la respuesta, el id
				// de correlación de la petición
				BasicProperties replyProps = new BasicProperties.Builder()
				.correlationId(props.getCorrelationId())
				.build();

				try {
					String reply = "";				
					String requestBody = new String(delivery.getBody(), "UTF-8");
					JSONObject requestAsJsonObject = new JSONObject(requestBody);
					String command = requestAsJsonObject.getString("command");
					String word = requestAsJsonObject.getString("word");
					if (command.equals(CommandType.PREFIX.toString())) {
						reply = addPrefix(word);
					} else if (command.equals(CommandType.SUFFIX.toString())) {
						reply = addSuffix(word);
					} else {
						throw new Exception("Comando desconocido. Una " + 
								"implementación más completa pondría el mensaje recibido "+
								"en un canal de mensaje inválido.");
					}							
					System.out.println("Recibido comando petición " + command + " con parámetro " + word);
					System.out.println("Con este id de correlación en la cabecera: " + props.getCorrelationId());
					System.out.println("Con esta dirección de respuesta: " + props.getReplyTo());
					System.out.println("Respondo con: " + reply);
					// Respondemos en la dirección de respuesta (props.getReplyTo)
					channel.basicPublish( "", props.getReplyTo(), replyProps, reply.getBytes("UTF-8"));				
				}	catch (Exception e){
					e.printStackTrace();				
				}
			}
		} catch (Exception e) {
			if (connection != null) {
				try {
					connection.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}			
			}      		      
		}
	}
}


