//
//  @file RigAvailableDeviceData.h
//  @library Rigablue
//
//  Created by Eric Stutzenberger on 4/18/14.
//  @copyright (c) 2014 Rigado, LLC. All rights reserved.
//
//  Source code licensed under BMD-200 Software License Agreement.
//  You should have received a copy with purchase of BMD-200 product.
//  If not, contact info@rigado.com for for a copy.

#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>

/*!
 *  This is a storage class that contains data pertaining to devices discovery through the CoreBluetooth discovery operation.
 *
 *  @sa RigLeDiscoveryManager
 */
@interface RigAvailableDeviceData : NSObject

/*!
 *  This method is used to initialize available devices using the input paramemters.
 *
 *  @param peripheral           The peripheral object for this device data.
 *  @param advData              The advertisement data as provided by CoreBluetooth.
 *  @param rssi                 The RSSI as indicated upon discovery.
 *  @param time                 The time at which the discovery of this device was reported by CoreBluetooth.
 *
 *  @return                     Returns an instance of <i>RigAvailableDeviceData</i>.
 */
- (id)initWithPeripheral:(CBPeripheral*)peripheral advertisementData:(NSDictionary*)advData rssi:(NSNumber*)rssi discoverTime:(NSDate*)time;

- (BOOL)containsUuid:(CBUUID*)uuid;
/*!
 *  @name Device Data Properties
 */
/*!
 *  This property contains the advertisement data provided by CoreBluetooth upon discovery.
 */
@property (nonatomic, strong) NSDictionary *advertisementData;

/*!
 *  This property contains the CBPeripheral object provided by CoreBluetooth upon discovery.
 */
@property (nonatomic, strong) CBPeripheral *peripheral;

/*!
 *  This property contains the RSSI as provided at time of discovery.  It does not update unless the device is re-discovered through a discovery session.
 */
@property (nonatomic, strong) NSNumber *rssi;

/*!
 *  This property provides a time stamp for when a device was discovered.  The intended use is to allow the application to remove devices that are really old.
 */
@property (nonatomic, strong) NSDate *discoverTime;

/*!
 *  This property provides a time stamp for when the device was last seen in a discovery event.
 */
@property (nonatomic, strong) NSDate *lastSeenTime;

@end
