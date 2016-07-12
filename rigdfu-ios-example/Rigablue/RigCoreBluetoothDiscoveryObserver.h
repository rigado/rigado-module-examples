//
//  @file RigCoreBluetoothDiscoveryObserver.h
//  @library Rigablue
//
//  Created by Eric Stutzenberger on 4/18/14.
//  @copyright (c) 2014 Rigado, LLC. All rights reserved.
//  Source code licensed under BMD-200 Software License Agreement.
//  You should have received a copy with purchase of BMD-200 product.
//  If not, contact info@rigado.com for for a copy.

#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>

@protocol RigCoreBluetoothDiscoveryObserver <NSObject>
- (void)didDiscoverDevice:(CBPeripheral*)peripheral advertisementData:(NSDictionary*)advData rssi:(NSNumber*)rssi;
- (void)discoveryTimeout;
- (void)btPoweredOff;
- (void)btReady;
@end
