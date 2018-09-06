package com.uniquid.tank.entity;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Timer;
import java.util.TimerTask;

/**
 * The Tank simulates a liquid container that have an input faucet and an output faucet.
 */
public class Tank {
	
	public static String mqttbroker = "";
	public static String tankname = "";

	private static final Logger LOGGER = LoggerFactory.getLogger(Tank.class);
	private static final String CONSOLE = "CONSOLE";
	
	private static final Marker MARKER = MarkerFactory.getMarker(CONSOLE);

	private static Tank INSTANCE;

	private boolean opened;
	private boolean inputOpen;
	private boolean outputOpen;

	private int level = 0;

	private final Object syncObject;

	private Timer inputTimer;
	private Timer outputTimer;

	private Tank() {
		this.opened = false;
		this.syncObject = new Object();
		this.inputTimer = new Timer();
		this.outputTimer = new Timer();
		this.inputOpen = false;
		this.outputOpen = false;

		createChart();
	}

	public static synchronized Tank getInstance() {

		if (INSTANCE == null) {
			INSTANCE = new Tank();
		}

		return INSTANCE;
	}

	public int getLevel() {

		synchronized (syncObject) {

			return level;

		}

	}

	public boolean isInputOpen() {

		synchronized (syncObject) {

			return inputOpen;

		}

	}

	public boolean isOutputOpen() {

		synchronized (syncObject) {

			return outputOpen;

		}

	}

	public void open() {

		synchronized (syncObject) {

			if (!opened) {

				this.opened = true;
				
				LOGGER.info(MARKER, "Tank opened!");
				
			}

		}
	}

	public void close() {

		synchronized (syncObject) {

			if (opened) {

				inputTimer.cancel();
				outputTimer.cancel();

				inputTimer = new Timer();
				outputTimer = new Timer();

				this.opened = false;
				
				LOGGER.info(MARKER, "Tank closed!");

			}

		}
	}

	public void openInput() {

		synchronized (syncObject) {

			if (!inputOpen) {

				inputOpen = true;
				inputTimer.schedule(new FillTankTimerTask(), 0, 500);

			}

		}

	}

	public void closeInput() {

		synchronized (syncObject) {

			if (inputOpen) {

				inputOpen = false;
				inputTimer.cancel();
				inputTimer = new Timer();

			}

		}

	}

	public void openOutput() {

		synchronized (syncObject) {

			if (!outputOpen) {

				outputOpen = true;
				outputTimer.schedule(new FlushTankTimerTask(), 0, 500);

			}

		}

	}

	public void closeOutput() {

		synchronized (syncObject) {

			if (outputOpen) {

				outputOpen = false;
				outputTimer.cancel();
				outputTimer = new Timer();

			}

		}

	}

	public class FillTankTimerTask extends TimerTask {

		@Override
		public void run() {

			synchronized (syncObject) {

				level += 1;

				LOGGER.info(MARKER, "Tank level: " + level);
				
				updateOnMQTT();
				
			}

		}

	}

	public class FlushTankTimerTask extends TimerTask {

		@Override
		public void run() {

			synchronized (syncObject) {

				if (level > 2) {
					level -= 2;
				}

				LOGGER.info(MARKER, "Tank level: " + level);
				
				updateOnMQTT();
				
			}

		}

	}
	
	private void updateOnMQTT() {
		
		// Create empty json object
		JSONObject jsonObject = new JSONObject();

		JSONArray jsonArray = new JSONArray();
		jsonArray.put(level);
		
		// populate sender
		jsonObject.put("series", jsonArray);
		
		jsonObject.put("message", opened ? "OPENED" : "CLOSED");
		
		BlockingConnection connection = null;
		
		try {
			final MQTT mqtt = new MQTT();
			
			mqtt.setHost(mqttbroker);
			
			connection = mqtt.blockingConnection();
			connection.connect();
			
			final String destinationTopic = "/outbox/" + tankname + "/status";
			
			final String sender = tankname;
			
			// to subscribe
			final Topic[] topics = { new Topic(sender, QoS.AT_LEAST_ONCE) };
			/*byte[] qoses = */connection.subscribe(topics);

			// consume
			connection.publish(destinationTopic, jsonObject.toString().getBytes(), QoS.AT_LEAST_ONCE, false);
			
		} catch (Throwable t) {
			
			LOGGER.error(MARKER, "Exception", t);
			
		} finally {
			
			// disconnect
			try {

				if (connection != null)
					connection.disconnect();

			} catch (Exception ex) {

				LOGGER.error(MARKER, "Catched Exception", ex);

			}

		} 
	}

	private void createChart() {

		BlockingConnection connection = null;

		try {
			final MQTT mqtt = new MQTT();

			mqtt.setHost(mqttbroker);

			connection = mqtt.blockingConnection();
			connection.connect();

			final String destinationTopic = "/outbox/" + tankname + "/deviceInfo";

			final String sender = tankname;

			// to subscribe
			final Topic[] topics = { new Topic(sender, QoS.AT_LEAST_ONCE) };
			/*byte[] qoses = */connection.subscribe(topics);

			String message = "{\"deviceInfo\":{\"name\":\"Tank\",\"endPoints\":{\"status\":{\"values\":{\"labels\": [\" \"],\"series\": [0],\"message\":\"CLOSED\"},\"total\": 100,\"centerSum\": 1,\"card-type\": \"crouton-chart-donut\"}},\"description\": \"Uniquid Java Tank Simulator\",\"status\": \"good\"}}";
			// consume
			connection.publish(destinationTopic, message.getBytes(), QoS.AT_LEAST_ONCE, false);

		} catch (Throwable t) {

			LOGGER.error(MARKER, "Exception", t);

		} finally {

			// disconnect
			try {

				if (connection != null)
					connection.disconnect();

			} catch (Exception ex) {

				LOGGER.error(MARKER, "Catched Exception", ex);

			}

		}

	}

}
