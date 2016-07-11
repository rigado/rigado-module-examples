//
//  @file RigDiscoveryManager.m
//  @library Rigablue
//
//  Created by Eric Stutzenberger on 4/17/14.
//  @copyright (c) 2014 Rigado, LLC. All rights reserved.
//  Source code licensed under BMD-200 Software License Agreement.
//  You should have received a copy with purchase of BMD-200 product.
//  If not, contact info@rigado.com for for a copy.

#import "RigLeDiscoveryManager.h"
#import "RigDeviceRequest.h"
#import "RigCoreBluetoothInterface.h"
#import "RigAvailableDeviceData.h"
#import "RigCoreBluetoothDiscoveryObserver.h"
#import "RigLeConnectionManager.h"

static RigLeDiscoveryManager *instance;
static id<RigLeDiscoveryManagerDelegate> delegate;

@interface RigLeDiscoveryManager() <RigCoreBluetoothDiscoveryObserver>
{
    NSMutableArray *discoveredDevices;
    NSLock *discoveredDevicesLock;
    BOOL btIsReady;
    RigDeviceRequest *delayedRequest;
}
@end

@implementation RigLeDiscoveryManager

- (id)init
{
    self = [super init];
    if (self) {
        discoveredDevices = [[NSMutableArray alloc] init];
        discoveredDevicesLock = [[NSLock alloc] init];
        _isDiscoveryRunning = NO;
        btIsReady = NO;
        delayedRequest = nil;
    }
    return self;
}

+ (id)sharedInstance
{
    if (instance == nil) {
        instance = [[RigLeDiscoveryManager alloc] init];
    }
    
    return instance;
}

- (void)startLeInterface
{
    RigCoreBluetoothInterface *rcb = [RigCoreBluetoothInterface sharedInstance];
    rcb.discoveryObserver = self;
    [[RigCoreBluetoothInterface sharedInstance] startUpCentralManager];
}


- (void)discoverDevices:(RigDeviceRequest*)request
{
    if (request == nil) {
        return;
    }
    
    if (!btIsReady) {
        //
        NSLog(@"Central manager not yet ready, delaying request until it is ready");
        delayedRequest = request;
        return;
    }
    
    [self startDiscovery:request];
}

- (void)startDiscovery:(RigDeviceRequest*)request
{
    [discoveredDevicesLock lock];
    [discoveredDevices removeAllObjects];
    [discoveredDevicesLock unlock];
    
    RigCoreBluetoothInterface *cbi = [RigCoreBluetoothInterface sharedInstance];
    cbi.discoveryObserver = self;
    delegate = request.delegate;
    NSNumber *timeoutObj = nil;
    if (request.timeout != 0) {
        timeoutObj = [NSNumber numberWithFloat:request.timeout];
    }
    _isDiscoveryRunning = YES;
    
    [cbi startDiscovery:request.uuidList timeout:timeoutObj allowDuplicates:request.allowDuplicates];

}

- (void)findConnectedDevices:(RigDeviceRequest*)request
{
    RigCoreBluetoothInterface *cbi = [RigCoreBluetoothInterface sharedInstance];
    NSArray *connectedList = [cbi getConnectedPeripheralsWithServices:request.uuidList];
    for (CBPeripheral *peripheral in connectedList) {
        NSMutableDictionary *advData = [NSMutableDictionary dictionaryWithObject:peripheral.name forKey:CBAdvertisementDataLocalNameKey];
        [self didDiscoverDevice:peripheral advertisementData:advData rssi:[NSNumber numberWithInt:-255]];
    }
}

- (void)stopDiscoveringDevices
{
    RigCoreBluetoothInterface *cbi = [RigCoreBluetoothInterface sharedInstance];
    [cbi stopDiscovery];
    _isDiscoveryRunning = NO;
}

- (NSArray*)retrieveDiscoveredDevices
{
    [discoveredDevicesLock lock];
    NSArray *deviceList = [NSArray arrayWithArray:discoveredDevices];
    [discoveredDevicesLock unlock];
    return deviceList;
}

- (void)clearDiscoveredDevices
{
    [discoveredDevicesLock lock];
    [discoveredDevices removeAllObjects];
    [discoveredDevicesLock unlock];
}

- (BOOL)removeAvailableDevice:(RigAvailableDeviceData*)device
{
    [discoveredDevicesLock lock];
    if ([discoveredDevices containsObject:device]) {
        [discoveredDevices removeObject:device];
        [discoveredDevicesLock unlock];
        return YES;
    }
    [discoveredDevicesLock unlock];
    return NO;
}

#pragma mark
#pragma mark - RigCoreBluetoothDiscoveryObserver Delegate methods
- (void)didDiscoverDevice:(CBPeripheral *)peripheral advertisementData:(NSDictionary *)advData rssi:(NSNumber *)rssi
{
    BOOL found = NO;
    if (rssi.intValue >= 0) {
        /* Sometimes an invalid RSSI is provided by the OS.  Connecting to devices reported in this state will generally result in an unknown connection error. */
        return;
    }
    
    RigAvailableDeviceData *availableDevice = nil;
    
    /* If we already have this peripheral in the list, don't show it or notify anyone we saw it again */
    [discoveredDevicesLock lock];
    for (RigAvailableDeviceData *device in discoveredDevices) {
        if (peripheral.identifier == device.peripheral.identifier) {
            [discoveredDevicesLock unlock];
            found = YES;
            device.advertisementData = advData;
            device.rssi = rssi;
            device.lastSeenTime = [NSDate date];
            if ([delegate respondsToSelector:@selector(didUpdateDeviceData:deviceIndex:)]) {
                [delegate didUpdateDeviceData:device deviceIndex:[discoveredDevices indexOfObject:device]];
            }

            availableDevice = device;
            break;
        }
    }
    
    if (!found) {
        availableDevice = [[RigAvailableDeviceData alloc] initWithPeripheral:peripheral advertisementData:advData rssi:rssi discoverTime:[NSDate date]];
        [discoveredDevices addObject:availableDevice];
        [discoveredDevicesLock unlock];
    }
    
    [delegate didDiscoverDevice:availableDevice];
}

- (void)discoveryTimeout
{
    [delegate discoveryDidTimeout];
    _isDiscoveryRunning = NO;
}

- (void)btPoweredOff
{
    btIsReady = NO;
    [delegate bluetoothNotPowered];
}

- (void)btReady
{
    btIsReady = YES;
    if (delayedRequest != nil) {
        [self startDiscovery:delayedRequest];
        delayedRequest = nil;
    }
}
@end
