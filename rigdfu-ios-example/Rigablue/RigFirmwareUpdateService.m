//
//  RigFirmwareUpdateService.m
//  Rigablue Library
//
//  Created by Eric Stutzenberger on 11/8/13.
//  @copyright (c) 2013-2014 Rigado, LLC. All rights reserved.
//
//  Source code licensed under BMD-200 Software License Agreement.
//  You should have received a copy with purchase of BMD-200 product.
//  If not, contact info@rigado.com for for a copy.

#import "RigFirmwareUpdateService.h"
#import "RigLeDiscoveryManager.h"
#import "RigLeConnectionManager.h"
#import "RigAvailableDeviceData.h"

#define SECURE_DFU_MODEL_NUMBER @"Rigado Secure DFU"

NSString *kupdateDFUReportCharUuidString = @"00001533-1212-efde-1523-785feabcd123";

// 200 Eval Board UUIDs named EvalDemo
NSString *kupdateDFUServiceUuidString200 = @"00001530-1212-efde-1523-785feabcd123";
NSString *kupdateDFUControlPointUuidString200 = @"00001531-1212-efde-1523-785feabcd123";
NSString *kupdateDFUPacketCharUuidString200 = @"00001532-1212-efde-1523-785feabcd123";

// 300 Eval Board UUIDs named RB-Demo
NSString *kupdateDFUServiceUuidString300 = @"41c89030-1756-4c30-93cc-a8fcc2fb0202";
NSString *kupdateDFUControlPointUuidString300 = @"41c89032-1756-4c30-93cc-a8fcc2fb0202";
NSString *kupdateDFUPacketCharUuidString300 = @"41c89031-1756-4c30-93cc-a8fcc2fb0202";

NSString *kDisUuidString = @"180A";
NSString *kDisFirmwareVersionUuidString = @"2a26";
NSString *kDisModelNumberUuidString = @"2a24";

@interface RigFirmwareUpdateService() <RigLeBaseDeviceDelegate, RigLeConnectionManagerDelegate, RigLeDiscoveryManagerDelegate>

@end

@implementation RigFirmwareUpdateService
{
    CBCentralManager    *centralManager;
    RigLeBaseDevice     *updateDevice;
    RigAvailableDeviceData *availDevice;
    CBPeripheral        *updatePeripheral;
    
    /* DIS Service and Characteristic objects */
    CBUUID              *disUuid;
    CBUUID              *disFirmwareVersionUuid;
    CBUUID              *disModelNumberUuid;
    
    CBService           *disService;
    CBCharacteristic    *disFirmwareVersionCharacteristic;
    CBCharacteristic    *disModelNumberChacteristic;
    
    /* DFU Service and Characteristic objects */
    CBUUID              *updateDFUServiceUuid;
    CBUUID              *updateDFUControlPointUuid;
    CBUUID              *updateDFUPacketCharUuid;
    CBUUID              *updateDFUReportCharUuid;
    
    CBService           *updateDFUService;
    CBCharacteristic    *updateDFUControlPointCharacteristic;
    CBCharacteristic    *updateDFUPacketCharacteristic;
    CBCharacteristic    *updateDFUReportCharacteristic;
    
    NSMutableArray      *updateDFUCharArray;
    int                 reconnectAttempts;
    
    id<RigLeConnectionManagerDelegate> oldDelegate;
    
    BOOL                isSecureDfu;
    BOOL                isConnected;
}

@synthesize delegate;
@synthesize shouldReconnectToPeripheral;
@synthesize alwaysReconnectOnDisconnect;
@synthesize updateDFUServiceUuidString;



- (id)init
{
    self = [super init];
    if (self) {
        oldDelegate = [RigLeConnectionManager sharedInstance].delegate;
        [RigLeConnectionManager sharedInstance].delegate = self;
        
        disUuid = [CBUUID UUIDWithString:kDisUuidString];
        disFirmwareVersionUuid = [CBUUID UUIDWithString:kDisFirmwareVersionUuidString];
        disModelNumberUuid = [CBUUID UUIDWithString:kDisModelNumberUuidString];
        
        updateDFUCharArray = [[NSMutableArray alloc] initWithCapacity:3];
        shouldReconnectToPeripheral = NO;
        isSecureDfu = NO;
    }
    return self;
}

- (RigDfuError_t)connectPeripheral
{
    if (updateDevice == nil) {
        return DfuError_BadDevice;
    } else if(updateDevice.peripheral == nil) {
        return DfuError_BadPeripheral;
    }
    
    if (updateDevice.peripheral.state == CBPeripheralStateDisconnected) {
        [[RigLeConnectionManager sharedInstance] connectDevice:availDevice connectionTimeout:MINIMUM_CONNECTION_TIMEOUT];
    }
    
    return DfuError_None;
}

