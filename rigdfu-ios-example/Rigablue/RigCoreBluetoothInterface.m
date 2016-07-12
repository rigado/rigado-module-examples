//
//  @file RigCoreBluetoothInterface.m
//  @library Rigablue
//
//  Created by Eric Stutzenberger on 4/18/14.
//  @copyright (c) 2014 Rigado, LLC. All rights reserved.
//  Source code licensed under BMD-200 Software License Agreement.
//  You should have received a copy with purchase of BMD-200 product.
//  If not, contact info@rigado.com for for a copy.

#import "RigCoreBluetoothInterface.h"

static CBCentralManager *manager = nil;
static RigCoreBluetoothInterface *instance = nil;
static BOOL isCentralManagerReady = NO;
static NSTimer *discoveryTimeoutTimer = nil;
static NSTimer *connectionTimeoutTimer = nil;

#define kBgQueue dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0)

@implementation RigCoreBluetoothInterface

+ (id)sharedInstance
{
    if (instance == nil) {
        instance = [[RigCoreBluetoothInterface alloc] init];
    }
    
    return instance;
}

- (id)init
{
    self = [super init];
    if (self) {
        [self initCentralManagerWithDelegate:self];
    }
    return self;
}

- (void)startUpCentralManager
{
    
}

- (BOOL)isCentralManagerReady
{
    return isCentralManagerReady;
}

- (NSArray*)getConnectedPeripheralsWithServices:(NSArray*)serviceList
{
    return [manager retrieveConnectedPeripheralsWithServices:serviceList];
}

- (void)initCentralManagerWithDelegate:(id<CBCentralManagerDelegate>)centralDelegate
{
    if (manager == nil) {
        manager = [[CBCentralManager alloc] initWithDelegate:centralDelegate queue:kBgQueue];
    }
    
    if (!manager) {
        return; //TODO: Throw error to someone...
    }
}

#pragma mark
#pragma mark - Discovery related methods
- (void)startDiscovery:(NSArray *)uuidList timeout:(NSNumber *)timeout allowDuplicates:(BOOL)duplicates
{
    NSDictionary *options;
    
    NSLog(@"CBI Start Discovery");
    if (!isCentralManagerReady) {
        NSLog(@"startDiscovery was called but central manager was not in the ready state!");
        //TODO: Wait until message is received that manager is ready and then start discovery
        //TODO: Discovery manager should really handle this and add a callback to discovery observer
        return; //TODO: Throw error to someone...
    }
    
    options	= [NSDictionary dictionaryWithObject:[NSNumber numberWithBool:duplicates] forKey:CBCentralManagerScanOptionAllowDuplicatesKey];
    [manager scanForPeripheralsWithServices:uuidList options:options];
    
    /* Schedule a timer to stop scanning after timeout passes */
    if (timeout != nil) {
        discoveryTimeoutTimer = [NSTimer scheduledTimerWithTimeInterval:timeout.floatValue target:self selector:@selector(discoveryTimeoutTimerDidFire:) userInfo:nil repeats:NO];
    }
}

- (void)stopDiscovery
{
    if (discoveryTimeoutTimer != nil) {
        [discoveryTimeoutTimer invalidate];
        discoveryTimeoutTimer = nil;
    }
    NSLog(@"CBI Stop Discovery: Stop scan");
    [manager stopScan];
}

- (void)discoveryTimeoutTimerDidFire:(NSTimer*)timer
{
    //TODO: Add a notification to a delegate to notify of timeout
    if (discoveryTimeoutTimer) {
        NSLog(@"CBI Discovery Timeout: Stop scan");
        [manager stopScan];
        [_discoveryObserver discoveryTimeout];
    }
}

#pragma mark
#pragma mark - Connection related methods
- (void)connectPeripheral:(CBPeripheral *)peripheral timeout:(NSNumber*)timeout
{
    void (^scheduleTimeout)(void) = ^void(void) {
        [manager connectPeripheral:peripheral options:nil];
        if (timeout) {
            connectionTimeoutTimer = [NSTimer scheduledTimerWithTimeInterval:timeout.doubleValue target:self selector:@selector(connectionTimeoutTimerDidFire:) userInfo:peripheral repeats:NO];
        }
    };
    if (![NSThread isMainThread]) {
        dispatch_sync(dispatch_get_main_queue(), scheduleTimeout);
    } else {
        scheduleTimeout();
    }
}

- (void)disconnectPeripheral:(CBPeripheral *)peripheral
{
    [manager cancelPeripheralConnection:peripheral];
}

- (void) connectionTimeoutTimerDidFire:(NSTimer*)timer;
{
    CBPeripheral *peripheral = (CBPeripheral*)timer.userInfo;
    [manager cancelPeripheralConnection:peripheral];
    if (_connectionObserver) {
        [_connectionObserver connectionDidTimeout:peripheral];
    }
}

#pragma mark -
#pragma mark CBCentralManagerDelegate methods
- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary *)advertisementData RSSI:(NSNumber *)RSSI
{
    if (_discoveryObserver != nil) {
        [_discoveryObserver didDiscoverDevice:peripheral advertisementData:advertisementData rssi:RSSI];
    }
}

- (void)centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)peripheral
{
    void (^disable_timer)(void) = ^void(void) {
        [connectionTimeoutTimer invalidate];
    };
    if (![NSThread isMainThread]) {
        dispatch_sync(dispatch_get_main_queue(), disable_timer);
    } else {
        disable_timer();
    }
    if (_connectionObserver != nil) {
        [_connectionObserver didConnectDevice:peripheral];
    }
}

- (void)centralManager:(CBCentralManager *)central didDisconnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error
{
    //TODO: Figure out a way to track this such that we know if it was deliberiate or not
    if (_connectionObserver != nil) {
        [_connectionObserver didDisconnectDevice:peripheral];
    }
}

- (void)centralManager:(CBCentralManager *)central didFailToConnectPeripheral:(CBPeripheral *)peripheral error:(NSError *)error
{
    if (_connectionObserver != nil) {
        [_connectionObserver didFailToConnectDevice:peripheral];
    }
}

- (void)centralManagerDidUpdateState:(CBCentralManager *)central
{
    switch (central.state) {
        case CBCentralManagerStatePoweredOff:
            isCentralManagerReady = NO;
            if (_discoveryObserver != nil) {
                [_discoveryObserver btPoweredOff];
            }
            break;
        case CBCentralManagerStatePoweredOn:
            isCentralManagerReady = YES;
            if (_discoveryObserver != nil) {
                [_discoveryObserver btReady];
            }
            break;
        case CBCentralManagerStateResetting:
            isCentralManagerReady = NO;
            break;
        case CBCentralManagerStateUnauthorized:
            isCentralManagerReady = NO;
            break;
        case CBCentralManagerStateUnknown:
            isCentralManagerReady = NO;
            break;
        case CBCentralManagerStateUnsupported:
            isCentralManagerReady = NO;
            break;
        default:
            break;
    }
}
@end
