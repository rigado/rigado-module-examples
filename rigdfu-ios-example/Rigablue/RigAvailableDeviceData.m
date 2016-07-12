//
//  @file RigAvailableDeviceData.m
//  @library Rigablue
//
//  Created by Eric Stutzenberger on 4/18/14.
//  @copyright (c) 2014 Rigado, LLC. All rights reserved.
//
//  Source code licensed under BMD-200 Software License Agreement.
//  You should have received a copy with purchase of BMD-200 product.
//  If not, contact info@rigado.com for for a copy.

#import "RigAvailableDeviceData.h"

@implementation RigAvailableDeviceData

- (id)initWithPeripheral:(CBPeripheral*)peripheral advertisementData:(NSDictionary*)advData rssi:(NSNumber*)rssi discoverTime:(NSDate *)time
{
    self = [super init];
    if (self) {
        _peripheral = peripheral;
        _advertisementData = advData;
        _rssi = rssi;
        _discoverTime = time;
        _lastSeenTime = time;
    }
    return self;
}

- (BOOL)containsUuid:(CBUUID *)uuid
{
    NSArray *uuidList = [_advertisementData objectForKeyedSubscript:CBAdvertisementDataServiceUUIDsKey];
    for (CBUUID *u in uuidList) {
        if ([u isEqual:uuid]) {
            return YES;
        }
    }
    return NO;
}
@end
