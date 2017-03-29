package es.unizar.tmdad.msg.exevents1;

import java.io.IOException;
import java.util.Scanner;

import com.rabbitmq.client.*;

/**
 * @author Rubén Béjar <http://www.rubenbejar.com>
 *
 */
public class Receptor {

	private final static String QUEUE_NAME = "hola";
	private final static String CONSUMER_TAG = "consumertag";
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
		System.out.println(" [*] Esperando mensajes en el thread " + 
					Thread.currentThread().getId() + ". CTRL+C para salir");
			
		// El broker no entregará un nuevo mensaje a este
		// consumidor hasta haber recibido un ACK del anterior 
		int prefetchCount = 1;
		channel.basicQos(prefetchCount);
		
		// El segundo parámetro indica autoACK = false, se envían
		// ACK explícitamente cuando hemos "procesado" los mensajes.
		// Hemos creado un consumidor básico. Se llamará a handleDelivery
		// cada vez que llege un mensaje a la cola QUEUE_NAME.
		// CONSUMER_TAG identifica al objeto consumidor
		channel.basicConsume(QUEUE_NAME, false, CONSUMER_TAG,
				new DefaultConsumer(channel) {
					@Override
					public void handleDelivery(String consumerTag, Envelope envelope, 
							AMQP.BasicProperties properties, byte[] body) throws IOException {												
						long deliveryTag = envelope.getDeliveryTag();
						String message = new String(body);
						// Simulamos un trabajo de tiempo variable (entre 0 y 3 segundos)
						// para procesar el mensaje
						try {
							Thread.sleep(new java.util.Random().nextInt(3000));
						} catch (InterruptedException e) {					
							e.printStackTrace();
						}
						System.out.println("[x] Recibido '" + message + "' en el thread id = " +
								Thread.currentThread().getId());
						getChannel().basicAck(deliveryTag,false);						
					}		
		});
					
		System.out.println("Pulsa <Enter> para que el consumidor deje de consumir mensajes");
		Scanner sc = new Scanner(System.in);
		sc.nextLine();		
		// Deja de consumir mensajes. Si no hubiéramos puesto qos a 1 y ACK manual,
		// el consumidor seguiría "procesando" los mensajes que tuviera buffereados
		// localmente, pero ya no aceptaría nuevos que enviamos. Como hemos puesto
		// qos a 1 y ACK manual, deja de consumir mensajes inmediatamente (aunque
		// si está a medio procesar de uno, ese terminará de procesarlo)
		channel.basicCancel(CONSUMER_TAG);
		

		System.out.println("Pulsa <Enter> de nuevo para terminar");
		sc = new Scanner(System.in);
		sc.nextLine();		
		// Cerramos la conexión: esto tb. cierra todos los canales asociados a la misma
		// y hace shutdown sobre el pool de threads asociado a la conexión y que se usa
		// para los consumidores. De esa forma ya no quedan threads activos y el programa
		// puede terminar
		connection.close();
		// Si no cerráramos la conexión, el programa no se terminaría porque 
		// seguiría habiendo threads activos (que no son de tipo deamon)			
	}
}
