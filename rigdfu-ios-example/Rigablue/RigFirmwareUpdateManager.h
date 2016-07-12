//
//  RigFirmwareUpdateManager.h
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
#import "RigFirmwareUpdateRequest.h"

@protocol RigFirmwareUpdateManagerDelegate <NSObject>
/**
 *  NOTE: The methods in this delegate are NOT called on the App's main thread.  The programmer must invoke
 *  any view updates on the application's main thread.  See RigTestDeviceViewController.h for an
 *  example.
 */
/**
 *  This method is called during a firmware update to report total progress to the applition.
 *
 *  @param progress The update progress between 0 and 1
 */
- (void)updateProgress:(float)progress;

/**
 *  Reports a status of the firmware update to the App.  This is a string that can be displayed to the user
 *  along with an error code if applicable.  Assuming all is well, error should be DfuError_None.
 *
 *  @param status The status
 *  @param error  The error code
 */
- (void)updateStatus:(NSString*)status errorCode:(RigDfuError_t)error;

/**
 *  This is called when a firmware update is complete and the new firmware is activated on the device.
 */
- (void)didFinishUpdate;

@optional
/**
 *  This method is called if an update fails for any given reason.
 *
 */
- (void)updateFailed:(NSString*)status errorCode:(RigDfuError_t)error;

@end

@interface RigFirmwareUpdateManager : NSObject

/**
 *  This delegate does not have to be set but if it is, then progress updates will be reported to the App.
 */
@property (weak, nonatomic) id<RigFirmwareUpdateManagerDelegate> delegate;

/**
 *  Initializes a new instance of RigFirmwareUpdateManager.
 *
 *  @return A firmware update manager instance
 */
- (id)init;

/**
 *  Begins a firmware update for the given device.  If a device is not currently running the bootloader, a command to enter
 *  the bootloader must be provided.  If it is already in the bootloader, then the command may be nil and the command len 0.
 *  Additionally, the calling code must provide a characteristic on the device to which the command will be sent.  Again, if
 *  already in the bootloader, this characteristic parameter may be nil.
 *
 *  @param device            The device for which to perform the firmware update
 *  @param isPatch			 Set to true if the image is a patch image, otherwise set false
 *  @param firmwareImage     The firmware image to send to the device
 *  @param firmwareImageSize The size of the firmware image
 *  @param characteristic    The command characteristic
 *  @param command           The command to send to the device to activate the bootloader
 *  @param commandLen        The length of the bootloader activation command
 *
 *  @return YES if successful, NO otherwise
 */
- (RigDfuError_t)updateFirmware:(RigLeBaseDevice*)device isPatch:(BOOL)isPatch image:(NSData*)firmwareImage imageSize:(uint32_t)firmwareImageSize activateChar:(CBCharacteristic*)characteristic
       activateCommand:(uint8_t*)command activateCommandLen:(uint8_t)commandLen;

- (RigDfuError_t)performUpdate:(RigFirmwareUpdateRequest*)request;
@end
