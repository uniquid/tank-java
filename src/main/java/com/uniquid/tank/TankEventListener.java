package com.uniquid.tank;

import java.util.Date;
import java.util.Set;

import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;

import com.uniquid.node.UniquidNodeState;
import com.uniquid.node.listeners.UniquidNodeEventListener;
import com.uniquid.register.provider.ProviderChannel;
import com.uniquid.register.user.UserChannel;

public class TankEventListener implements UniquidNodeEventListener {

	@Override
	public void onNodeStateChange(UniquidNodeState arg0) {
		
		
		
	}

	@Override
	public void onPeerConnected(Peer arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPeerDisconnected(Peer arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPeersDiscovered(Set<PeerAddress> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderContractCreated(ProviderChannel arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderContractRevoked(ProviderChannel arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSyncEnded() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSyncNodeEnd() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSyncNodeStart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSyncProgress(double arg0, int arg1, Date arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSyncStarted(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUserContractCreated(UserChannel arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUserContractRevoked(UserChannel arg0) {
		// TODO Auto-generated method stub
		
	}

}
