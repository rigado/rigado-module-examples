package com.rigado.rigablue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by stutzenbergere on 11/17/14.
 */

enum FirmwareUpdateTypeEnum {
    UpdateType_C9LightString,
    UpdateType_Foliage
}

enum DfuOpCodeEnum {
    DfuOpCode_Reserved_0,
    DfuOpCode_Start,
    DfuOpCode_Init,
    DfuOpCode_ReceiveFirmwareImage,
    DfuOpCode_ValidateFirmwareImage,
    DfuOpCode_ActivateFirmwareImage,
    DfuOpCode_Reserved_6,
    DfuOpCode_Reserved_7,
    DfuOpCode_Reserved_8,
    DfuOpCode_EraseAndReset,
    DfuOpCode_EraseSizeRequest
}

enum FirmwareManagerStateEnum {
    State_Init,
    State_UserUnpluggedDeviceAwaitingReconnect,
    State_DiscoverFirmwareServiceCharacteristics,
    State_CheckEraseAfterUnplug,
    State_TriggeredErase,
    State_ReconnectAfterInitialFlashErase,
    State_TransferringStmUpdateImage,
    State_ActivatingStmUpdaterImage,
    State_ReconnectAfterStmUpdate,
    State_WaitReconnectAfterStmUpdateAndInvalidate,
    State_ReconnectAfterStmUpdateFlashErase,
    State_TransferringRadioImage,
    State_FinishedRadioImageTransfer
}

