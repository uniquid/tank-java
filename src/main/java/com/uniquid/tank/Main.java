package com.uniquid.tank;

import com.uniquid.connector.Connector;
import com.uniquid.connector.impl.MQTTConnector;
import com.uniquid.core.impl.UniquidSimplifier;
import com.uniquid.messages.AnnounceMessage;
import com.uniquid.node.UniquidNodeState;
import com.uniquid.node.impl.UniquidNodeImpl;
import com.uniquid.node.listeners.EmptyUniquidNodeEventListener;
import com.uniquid.params.UniquidRegTest;
import com.uniquid.register.RegisterFactory;
import com.uniquid.register.impl.sql.SQLiteRegisterFactory;
import com.uniquid.register.provider.ProviderChannel;
import com.uniquid.register.user.UserChannel;
import com.uniquid.tank.entity.Tank;
import com.uniquid.tank.function.InputFaucetFunction;
import com.uniquid.tank.function.OutputFaucetFunction;
import com.uniquid.tank.function.TankFunction;
import com.uniquid.userclient.impl.MQTTUserClient;
import com.uniquid.utils.BackupData;
import com.uniquid.utils.SeedUtils;
import com.uniquid.utils.StringUtils;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Utils;
import org.bitcoinj.wallet.DeterministicSeed;
import org.gmagnotta.log.LogEventWriter;
import org.gmagnotta.log.LogLevel;
import org.gmagnotta.log.impl.filesystem.FileSystemLogEventWriter;
import org.gmagnotta.log.impl.filesystem.FileSystemLogStore;
import org.gmagnotta.log.impl.system.MarkerAwareConsoleLogEventWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/*
 * Example to show how to build a Tank simulator with Uniquid Node capabilities
 */
