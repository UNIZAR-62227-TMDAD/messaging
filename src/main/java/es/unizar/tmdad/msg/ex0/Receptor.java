package es.unizar.tmdad.msg.ex0;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;

/**
 * Adaptado de <https://www.rabbitmq.com/tutorials/tutorial-one-java.html>
 * @author Rubén Béjar <http://www.rubenbejar.com>
 *
 */
public class Receptor {
	private final static String QUEUE_NAME = "hola";
	private final static String ENV_AMQPURL_NAME = "CLOUDAMQP_URL";

	public static void main(String[] argv) throws Exception {
		// Conexión al broker RabbitMQ broker (prueba en la URL de
		// la variable de entorno que se llame como diga ENV_AMQPURL_NAME
		// o sino en localhost)
		ConnectionFactory factory = new ConnectionFactory();
		String amqpURL = System.getenv().get(ENV_AMQPURL_NAME) != null ? System.getenv().get(ENV_AMQPURL_NAME) : "amqp://localhost";
		try {
			factory.setUri(amqpURL);
		} catch (Exception e) {
			System.out.println(" [*] AQMP broker not found in " + amqpURL);
			System.exit(-1);
		}
		System.out.println(" [*] AQMP broker found in " + amqpURL);
		Connection connection = factory.newConnection();
		// Con un solo canal
		Channel channel = connection.createChannel();

		// Declaramos una cola en el broker a través del canal
		// recién creado llamada QUEUE_NAME (operación
		// idempotente: solo se creará si no existe ya)
		// Se crea tanto en el emisor como en el receptor, porque no
		// sabemos cuál se lanzará antes
		// Indicamos que no sea durable ni exclusiva
		channel.queueDeclare(QUEUE_NAME, false, false, false, null);
		System.out.println(" [*] Esperando mensajes. CTRL+C para salir");

		while (true) {
			// pide un mensaje a la cola QUEUE_NAME
			// autoAck a True
			GetResponse delivery = channel.basicGet(QUEUE_NAME, true);
			if (delivery != null) {
				String message = new String(delivery.getBody());
				System.out.println(" [x] Recibido '" + message + "'");
			}
		}
	}
}
