//
//  @file RigCoreBluetoothInterface.h
//  @library Rigablue
//
//  Created by Eric Stutzenberger on 4/18/14.
//  @copyright (c) 2014 Rigado, LLC. All rights reserved.
//  Source code licensed under BMD-200 Software License Agreement.
//  You should have received a copy with purchase of BMD-200 product.
//  If not, contact info@rigado.com for for a copy.

#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>
#import "RigCoreBluetoothDiscoveryObserver.h"
#import "RigCoreBluetoothConnectionObserver.h"

@interface RigCoreBluetoothInterface : NSObject <CBCentralManagerDelegate>

@property (nonatomic, strong) id<RigCoreBluetoothDiscoveryObserver> discoveryObserver;
@property (nonatomic, strong) id<RigCoreBluetoothConnectionObserver> connectionObserver;

+ (id)sharedInstance;

- (void)startUpCentralManager;
- (BOOL)isCentralManagerReady;
- (void)startDiscovery:(NSArray*)uuidList timeout:(NSNumber*)timeout allowDuplicates:(BOOL)duplicates;
- (NSArray*)getConnectedPeripheralsWithServices:(NSArray*)serviceList;
- (void)stopDiscovery;

- (void)connectPeripheral:(CBPeripheral*)peripheral timeout:(NSNumber*)timeout;
- (void)disconnectPeripheral:(CBPeripheral*)peripheral;
@end

