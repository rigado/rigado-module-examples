//
//  RigFirmwareUpdateService.h
//  Rigablue Library
//
//  Created by Eric Stutzenberger on 11/8/13.
//  @copyright (c) 2013-2014 Rigado, LLC. All rights reserved.
//
//  Source code licensed under BMD-200 Software License Agreement.
//  You should have received a copy with purchase of BMD-200 product.
//  If not, contact info@rigado.com for for a copy.

#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>
#import "RigLeBaseDevice.h"
#import "RigDfuError.h"

extern NSString *kupdateDFUServiceUuidString200;
extern NSString *kupdateDFUServiceUuidString300;

@protocol RigFirmwareUpdateServiceDelegate <NSObject>

/**
 *  This method is called when notifications have been enabled for the DFU Control Point characteristic.
 */
- (void)didEnableControlPointNotifications;

/**
 *  This method is called when a notification is received from the DFU on its Control Point characteristic.
 *
 *  @param value The value array sent by the DFU.  Typically, the value contains an operation code, and
 *               data depending on the operation code sent to the device.  See RigFirmwareUpdateManager
 *               for the DFU operation codes.
 */
- (void)didUpdateValueForControlPoint:(uint8_t*)value;

/**
 *  This method is called when CoreBluetooth alerts the application that a value was properly written to
 *  the control point characteristic.  This is only issued if a characteristic is written to with a
 *  type of WriteWithResponse.
 */
- (void)didWriteValueForControlPoint;

/**
 *  This method is called upon establishing a connection with a device.
 */
- (void)didConnectPeripheral;

/**
 *  This method is called after discovery has been performed on the DFU service.
 */
- (void)didDiscoverCharacteristicsForDFUSerivce;

/**
 *  This method is called when a connection to RigDFU could not be obtained.
 */
- (void)didFailToConnectToBootloader;
@end

@interface RigFirmwareUpdateService : NSObject

/**
 *  The delegate for the FirmwareUpdateService should be set prior to starting firmware updates.  See
 *  RigFirmwareUpdateManager.
 */
@property (weak, nonatomic) id<RigFirmwareUpdateServiceDelegate> delegate;

/**
 *  If this is set to YES, then if a disconnect occurs, a connection will automatically be reattempted for
 *  up to 10 tries.  This property allows for indiviual reconnection control.
 */
@property (nonatomic) BOOL shouldReconnectToPeripheral;

/**
 *  If this is set to YES, then anytime a disconnect occurs for the update device, a reconnect will be issued.
 */
@property (nonatomic) BOOL alwaysReconnectOnDisconnect;

/**
 *  This is the Firmware Update Service UUID for the connected device.
 */
@property (strong, nonatomic)NSString *updateDFUServiceUuidString;


/**
 *  Initializes the update service object.
 *
 *  @return A new instance of an update service.
 */
- (id)init;

/**
 *  Sets the device to be used for the firmware update.
 *
 *  @param baseDevice The device to use
 *
 *  @return DfuError_None on success, an error otherwise
 */
- (RigDfuError_t)setDevice:(RigLeBaseDevice*)baseDevice;

/* The below are general delegate methods for CoreBlueooth */
- (void)didWriteValueForCharacteristic:(CBCharacteristic*)characteristic error:(NSError*)error;
- (void)didUpdateValueForCharacteristic:(CBCharacteristic*)characteristic error:(NSError*)error;
- (void)didUpdateNotifyStateForCharacteristic:(CBCharacteristic*)characteristic error:(NSError*)error;

/**
 *  This method finishes a firmware update by cleaning up and disconnecting from the device.  It MUST be called
 *  after the firmware image is successfully transferred and activated.
 */
- (void)completeUpdate;

/**
 *  Connects to the update device.
 *
 *  @return DfuError_None if successful, an error otherwise.
 */
- (RigDfuError_t)connectPeripheral;

/**
 *  Starts a service discovery on the update device. This is useful if the device is already in the DFU and does not
 *  first need to be reset to activate the bootloader.
 *
 *  @return DfuError_None if successful, an error otherwise.
 */
- (RigDfuError_t)triggerServiceDiscovery;

/**
 *  Disconnects from the update device.
 *
 *  @return DfuError_None if successful, an error otherwise.
 */
- (RigDfuError_t)disconnectPeripheral;

/**
 *  Reports if the connected firmware update service is a secure dfu service.
 *
 *  @return YES if secure dfu; NO otherwise
 */
- (BOOL)isSecureDfu;

/**
 *  This is called if the bootloader device has already been connected with a complete discovery already performed.
 *  In this case, the DIS data needs to be examined to see if the connection is to a secure DFU.
 *
 *  @return nothing
 */
- (void)determineSecureDfuStatus;

/**
 *  Writes data to the DFU Control Point characteristic.
 *
 *  @param data        The data to write
 *  @param len         Data length
 *  @param getResponse If YES, write data with CBCharacteristicWriteWithResponse,
 *                     if NO, write data with CBCharacteristicWriteWithoutResponse
 *
 *  @return DfuError_None if successful, an error otherwise
 */
- (RigDfuError_t)writeDataToControlPoint:(const uint8_t*)data withLen:(uint8_t)len shouldGetResponse:(BOOL)getResponse;

/**
 *  Writes data to the DFU Packet characteristic.
 *
 *  @param data        The data to write
 *  @param len         Data Length
 *  @param getResponse If YES, write data with CBCharacteristicWriteWithResponse,
 *                     if NO, write data with CBCharacteristicWriteWithoutResponse
 *
 *  @return DfuError_None if successful, an error otherwise
 */
- (RigDfuError_t)writeDataToPacketCharacteristic:(const uint8_t*)data withLen:(uint8_t)len shouldGetResponse:(BOOL)getResponse;

/**
 *  Enables notifications for the DFU Control Point characteristic.
 *
 *  @return DfuError_None if successful, an error otherwise
 */
- (RigDfuError_t)enableControlPointNotifications;

/**
 *  Disables notifications for the DFU Control Point characteristic.
 *
 *  @return DfuError_None if successful, an error otherwise
 */
- (RigDfuError_t)disableControlPointNotifications;

@end
