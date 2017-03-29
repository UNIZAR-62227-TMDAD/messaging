package es.unizar.tmdad.msg.exevents2;

import java.io.IOException;
import java.util.Scanner;

import com.rabbitmq.client.*;

/**
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
		
		
		// Creamos un canal
		Channel channel1 = connection.createChannel();

		// Declaramos una cola en el broker a través del canal
		// recién creado llamada QUEUE_NAME (operación
		// idempotente: solo se creará si no existe ya)
		// Se crea tanto en el emisor como en el receptor, porque no
		// sabemos cuál se lanzará antes
		// Indicamos que no sea durable ni exclusiva
		channel1.queueDeclare(QUEUE_NAME, false, false, false, null);
		System.out.println(" [*] Esperando mensajes en el thread " + 
					Thread.currentThread().getId() + ". CTRL+C para salir");

		
		// No entregar un nuevo mensaje hasta haber ack del anterior 
		channel1.basicQos(1);
		
		// El segundo parámetro indica autoACK = false, se envían
		// ACK explícitamente cuando hemos "procesado" los mensajes.
		// Hemos creado un consumidor básico. Se llamará a handleDelivery
		// cada vez que llege un mensaje a la cola QUEUE_NAME.
		channel1.basicConsume(QUEUE_NAME, false, 
				new DefaultConsumer(channel1) {
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
						System.out.println("[Canal 1] Recibido '" + message + "' en el thread id = " +
								Thread.currentThread().getId());
						getChannel().basicAck(deliveryTag,false);
						
					}		
		});
		
		
		// Creamos otro canal
		Channel channel2 = connection.createChannel();
		// No entregar un nuevo mensaje hasta haber ack del anterior 
		channel2.basicQos(1);
		
		// El segundo parámetro indica autoACK = false, se envían
		// ACK explícitamente cuando hemos "procesado" los mensajes.
		// Hemos creado un consumidor básico. Se llamará a handleDelivery
		// cada vez que llege un mensaje a la cola QUEUE_NAME.
		channel2.basicConsume(QUEUE_NAME, false, 
				new DefaultConsumer(channel2) {
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
						System.out.println("[Canal 2] Recibido '" + message + "' en el thread id = " +
								Thread.currentThread().getId());
						getChannel().basicAck(deliveryTag,false);
					}		
		});
			
		System.out.println("Pulsa <Enter> para terminar");
		Scanner sc = new Scanner(System.in);
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
