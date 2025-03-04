package es.unizar.tmdad.msg.expubsub;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;

/** 
 * @author Rubén Béjar <http://www.rubenbejar.com>
 */
public class ReceptorPubSub {

	private final static String EXCHANGE_NAME = "expubsub";
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

		// Declaramos una centralita de tipo fanout llamada EXCHANGE_NAME
		channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
		// Creamos una nueva cola temporal (no durable, exclusiva y
		// que se borrará automáticamente cuando nos desconectemos
		// del servidor de RabbitMQ). El servidor le dará un
		// nombre aleatorio que guardaremos en queueName
		String queueName = channel.queueDeclare().getQueue();
		// E indicamos que queremos que la centralita EXCHANGE_NAME
		// envíe los mensajes a la cola recién creada. Para ello creamos
		// una unión (binding) entre ellas (la clave de enrutado
		// la ponemos vacía, porque se va a ignorar)	
		channel.queueBind(queueName, EXCHANGE_NAME, "");
		
		
		System.out.println(" [*] Esperando mensajes. CTRL+C para salir");

		while (true) {
			// pide un mensaje a la cola queueName
			// autoAck a True
			GetResponse delivery = channel.basicGet(queueName, true);
			if (delivery != null) {
				String message = new String(delivery.getBody());
				System.out.println(" [x] Recibido '" + message + "'");
			}
		}
	}
}
