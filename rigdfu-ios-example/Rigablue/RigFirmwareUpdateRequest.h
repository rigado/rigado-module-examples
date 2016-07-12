//
//  RigFirmwareUpdateRequest.h
//  Rigablue
//
//  Created by Eric P. Stutzenberger on 9/17/15.
//  Copyright © 2015 Rigado, LLC. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "RigLeBaseDevice.h"

@interface RigFirmwareUpdateRequest : NSObject

+ (RigFirmwareUpdateRequest*) updateRequestWithDevice:(RigLeBaseDevice*)device image:(NSData*)image activationChar:(CBCharacteristic*)activateChar activationCommand:(NSData*)activateCommand maxRssi:(int)rssi;
+ (RigFirmwareUpdateRequest*) patchRequestWithDevice:(RigLeBaseDevice*)device image:(NSData*)image activationChar:(CBCharacteristic*)activateChar activationCommand:(NSData*)activateCommand maxRssi:(int)rssi;

@property (nonatomic, strong) RigLeBaseDevice *updateDevice;
@property (nonatomic, strong) NSData *image;
@property (nonatomic, strong) CBCharacteristic *activationCharacteristic;
@property (nonatomic, strong) NSData *activationCommand;
@property BOOL isPatch;
@property int maxRssi;

@end
