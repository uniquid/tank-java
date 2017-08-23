package com.uniquid.tank;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import java.util.Set;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.Utils;
import org.bitcoinj.wallet.DeterministicSeed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.uniquid.core.ProviderRequest;
import com.uniquid.core.connector.Connector;
import com.uniquid.core.connector.UserClient;
import com.uniquid.core.connector.mqtt.AnnouncerProviderRequest;
import com.uniquid.core.connector.mqtt.MQTTConnector;
import com.uniquid.core.connector.mqtt.MQTTUserClient;
import com.uniquid.core.impl.UniquidSimplifier;
import com.uniquid.node.UniquidNodeState;
import com.uniquid.node.impl.UniquidNodeImpl;
import com.uniquid.node.impl.UniquidNodeImpl.Builder;
import com.uniquid.node.listeners.UniquidNodeEventListener;
import com.uniquid.register.impl.sql.SQLiteRegisterFactory;
import com.uniquid.register.provider.ProviderChannel;
import com.uniquid.register.user.UserChannel;
import com.uniquid.tank.function.InputFaucetFunction;
import com.uniquid.tank.function.OutputFaucetFunction;
import com.uniquid.tank.function.TankFunction;
import com.uniquid.utils.SeedUtils;

/*
 * Example to show how to build a Tank simulator with Uniquid Node capabilities
 */
public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class.getName());

	private static final String APPCONFIG_PROPERTIES = "/appconfig.properties";

	public static void main(String[] args) throws Exception {
		
		// Read configuration properties
		InputStream inputStream = null;
		
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
		String machineName = appSettings.getMQTTTopic();
		
		// Seed backup file
		File seedFile = appSettings.getSeedFile();
		
		// Create Register Factory
		SQLiteRegisterFactory registerFactory = new SQLiteRegisterFactory(appSettings.getDBUrl());
		
		// Now start to construct an UniquidNode...
		final UniquidNodeImpl uniquidNode;
		
		// ... if the seed file exists, then we read it and decrypt the seed and then we create the node with
		// the mnemonics inside it
		// otherwise we create a new node (build with random seed) and backup the seed file
		if (seedFile.exists() && !seedFile.isDirectory()) {
			
			// create a seed utils
			SeedUtils seedUtils = new SeedUtils(seedFile);
			
			// decrypt and read data
			Object[] readData = seedUtils.readData(appSettings.getSeedPassword());

			// fetch mnemonics
			final String mnemonic = (String) readData[0];

			// and creation time
			final int creationTime = (Integer) readData[1];
			
			// now build an UniquidNode with the read mnemonics and timestamp
			uniquidNode = new UniquidNodeImpl.Builder().
					setNetworkParameters(networkParameters).
					setProviderFile(providerWalletFile).
					setUserFile(userWalletFile).
					setChainFile(chainFile).
					setUserChainFile(userChainFile).
					setRegisterFactory(registerFactory).
					setNodeName(machineName).
					buildFromMnemonic(mnemonic, creationTime);
			
		} else {
		
			// prepare the builder
			Builder builder = new UniquidNodeImpl.Builder().
					setNetworkParameters(networkParameters).
					setProviderFile(providerWalletFile).
					setUserFile(userWalletFile).
					setChainFile(chainFile).
					setUserChainFile(userChainFile).
					setRegisterFactory(registerFactory).
					setNodeName(machineName);
			
			// build a node with random seed
			uniquidNode = builder.build();
			
			// Fetch mnemonics and creation time
			DeterministicSeed seed = builder.getDeterministicSeed();
			
			long creationTime = seed.getCreationTimeSeconds();
			
			String mnemonics = Utils.join(seed.getMnemonicCode());
			
			Object[] saveData = new Object[2];
			
			saveData[0] = mnemonics;
			saveData[1] = creationTime;
			
			// construct a seedutils
			SeedUtils seedUtils = new SeedUtils(seedFile);
			
			// now backup mnemonics encrypted on disk
			seedUtils.saveData(saveData, appSettings.getSeedPassword());
		
		}
		
		// Create connector
		final Connector mqttProviderConnector = new MQTTConnector.Builder()
				.set_broker(appSettings.getMQTTBroker())
				.set_topic(appSettings.getMQTTTopic())
				.build();
		
		// Register inside the node a callback that allow us to be triggered when some events happens
		uniquidNode.addUniquidNodeEventListener(new UniquidNodeEventListener() {
			
			@Override
			public void onUserContractRevoked(UserChannel arg0) {
				// NOTHING TO DO
			}
			
			@Override
			public void onUserContractCreated(UserChannel arg0) {
				// NOTHING TO DO				
			}
			
			@Override
			public void onSyncStarted(int arg0) {
				// NOTHING TO DO				
			}
			
			@Override
			public void onSyncProgress(double arg0, int arg1, Date arg2) {
				// NOTHING TO DO				
			}
			
			@Override
			public void onSyncNodeStart() {
				// NOTHING TO DO				
			}
			
			@Override
			public void onSyncNodeEnd() {
				// NOTHING TO DO				
			}
			
			@Override
			public void onSyncEnded() {
				// NOTHING TO DO				
			}
			
			@Override
			public void onProviderContractRevoked(ProviderChannel arg0) {
				// NOTHING TO DO				
			}
			
			@Override
			public void onProviderContractCreated(ProviderChannel arg0) {
				// NOTHING TO DO				
			}
			
			@Override
			public void onPeersDiscovered(Set<PeerAddress> arg0) {
				// NOTHING TO DO				
			}
			
			@Override
			public void onPeerDisconnected(Peer arg0, int arg1) {
				// NOTHING TO DO				
			}
			
			@Override
			public void onPeerConnected(Peer arg0, int arg1) {
				// NOTHING TO DO				
			}
			
			@Override
			public void onNodeStateChange(UniquidNodeState arg0) {

				// Register an handler that allow to send an imprinting message to the imprinter
				try {

					// If the node is ready to be imprinted...
					if (UniquidNodeState.IMPRINTING.equals(arg0)) {

						// Create a MQTTClient pointing to the broker on the UID/announce topic
						final UserClient rpcClient = new MQTTUserClient(appSettings.getMQTTBroker(), "UID/announce", 0);
						
						// Create announce request
						final ProviderRequest providerRequest = new AnnouncerProviderRequest.Builder()
								.set_sender(uniquidNode.getNodeName())
								.set_name(uniquidNode.getNodeName())
								.set_xpub(uniquidNode.getPublicKey())
								.build();
						
						// send the request.  The server will not reply (but will do an imprint on blockchain) so
						// the timeout exception here is expected
						rpcClient.sendOutputMessage(providerRequest);
						
					}

				} catch (Exception ex) {
					// expected! the server will not reply
				}
			}

		});
		
		// Create UID Core library
		final UniquidSimplifier simplifier = new UniquidSimplifier(registerFactory, mqttProviderConnector, uniquidNode);
		
		// Register custom function on slot 34, 35, 36
		simplifier.addFunction(new TankFunction(), 34);
		simplifier.addFunction(new InputFaucetFunction(), 35);
		simplifier.addFunction(new OutputFaucetFunction(), 36);
		
		// start Uniquid core library: this will init the node, sync on blockchain, etc.
		simplifier.start();
		
		// Register shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			
			public void run() {

				LOGGER.info("Terminating tank");
				try {

					// tell the library to shutdown
					simplifier.shutdown();

				} catch (Exception ex) {

					LOGGER.error("Exception while terminating tank", ex);

				}
			}
		});
		
		LOGGER.info("Exiting");
		
	}

}
