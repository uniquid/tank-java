/*
 * Copyright (c) 2016-2018. Uniquid Inc. or its affiliates. All Rights Reserved.
 *
 * License is in the "LICENSE" file accompanying this file.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.uniquid.tank;

import com.uniquid.params.UniquidLitecoinRegTest;
import com.uniquid.params.UniquidLitecoinTest;
import com.uniquid.params.UniquidRegTest;
import com.uniquid.settings.exception.SettingValidationException;
import com.uniquid.settings.exception.UnknownSettingException;
import com.uniquid.settings.model.AbstractSettings;
import com.uniquid.settings.model.Setting;
import com.uniquid.settings.validator.NotEmpty;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;

import java.io.File;
import java.util.Properties;
import java.util.Set;

public class TankSettings extends AbstractSettings {

	private static final long serialVersionUID = 1L;

	public static final Setting NETWORK_PARAMETERS = new Setting(
			"networkParameters",
			"Blockchain configuration",
			"Please set description",
			UniquidRegTest.class.getCanonicalName(), new NotEmpty());
	
	public static final Setting PROVIDERWALLET_FILE = new Setting(
			"providerWalletFile",
			"Provider Wallet File",
			"Description", 
			"provider.wallet", new NotEmpty());
	
	public static final Setting USERWALLET_FILE = new Setting(
			"userWalletFile",
			"User Wallet File",
			"Description", 
			"user.wallet", new NotEmpty());
	
	public static final Setting CHAIN_FILE = new Setting(
			"chainFile",
			"User Wallet File",
			"Description", 
			"chain.spvchain", new NotEmpty());
	
	public static final Setting USER_CHAIN_FILE = new Setting(
			"userChainFile",
			"User Wallet File",
			"Description", 
			"userchain.spvchain", new NotEmpty());
	
	public static final Setting MQTT_BROKER = new Setting(
			"mqtt.broker",
			"User Wallet File",
			"Description", 
			"tcp://appliance4.uniquid.co:1883", new NotEmpty());
	
	public static final Setting DB_URL = new Setting(
			"db.url",
			"User Wallet File",
			"Description", 
			"jdbc:sqlite:tank.db", new NotEmpty());
	
	public static final Setting SEED_FILE = new Setting(
			"seedFile",
			"seed file",
			"Seed file",
			"seed.backup", new NotEmpty());
	
	public static final Setting SEED_PASSWORD = new Setting(
			"seedPassword",
			"seed password",
			"Seed password",
			"defaultpassword", new NotEmpty());
	
	public static final Setting ORG_ID = new Setting(
			"orgId",
			"Organization",
			"Organization id that use to coordinate messages in MQTT",
			"organization", new NotEmpty());
	
	public static final Setting BC_PEERS = new Setting(
			"bc.peers",
			"Blockchain peers",
			"Blockchain peers",
			"127.0.0.1", new NotEmpty());

	public static final Setting REGISTRY_URL = new Setting(
			"registry.url",
			"Registry url",
			"Registry url",
			"http://localhost:8060");
	
	public TankSettings() throws SettingValidationException, UnknownSettingException {
		super();
	}
	
	public TankSettings(Properties properties) throws SettingValidationException, UnknownSettingException {
		super(properties);
	}
	
	public TankSettings(Properties properties, Set<Setting> excludedSettings) throws SettingValidationException, UnknownSettingException {
		super(properties, excludedSettings);
	}
	
	public NetworkParameters getNetworkParameters() {
		
		// Read network type from config
        String networkParametersProperties = getAsString(NETWORK_PARAMETERS);
        
        // Select proper NetworkParameters
        NetworkParameters networkParameters = null;
        
        if (MainNetParams.get().getId().equals(networkParametersProperties)) {
        		networkParameters  = MainNetParams.get();
        } else if (TestNet3Params.get().getId().equals(networkParametersProperties)) {
        		networkParameters = TestNet3Params.get();
        } else if (RegTestParams.get().getId().equals(networkParametersProperties)) {
			networkParameters = RegTestParams.get();
		} else if (UniquidRegTest.get().getId().equals(networkParametersProperties)) {
			networkParameters = UniquidRegTest.get();
		} else if (UniquidLitecoinTest.get().getId().equals(networkParametersProperties)) {
        	networkParameters = UniquidLitecoinTest.get();
		} else if (UniquidLitecoinRegTest.get().getId().equals(networkParametersProperties)) {
        	networkParameters = UniquidLitecoinRegTest.get();
		}

        return networkParameters;
	}
	
	public File getUserWalletFile() {
		return new File(getAsString(USERWALLET_FILE));
	}
	
	public File getProviderWalletFile() {
		return new File(getAsString(PROVIDERWALLET_FILE));
	}
	
	public File getChainFile() {
		return new File(getAsString(CHAIN_FILE));
	}
	
	public File getUserChainFile() {
		return new File(getAsString(USER_CHAIN_FILE));
	}
	
	public String getMQTTBroker() {
		return getAsString(MQTT_BROKER);
	}
	
	public String getDBUrl() {
		return getAsString(DB_URL);
	}
	
	public File getSeedFile() {
		return new File(getAsString(SEED_FILE));
	}
	
	public String getSeedPassword() {
		return getAsString(SEED_PASSWORD);
	}
	
	public String getOrgId() {
		return getAsString(ORG_ID);
	}

	public String getAnnounceTopic() {
		return getAsString(ORG_ID) + "/announce";
	}
	
	public String getBlockChainPeers() {
		return getAsString(BC_PEERS);
	}

	public String getRegistryUrl() {
		return getAsString(REGISTRY_URL);
	}
}