- (RigDfuError_t)reconnectPeripheral
{
    if (updateDevice == nil) {
        return DfuError_BadDevice;
    } else if(updateDevice.peripheral == nil) {
        return DfuError_BadPeripheral;
    }
    
    shouldReconnectToPeripheral = YES;
    reconnectAttempts = 0;
    [[RigLeConnectionManager sharedInstance] disconnectDevice:updateDevice];
    
    return DfuError_None;
}

- (RigDfuError_t)setDevice:(RigLeBaseDevice *)baseDevice
{
    if (baseDevice == nil) {
        return DfuError_BadDevice;
    }
    updateDFUReportCharUuid = [CBUUID UUIDWithString:kupdateDFUReportCharUuidString];
    
    updateDevice = baseDevice;
    updatePeripheral = updateDevice.peripheral;
    updateDevice.delegate = self;
    
    if (updateDevice.peripheral.state == CBPeripheralStateConnected) {
        isConnected = YES;
    }
    
    availDevice = [[RigAvailableDeviceData alloc] initWithPeripheral:baseDevice.peripheral advertisementData:NULL rssi:NULL discoverTime:NULL];
    
    if (updateDevice.isDiscoveryComplete) {
        [self assignServicesAndCharacteristics];
    }
    
    return DfuError_None;
}

- (void)assignServicesAndCharacteristics
{
    if ([updateDevice getServiceWithUuid:[CBUUID UUIDWithString:kupdateDFUServiceUuidString300]]) {
        updateDFUServiceUuid = [CBUUID UUIDWithString:kupdateDFUServiceUuidString300];
        updateDFUControlPointUuid = [CBUUID UUIDWithString:kupdateDFUControlPointUuidString300];
        updateDFUPacketCharUuid = [CBUUID UUIDWithString:kupdateDFUPacketCharUuidString300];
        updateDFUServiceUuidString = kupdateDFUServiceUuidString300;
    } else if ([updateDevice getServiceWithUuid:[CBUUID UUIDWithString:kupdateDFUServiceUuidString200]]) {
        updateDFUServiceUuid = [CBUUID UUIDWithString:kupdateDFUServiceUuidString200];
        updateDFUControlPointUuid = [CBUUID UUIDWithString:kupdateDFUControlPointUuidString200];
        updateDFUPacketCharUuid = [CBUUID UUIDWithString:kupdateDFUPacketCharUuidString200];
        updateDFUServiceUuidString = kupdateDFUServiceUuidString200;
    }
    for (CBService *service in [updateDevice getSerivceList]) {
        if ([[service UUID] isEqual:updateDFUServiceUuid]) {
            updateDFUService = service;
        } else if([service.UUID isEqual:disUuid]) {
            disService = service;
        }
    }
    
    if (updateDFUService) {
        [self addCharacteristics:updateDFUService.characteristics];
    }
    
    if (disService) {
        [self updateDisChacteristics:disService.characteristics];
    }
    
}

- (RigDfuError_t)triggerServiceDiscovery
{
    if (updateDevice == nil) {
        return DfuError_BadDevice;
    } else if(updateDevice.peripheral == nil) {
        return DfuError_BadPeripheral;
    }
    
    if (updateDevice.peripheral.state == CBPeripheralStateConnected) {
        [updateDevice runDiscovery];
    } else {
        [self connectPeripheral];
    }
    
    return DfuError_None;
}

- (RigDfuError_t)disconnectPeripheral
{
    if (updateDevice == nil) {
        return DfuError_BadDevice;
    } else if(updateDevice.peripheral == nil) {
        return DfuError_BadPeripheral;
    }
    
    [[RigLeConnectionManager sharedInstance] disconnectDevice:updateDevice];
    
    return DfuError_None;
}

- (BOOL)isSecureDfu
{
    return isSecureDfu;
}

- (void)determineSecureDfuStatus
{
    
}

- (void)addCharacteristics:(NSArray*)characteristics
{
    CBCharacteristic *characteristic;
    for (characteristic in characteristics) {
        
        if ([[characteristic UUID] isEqual:updateDFUControlPointUuid]) {
            updateDFUControlPointCharacteristic = characteristic;
            [updateDFUCharArray addObject:updateDFUControlPointCharacteristic];
        } else if ([[characteristic UUID] isEqual:updateDFUPacketCharUuid]) {
            updateDFUPacketCharacteristic = characteristic;
            [updateDFUCharArray addObject:updateDFUPacketCharacteristic];
        } else if ([[characteristic UUID] isEqual:updateDFUReportCharUuid]) {
            updateDFUReportCharacteristic = characteristic;
            [updateDFUCharArray addObject:updateDFUReportCharacteristic];
        }
    }
}

