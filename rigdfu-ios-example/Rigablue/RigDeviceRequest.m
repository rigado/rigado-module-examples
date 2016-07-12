//
//  @file DeviceRequest.m
//  @library Rigablue
//
//  Created by Eric Stutzenberger on 4/17/14.
//  @copyright (c) 2014 Rigado, LLC. All rights reserved.
//
//  Source code licensed under BMD-200 Software License Agreement.
//  You should have received a copy with purchase of BMD-200 product.
//  If not, contact info@rigado.com for for a copy.

#import "RigDeviceRequest.h"

@implementation RigDeviceRequest

+ (id)deviceRequestWithUuidList:(NSArray*)uuidList timeout:(float)timeout delegate:(id<RigLeDiscoveryManagerDelegate>)delegate allowDuplicates:(BOOL)allowDuplicates
{
    RigDeviceRequest *request = [[RigDeviceRequest alloc] init];
    
    request.uuidList = uuidList;
    request.timeout = timeout;
    request.delegate = delegate;
    request.allowDuplicates = allowDuplicates;
    
    return request;
}
@end
