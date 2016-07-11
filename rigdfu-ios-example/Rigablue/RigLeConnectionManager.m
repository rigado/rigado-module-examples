//
//  @file RigLeConnectionManager.m
//  @library Rigablue
//
//  Created by Eric Stutzenberger on 4/18/14.
//  @copyright (c) 2014 Rigado, LLC. All rights reserved.
//  Source code licensed under BMD-200 Software License Agreement.
//  You should have received a copy with purchase of BMD-200 product.
//  If not, contact info@rigado.com for for a copy.

#import "RigLeConnectionManager.h"
#import "RigCoreBluetoothInterface.h"
#import "RigLeBaseDevice.h"
#import "RigCoreBluetoothConnectionObserver.h"
#import "RigLeDiscoveryManager.h"

static RigLeConnectionManager *instance = nil;
static RigAvailableDeviceData *connectingDevice = nil;
static NSMutableDictionary *connectionDict = nil;

@interface RigLeConnectionManager() <RigCoreBluetoothConnectionObserver>
{
    NSMutableArray *connectedDevices;
    BOOL shouldAutoReconnect;
    CBPeripheral *disconnectingPeripheral;
}
@end

@implementation RigLeConnectionManager

- (id)init
{
    self = [super init];
    if (self) {
        connectedDevices = [[NSMutableArray alloc] initWithCapacity:5];
        disconnectingPeripheral = nil;
        connectionDict = [[NSMutableDictionary alloc] initWithCapacity:5];
        shouldAutoReconnect = NO;
    }
    return self;
}

+ (instancetype)sharedInstance
{
    if (instance == nil) {
        instance = [[RigLeConnectionManager alloc] init];
    }
    return instance;
}

- (void)connectDevice:(RigAvailableDeviceData*)device connectionTimeout:(float)timeout
{
    float connTimeout = 0.0f;
    
    
    RigCoreBluetoothInterface *cbi = [RigCoreBluetoothInterface sharedInstance];
    cbi.connectionObserver = self;
    
    if (timeout != 0.0f && timeout < MINIMUM_CONNECTION_TIMEOUT) {
        timeout = MINIMUM_CONNECTION_TIMEOUT;
    } else {
        connTimeout = timeout;
        connectingDevice = device;
    }
    
    NSNumber *timeoutObj = [NSNumber numberWithFloat:connTimeout];
    if (device.advertisementData != nil) {
        [connectionDict setObject:device.advertisementData forKey:device.peripheral];
    }
    
    [[RigCoreBluetoothInterface sharedInstance] connectPeripheral:device.peripheral timeout:timeoutObj];
}

- (void)disconnectDevice:(RigLeBaseDevice*)device
{
    disconnectingPeripheral = device.peripheral;
    [[RigCoreBluetoothInterface sharedInstance] disconnectPeripheral:device.peripheral];
}

- (NSArray*)getConnectedDevices
{
    return connectedDevices;
}

#pragma mark
#pragma mark - RigCoreBluetoothConnectionObserver methods
- (void)didConnectDevice:(CBPeripheral *)peripheral
{
    RigLeBaseDevice *device = [[RigLeBaseDevice alloc] initWithPeripheral:peripheral];
    NSDictionary *advertisementData = [connectionDict objectForKey:peripheral];
    [device setAdvertisementData:advertisementData];
    [connectionDict removeObjectForKey:peripheral];
    [connectedDevices addObject:device];
    RigAvailableDeviceData *deviceToRemove = nil;
    for (RigAvailableDeviceData *availableDevice in [[RigLeDiscoveryManager sharedInstance] retrieveDiscoveredDevices]) {
        if (availableDevice.peripheral == device.peripheral) {
            deviceToRemove = availableDevice;
            break;
        }
    }
    [[RigLeDiscoveryManager sharedInstance] removeAvailableDevice:deviceToRemove];
    [_delegate didConnectDevice:device];
}

- (void)didDisconnectDevice:(CBPeripheral*)peripheral
{
    if (disconnectingPeripheral == nil) {
        //An unanticipated disconnect occurred; issue reconnection.
        if (shouldAutoReconnect) {
            NSNumber* timeout = [NSNumber numberWithFloat:5.0f];
            [[RigCoreBluetoothInterface sharedInstance] connectPeripheral:peripheral timeout:timeout];
            return;
        }
    }
    
    disconnectingPeripheral = nil;
    RigLeBaseDevice *deviceToRemove = nil;
    for (RigLeBaseDevice *device in connectedDevices) {
        if (device.peripheral == peripheral) {
            deviceToRemove = device;
            break;
        }
    }
    
    if (deviceToRemove) {
        [connectedDevices removeObject:deviceToRemove];
    }
    
    [_delegate didDisconnectPeripheral:peripheral];
}

- (void)connectionDidTimeout:(CBPeripheral *)peripheral
{
    [_delegate deviceConnectionDidTimeout:connectingDevice];
}

-(void)didFailToConnectDevice:(CBPeripheral *)peripheral
{
    [_delegate deviceConnectionDidFail:connectingDevice];
}
@end
