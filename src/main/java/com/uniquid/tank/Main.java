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
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.uniquid.core.ProviderRequest;
import com.uniquid.core.ProviderResponse;
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

public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class.getName());

	// Resource path pointing to where the WEBROOT is
	private static final String APPCONFIG_PROPERTIES = "/appconfig.properties";

	public static void main(String[] args) throws Exception {
		
		// Read configuration properties
		InputStream inputStream = null;
		
		if (args.length != 0) {
			
			inputStream = new FileInputStream(new File(args[0]));
			
		} else {
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
		
		File seedFile = appSettings.getSeedFile();
		
		// Create Register Factory
		SQLiteRegisterFactory registerFactory = new SQLiteRegisterFactory(appSettings.getDBUrl());
		
		final UniquidNodeImpl uniquidNode;
		
		if (seedFile.exists() && !seedFile.isDirectory()) {
			
			// read file
			
			SeedUtils seedUtils = new SeedUtils(seedFile);
			
			Object[] readData = seedUtils.readData(appSettings.getSeedPassword());

			final String mnemonic = (String) readData[0];

			final int creationTime = (Integer) readData[1];
			
			// Create new SpvNode
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
		
			Builder builder = new UniquidNodeImpl.Builder().
					setNetworkParameters(networkParameters).
					setProviderFile(providerWalletFile).
					setUserFile(userWalletFile).
					setChainFile(chainFile).
					setUserChainFile(userChainFile).
					setRegisterFactory(registerFactory).
					setNodeName(machineName);
			
			// Create new node
			uniquidNode = builder.build();
			
			DeterministicSeed seed = builder.getDeterministicSeed();
			
			long creationTime = seed.getCreationTimeSeconds();
			
			String mnemonics = Utils.join(seed.getMnemonicCode());
			
			Object[] saveData = new Object[2];
			
			saveData[0] = mnemonics;
			saveData[1] = creationTime;
			
			SeedUtils seedUtils = new SeedUtils(seedFile);
			
			seedUtils.saveData(saveData, appSettings.getSeedPassword());
		
		}
		
		// Create connector
		final Connector mqttProviderConnector = new MQTTConnector.Builder()
				.set_broker(appSettings.getMQTTBroker())
				.set_topic(appSettings.getMQTTTopic())
				.build();
		
		uniquidNode.addUniquidNodeEventListener(new UniquidNodeEventListener() {
			
			@Override
			public void onUserContractRevoked(UserChannel arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onUserContractCreated(UserChannel arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onSyncStarted(int arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onSyncProgress(double arg0, int arg1, Date arg2) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onSyncNodeStart() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onSyncNodeEnd() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onSyncEnded() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProviderContractRevoked(ProviderChannel arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProviderContractCreated(ProviderChannel arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onPeersDiscovered(Set<PeerAddress> arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onPeerDisconnected(Peer arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onPeerConnected(Peer arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onNodeStateChange(UniquidNodeState arg0) {

				try {

					if (UniquidNodeState.IMPRINTING.equals(arg0)) {

						final UserClient rpcClient = new MQTTUserClient(appSettings.getMQTTBroker(), "UID/announce", 0);
						
						// Create request
						final ProviderRequest providerRequest = new AnnouncerProviderRequest.Builder()
								.set_sender(uniquidNode.getNodeName())
								.set_name(uniquidNode.getNodeName())
								.set_xpub(uniquidNode.getPublicKey())
								.build();
						
						final ProviderResponse userResponse = rpcClient.sendOutputMessage(providerRequest);
						
					}

				} catch (Exception ex) {
					// expected! LOGGER.warn("Exception while sending announce message", ex); 
				}
			}
		});
		
		// Create UID Core library
		final UniquidSimplifier simplifier = new UniquidSimplifier(registerFactory, mqttProviderConnector, uniquidNode);
		
		simplifier.addFunction(new TankFunction(), 34);
		simplifier.addFunction(new InputFaucetFunction(), 35);
		simplifier.addFunction(new OutputFaucetFunction(), 36);
		
		// start Uniquid core library
		simplifier.start();
		
		// Register shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {

				LOGGER.info("Terminating Uniquid imprinter");
				try {

					simplifier.shutdown();

				} catch (Exception ex) {

					LOGGER.error("Exception while terminating HTTP", ex);

				}
			}
		});
		
		// Start web server part

		LOGGER.info("Exiting");
	}

}