- (void)updateDisChacteristics:(NSArray*)characteristics
{
    for (CBCharacteristic *characteristic in characteristics) {
        if ([characteristic.UUID isEqual:disFirmwareVersionUuid]) {
            disFirmwareVersionCharacteristic = characteristic;
        } else if([characteristic.UUID isEqual:disModelNumberUuid]) {
            disModelNumberChacteristic = characteristic;
        }
    }
    
    /* Check for secure bootloader */
    if (disModelNumberChacteristic) {
        char * val = (char*)disModelNumberChacteristic.value.bytes;
        NSString *modelNumberString = [NSString stringWithUTF8String:val];
        if ([modelNumberString isEqualToString:SECURE_DFU_MODEL_NUMBER]) {
            NSLog(@"Secure DFU detected");
            isSecureDfu = YES;
        }
    }
}

//- (NSArray*)getCharacteristicUuids
//{
//    NSArray *characteristicUuids = [NSArray arrayWithObjects:updateDFUServiceUuid,
//                                    updateDFUPacketCharUuid,
//                                    updateDFUReportCharUuid, nil];
//
//    return characteristicUuids;
//}

- (NSArray*)getCharacteristicArray
{
    return updateDFUCharArray;
}

- (RigDfuError_t)writeDataToControlPoint:(const uint8_t*)data withLen:(uint8_t)len shouldGetResponse:(BOOL)getResponse
{
    NSData * dataToSend = nil;
    
    if (!updatePeripheral) {
        NSLog(@"Not connected to a peripheral!");
        return DfuError_BadPeripheral;
    }
    
    if(updateDFUControlPointCharacteristic == nil) {
        NSLog(@"Control Point characteristic missing!");
        return DfuError_ControlPointCharacteristicMissing;
    }
    
    dataToSend = [NSData dataWithBytes:data length:len];
    
    if (getResponse) {
        [updatePeripheral writeValue:dataToSend forCharacteristic:updateDFUControlPointCharacteristic type:CBCharacteristicWriteWithResponse];
    } else {
        [updatePeripheral writeValue:dataToSend forCharacteristic:updateDFUControlPointCharacteristic type:CBCharacteristicWriteWithoutResponse];
    }
    
    return DfuError_None;
}

- (RigDfuError_t)writeDataToPacketCharacteristic:(const uint8_t*)data withLen:(uint8_t)len shouldGetResponse:(BOOL)getResponse
{
    NSData * dataToSend = nil;
    
    if (!updatePeripheral) {
        NSLog(@"Not connected to a peripheral!");
        return DfuError_BadPeripheral;
    }
    
    if(updateDFUPacketCharacteristic == nil) {
        NSLog(@"Control Point characteristic missing!");
        return DfuError_ControlPointCharacteristicMissing;
    }
    
    dataToSend = [NSData dataWithBytes:data length:len];
    
    if (getResponse) {
        [updatePeripheral writeValue:dataToSend forCharacteristic:updateDFUPacketCharacteristic type:CBCharacteristicWriteWithResponse];
    } else {
        [updatePeripheral writeValue:dataToSend forCharacteristic:updateDFUPacketCharacteristic type:CBCharacteristicWriteWithoutResponse];
    }
    
    return DfuError_None;
}

- (RigDfuError_t)enableControlPointNotifications
{
    NSLog(@"__LeFirmwareUpdateService->enableControlPointNotifications__");
    if (!updatePeripheral) {
        NSLog(@"Not connected to a peripheral!");
        return DfuError_BadPeripheral;
    }
    
    if (!updateDFUControlPointCharacteristic) {
        NSLog(@"Control Point characteristic missing!");
        return DfuError_ControlPointCharacteristicMissing;
    }
    [updatePeripheral setNotifyValue:YES forCharacteristic:updateDFUControlPointCharacteristic];
    
    return DfuError_None;
}

- (RigDfuError_t)disableControlPointNotifications
{
    NSLog(@"__LeFirmwareUpdateService->disableControlPointNotifications__");
    if (!updatePeripheral) {
        NSLog(@"Not connected to a peripheral!");
        return DfuError_BadPeripheral;
    }
    
    if (!updateDFUControlPointCharacteristic) {
        NSLog(@"Control Point characteristic missing!");
        return DfuError_ControlPointCharacteristicMissing;
    }
    [updatePeripheral setNotifyValue:NO forCharacteristic:updateDFUControlPointCharacteristic];
    
    return DfuError_None;
}

- (void)didWriteValueForCharacteristic:(CBCharacteristic*)characteristic error:(NSError*)error
{
    if (characteristic == updateDFUControlPointCharacteristic) {
        [delegate didWriteValueForControlPoint];
    }
}

