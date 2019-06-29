package ru.spbhse.brainring.network.callbacks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateCallback;

import java.util.List;

import ru.spbhse.brainring.network.LocalNetwork;
import ru.spbhse.brainring.utils.Constants;

/** Reacts on room status update. Used in {@code LocalNetwork} */
public class LocalRoomStatusUpdateCallback extends RoomStatusUpdateCallback {
    private LocalNetwork network;

    public LocalRoomStatusUpdateCallback(LocalNetwork network) {
        this.network = network;
    }

    @Override
    public void onRoomConnecting(@Nullable Room room) {
        Log.d(Constants.APP_TAG, "onRoomConnecting");
        network.setRoom(room);
    }

    @Override
    public void onRoomAutoMatching(@Nullable Room room) {
        Log.d(Constants.APP_TAG, "onRoomAutoMatching");
        network.setRoom(room);
    }

    @Override
    public void onPeerInvitedToRoom(@Nullable Room room, @NonNull List<String> list) {
        Log.d(Constants.APP_TAG, "onPeerInvitedToRoom");
        network.setRoom(room);
    }

    @Override
    public void onPeerDeclined(@Nullable Room room, @NonNull List<String> list) {
        Log.d(Constants.APP_TAG, "onPeerDeclined");
        network.setRoom(room);
    }

    @Override
    public void onPeerJoined(@Nullable Room room, @NonNull List<String> list) {
        Log.d(Constants.APP_TAG, "onPeerJoined");
        network.setRoom(room);
    }

    @Override
    public void onPeerLeft(@Nullable Room room, @NonNull List<String> list) {
        Log.d(Constants.APP_TAG, "onPeerLeft");
        network.setRoom(room);
        if (!network.gameIsFinished()) {
            network.getManager().finishGame();
            network.getManager().getActivity().finish();
        }
    }

    @Override
    public void onConnectedToRoom(@Nullable Room room) {
        Log.d(Constants.APP_TAG, "onConnectedToRoom");
        network.setRoom(room);
    }

    @Override
    public void onDisconnectedFromRoom(@Nullable Room room) {
        Log.d(Constants.APP_TAG, "onDisconnectedFromRoom");
        network.setRoom(room);
        if (!network.gameIsFinished()) {
            network.getManager().finishGame();
            network.getManager().getActivity().finish();
        }
    }

    @Override
    public void onPeersConnected(@Nullable Room room, @NonNull List<String> list) {
        Log.d(Constants.APP_TAG, "onPeersConnected");
        network.setRoom(room);
    }

    @Override
    public void onPeersDisconnected(@Nullable Room room, @NonNull List<String> list) {
        Log.d(Constants.APP_TAG, "onPeersDisconnected");
        network.setRoom(room);
        if (!network.gameIsFinished()) {
            network.getManager().finishGame();
            network.getManager().getActivity().finish();
        }
    }

    /** If I am server and both players are p2p connected starts handshake process */
    @Override
    public void onP2PConnected(@NonNull String s) {
        Log.d(Constants.APP_TAG, "onP2PConnected " + s);
        network.plusP2PConnected();
        if (network.isServerRoomConnected() && network.getP2PConnected() == 2) {
            network.handshake();
        }
    }

    @Override
    public void onP2PDisconnected(@NonNull String s) {
        Log.d(Constants.APP_TAG, "onP2PDisconnected");
        network.minusP2PConnected();
        if (!network.gameIsFinished()) {
            network.getManager().finishGame();
            network.getManager().getActivity().finish();
        }
    }
}
