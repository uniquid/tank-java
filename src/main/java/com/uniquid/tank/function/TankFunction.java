/*
 * Copyright (c) 2016-2018. Uniquid Inc. or its affiliates. All Rights Reserved.
 *
 * License is in the "LICENSE" file accompanying this file.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.uniquid.tank.function;

import com.uniquid.core.provider.exception.FunctionException;
import com.uniquid.core.provider.impl.GenericFunction;
import com.uniquid.messages.FunctionRequestMessage;
import com.uniquid.messages.FunctionResponseMessage;
import com.uniquid.tank.entity.Tank;

import java.io.IOException;

public class TankFunction extends GenericFunction {

	@Override
	public void service(FunctionRequestMessage inputMessage, FunctionResponseMessage outputMessage, byte[] payload)
			throws FunctionException, IOException {
		
		Tank tank = Tank.getInstance();
		
		String params = inputMessage.getParameters();
		String result = "";
		if (params.startsWith("open")) {
			
			tank.open();
			
			result = "\nOpening Machine\n-- Level " + tank.getLevel() + " in faucet = " + booleanToInt(tank.isInputOpen()) + " out faucet = " +  booleanToInt(tank.isOutputOpen()) + "\n";
			
		} else if (params.startsWith("close")) {
			
			tank.close();
			
			result = "\nClosing Machine\n-- Level " + tank.getLevel() + " in faucet = " + booleanToInt(tank.isInputOpen()) + " out faucet = " +  booleanToInt(tank.isOutputOpen()) + "\n";
			
		}
		
		outputMessage.setResult(result);
		
	}
	
	private static int booleanToInt(final boolean open) {
		if (open) {
			return 1;
		} else {
			return 0;
		}
		
	}

}