- (void)didUpdateValueForCharacteristic:(CBCharacteristic*)characteristic error:(NSError*)error
{
    if (characteristic ==updateDFUControlPointCharacteristic) {
        NSData * data = characteristic.value;
        uint8_t * value = (uint8_t*)[data bytes];
        [delegate didUpdateValueForControlPoint:value];
        //[delegate didUpdateValueForCharacteristic:characteristic error:error];
    }
}

- (void)didUpdateNotifyStateForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error
{
    if (characteristic == updateDFUControlPointCharacteristic) {
        [delegate didEnableControlPointNotifications];
    }
}

- (void)completeUpdate
{
    [RigLeConnectionManager sharedInstance].delegate = oldDelegate;
    //[self disconnectPeripheral];
}

#pragma mark
#pragma mark - RigLeDiscoveryManagerDelegate methods
- (void)didDiscoverDevice:(RigAvailableDeviceData *)device
{
    if ([device.peripheral.name isEqualToString:@"RigDfu"]) {
        availDevice = device;
        [[RigLeConnectionManager sharedInstance] connectDevice:availDevice connectionTimeout:5.0f];
    }
}

- (void)discoveryDidTimeout
{
    
}

- (void)bluetoothNotPowered
{
    
}

#pragma mark
#pragma mark - RigLeConnectionManagerDelegate methods
- (void)didConnectDevice:(RigLeBaseDevice *)device
{
    if (device.peripheral == updatePeripheral) {
        NSLog(@"Connected!  Starting Discovery...");
        updateDevice = device;
        updateDevice.delegate = self;
        isConnected = YES;
        [updateDevice runDiscovery];
    }
}

- (void)didDisconnectPeripheral:(CBPeripheral *)peripheral
{
    if (!isConnected) {
        NSLog(@"Warning: Received disconnect but was not recently connected.");
        return;
    }
    
    NSLog(@"Disconnected!");
    isConnected = NO;
    if (peripheral == updateDevice.peripheral) {
        [updateDFUCharArray removeAllObjects];
        if (shouldReconnectToPeripheral || alwaysReconnectOnDisconnect) {
            NSLog(@"Reconnecting...");
            reconnectAttempts++;
            if (reconnectAttempts == 5) {
                shouldReconnectToPeripheral = NO;
            }
            [NSThread sleepForTimeInterval:.3];
            availDevice = [[RigAvailableDeviceData alloc] initWithPeripheral:peripheral advertisementData:NULL rssi:NULL discoverTime:NULL];
            [[RigLeConnectionManager sharedInstance] connectDevice:availDevice connectionTimeout:5.0f];
        }
    }
}

- (void)deviceConnectionDidFail:(RigAvailableDeviceData *)device
{
    
}

- (void)deviceConnectionDidTimeout:(RigAvailableDeviceData *)device
{
    NSLog(@"Connection Timeout!");
    if (device.peripheral == updateDevice.peripheral) {
        [updateDFUCharArray removeAllObjects];
        if (shouldReconnectToPeripheral || alwaysReconnectOnDisconnect) {
            NSLog(@"Reconnecting...");
            reconnectAttempts++;
            if (reconnectAttempts == 5) {
                shouldReconnectToPeripheral = NO;
                alwaysReconnectOnDisconnect = NO;
            }
            [NSThread sleepForTimeInterval:.3];
            availDevice = [[RigAvailableDeviceData alloc] initWithPeripheral:device.peripheral advertisementData:NULL rssi:NULL discoverTime:NULL];
            [[RigLeConnectionManager sharedInstance] connectDevice:availDevice connectionTimeout:5.0f];
        } else {
            [delegate didFailToConnectToBootloader];
        }
    }
}

#pragma mark
#pragma mark - RigLeBaseDeviceDelegate methods
- (void)discoveryDidCompleteForDevice:(RigLeBaseDevice *)device
{
    NSLog(@"Discovery complete!");
    updateDevice = device;
    [self assignServicesAndCharacteristics];
    
    NSLog(@"Starting update!");
    [delegate didDiscoverCharacteristicsForDFUSerivce];
}

- (void)didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic forDevice:(RigLeBaseDevice *)device
{
    if (characteristic ==updateDFUControlPointCharacteristic) {
        NSData * data = characteristic.value;
        uint8_t * value = (uint8_t*)[data bytes];
        [delegate didUpdateValueForControlPoint:value];
    }
}

- (void)didUpdateNotifyStateForCharacteristic:(CBCharacteristic *)characteristic forDevice:(RigLeBaseDevice *)device
{
    if (characteristic == updateDFUControlPointCharacteristic) {
        [delegate didEnableControlPointNotifications];
    }
}

- (void)didWriteValueForCharacteristic:(CBCharacteristic *)characteristic forDevice:(RigLeBaseDevice *)device
{
    if (characteristic == updateDFUControlPointCharacteristic) {
        [delegate didWriteValueForControlPoint];
    }
}

@end