public class RigFirmwareUpdateManager implements IRigLeDiscoveryManagerObserver, IRigLeConnectionManagerObserver,
                                                IRigFirmwareUpdateServiceObserver {
    private static final int PacketReceivedNotificationRequest = 8;
    private static final int NumberOfPackets = 1;
    private static final int PacketReceivedNotification = 17;
    private static final int ReceivedOpcode = 16;
    private static final int OperationSuccess = 1;

    private static final int BytesInOnePacket = 20;
    
    private static final int ImageStartIndex = 0;
    private static final int ImageStartPacketSize = 12;
    private static final int ImageInitPacketIndex = ImageStartPacketSize;
    private static final int ImageInitPacketSize = 32;
    private static final int ImageSecureDataStart = ImageInitPacketIndex + ImageInitPacketSize;

    private RigFirmwareUpdateService mFirmwareUpdateService;
    private FirmwareManagerStateEnum mState;

    byte [] mImage;
    int mImageSize;

    boolean mIsFileSizeWritten;
    boolean mIsInitPacketSent;
    boolean mIsPacketNotificationEnabled;
    boolean mIsReceivingFirmwareImage;
    boolean mIsLastPacket;
    boolean mShouldStopSendingPackets;
    boolean mShouldWaitForErasedSize;
    boolean mDidDisconnectToErase;
    boolean mDidActivateFirmware;
    boolean mDidForceEraseAfterStmUpdateImageRan;

    int mTotalPackets;
    int mPacketNumber;
    int mTotalBytesSent;
    int mTotalBytesErased;
    int mLastPacketSize;

    private IRigFirmwareUpdateManagerObserver mObserver;

    IRigLeConnectionManagerObserver mOldConnectionObserver;
    RigLeBaseDevice mUpdateDevice;
    String mInitialDeviceAddress;

    public RigFirmwareUpdateManager() {
    	initStateVariables();
    }

    public boolean updateFirmware(RigLeBaseDevice device, InputStream firmwareImage, BluetoothGattCharacteristic activateCharacteristic,
    		byte[] activateCommand) {
        //BufferedReader radioStreamReader = new BinaryReader(new InputStreamReader(radioImageStream));
        RigLog.i("__RigFirmwareUpdateManager.updateFirmware__");
        mUpdateDevice = device;
        try {
            mImageSize = firmwareImage.available();
            mImage = new byte[mImageSize];
            firmwareImage.read(mImage);
        } catch(IOException e) {
            RigLog.e("IOException occurred while reading binary image data!");
        }

//        mImage = new byte[mControllerImageSize];
//        System.arraycopy(mControllerImage, 0, mImage, 0, mControllerImageSize);

        //TODO: Remove this
//        UUID lumenplayServiceUuid = UUID.fromString(LumenplayServiceUuidString);
//        UUID lumenplayControlPointUuid = UUID.fromString(LumenplayControlPointUuidString);

        mFirmwareUpdateService = new RigFirmwareUpdateService();
        mFirmwareUpdateService.setObserver(this);

        mFirmwareUpdateService.setShouldReconnectState(true);
        mState = FirmwareManagerStateEnum.State_DiscoverFirmwareServiceCharacteristics;

        //TODO: Change this to be based on the available services and not the name
        if(mUpdateDevice.getName().equals("RigDfu")) {
            mFirmwareUpdateService.setDevice(mUpdateDevice);
            if(RigCoreBluetooth.getInstance().getDeviceConnectionState(mUpdateDevice.getBluetoothDevice()) ==
                    BluetoothProfile.STATE_CONNECTED) {
                if(!mUpdateDevice.isDiscoveryComplete()) {
                    mFirmwareUpdateService.triggerServiceDiscovery();
                } else {
                    if(mFirmwareUpdateService.getServiceAndCharacteristics()) {
                        mFirmwareUpdateService.enableControlPointNotifications();
                    } else {
                        RigLog.e("Failed when attempting to find dfu service and characteristics");
                        return false;
                    }
                }
            } else {
                mFirmwareUpdateService.connectDevice();
            }
        } else {
            return sendEnterBootloaderCommand(activateCharacteristic, activateCommand);
        }

        return true;
    }
    
    private void initStateVariables() {
    	mIsFileSizeWritten = false;
    	mIsInitPacketSent = false;
    	mIsPacketNotificationEnabled = false;
    	mIsReceivingFirmwareImage = false;
    	mIsLastPacket = false;
    	mShouldStopSendingPackets = false;
    	mDidForceEraseAfterStmUpdateImageRan = false;
    	
    	mObserver = null;
    	mState = FirmwareManagerStateEnum.State_Init;
    	mImageSize = 0;
    	mImage = null;
    }

    public void setObserver(IRigFirmwareUpdateManagerObserver observer) {
        mObserver = observer;
    }

    private boolean sendEnterBootloaderCommand(BluetoothGattCharacteristic characteristic, byte [] command) {
        RigLog.d("__RigFirmwareUpdateManager.sendEnterBootloaderCommand__");

        RigLeDiscoveryManager dm = RigLeDiscoveryManager.getInstance();
        dm.stopDiscoveringDevices();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String dfuServiceUuidString = mFirmwareUpdateService.getDfuServiceUuidString();
        RigDeviceRequest dr = new RigDeviceRequest(new String [] { dfuServiceUuidString }, 0);
        dr.setObserver(this);
        dm.startDiscoverDevices(dr);
        if(mObserver != null) {
            mObserver.updateStatus("Searching for updater service...", 0);
        }

        mFirmwareUpdateService.setInitialNonBootloaderDevice(mUpdateDevice);
        mInitialDeviceAddress = mUpdateDevice.getBluetoothDevice().getAddress();
        mOldConnectionObserver = RigLeConnectionManager.getInstance().getObserver();
        RigLeConnectionManager.getInstance().setObserver(this);

        RigLog.d("Send enter bootloader command");
        mUpdateDevice.writeCharacteristic(characteristic, command);

        return true;
    }
    
    private void cleanUpAfterFailure() {
    	RigLeConnectionManager.getInstance().setObserver(mOldConnectionObserver);
    	
    	if(mUpdateDevice != null) {
    		if(RigCoreBluetooth.getInstance().getDeviceConnectionState(mUpdateDevice.getBluetoothDevice()) ==
                    BluetoothProfile.STATE_CONNECTED) {
    			RigLeConnectionManager.getInstance().disconnectDevice(mUpdateDevice);
    		}
    	}
    	
    	initStateVariables();
    }
    
    private int getImageSize() {
    	int size = mImageSize;
    	if(mFirmwareUpdateService.isSecureDfu()) {
    		size -= (ImageStartPacketSize + ImageInitPacketSize);
    	}
    	return size;
    }

    private void resetFlags() {
        RigLog.d("__RigFirmwareUpdateManager.resetFlags()__");
        mIsFileSizeWritten = false;
        mIsPacketNotificationEnabled = false;
        mIsReceivingFirmwareImage = false;
        mPacketNumber = 0;
        mTotalBytesSent = 0;
        mIsLastPacket = false;
    }
    
    private void updateDeviceAndTriggerDiscovery() {
    	mFirmwareUpdateService.setShouldReconnectState(true);
    	mFirmwareUpdateService.triggerServiceDiscovery();
    }

    private void writeFileSize() {
        RigLog.d("__RigFirmwareUpdateManager.writeFileSize__");

        if(mFirmwareUpdateService.isSecureDfu()) {
        	byte [] data = new byte[ImageStartPacketSize];
        	System.arraycopy(mImage, 0, data, 0, ImageStartPacketSize);
        	
        	mFirmwareUpdateService.writeDataToPacketCharacteristic(data);
        	mObserver.updateStatus("Writing device update size", 0);
        	
        } else {
	        byte [] data = new byte[4];
	        data[0] = (byte)(mImageSize & 0xFF);
	        data[1] = (byte)((mImageSize >> 8) & 0xFF);
	        data[2] = (byte)((mImageSize >> 16) & 0xFF);
	        data[3] = (byte)((mImageSize >> 24) & 0xFF);
	
	        RigLog.d("File size array: " + Arrays.toString(data));

        	mFirmwareUpdateService.writeDataToPacketCharacteristic(data);
        	mObserver.updateStatus("Writing device update size", 0);
        }
    }

    private void enablePacketNotification() {
        RigLog.d("__enablePacketNotification__");
        byte [] data = { PacketReceivedNotificationRequest, NumberOfPackets, 0 };

        mFirmwareUpdateService.writeDataToControlPoint(data);
        mObserver.updateStatus("Enabling packet notifications", 0);
    }
    
    private void sendInitPacket() {
    	RigLog.d("__sendInitPacket__");
    	byte [] initPacketOne = new byte[ImageInitPacketSize/2];
    	byte [] initPacketTwo = new byte[ImageInitPacketSize/2];
    	
    	System.arraycopy(mImage, ImageInitPacketIndex, initPacketOne, 0, ImageInitPacketSize/2);
    	System.arraycopy(mImage, ImageInitPacketIndex + (ImageInitPacketSize/2), initPacketTwo, 0, ImageInitPacketSize/2);
    	
    	mFirmwareUpdateService.writeDataToPacketCharacteristic(initPacketOne);
    	
    	try {
    		Thread.sleep(100);
    	} catch (Exception e) {
    		
    	}
    	mFirmwareUpdateService.writeDataToPacketCharacteristic(initPacketTwo);
    }

    private void receiveFirmwareImage() {
        RigLog.d("__receiveFirmwareImage__");
        byte [] data = { (byte)DfuOpCodeEnum.DfuOpCode_ReceiveFirmwareImage.ordinal() };

        mFirmwareUpdateService.writeDataToControlPoint(data);
    }

    private void startUploadingFile() {
        RigLog.d("__startUploadingFile__");
        int size = getImageSize();
        mTotalPackets = (size / BytesInOnePacket);
        if((size % BytesInOnePacket) != 0) {
            mTotalPackets++;
        }
        
        determineLastPacketSize();

        mObserver.updateStatus("Transferring New Device Software", 0);
        sendPacket();
    }
    
    private void determineLastPacketSize() {
    	int imageSizeLocal = getImageSize();
    	if((imageSizeLocal % BytesInOnePacket) == 0) {
    		mLastPacketSize = BytesInOnePacket;
    	} else {
    		mLastPacketSize = (imageSizeLocal - ((mTotalPackets - 1) * BytesInOnePacket));
    	}
    	RigLog.d("Last packet size: " + mLastPacketSize);
    }

    private void sendPacket() {
        RigLog.d("__RigFirmwareUpdateManager.sendPacket__");

        mPacketNumber++;
        byte [] packet;
        int packetSize = BytesInOnePacket;
        
        if(mPacketNumber == mTotalPackets) {
            RigLog.i("Sending last packet: " + mPacketNumber);
            mIsLastPacket = true;
//            packet = new byte[mLastPacketSize];
            packetSize = mLastPacketSize;
//            System.arraycopy(mImage, (mPacketNumber - 1) * BytesInOnePacket, packet, 0, mLastPacketSize);
        } else {
            RigLog.i("Sending packet: " + mPacketNumber + "/" + mTotalPackets);
//            packet = new byte[BytesInOnePacket];
//            System.arraycopy(mImage, (mPacketNumber - 1) * BytesInOnePacket, packet, 0, BytesInOnePacket);
        }
        
        packet = new byte[packetSize];
        int index;
        if(mFirmwareUpdateService.isSecureDfu()) {
        	index = ImageSecureDataStart + (mPacketNumber - 1) * BytesInOnePacket;
        } else {
        	index = (mPacketNumber - 1) * BytesInOnePacket;
        }
        
        System.arraycopy(mImage, index, packet, 0, packetSize);
        mFirmwareUpdateService.writeDataToPacketCharacteristic(packet);
    }

    private void validateFirmware() {
        RigLog.d("__RigFirmwareUpdateManager.validateFirmware__");
        byte [] cmd = { (byte)DfuOpCodeEnum.DfuOpCode_ValidateFirmwareImage.ordinal() };

        mFirmwareUpdateService.writeDataToControlPoint(cmd);
        mObserver.updateStatus("Validating updated device software", 0);
    }

    private void activateFirmware() {
        RigLog.d("__RigFirmwareUpdateManager.activateFirmware__");
        byte [] cmd = { (byte)DfuOpCodeEnum.DfuOpCode_ActivateFirmwareImage.ordinal() };
        if(mState == FirmwareManagerStateEnum.State_ActivatingStmUpdaterImage) {
            mDidActivateFirmware = true;
        }

        /* Set these fields here as the disconnect may be reported during the onWriteValue callback
         * and then a reconnect will be attempted. */
        mFirmwareUpdateService.setShouldReconnectState(false);
        mFirmwareUpdateService.setShouldAlwaysReconnectState(false);
        mFirmwareUpdateService.completeUpdate();
        
        mFirmwareUpdateService.writeDataToControlPoint(cmd);
        mObserver.updateStatus("Activating device software", 0);
    }

//    private void scheduleReconnect() {
//        RigLog.d("__RigFirmwareUpdateManager.scheduleReconnect__");
//        Handler mHandler = new Handler(Looper.getMainLooper());
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                    activateStmUpdateImageTimerFired();
//                }
//        }, 4000);
//    }
    
    @Override
    public void didConnectPeripheral() {
    	mFirmwareUpdateService.triggerServiceDiscovery();
    }
    
	@Override
    public void didDiscoverCharacteristicsForDFUService() {
        RigLog.d("__RigFirmwareUpdateManager.didDiscoverCharacteristicsForDFUService__");

        if(mState == FirmwareManagerStateEnum.State_TransferringRadioImage) {
        	mState = FirmwareManagerStateEnum.State_Init;
        	mIsFileSizeWritten = false;
        	mIsInitPacketSent = false;
        	mIsPacketNotificationEnabled = false;
        	mIsReceivingFirmwareImage = false;
        	mObserver.updateProgress(0);
        	mPacketNumber = 0;
        }
        
        mFirmwareUpdateService.enableControlPointNotifications();
    }

    @Override
    public void didEnableControlPointNotifications() {
        RigLog.d("__RigFirmwareUpdateManager.didEnableControlPointNotifications__");

        byte [] cmd = { (byte)DfuOpCodeEnum.DfuOpCode_Start.ordinal() };

        mShouldWaitForErasedSize = false;
        mState = FirmwareManagerStateEnum.State_TransferringRadioImage;
        mFirmwareUpdateService.writeDataToControlPoint(cmd);
        mObserver.updateStatus("Initializing Device Firmware Update", 0);
    }

    @Override
    public void didWriteValueForControlPoint() {
        RigLog.d("__RigFirmwareUpdateManager.didWriteValueForControlPoint__");

        if(mShouldWaitForErasedSize) {
            return;
        }

        if(mState == FirmwareManagerStateEnum.State_FinishedRadioImageTransfer) {
        	try {
        		Thread.sleep(2000);
        	} catch(Exception e) {
        		
        	}
            mObserver.didFinishUpdate();
        }

        if(!mIsFileSizeWritten) {
            writeFileSize();
        } else if(!mIsInitPacketSent && mFirmwareUpdateService.isSecureDfu()) {
        	sendInitPacket();
        } else if(!mIsPacketNotificationEnabled) {
            mIsPacketNotificationEnabled = true;
            receiveFirmwareImage();
        } else if(!mIsReceivingFirmwareImage) {
            mIsReceivingFirmwareImage = true;
            startUploadingFile();
        }
    }

    @Override
    public void didDisconnectPeripheral() {
        RigLog.d("__RigFirmwareUpdateManager.didDisconnectDevice__");

//        if(mState == FirmwareManagerStateEnum.State_TransferringStmUpdateImage) {
//            mState = FirmwareManagerStateEnum.State_DiscoverFirmwareServiceCharacteristics;
//            resetFlags();
//        } else if(mState == FirmwareManagerStateEnum.State_TransferringRadioImage) {
//            mState = FirmwareManagerStateEnum.State_ReconnectAfterStmUpdate;
//            resetFlags();
//        }
    }

    @Override
    public void didUpdateValueForControlPoint(byte [] value) {
        RigLog.d("__RigFirmwareUpdateManager.didUpdateValueForControlPoint__");

        byte opCode = value[0];
        byte request = value[1];

        if(opCode == ReceivedOpcode && request == (byte)DfuOpCodeEnum.DfuOpCode_Start.ordinal()) {
            if(value[2] == OperationSuccess) {
                RigLog.i("Received notification for DFU_START");
                mIsFileSizeWritten = true;
                if(mFirmwareUpdateService.isSecureDfu()) {
                	RigLog.d("Start Init packet sequence");
                	byte [] cmd = { (byte)DfuOpCodeEnum.DfuOpCode_Init.ordinal() };
                	mFirmwareUpdateService.writeDataToControlPoint(cmd);
                } else {
                	enablePacketNotification();
                }
            }
        } else if(opCode == ReceivedOpcode && request == (byte)DfuOpCodeEnum.DfuOpCode_Init.ordinal()) {
        	if(value[2] == OperationSuccess) {
        		mIsInitPacketSent = true;
        		enablePacketNotification();
        	}
        } else if(opCode == PacketReceivedNotification) {
            mTotalBytesSent = ((value[1] & 0xFF) + ((value[2] & 0xFF) << 8) + ((value[3] & 0xFF) << 16) + ((value[4] & 0xFF) << 24));

            float progress = (float)mTotalBytesSent / (float)mImageSize;
            int progressPercentage = (int)(progress * 100);
            mObserver.updateProgress(progressPercentage);
            RigLog.i("Transferred " + mTotalBytesSent + "/" + mImageSize);

            if(!mIsLastPacket && !mShouldStopSendingPackets) {
                if((mTotalBytesSent / 20) != mPacketNumber) {
                    RigLog.e("Data consistency error!!!!");
                }
                sendPacket();
            } else {
                RigLog.i("Last packet notification received");
                mObserver.updateProgress(100);
            }
        } else if(opCode == ReceivedOpcode && request == (byte)DfuOpCodeEnum.DfuOpCode_ReceiveFirmwareImage.ordinal()) {
            if(value[2] == OperationSuccess) {
                RigLog.i("Firmware transfer successful");
                mObserver.updateStatus("Firmware transfer successful.  Validating...", 0);
                validateFirmware();
            } else {
                //TODO: Figure out how to recover from this situation
                RigLog.e("Error during firmware image transfer " + value[2]);
                mObserver.updateStatus("Error during firmware transfer", value[2]);
            }
        } else if(opCode == ReceivedOpcode && request == (byte)DfuOpCodeEnum.DfuOpCode_ValidateFirmwareImage.ordinal()) {
            if(value[2] == OperationSuccess) {
                RigLog.i("Successful transfer and validation of firmware image!");
                if(mState == FirmwareManagerStateEnum.State_TransferringRadioImage) {
                    mObserver.updateStatus("Device software validated successfully!", 0);
                    mState = FirmwareManagerStateEnum.State_FinishedRadioImageTransfer;
                }
                activateFirmware();
            } else {
                //TODO: Figure out how to recover from this situation
                RigLog.e("An error occurred during firmware image validation " + value[2]);
                mObserver.updateStatus("Error during validation!", value[2]);
            }
        } else if(opCode == ReceivedOpcode && request == (byte)DfuOpCodeEnum.DfuOpCode_EraseSizeRequest.ordinal()) {
            if(value[2] == OperationSuccess) {
            	/* Note: This is not longer used for the dual bank bootloader.  The entire bank does not need to be
            	 * erased to perform a firmware update.  In additional, as of S110 7.0, it is no longer necessary to
            	 * disable the radio when reading/write to the internal flash of the nRF.  This code is left for reference
            	 * and backwards compatibility with old bootloaders.
            	 */
                mTotalBytesErased = (value[3] + (value[4] << 8) + (value[5] << 16) + (value[6] << 24)) & 0xFFFFFFFF;
                RigLog.i("TotalBytesErased: " + mTotalBytesErased);
                if(mTotalBytesErased < mImageSize) {
                    byte [] cmd = { (byte)DfuOpCodeEnum.DfuOpCode_EraseAndReset.ordinal() };
                    if(mState == FirmwareManagerStateEnum.State_CheckEraseAfterUnplug) {
                        mState = FirmwareManagerStateEnum.State_ReconnectAfterInitialFlashErase;
                    } else if(mState == FirmwareManagerStateEnum.State_ReconnectAfterStmUpdate) {
                        mState = FirmwareManagerStateEnum.State_ReconnectAfterStmUpdateFlashErase;
                    }
                    mFirmwareUpdateService.setShouldReconnectState(true);
                    mFirmwareUpdateService.writeDataToControlPoint(cmd);
                } else {
                    byte [] cmd = { (byte)DfuOpCodeEnum.DfuOpCode_Start.ordinal() };
                    mShouldWaitForErasedSize = false;
                    if(mState == FirmwareManagerStateEnum.State_CheckEraseAfterUnplug) {
                        mState = FirmwareManagerStateEnum.State_TransferringStmUpdateImage;
                    }
                    mFirmwareUpdateService.writeDataToControlPoint(cmd);
                }
            }
        }
    }

    /* IRigLeDiscoveryManagerObserver methods */
    @Override
    public void didDiscoverDevice(RigAvailableDeviceData device) {
        RigLog.d("__RigFirmwareUpdateManager.didDiscoverDevice__");
        RigLog.d("Found dfu device!");
        RigLeDiscoveryManager.getInstance().stopDiscoveringDevices();
        RigLeConnectionManager.getInstance().setObserver(this);
        RigLeConnectionManager.getInstance().connectDevice(device, 10000);
    }

    @Override
    public void discoveryDidTimeout() {
        RigLog.d("__RigFirmwareUpdateManager.discoveryDidTimetout__");
        RigLog.e("Did not find DFU device!!");
    }

    @Override
    public void bluetoothPowerStateChanged(boolean enabled) {
        RigLog.w("__RigFirmwareUpdateManager.bluetoothPowerStateChanged__");
    }

    @Override
    public void bluetoothDoesNotSupported() {
        RigLog.e("__RigFirmwareUpdateManager.bluetoothLeNotSupported__");
    }

    /* IRigLeConnectionManagerObserver methods */
    @Override
    public void didConnectDevice(RigLeBaseDevice device) {
        RigLog.d("__RigFirmwareUpdateManager.didConnectDevice__");
        RigLog.d("Connected!");
        mUpdateDevice = device;

        RigLeConnectionManager.getInstance().setObserver(mOldConnectionObserver);
        mFirmwareUpdateService.setShouldReconnectState(true);
        mFirmwareUpdateService.setDevice(device);
        mFirmwareUpdateService.triggerServiceDiscovery();
    }

    @Override
    public void didDisconnectDevice(BluetoothDevice btDevice) {
        if(btDevice.getAddress().equals(mInitialDeviceAddress)) {
            //Give this to the original observer
            mFirmwareUpdateService.didDisconnectInitialNonBootloaderDevice();
        }
    }

    @Override
    public void deviceConnectionDidFail(RigAvailableDeviceData device) {
        RigLog.e("RigFirmwareUpdateManager.deviceConnectionDidFail:Connection failed!");
    }

    @Override
    public void deviceConnectionDidTimeout(RigAvailableDeviceData device) {
        RigLog.e("RigFirmwareUpdateManager.deviceConnectionDidTimeout:Connection failed!");
        //Try again??
        RigLeConnectionManager.getInstance().connectDevice(device, 10000);
    }
}
