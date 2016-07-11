//
//  @file RigLeBaseDevice.m
//  @library Rigablue
//
//  Created by Eric Stutzenberger on 4/28/14.
//  @copyright (c) 2014 Rigado, LLC. All rights reserved.
//
//  Source code licensed under BMD-200 Software License Agreement.
//  You should have received a copy with purchase of BMD-200 product.
//  If not, contact info@rigado.com for for a copy.

#import "RigLeBaseDevice.h"

@interface RigLeBaseDevice() <CBPeripheralDelegate>
{
    NSMutableArray * _serviceList;
    int serviceIndex;
    int characteristicIndex;
    NSDictionary * advertisementData;
}
@end

static int discoveredServicesCount = 0;

@implementation RigLeBaseDevice

- (id)initWithPeripheral:(CBPeripheral*)peripheral
{
    self = [super init];
    if (self) {
        _peripheral = peripheral;
        _peripheral.delegate = self;
        _serviceList = [[NSMutableArray alloc] initWithCapacity:5];
        _name = peripheral.name;
        advertisementData = nil;
    }
    return self;
}

//TODO: Add parameter to select whether or not to read all characteristic values during discovery
//      as this is not always necessary. For example, the OTA DFU does not need to read characteristic
//      values before it can start a firmware upgrade.  However, in the case of the Lumenplay app,
//      all values should be populated before we create the Lumenplay device object in the app.
- (void)runDiscovery
{
    discoveredServicesCount = 0;
    serviceIndex = 0;
    characteristicIndex = 0;
    _isDiscoveryComplete = NO;
    _peripheral.delegate = self;
    [_peripheral discoverServices:nil];
}

- (NSArray*)getSerivceList
{
    return _serviceList;
}

- (CBService*)getServiceWithUuid:(CBUUID*)uuid
{
    if (uuid == nil) {
        return nil;
    }
    for (CBService *service in _serviceList) {
        if ([service.UUID isEqual:uuid]) {
            return service;
        }
    }
    
    return nil;
}

- (CBCharacteristic*)getCharacteristicWithUuid:(CBUUID*)uuid forService:(CBService*)service
{
    if (uuid == nil || service == nil) {
        return nil;
    }
    
    for (CBCharacteristic *characteristic in service.characteristics) {
        if ([characteristic.UUID isEqual:uuid]) {
            return characteristic;
        }
    }
    
    return nil;
}

- (void)runDiscoveryForServices:(NSArray*)serviceList
{
    [_peripheral discoverServices:serviceList];
}

- (BOOL)enableNotificationsForCharacteristic:(CBCharacteristic *)characteristic
{
    if (characteristic == nil) {
        return NO;
    }
    
    if((characteristic.properties & CBCharacteristicPropertyNotify) == CBCharacteristicPropertyNotify) {
        [_peripheral setNotifyValue:YES forCharacteristic:characteristic];
    } else {
        return NO;
    }
    
    return YES;
}

- (void)setAdvertisementData:(NSDictionary *)advData
{
    advertisementData = [[NSDictionary alloc] initWithDictionary:advData];
    _name = [advertisementData objectForKey:CBAdvertisementDataLocalNameKey];
}

- (NSDictionary *)getAdvertisementData
{
    return advertisementData;
}

#pragma mark
#pragma mark - CBPeripheralDelegate methods
- (void)peripheral:(CBPeripheral *)peripheral didDiscoverServices:(NSError *)error
{
    NSArray *services = peripheral.services;
    for (CBService *service in services) {
        [_serviceList addObject:service];
        [_peripheral discoverCharacteristics:nil forService:service];
    }
}

- (void)peripheral:(CBPeripheral *)peripheral didDiscoverCharacteristicsForService:(CBService *)service error:(NSError *)error
{
    discoveredServicesCount++;
    if (discoveredServicesCount == peripheral.services.count) {
        /* Read values for all charactersistics... :$ */
        [self peripheral:peripheral didUpdateValueForCharacteristic:nil error:nil];
    }
}

- (void)peripheral:(CBPeripheral *)peripheral didUpdateNotificationStateForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error
{
    NSLog(@"Notification state updated");
    if (_delegate) {
        [_delegate didUpdateNotifyStateForCharacteristic:characteristic forDevice:self];
    }
}

- (void)peripheral:(CBPeripheral *)peripheral didWriteValueForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error
{
    NSLog(@"Did Write Value for Characteristic");
    if (_delegate) {
        [_delegate didWriteValueForCharacteristic:characteristic forDevice:self];
    }
}

- (void)peripheral:(CBPeripheral *)peripheral didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error
{
    if(_isDiscoveryComplete) {
        NSLog(@"Data received");
        [_delegate didUpdateValueForCharacteristic:characteristic forDevice:self];
    } else {
        while (serviceIndex < _serviceList.count) {
            CBService *service = _serviceList[serviceIndex];
            while (characteristicIndex < service.characteristics.count) {
                CBCharacteristic *c = service.characteristics[characteristicIndex];
                characteristicIndex++;
                NSLog(@"Read Characteristic: %@", c.UUID.UUIDString);
                if ((c.properties & CBCharacteristicPropertyRead)) {
                    [peripheral readValueForCharacteristic:c];
                    return;
                } else {
                    NSLog(@"Not Readable!");
                }
            }
            characteristicIndex = 0;
            serviceIndex++;
        }
        
        //At this point, all services and characteristics have been read
        //TODO: Add reading of characteristic descriptors
        _isDiscoveryComplete = YES;
        if (_delegate) {
            [_delegate discoveryDidCompleteForDevice:self];
        }
        
        
//        CBService *service = (CBService*)_serviceList[serviceIndex];
//        CBCharacteristic *nextChar;
//        characteristicIndex++;
//        if (characteristicIndex == service.characteristics.count) {
//            serviceIndex++;
//            if (serviceIndex == _serviceList.count) {
//                _isDiscoveryComplete = YES;
//                if (_delegate) {
//                    [_delegate discoveryDidCompleteForDevice:self];
//                }
//                return;
//            }
//
//            service = _serviceList[serviceIndex];
//            characteristicIndex = 0;
//            nextChar = (CBCharacteristic*)service.characteristics[characteristicIndex];
//        } else {
//            nextChar = (CBCharacteristic*)service.characteristics[characteristicIndex];
//        }
//        
//        //if ((characteristic.properties & CBCharacteristicPropertyRead) == CBCharacteristicPropertyRead) {
//            [peripheral readValueForCharacteristic:nextChar];
//        //}
    }
}
@end
