//
//  @file RigLeConnectionManager.h
//  @library Rigablue
//
//  Created by Eric Stutzenberger on 4/18/14.
//  @copyright (c) 2014 Rigado, LLC. All rights reserved.
//  Source code licensed under BMD-200 Software License Agreement.
//  You should have received a copy with purchase of BMD-200 product.
//  If not, contact info@rigado.com for for a copy.

#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>
#import "RigAvailableDeviceData.h"

#define MINIMUM_CONNECTION_TIMEOUT  5.0f

@class RigLeBaseDevice;

/**
 *  This protocol provides events to the object that requested a connection or disconntion.
 */
@protocol RigLeConnectionManagerDelegate <NSObject>

/**
 *  This method is called when a connection is successfully made to a CBPeripheral.  At this point however, no discovery
 *  of services or characteristics has been performed.  This is acheieved by calling <code>runDiscovery</code> on the
 *  object.
 *
 *  @param device The <code>RigLeBaseDevice</code> object created after a successful connection.
 *
 *  @seealso RigLeBaseDevice
 */
- (void)didConnectDevice:(RigLeBaseDevice*)device;

/**
 *  This method is called when a peripheral has been disconnected.
 *
 *  @param peripheral The peripheral that was disconnected.
 */
- (void)didDisconnectPeripheral:(CBPeripheral*)peripheral;

/**
 *  This method is called when a failure occurs and CoreBluetooth was unable to successfully connect to the device.  Note that
 *  this method will only be called if the central manager reports the connection failed.
 *
 *  @param device The device data for which the connection failed.
 *
 */
- (void)deviceConnectionDidFail:(RigAvailableDeviceData*)device;

/**
 *  This method is called when the timeout value passed to <code>connectDevice:connectionTimeout:</code> elapses prior to a
 *  successful connection being made.
 *
 *  @param device The device data for which the connection timed out.
 */
- (void)deviceConnectionDidTimeout:(RigAvailableDeviceData*)device;
@end

/**
 *  This class provide control of connections to and disconnections from discovery CBPeripheral devices.
 *
 *  Connection to a device is performed using <code>RigAvailableDeviceData</code> objects.  Disconnections
 *  are performed using <code>RigLeBaseDevice</code> objects.  Additionally, a delegate is provided to alert
 *  calling objects when certain events have been performed (e.g. connection/disconnection, connection
 *  timeout, etc).
 */
@interface RigLeConnectionManager : NSObject


/**
 *
 *  This method provides access to the singleton instance of the <code>RigLeConnectionManager</code>.
 *
 *  @return Returns the singleton instance of the <code>RigLeConnectionManager</code>.
 */
+ (instancetype)sharedInstance;

/** @name Connect and Disconnect Devices */
/**
 *  This method connects to a device discovered during a discovery session.  The delegate will be notified of events related to the 
 *  connection through the <code>RigLeConnectionManagerDelegate</code> protocol.
 *
 *  @param device  The device data for which a connection should be made.
 *  @param timeout The amount of time for which the connection should be attempted.  Pass 0 for infinite connection time.
 *
 */
- (void)connectDevice:(RigAvailableDeviceData*)device connectionTimeout:(float)timeout;

/**
 *  This method disconnects a device. The delegate will be notified of events related to the connection through 
 *  the RigLeConnectionManagerDelegate protocol.
 *
 *  @param device The device to be disconnected.
 */
- (void)disconnectDevice:(RigLeBaseDevice*)device;

/** @name Retrieved Connected Devices */

/**
 *  This method retrieves a list of devices currently connected through CoreBluetooth.
 *
 *  @return A list of devices connected through CoreBluetooth
 */
- (NSArray*)getConnectedDevices;

/** @name Delegate Property */

/*!
 *  The delegate object for the connection manager.
 *
 *  @sa RigLeConnectionManagerDelegate
 */
@property (nonatomic, weak) id<RigLeConnectionManagerDelegate> delegate;

@end