public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class.getName());
	private static final String CONSOLE = "CONSOLE";
	
	private static final Marker MARKER = MarkerFactory.getMarker(CONSOLE);
	
	private static final String APPCONFIG_PROPERTIES = "/appconfig.properties";
	
	private static final String DEBUG = "DEBUG";
	
	public static void main(String[] args) throws Exception {
		
		org.gmagnotta.log.LogEventCollector.getInstance().setLogLevelThreshold(LogLevel.INFO);
		
		String debug = System.getProperty(DEBUG);
		
		if (debug != null) {
			org.gmagnotta.log.LogEventCollector.getInstance().setLogLevelThreshold(LogLevel.TRACE);
		}

		LOGGER.info(MARKER, "Starting tank...");

		FileSystemLogStore fileSystemLogStore = new FileSystemLogStore(1 * 1024 * 1024, 3, new File("."));
		
		FileSystemLogEventWriter fs = new FileSystemLogEventWriter(fileSystemLogStore);
		
		org.gmagnotta.log.LogEventCollector.getInstance().addLogEventWriter(fs);
		
		MarkerAwareConsoleLogEventWriter markerAwareConsoleLogEventWriter = new MarkerAwareConsoleLogEventWriter(CONSOLE);
		
		org.gmagnotta.log.LogEventCollector.getInstance().addLogEventWriter(markerAwareConsoleLogEventWriter);
		
		// Read configuration properties
		InputStream inputStream;
		
		if (args.length != 0) {
			
			// the first parameter is the properties file that contains application's configuration
			inputStream = new FileInputStream(new File(args[0]));
			
		} else {
			
			// if the user did not pass properties file, then we use our default one (inside the jar)
			inputStream = Main.class.getResourceAsStream(APPCONFIG_PROPERTIES);
			
		}
		
		Properties properties = new Properties();
		properties.load(inputStream);
		
		// close input stream
		inputStream.close();

		// Init settings
		final TankSettings appSettings = new TankSettings(properties);

		// Read network parameters
		NetworkParameters networkParameters = appSettings.getNetworkParameters();

		// Read provider wallet
		File providerWalletFile = appSettings.getProviderWalletFile();

		// Read wallet file
		File userWalletFile = appSettings.getUserWalletFile();
		
		// Read chain file
		File chainFile = appSettings.getChainFile();
		
		// Read chain file
		File userChainFile = appSettings.getUserChainFile();
		
		// Machine name
		String machineName = "JTank" + StringUtils.getRandomName(12);
		
		// Seed backup file
		File seedFile = appSettings.getSeedFile();
		
		// Retrieve list of blockchain peers
		String bcpeers = appSettings.getBlockChainPeers();
		
		// Tell library to use those peers
		UniquidRegTest.get().overridePeers(bcpeers);
		
		//
		// 1 Create Register Factory: we choose the SQLiteRegisterFactory implementation.
		//
		RegisterFactory registerFactory = new SQLiteRegisterFactory(appSettings.getDBUrl());
		
		//
		// 2 start to construct an UniquidNode...
		//
		final UniquidNodeImpl uniquidNode;
		
		// ... if the seed file exists then we use the SeedUtils to open it and decrypt its content: we can extract the
		// mnemonic string, creationtime and name to restore the node; otherwise we create a new node initialized with a
		// random seed and then we use a SeedUtils to perform an encrypted backup of the seed and other properties
		if (seedFile.exists() && !seedFile.isDirectory()) {
			
			// create a SeedUtils (the wrapper that is able to load/read/decrypt the seed file)
			SeedUtils<BackupData> seedUtils = new SeedUtils<BackupData>(seedFile);
			
			// decrypt the content with the password read from the application setting properties
			BackupData readData = new BackupData(); 

			seedUtils.readData(appSettings.getSeedPassword(), readData);

			// fetch mnemonic string
			final String mnemonic = readData.getMnemonic();

			// fetch creation time
			final long creationTime = readData.getCreationTime();
			
			machineName = readData.getName();
			
			// now we build an UniquidNode with the data read from seed file: we choose the UniquidNodeImpl
			// implementation
			@SuppressWarnings("rawtypes")
			UniquidNodeImpl.UniquidNodeBuilder builder = new UniquidNodeImpl.UniquidNodeBuilder();
			
			builder.setNetworkParameters(networkParameters).
					setProviderFile(providerWalletFile).
					setUserFile(userWalletFile).
					setProviderChainFile(chainFile).
					setUserChainFile(userChainFile).
					setRegisterFactory(registerFactory).
					setNodeName(machineName);
			
			uniquidNode = builder.buildFromMnemonic(mnemonic, creationTime);
			
		} else {
		
			// We create a builder with specified settings
			@SuppressWarnings("rawtypes")
			UniquidNodeImpl.UniquidNodeBuilder builder = new UniquidNodeImpl.UniquidNodeBuilder();
					builder.setNetworkParameters(networkParameters)
					.setProviderFile(providerWalletFile)
					.setUserFile(userWalletFile)
					.setProviderChainFile(chainFile)
					.setUserChainFile(userChainFile)
					.setRegisterFactory(registerFactory)
					.setNodeName(machineName);
			
			// ask the builder to create a node with a random seed
			uniquidNode = builder.build();
			
			// Now we fetch from the builder the DeterministicSeed that allow us to export mnemonics and creationtime
			DeterministicSeed seed = uniquidNode.getDeterministicSeed();
			
			// we save the creation time
			long creationTime = seed.getCreationTimeSeconds();
			
			// we save mnemonics
			String mnemonics = Utils.join(seed.getMnemonicCode());
			
			// we prepare the data to save for seedUtils
			BackupData backupData = new BackupData();
			backupData.setMnemonic(mnemonics);
			backupData.setCreationTime(creationTime);
			backupData.setName(machineName);
			
			// we construct a seedutils
			SeedUtils<BackupData> seedUtils = new SeedUtils<BackupData>(seedFile);
			
			// now backup mnemonics encrypted on disk
			seedUtils.saveData(backupData, appSettings.getSeedPassword());
		
		}
		
		final String senderTopic = machineName;

		//
		// 2 ...we finished to build an UniquidNode
		// 
		
		// Here we register a callback on the uniquidNode that allow us to be triggered when some interesting events happens
		// Currently we are only interested in receiving the onNodeStateChange() event. The other methods are present
		// because we decided to use an anonymous inner class.
		uniquidNode.addUniquidNodeEventListener(new EmptyUniquidNodeEventListener() {
			
			@Override
			public void onNodeStateChange(UniquidNodeState arg0) {

				// Register an handler that allow to send an imprinting message to the imprinter
				try {
					
					LOGGER.info(MARKER, "Tank new state: " + arg0);

					// If the node is ready to be imprinted...
					if (UniquidNodeState.IMPRINTING.equals(arg0)) {

						// Create a MQTTClient pointing to the broker on the UID/announce topic and specify
						// 0 timeout: we don't want a response.
						final MQTTUserClient userClient = new MQTTUserClient(appSettings.getMQTTBroker(), appSettings.getAnnounceTopic(), 0, senderTopic);
						
						AnnounceMessage announceMessage = new AnnounceMessage();
						announceMessage.setName(uniquidNode.getNodeName());
						announceMessage.setPubKey(uniquidNode.getPublicKey());
						
						LOGGER.info(MARKER, "Announcing tank on MQTT to imprinter");
						
						// send the request.  The server will not reply (but will do an imprint on blockchain)
						userClient.send(announceMessage);
						
					}

				} catch (Exception ex) {
					// expected! the server will not reply
				}
			}
			
			@Override
			public void onProviderContractCreated(ProviderChannel providerChannel) {
				LOGGER.info(MARKER, "Created Provider Contract: " + providerChannel);
			}

			@Override
			public void onProviderContractRevoked(ProviderChannel providerChannel) {
				LOGGER.info(MARKER, "Revoked Provider Contract: " + providerChannel);
			}

			@Override
			public void onUserContractCreated(UserChannel userChannel) {
				LOGGER.info(MARKER, "Created User Contract: " + userChannel);
			}

			@Override
			public void onUserContractRevoked(UserChannel userChannel) {
				LOGGER.info(MARKER, "Revoked User Contract: " + userChannel);
			}

		});
		
		// 
		// 3 Create UniquidSimplifier that wraps registerFactory, connector and uniquidnode
		final UniquidSimplifier simplifier = new UniquidSimplifier(registerFactory, uniquidNode);
		
		// 4 Register custom functions on slot 34, 35, 36
		simplifier.addFunction(new TankFunction(), 34);
		simplifier.addFunction(new InputFaucetFunction(), 35);
		simplifier.addFunction(new OutputFaucetFunction(), 36);
		
		LOGGER.info(MARKER, "Starting Uniquid library with node: " + machineName);
		
		// Set static values for Tank singleton
		Tank.mqttbroker = appSettings.getMQTTBroker();
		Tank.tankname = machineName;
		
		//
		// 5 start Uniquid core library: this will init the node, sync on blockchain, and use the provided
		// registerFactory to interact with the persistence layer
		simplifier.syncBlockchain();

		//
		// 6 Create connector: we choose the MQTTConnector implementation
		//
		final Connector mqttProviderConnector = new MQTTConnector.Builder()
				.set_broker(appSettings.getMQTTBroker())
				.set_topic(machineName)
				.build();
		simplifier.start(mqttProviderConnector);
		
		// Register shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			
			public void run() {

				LOGGER.info(MARKER, "Terminating tank");
				try {

					// tell the library to shutdown and close all opened resources
					simplifier.shutdown();

					// explicitly stop logging
					for (LogEventWriter writer : org.gmagnotta.log.LogEventCollector.getInstance().getLogEventWriters()) {

						writer.stop();

					}

					org.gmagnotta.log.LogEventCollector.getInstance().stop();

				} catch (Exception ex) {

					LOGGER.error("Exception while terminating tank", ex);

				}
			}
		});
		
		LOGGER.info(MARKER, "Tank ready");
		
	}
	
}
