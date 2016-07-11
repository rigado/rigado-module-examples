//
//  @file RigDiscoveryManager.h
//  @library Rigablue
//
//  Created by Eric Stutzenberger on 4/17/14.
//  @copyright (c) 2014 Rigado, LLC. All rights reserved.
//  Source code licensed under BMD-200 Software License Agreement.
//  You should have received a copy with purchase of BMD-200 product.
//  If not, contact info@rigado.com for for a copy.

#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>
#import "RigAvailableDeviceData.h"

@class RigDeviceRequest;

/*!
 * @protocol RigLeDiscoveryManagerDelegate
 *
 * @discussion                  This protocol provides an interface for the <code>RigLeDiscoveryManager</code> to send event information back to
 *                              the object that requested the discovery operation.
 */
@protocol RigLeDiscoveryManagerDelegate <NSObject>
/*!
 *  @method disDiscoverDevice:
 *
 *  @param device               A <code>RigAvailableDeviceData</code> object.
 *
 *  @discussion This method is called at any time a device is dicovered assuming the device has not already been discovered during
 *                              the current discovery session.  After discovery, applications will use the device objects to initial connections to
 *                              discovered devices.
 */
- (void)didDiscoverDevice:(RigAvailableDeviceData*)device;

/*!
 *  @method discoveryDidTimeout
 *
 *  @discussion                 If a timeout value is provided when a discovery session is initiated, this method is called when that timeout has
 *                              elapsed.  Once this method is called, discovery is no longer being performed an new devices will not continue to
 *                              be discovered until a new discovery session is started.
 */
- (void)discoveryDidTimeout;

- (void)bluetoothNotPowered;

@optional
/*!
 *  @method didUpdateDeviceData:
 *
 *  @discussion                 This method is called if a new discovery event is triggered for a device which has already been discovered.  This
 *                              allows for tracking RSSI, device name changes, and general advertisement data monitoring.
 *
 *  @param device               The device data which changed.
 *  @param index                The index of the device data in the discovered devices array provided by <code>retrieveDiscoveredDevices</code>.
 *
 */
- (void)didUpdateDeviceData:(RigAvailableDeviceData*)device deviceIndex:(NSUInteger)index;

@end

/*!
 * @class RigLeDiscoveryManager
 *
 * @discussion                  This class provides an interface to discover devices.
 
 * @warning                     Discovery events are generated on a background thread and not the main UI thread. To send messages to the main queue,
 *                              the following should be performed when ANY event is received through the delegate protocol:
 *                              <code>[self performSelectorOnMainThread:@selector(yourSelectorNameHere) withObject:message waitUntilDone:YES];</code>
 *                              The above will cause the context of the selector to be executed on the main thread where you can update the UI.
 */
@interface RigLeDiscoveryManager : NSObject

/*!
 *  @property isDiscoveryRunning
 *
 *  @discussion                 This property denotes when a discovery session is in progress.  If YES, then a discovery session is running.  To start a new
 *                              session, either wait for the first session to complete or stop the current session and then begin a new session.
 */
@property (readonly) BOOL isDiscoveryRunning;

/*!
 *  @method sharedInstance
 *
 *  @discussion                 This method acquires the shared instance of the RigLeDiscoveryManager class.  As this class is a singleton, only one instance
 *                              will exist for the life of the application.
 *
 *  @return                     RigLeDiscoveryManager instance
 *
 */
+ (id)sharedInstance;

/*!
 *  @method startLeInterface
 *
 *  @discussion                 This method should be invoked prior to performing any discovery operations using the library.  It is responsible for
 *                              setting up the central manager instance and configuration of CoreBluetooth as necessary.
 *
 *  @seealso                    RigLeDiscoveryManager.h
 *
 */
- (void)startLeInterface;

/*!
 *  @method discoverDevices:
 *
 *  @param request              A RigDeviceRequest object containing information about devices that should be discovered.
 *
 *  @discussion                 This method should be invoked any time the application needs to discover available LE devices.  Note that only one
 *                              discovery session can be running at any time.  Requests may contain more than on UUID if necessary. The status of
 *                              a discovery can be checked using the <i>isDiscoveryRunning</i> property.
 *
 *
 */
- (void)discoverDevices:(RigDeviceRequest*)request;

/*!
 *  @method findConnectedDevices:
 *
 *  @param request              A RigDeviceRequest object containing information about devices for which to search.
 *
 *  @discussion                 This method should be invoked any time the application needs to discover devices that match the search parameters
 *                              that are already connected to the system.  This is useful for finding bonded devices that are automatically connected
 *                              when in range of the iOS device.  Devices are not returned from this function.  They are pushed through the
 *                              delegate method <code>didDiscoveryDevice:</code>.
 *
 *  @note                       This method CANNOT retrieve all connected devices in the manner in which any non-connected device is discovered (e.g.
 *                              supplying nil as the UUID).  A UUID MUST be provided, or this function will not discovery any devices.
 *
 *
 */
- (void)findConnectedDevices:(RigDeviceRequest*)request;

/*!
 *  @method stopDiscoveringDevices
 *
 *  @discussion                 This method stops the current discovery session.
 *
 */
- (void)stopDiscoveringDevices;

/*!
 *  @method retrieveDiscoveredDevices
 *
 *  @discussion                 This method returns all devices found during the most resent discovery session.  Subsequent discovery session will clear this list.
 *
 *  @return                     NSArray filled with <i>RigAvailableDeviceData</i> objects.
 */
- (NSArray*)retrieveDiscoveredDevices;

/*!
 *  @method clearDiscoveredDevices
 *
 *  @discussion                 This method clears all stored discovery devices from the most recent discovery session.
 */
- (void)clearDiscoveredDevices;

/*!
 *  @method removeAvailableDevice:
 *
 *  @param device               The <i>RigAvailableDeviceData</i> object to remove from the discovered devices list.
 *
 *  @discussion                 This method is used to remove a device from the available devices list after it has been successfully connected.  Once connected,
 *                              devices no longer advertise and as such should be removed from the list of devices available for connection.  Due to this behavior,
 *                              devices must be re-discovered through a discovery session.
 *
 *  @seealso                    RigAvailableDeviceData
 */
- (BOOL)removeAvailableDevice:(RigAvailableDeviceData*)device;

@end
