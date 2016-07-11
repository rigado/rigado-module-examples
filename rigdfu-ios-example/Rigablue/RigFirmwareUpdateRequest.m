//
//  RigFirmwareUpdateRequest.m
//  Rigablue
//
//  Created by Eric P. Stutzenberger on 9/17/15.
//  Copyright © 2015 Rigado, LLC. All rights reserved.
//

#import "RigFirmwareUpdateRequest.h"
#import "RigLeBaseDevice.h"

@implementation RigFirmwareUpdateRequest
+ (RigFirmwareUpdateRequest*) updateRequestWithDevice:(RigLeBaseDevice*)device image:(NSData*)image activationChar:(CBCharacteristic*)activateChar activationCommand:(NSData*)activateCommand maxRssi:(int)rssi
{
    RigFirmwareUpdateRequest *request = [[RigFirmwareUpdateRequest alloc] init];
    request.updateDevice = device;
    request.image = image;
    request.activationCharacteristic = activateChar;
    request.activationCommand = activateCommand;
    request.isPatch = NO;
    request.maxRssi = rssi;
    
    return request;
}

+ (RigFirmwareUpdateRequest*) patchRequestWithDevice:(RigLeBaseDevice*)device image:(NSData*)image activationChar:(CBCharacteristic*)activateChar activationCommand:(NSData*)activateCommand maxRssi:(int)rssi
{
    RigFirmwareUpdateRequest *request = [[RigFirmwareUpdateRequest alloc] init];
    request.updateDevice = device;
    request.image = image;
    request.activationCharacteristic = activateChar;
    request.activationCommand = activateCommand;
    request.isPatch = YES;
    request.maxRssi = rssi;
    
    return request;
}

@end
