//
//  @file DeviceRequest.h
//  @library Rigablue
//
//  Created by Eric Stutzenberger on 4/17/14.
//  @copyright (c) 2014 Rigado, LLC. All rights reserved.
//
//  Source code licensed under BMD-200 Software License Agreement.
//  You should have received a copy with purchase of BMD-200 product.
//  If not, contact info@rigado.com for for a copy.

#import <Foundation/Foundation.h>
#import "RigLeDiscoveryManager.h"

/**
 *  @class RigDeviceRequest
 *
 *  @discussion This class provides data to the <code>RigLeDiscoveryManager</code> on what UUIDs to look for during discovery.  Additionally, it provides
 *  a timeout for how long the discovery operation should run.  To run discovery indefinitely, using a timeout of 0.
 */
@interface RigDeviceRequest : NSObject

/**
 *  @name Creating A Device Request
 */
/**
 *  @method deviceRequestWithUuidList:timeout:delegate:allowDuplicates:
 *
 *  @param uuidList        List of UUIDs to search for during discovery.
 *  @param timeout         The length of time for which discovery should run; use 0 to run discovery indefinitely.
 *  @param delegate        The delegate object to be notified by the dicovery manager.
 *  @param allowDuplicates See {@link allowDuplicates} for more information.
 *
 *  @return Returns a <code>RigDeviceRequest</code> object with all approprite fields filled in.
 */
+ (id)deviceRequestWithUuidList:(NSArray*)uuidList timeout:(float)timeout delegate:(id<RigLeDiscoveryManagerDelegate>)delegate allowDuplicates:(BOOL)allowDuplicates;

/*!
 *  @name Device Request properties
 */
/**
 *  @property uuidList
 *
 *  @discussion This property contians all UUIDs to be discovered for the discovery session associated with this device request.
 */
@property (nonatomic, strong) NSArray* uuidList;

/**
 *  @property discoveryDelegate
 *
 *  @discussion The delegate object for the discovery session.
 */
@property (nonatomic, strong) id<RigLeDiscoveryManagerDelegate> delegate;

/**
 *  @property timeout
 *
 *  @discussion This property denotes how long a discovery session should run.  If 0, discovery runs indefinitely.
 */
@property float timeout;

/**
 *  @property allowDuplicates
 *
 *  @discussion If <code>YES</code>, the central manager will be instructed to provide discovery information each time an advertisment packet is seen from a device.
 *              If <code>NO</code>, the central manager will only reported discovery of a device the first time it is seen after discovery commences.
 */
@property BOOL allowDuplicates;

@end
