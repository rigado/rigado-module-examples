//
//  @file RigLeBaseDevice.h
//  @library Rigablue 
//
//  Created by Eric Stutzenberger on 4/28/14.
//  @copyright (c) 2014 Rigado, LLC. All rights reserved.
//
//  Source code licensed under BMD-200 Software License Agreement.
//  You should have received a copy with purchase of BMD-200 product.
//  If not, contact info@rigado.com for for a copy.

#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>

@class RigLeBaseDevice;

/**
 *  @protocol RigLeBaseDeviceDelegate
 *
 *  @discussion This protocol provides a delegate interface for notifying when certain device events occur.
 */
@protocol RigLeBaseDeviceDelegate <NSObject>

/**
 *  @method discoveryDidCompleteForDevice:
 *
 *  This method is called when the discovery of all services and characteristics
 *  has completed.  After this discovery is complete, all characteristic property values
 *  will have been read.
 *
 *  @param device The <code>RigLeBaseDevice</code> object for which discovery has finished.
 *
 *  @discussion This method is called when all services and characteristics have been discovered for a given device.
 */
- (void)discoveryDidCompleteForDevice:(RigLeBaseDevice*)device;

/**
 *  @method didUpdateValueForCharacteristic:
 *
 *  This is called when a read request is performed or when
 *  a characteristic changes its value due to a notification from the peripheral.
 *
 *  @param characteristic   The characteristic for which the value was read.
 *  @param device           The device to which this characteristic belongs.
 */
- (void)didUpdateValueForCharacteristic:(CBCharacteristic*)characteristic forDevice:(RigLeBaseDevice*)device;

/**
 *  @method didUpdateNotifyStateForCharacteristic:
 *
 *  This is called when a notification enable request has
 *  been request for a particular characteristic with the notify property set.
 *
 *  @param characteristic   The characteristic for which notifications were enabled.
 *  @param device           The device to which this characteristic belongs.
 */
- (void)didUpdateNotifyStateForCharacteristic:(CBCharacteristic*)characteristic forDevice:(RigLeBaseDevice*)device;

/**
 *  @method didWriteValueForCharacteristic:
 *
 *  This method is called ONLY when a characteristic is writen with the property
 *  WriteWithResponse.  Additionally, if the characteristic does not support
 *  WriteWithResponse (CBCharacteristicPropertyWrite), this callback will not be issued.
 *
 *  @param characteristic   The characteristic that was written.
 *  @param device           The device to which this characteristic belongs.
 */
- (void)didWriteValueForCharacteristic:(CBCharacteristic*)characteristic forDevice:(RigLeBaseDevice*)device;
@end

/**
 *  @class RigLeBaseDevice
 *
 *  @discussion This class provides a base class for all connected peripherals.  It stores all services and characteristics for a connected <code>CBPeripheral</code>
 *              object.  The idea is for specific device classes to extend this class to implement required device functionality.
 */
@interface RigLeBaseDevice : NSObject

/** @name Creating Base Device Objects */
/**
 *  Initialize a base device object with the provide peripheral.  Use this rather than the base init method.
 *
 *  @param peripheral       The peripheral to initiailize this object against.
 *
 *  @return Returns new base device oject.
 */
- (id)initWithPeripheral:(CBPeripheral*)peripheral;

/**
 *  @name Discovering And Retrieving Devices
 */
/**
 *  @method runDiscovery
 *
 *  @discussion This method starts the process of discovering available device services.  Once complete, all characteristics will be discovered for each service.
 *              Once all services and characteristics have been discovered, the delegate will be notified with the <code>discoveryDidCompleteForDevice:</code> method.
 */
- (void)runDiscovery;

/**
 *  @method getServiceList
 *
 *  @discussion This method provides access to the <code>CBService</code> objects discovered during device discovery.  Characteristics can be accessed through these
 *              service objects.
 *
 *  @return List of discovered services.
 */
- (NSArray*)getSerivceList;
- (CBService*)getServiceWithUuid:(CBUUID*)uuid;
- (CBCharacteristic*)getCharacteristicWithUuid:(CBUUID*)uuid forService:(CBService*)service;

/**
 *  @method enableNotificationsForCharacteristic:
 *
 *  @param characteristic   The characteristic on which to enable notifications.
 *
 *  @return Returns YES if the request was successful.  If NO is returned, then it is likely that the characteristic does not have the notify property enabled.
 *          However, even if this function returns YES, notifications are not guaranteed to be enabled.  CoreBluetooth must issue the appropriate callback.
 *          Rigablue will forward this callback to the delete with the <code>didUpdateNotifyStateForCharacteristic</code> message.
 */
- (BOOL)enableNotificationsForCharacteristic:(CBCharacteristic*)characteristic;

/**
 *  @method setAdvertisementData
 *
 *  This method sets the advertisement data record for the device.  The storage of the advertisement data allows it to be accessed
 *  once the device has been connected.
 *
 *  @param advData          The advertisement data to associate with this base device object.
 */
- (void)setAdvertisementData:(NSDictionary*)advData;

/**
 *  @method getAdvertisementData
 *
 *  This method simply retrieves the advertisement data as set with <code>setAdvertisementData</code>.
 *
 *  @returns nil if advertisement data not set; else the advertisement data array
 */
- (NSDictionary*)getAdvertisementData;

/**
 *  @name Base Device properties
 */
/**
 *  @property peripheral
 *
 *  @discussion The <code>CBPeripheral</code> object for this device.
 *
 */
@property (nonatomic, strong) CBPeripheral* peripheral;

/**
 *  @property name
 *
 *  @discussion The name field from the peripheral object represented by this device object.  This name will be based on the
 *              advertisement data since it updates more quickly than the name.  However, if the advertisement data is not
 *              supplied, then the name will be as provide by CoreBluetooth in the CBPeripheral object use to initialize this object.
 */
@property (nonatomic, strong) NSString *name;

/**
 *  @property isDiscoveryComplete
 *
 *  @discussion This boolean value denotes whether or not discovery of services and characteristics for this devices has been completed.
 */
@property (readonly) BOOL isDiscoveryComplete;

/**
 *  @property delegate
 *
 *  @discussion The delegate for this object.
 */
@property (nonatomic, weak) id<RigLeBaseDeviceDelegate> delegate;

@end
